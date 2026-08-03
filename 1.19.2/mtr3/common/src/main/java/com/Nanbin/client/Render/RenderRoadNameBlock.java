package com.Nanbin.client.Render;

import com.Nanbin.Init;
import com.Nanbin.Registry.RegBlock.BlockRoadName;
import com.Nanbin.client.Drawing.CustomFontTextureCache;
import com.Nanbin.client.Drawing.CustomFontTextureCache.FittedTextTexture;
import com.Nanbin.client.Drawing.CustomFontTextureCache.FontType;
import mtr.client.IDrawing;
import mtr.render.RenderRouteBase;
import mtr.render.StoredMatrixTransformations;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3f;
import net.minecraft.world.World;

import java.awt.Color;

/**
 * 路名方块渲染器：在方块模型（3 块宽 × 1 块高 × 2px 厚的标牌）的正反两面渲染四个文本显示框，
 * 与配置屏幕中的预览布局一致。文本使用 {@link CustomFontTextureCache} 的 SOURCE_HAN 字体，
 * 超出显示框时自动等比缩小。
 */
public class RenderRoadNameBlock implements BlockEntityRenderer<BlockRoadName.BlockEntity> {

	private static final FontType FONT_TYPE = FontType.SOURCE_HAN;

	private static final int COLOR_TEXT_MAIN = 0xFFFFFFFF;
	private static final int COLOR_TEXT_CORNER = 0xFF0059C6;

	/** MTR 的 Render 加载早于 BlockEntity，且字体纹理有缓存，需定时强制刷新 */
	private static final long REFRESH_INTERVAL_MS = 1000L;

	private long lastRefreshTime = 0;
	private String lastRenderStateKey = "";

	/** 每块（block）对应的贴图像素，用于把生成的字体贴图像素换算为世界单位 */
	private static final float PIXELS_PER_BLOCK = 120.0F;

	/** 标牌半厚：碰撞箱 z 为 7..9，中心在 8/16=0.5，面到中心距离 1/16 */
	private static final float SIGN_HALF_THICKNESS = 0.064F;

	/** 绘制面 Z 偏移（正面）。文字必须画在模型表面之外，否则被不透明模型遮挡。*/
	private static final float Z_FACE = -(SIGN_HALF_THICKNESS + 0.003125F);

	/** 整个路名牌宽度/高度（块）：LEFT 半块 + MIDDLE 整块 + RIGHT 半块拼成 2 块宽、1 块高（与 2:1 贴图一致） */
	private static final float SIGN_WIDTH = 2.0F;
	private static final float SIGN_HEIGHT = 1.0F;

	// 四个显示框：中心 (u, v)（0..1，v 从顶到底），最大尺寸相对整块贴图的比例
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

	public RenderRoadNameBlock(BlockEntityRendererFactory.Context context) {
	}

	@Override
	public void render(BlockRoadName.BlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
		// MTR 的 Render 加载早于 BlockEntity，且字体纹理被缓存不会随数据变化自动失效，
		// 因此每秒强制刷新一次缓存，确保文字显示最新内容。
		final long now = System.currentTimeMillis();
		if (now - lastRefreshTime >= REFRESH_INTERVAL_MS) {
			lastRefreshTime = now;
			CustomFontTextureCache.instance.clearFittedTextureCache();
		}

		final World world = entity.getWorld();
		if (world == null) {
			return;
		}
		final BlockPos pos = entity.getPos();
		final BlockState state = world.getBlockState(pos);
		// 方块被打破后 getBlockState 会返回 air，访问其属性会抛异常
		if (!(state.getBlock() instanceof BlockRoadName)) {
			return;
		}
		final Direction facing = state.get(BlockRoadName.FACING);
		if (state.get(BlockRoadName.SIDE) != BlockRoadName.EnumSide.LEFT) {
			return;
		}

		// 优先读取 LEFT 方块实体上的数据；若为空（例如旧存档只有 MIDDLE 有数据），
		// 回退到中间方块实体上的数据，保证文字总能显示。
		String[] texts = entity.getTexts();
		if (texts == null || texts.length < 4 || allEmpty(texts)) {
			final BlockPos middlePos = pos.offset(facing.rotateYClockwise(), 1);
			if (world.getBlockEntity(middlePos) instanceof BlockRoadName.BlockEntity middleEntity) {
				final String[] middleTexts = middleEntity.getTexts();
				if (middleTexts != null && middleTexts.length >= 4 && !allEmpty(middleTexts)) {
					texts = middleTexts;
				}
			}
		}
		if (texts == null || texts.length < 4) {
			return;
		}
		// 仅当内容变化时记录一次，避免每帧刷屏
		final String stateKey = pos.toShortString() + "|" + facing + "|" + String.join("|", texts);
		if (!stateKey.equals(lastRenderStateKey)) {
			lastRenderStateKey = stateKey;
			Init.LOGGER.info("RenderRoadNameBlock: drawing texts on facing={} texts=[{}]", facing, String.join("|", texts));
		}

		final int shadingColor = RenderRouteBase.getShadingColor(facing, 0xFFFFFFFF);

		// 标牌中心：LEFT 方块中心再向 rotateYClockwise 方向偏移 1 格（即中间方块中心）。
		// vanilla 的 BlockEntityRenderDispatcher 已将 matrices 平移到本方块坐标原点，因此用相对坐标。
		final Direction right = facing.rotateYClockwise();
		final StoredMatrixTransformations baseMatrix = new StoredMatrixTransformations();
		baseMatrix.add(graphics -> {
			graphics.translate(0.5 + right.getOffsetX() * 1.0, 0.5, 0.5 + right.getOffsetZ() * 1.0);
			graphics.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(-facing.asRotation()));
			graphics.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(180.0F));
		});

		// 正面
		renderFace(baseMatrix, matrices, vertexConsumers, facing, texts, shadingColor, light, false);
		// 背面（旋转 180° 后在同一 z 平面绘制，背面文字从背后可读）
		renderFace(baseMatrix, matrices, vertexConsumers, facing, texts, shadingColor, light, true);
	}

	private void renderFace(StoredMatrixTransformations baseMatrix, MatrixStack matrices, VertexConsumerProvider vertexConsumers, Direction facing, String[] texts, int shadingColor, int light, boolean back) {
		final StoredMatrixTransformations matrix = baseMatrix.copy();
		if (back) {
			matrix.add(graphics -> graphics.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(180.0F)));
		}
		matrix.add(graphics -> graphics.translate(0, 0, Z_FACE));

		// 背面与正面不能完全一样：互换左下角（index 2）与右下角（index 3）的文字内容
		final String[] faceTexts = back ? swapCornerTexts(texts) : texts;
		for (int i = 0; i < 4; i++) {
			drawDisplay(matrix, matrices, vertexConsumers, facing, faceTexts[i], i, shadingColor, light);
		}
	}

	/** 四个文本框是否都为空 */
	private static boolean allEmpty(String[] texts) {
		for (final String text : texts) {
			if (text != null && !text.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	/** 复制数组并互换 index 2（左下角）与 index 3（右下角）的文字内容 */
	private static String[] swapCornerTexts(String[] texts) {
		final String[] swapped = texts.clone();
		final String temp = swapped[2];
		swapped[2] = swapped[3];
		swapped[3] = temp;
		return swapped;
	}

	private void drawDisplay(StoredMatrixTransformations matrix, MatrixStack matrices, VertexConsumerProvider vertexConsumers, Direction facing, String text, int index, int shadingColor, int light) {
		if (text == null || text.isEmpty()) {
			return;
		}

		final FittedTextTexture fitted = CustomFontTextureCache.instance.getFittedTextTexture(text, FONT_TYPE, DISPLAY_FONT_SIZES[index], new Color(DISPLAY_COLORS[index], true));
		if (fitted.identifier == null || fitted.width <= 0 || fitted.height <= 0) {
			Init.LOGGER.error("RenderRoadNameBlock: fitted texture invalid for text='{}' index={} id={} w={} h={}", text, index, fitted.identifier, fitted.width, fitted.height);
			return;
		}

		// 把显示框（相对贴图的比例）换算为世界单位
		final float maxW = SIGN_WIDTH * DISPLAY_MAX_SIZES[index][0];
		final float maxH = SIGN_HEIGHT * DISPLAY_MAX_SIZES[index][1];
		// 中心：u 水平 0..1；v 垂直从顶到底（本地 +y 向下，与 GUI 一致）
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
				x = anchorX;          // 左对齐：anchorX 为文字左边
				break;
			case 1:
				x = anchorX - w;      // 右对齐：anchorX 为文字右边
				break;
			default:
				x = anchorX - w / 2.0F; // 居中
				break;
		}
		final float y = centerY - h / 2.0F;

		// 不使用 RenderTrains.scheduleRender 队列（该队列只在渲染列车/座位实体时才冲刷，无列车时文字不显示），
		// 而是直接用 vanilla 渲染管线：根据动态纹理创建 RenderLayer，从 VertexConsumerProvider 获取 buffer。
		final RenderLayer renderLayer = RenderLayer.getEntityTranslucent(fitted.identifier);
		final VertexConsumer vertexConsumer = vertexConsumers.getBuffer(renderLayer);

		// StoredMatrixTransformations.transform 内部会 push，因此绘制后需要 pop
		matrix.transform(matrices);
		IDrawing.drawTexture(matrices, vertexConsumer, x, y, w, h, 0F, 0F, 1F, 1F, facing, shadingColor, light);
		matrices.pop();
	}

	@Override
	public boolean rendersOutsideBoundingBox(BlockRoadName.BlockEntity entity) {
		return true;
	}
}
