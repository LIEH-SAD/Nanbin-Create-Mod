package com.Nanbin.client.Render;

import com.Nanbin.Registry.RegBlock.BlockCRTStationName1.BlockEntity.ResolvedRouteData;
import com.Nanbin.Registry.RegBlock.BlockOrdinaryStationName;
import com.Nanbin.client.Drawing.CustomFontTextureCache;
import com.Nanbin.client.Drawing.CustomFontTextureCache.FontType;
import mtr.block.BlockStationNameBase;
import mtr.block.IBlock;
import mtr.client.ClientData;
import mtr.client.IDrawing;
import mtr.data.Platform;
import mtr.data.Station;
import mtr.render.RenderRouteBase;
import mtr.render.StoredMatrixTransformations;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3f;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

/**
 * 普通站名牌渲染器：白底黑字（ALIBABA 字体），下方绘制站台/线路徽标圆。
 * 站名与站色取自 {@link ClientData#DATA_CACHE}（按方块坐标归属车站），
 * 线路颜色/编号由 {@link com.Nanbin.Registry.RegBlock.BlockCRTStationName1.BlockEntity} 解析，
 * 文字贴图由 {@link CustomFontTextureCache#getSignTexture} 生成。
 */
public class RenderOrdinaryStationName implements BlockEntityRenderer<BlockOrdinaryStationName.BlockEntity> {

	private static final float Z_FROM_CENTER = 0.459375F;
	private static final float BG_WIDTH = 1.5F;
	private static final float BG_HEIGHT = 1.7F;
	private static final float TEXT_MARGIN = 0.05F;
	private static final float TEXT_SCALE = 0.6F;

	private static final boolean USE_CUSTOM_FONT = true;

	// ---- 白底黑字，ALIBABA 字体，等比缩放 ----
	private static final FontType FONT_TYPE = FontType.ALIBABA;
	private static final FontType FONT_TYPE_2 = FontType.SOURCE_SANS_3; // 圆内文字字体（仅纯数字用 Source Sans 3，其余用主字体 FONT_TYPE）
	private static final boolean WHITE_BACKGROUND = true;
	private static final int FONT_SIZE = 81;
	private static final float TOP_BAR_END = 0F;          // 无顶条（白底）
	private static final float TEXT_END = 0.62F;          // 文字区底缘（相对 H）
	private static final float MIDDLE_BAR_START = 0F;     // 无中间条
	private static final float MIDDLE_BAR_END = 0F;
	private static final float MIDDLE_BAR_WIDTH = 0F;
	private static final float BOTTOM_BAR_START = 1F;     // 无底条
	// ---- 文字排版参数 ----
	private static final float LATIN_FONT_RATIO = 0.5F;   // 拉丁行字号 = FONT_SIZE * ratio
	private static final float GAP_RATIO = 0.02F;         // 中/拉行距 = FONT_SIZE * ratio
	private static final float VERTICAL_BIAS = 0.55F;     // 文字块垂直偏置
	private static final float EXTRA_OFFSET_RATIO = 0F;
	private static final float TEXT_MARGIN_DEFAULT = 0.10F;
	private static final float TEXT_MARGIN_MEDIUM = 0.20F;
	private static final float TEXT_MARGIN_SHORT = 0.30F;
	// ---- 圆参数（线路/站台徽标） ----
	private static final float CIRCLE_CENTER_Y_OFFSET = 0.36F; // 圆心在文字区下方
	private static final float CIRCLE_RADIUS_H_RATIO = 0.09F;
	private static final float CIRCLE_RADIUS_W_RATIO = 0.13F;
	private static final float STROKE_H_RATIO = 0.006F;
	private static final float CIRCLE_FONT_RATIO = 0.32F;
	private static final boolean INVERTED_CIRCLE = false;

	/** MTR 的 Render 加载早于 BlockEntity，且字体纹理有缓存，需定时强制刷新 */
	private static final long REFRESH_INTERVAL_MS = 1000L;

	private long lastRefreshTime = 0;

	/** 未选中站台时的就近车站查找缓存（方块坐标 -> 车站 id，0 表示未找到），随 1 秒刷新一起清空 */
	private final Map<Long, Long> stationSearchCache = new HashMap<>();

	public RenderOrdinaryStationName(BlockEntityRendererFactory.Context context) {
		CustomFontTextureCache.instance.selectedFont = FONT_TYPE;
		CustomFontTextureCache.instance.fontSize = FONT_SIZE;
	}

	@Override
	public void render(BlockOrdinaryStationName.BlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
		// 字体纹理被缓存不会随数据/布局变化自动失效，每秒强制刷新一次
		final long now = System.currentTimeMillis();
		if (now - lastRefreshTime >= REFRESH_INTERVAL_MS) {
			lastRefreshTime = now;
			CustomFontTextureCache.instance.clearFittedTextureCache();
			stationSearchCache.clear();
		}

		final World world = entity.getWorld();
		if (world == null) {
			return;
		}
		final BlockPos pos = entity.getPos();
		final BlockState state = world.getBlockState(pos);
		if (!(state.getBlock() instanceof BlockOrdinaryStationName)) {
			return;
		}

		final Direction facing = IBlock.getStatePropertySafe(state, BlockStationNameBase.FACING);
		final int shadingColor = RenderRouteBase.getShadingColor(facing, entity.getColor(state));

		// 站名/站色：优先按已配置站台归属车站；未配置时按方块坐标就近归属车站
		final long savedPlatformId = entity.getPlatformId();
		Station station = savedPlatformId != 0 ? ClientData.DATA_CACHE.platformIdToStation.get(savedPlatformId) : null;
		if (station == null) {
			station = findStationByPosition(pos);
		}
		final String stationName = station != null ? station.name : "";
		final int stationColor = station != null ? station.color : 0;

		// 站台编号（仅已配置站台时显示）
		String platformNumber = "";
		if (savedPlatformId != 0) {
			final Platform platform = ClientData.DATA_CACHE.platformIdMap.get(savedPlatformId);
			if (platform != null) {
				platformNumber = platform.name;
			}
		}

		final ResolvedRouteData resolved = entity.getResolvedData(stationColor);
		final int themeColor = resolved.themeColor();
		final int routeColor = resolved.routeColor();
		final String routeNumber = resolved.routeNumber();

		final float drawAspect = (BG_WIDTH - TEXT_MARGIN * 2) / (BG_HEIGHT - TEXT_MARGIN * 2);
		final float textX = (-BG_WIDTH / 2.0F + TEXT_MARGIN) * TEXT_SCALE;
		final float textY = (-BG_HEIGHT / 2.0F + TEXT_MARGIN) * TEXT_SCALE;
		final float textWidth = (BG_WIDTH - TEXT_MARGIN * 2) * TEXT_SCALE;
		final float textHeight = (BG_HEIGHT - TEXT_MARGIN * 2) * TEXT_SCALE;

		final Identifier textureId;
		if (USE_CUSTOM_FONT) {
			textureId = CustomFontTextureCache.instance.getSignTexture(stationName, themeColor, routeColor, routeNumber, platformNumber, drawAspect, FONT_TYPE, FONT_SIZE, FONT_TYPE_2, TOP_BAR_END, TEXT_END, MIDDLE_BAR_START, MIDDLE_BAR_END, MIDDLE_BAR_WIDTH, BOTTOM_BAR_START, LATIN_FONT_RATIO, GAP_RATIO, VERTICAL_BIAS, EXTRA_OFFSET_RATIO, TEXT_MARGIN_DEFAULT, TEXT_MARGIN_MEDIUM, TEXT_MARGIN_SHORT, CIRCLE_CENTER_Y_OFFSET, CIRCLE_RADIUS_H_RATIO, CIRCLE_RADIUS_W_RATIO, STROKE_H_RATIO, CIRCLE_FONT_RATIO, INVERTED_CIRCLE, WHITE_BACKGROUND);
		} else {
			textureId = CustomFontTextureCache.instance.getSignTexture(stationName, themeColor, routeColor, routeNumber, platformNumber, drawAspect, FONT_TYPE);
		}

		final StoredMatrixTransformations baseMatrix = new StoredMatrixTransformations();
		baseMatrix.add(graphics -> {
			graphics.translate(0.5, 0.5, 0.5);
			graphics.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(-facing.asRotation()));
			graphics.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(180.0F));
		});
		final StoredMatrixTransformations matrix = baseMatrix.copy();
		matrix.add(graphics -> graphics.translate(0, 0, Z_FROM_CENTER));

		final RenderLayer renderLayer = RenderLayer.getEntityTranslucent(textureId);
		final VertexConsumer vertexConsumer = vertexConsumers.getBuffer(renderLayer);

		matrix.transform(matrices);
		IDrawing.drawTexture(matrices, vertexConsumer, textX, textY, textWidth, textHeight, 0F, 0F, 1F, 1F, facing, shadingColor, light);
		matrices.pop();
	}

	private Station findStationByPosition(BlockPos pos) {
		final Long cachedStationId = stationSearchCache.get(pos.asLong());
		if (cachedStationId != null) {
			return cachedStationId == 0 ? null : ClientData.DATA_CACHE.stationIdMap.get(cachedStationId);
		}

		Station found = null;
		for (final Station station : ClientData.STATIONS) {
			if (station.inArea(pos.getX(), pos.getZ())) {
				found = station;
				break;
			}
		}

		stationSearchCache.put(pos.asLong(), found == null ? 0 : found.id);
		return found;
	}

	@Override
	public boolean rendersOutsideBoundingBox(BlockOrdinaryStationName.BlockEntity entity) {
		return true;
	}
}
