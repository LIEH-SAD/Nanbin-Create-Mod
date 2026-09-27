package com.Nanbin.client.Render;

import com.Nanbin.InitClient;
import com.Nanbin.Registry.RegBlock.BlockRoadName;
import com.Nanbin.client.Drawing.CustomFontTextureCache;
import com.Nanbin.client.Drawing.CustomFontTextureCache.FittedTextTexture;
import com.Nanbin.client.Drawing.CustomFontTextureCache.FontType;
import com.Nanbin.mapping.DirectionHelper;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.mtr.block.IBlock;
import org.mtr.render.BlockEntityRendererExtension;
import org.mtr.render.MainRenderer;
import org.mtr.render.QueuedRenderLayer;
import org.mtr.render.RenderRouteBase;
import org.mtr.render.StoredMatrixTransformations;
import org.mtr.tool.Drawing;

import java.awt.Color;

/**
 * 路名方块渲染器：在方块模型（3 块宽 × 1 块高 × 2px 厚的标牌）的正反两面渲染四个文本显示框，
 * 与配置屏幕中的预览布局一致。文本使用 {@link CustomFontTextureCache} 的 SOURCE_HAN 字体，
 * 超出显示框时自动等比缩小。
 * <p>
 * 4.1 适配：{@code BlockEntityRenderer}（4.0 映射层）→ {@link BlockEntityRendererExtension}，
 * render 的签名字段全变（{@code MatrixStack} / {@code VertexConsumerProvider} /
 * {@code ClientWorld} / {@code ClientPlayerEntity}），{@code GraphicsHolder} 那套
 * rotate/translate/pop 直接改成 {@code MatrixStack} + {@link Drawing}；
 * {@code rendersOutsideBoundingBox2} → 原版 {@code shouldRenderOffScreen}。
 */
public class RenderRoadNameBlock extends BlockEntityRendererExtension<BlockRoadName.BlockEntity> {

	private static final FontType FONT_TYPE = FontType.SOURCE_HAN;

	private static final int COLOR_TEXT_MAIN = 0xFFFFFFFF;
	private static final int COLOR_TEXT_CORNER = 0xFF0059C6;

	/** MTR 的 Render 加载早于 BlockEntity，且字体纹理有缓存，需定时强制刷新 */
	private static final long REFRESH_INTERVAL_MS = 1000L;

	private long lastRefreshTime = 0;

	/** 每块（block）对应的贴图像素，用于把生成的字体贴图像素换算为世界单位 */
	private static final float PIXELS_PER_BLOCK = 120.0F;

	/** 标牌半厚：碰撞箱 z 为 7..9，中心在 8/16=0.5，面到中心距离 1/16 */
	private static final float SIGN_HALF_THICKNESS = 0.064F;

	/** 绘制面 Z 偏移（正面）。文字必须画在模型表面之外，否则被不透明模型遮挡。 */
	private static final float Z_FACE = -(SIGN_HALF_THICKNESS + 0.003125F);

	/** 整个路名牌宽度 / 高度（块）：LEFT 半块 + MIDDLE 整块 + RIGHT 半块拼成 2 块宽、1 块高 */
	private static final float SIGN_WIDTH = 2.0F;
	private static final float SIGN_HEIGHT = 1.0F;

	// 四个显示框：中心 (u, v)，u 0..1，v 从顶到底，最大尺寸相对整块贴图的比例
	private static final float[][] DISPLAY_CENTERS = {
			{0.5F, 0.35F},
			{0.5F, 0.55F},
			{0.04F, 0.9375F},
			{0.96F, 0.9375F}
	};
	private static final float[][] DISPLAY_MAX_SIZES = {
			{0.92F, 0.40F},
			{0.90F, 0.20F},
			{0.92F, 0.12F},
			{0.92F, 0.12F}
	};
	private static final int[] DISPLAY_FONT_SIZES = {98, 46, 46, 46};
	// 水平对齐：0=居中（DISPLAY_CENTERS u 为文字中心），-1=左对齐（u 为文字左边），1=右对齐（u 为文字右边）
	private static final int[] DISPLAY_ALIGN = {0, 0, -1, 1};
	private static final int[] DISPLAY_COLORS = {COLOR_TEXT_MAIN, COLOR_TEXT_MAIN, COLOR_TEXT_CORNER, COLOR_TEXT_CORNER};

	@Override
	public void render(BlockRoadName.BlockEntity entity, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, float tickDelta, int light, int overlay) {
		// MTR 的 Render 加载早于 BlockEntity，且字体纹理被缓存不会随数据变化自动失效，
		// 因此每秒强制刷新一次缓存，确保文字显示最新内容。
		final long now = System.currentTimeMillis();
		if (now - lastRefreshTime >= REFRESH_INTERVAL_MS) {
			lastRefreshTime = now;
			CustomFontTextureCache.instance.clearFittedTextureCache();
		}

		final BlockPos pos = entity.getPos();
		final BlockState state = clientWorld.getBlockState(pos);
		final Direction facing = IBlock.getStatePropertySafe(state, Properties.HORIZONTAL_FACING);
		if (IBlock.getStatePropertySafe(state, IBlock.SIDE_EXTENDED) != IBlock.EnumSide.LEFT) {
			return;
		}

		final String[] texts = entity.getTexts();
		if (texts == null || texts.length < 4) {
			return;
		}

		final int shadingColor = RenderRouteBase.getShadingColor(facing, 0xFFFFFFFF);

		// 标牌中心：LEFT 方块中心再向 rotateYClockwise 方向偏移 1 格（即中间方块中心）
		final Direction right = facing.rotateYClockwise();
		final StoredMatrixTransformations baseMatrix = new StoredMatrixTransformations(
				pos.getX() + 0.5 + right.getOffsetX(),
				pos.getY() + 0.5,
				pos.getZ() + 0.5 + right.getOffsetZ()
		);
		baseMatrix.add(matrices -> {
			Drawing.rotateYDegrees(matrices, -DirectionHelper.asRotation(facing));
			Drawing.rotateZDegrees(matrices, 180);
		});

		// 正面
		renderFace(baseMatrix, facing, texts, shadingColor, light, false);
		// 背面（旋转 180° 后在同一 z 平面绘制）
		renderFace(baseMatrix, facing, texts, shadingColor, light, true);
	}

	private void renderFace(StoredMatrixTransformations baseMatrix, Direction facing, String[] texts, int shadingColor, int light, boolean back) {
		final StoredMatrixTransformations matrix = baseMatrix.copy();
		if (back) {
			matrix.add(matrices -> Drawing.rotateYDegrees(matrices, 180));
		}
		matrix.add(matrices -> matrices.translate(0, 0, Z_FACE));

		// 背面与正面不能完全一样：互换左下角（index 2）与右下角（index 3）的文字内容
		final String[] faceTexts = back ? swapCornerTexts(texts) : texts;
		for (int i = 0; i < 4; i++) {
			drawDisplay(matrix, faceTexts[i], i, shadingColor, light);
		}
	}

	/** 复制数组并互换 index 2（左下角）与 index 3（右下角）的文字内容 */
	private static String[] swapCornerTexts(String[] texts) {
		final String[] swapped = texts.clone();
		final String temp = swapped[2];
		swapped[2] = swapped[3];
		swapped[3] = temp;
		return swapped;
	}

	private void drawDisplay(StoredMatrixTransformations matrix, String text, int index, int shadingColor, int light) {
		if (text == null || text.isEmpty()) {
			return;
		}

		final FittedTextTexture fitted = CustomFontTextureCache.instance.getFittedTextTexture(text, FONT_TYPE, DISPLAY_FONT_SIZES[index], new Color(DISPLAY_COLORS[index], true));
		if (fitted.identifier == null || fitted.width <= 0 || fitted.height <= 0) {
			InitClient.LOGGER.error("RenderRoadNameBlock: fitted texture invalid for text='{}' index={}", text, index);
			return;
		}

		// 把显示框（相对贴图的比例）换算为世界单位
		final float maxW = SIGN_WIDTH * DISPLAY_MAX_SIZES[index][0];
		final float maxH = SIGN_HEIGHT * DISPLAY_MAX_SIZES[index][1];
		final float anchorX = (DISPLAY_CENTERS[index][0] - 0.5F) * SIGN_WIDTH;
		final float centerY = (DISPLAY_CENTERS[index][1] - 0.5F) * SIGN_HEIGHT;

		final float fittedW = fitted.width / PIXELS_PER_BLOCK;
		final float fittedH = fitted.height / PIXELS_PER_BLOCK;
		final float scale = Math.min(1.0F, Math.min(maxW / fittedW, maxH / fittedH));
		final float w = fittedW * scale;
		final float h = fittedH * scale;
		final float x;
		switch (DISPLAY_ALIGN[index]) {
			case -1:
				x = anchorX;
				break;
			case 1:
				x = anchorX - w;
				break;
			default:
				x = anchorX - w / 2.0F;
				break;
		}
		final float y = centerY - h / 2.0F;

		MainRenderer.scheduleRender(fitted.identifier, false, QueuedRenderLayer.EXTERIOR, (matrices, vertexConsumer, offset) -> {
			matrix.transform(matrices, offset);
			new Drawing(matrices, vertexConsumer)
					.setVerticesWH(x, y, w, h)
					.setUv(0, 0, 1, 1)
					.setColor(shadingColor)
					.setLight(light)
					.draw();
			matrices.pop();
		});
	}

	@Override
	public boolean rendersOutsideBoundingBox(BlockRoadName.BlockEntity entity) {
		return true;
	}
}
