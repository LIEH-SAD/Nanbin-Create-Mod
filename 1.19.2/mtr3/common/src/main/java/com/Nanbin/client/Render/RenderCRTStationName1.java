package com.Nanbin.client.Render;

import com.Nanbin.Init;
import com.Nanbin.Registry.RegBlock.BlockCRTStationName1;
import com.Nanbin.Registry.RegBlock.BlockCRTStationName1.BlockEntity.ResolvedRouteData;
import com.Nanbin.client.Drawing.CustomFontTextureCache;
import com.Nanbin.client.Drawing.CustomFontTextureCache.FontType;
import com.Nanbin.client.RouteMap.RouteMapGenerator.StationNameLayout;
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
 * CRT 站名牌（样式 1）渲染器：在单块薄板站名牌的正面绘制车站名/线路信息。
 * 数据来源：站名/站色取自 {@link ClientData#DATA_CACHE}（按方块坐标归属车站），
 * 线路颜色/编号由 {@link RouteMapGenerator} 解析，文字贴图由
 * {@link CustomFontTextureCache#getSignTexture} 生成。
 */
public class RenderCRTStationName1 implements BlockEntityRenderer<BlockCRTStationName1.BlockEntity> {

	private static final FontType FONT_TYPE = FontType.SOURCE_HAN;
	private static final int FONT_SIZE = 92;

	/** MTR 的 Render 加载早于 BlockEntity，且字体纹理有缓存，需定时强制刷新 */
	private static final long REFRESH_INTERVAL_MS = 1000L;

	private long lastRefreshTime = 0;

	/** 未选中站台时的就近车站查找缓存（方块坐标 -> 车站 id，0 表示未找到），随 1 秒刷新一起清空 */
	private final Map<Long, Long> stationSearchCache = new HashMap<>();

	/** 状态诊断日志去重：仅当解析结果变化时记录一次 */
	private String lastDiagnosticKey = "";

	/** 绘制面 Z 偏移（相对方块中心）：模型正面在 z=0，方块中心在 0.5，文字画在正面之外 */
	private static final float Z_FROM_CENTER = StationNameLayout.Z_FROM_CENTER;

	/** 贴图绘制尺寸（世界单位），布局规范见 {@link StationNameLayout} */
	private static final float BG_WIDTH = StationNameLayout.BG_WIDTH;
	private static final float BG_HEIGHT = StationNameLayout.BG_HEIGHT;
	private static final float TEXT_MARGIN = StationNameLayout.TEXT_MARGIN;
	private static final float TEXT_SCALE = StationNameLayout.TEXT_SCALE;

	public RenderCRTStationName1(BlockEntityRendererFactory.Context context) {
		CustomFontTextureCache.instance.selectedFont = FONT_TYPE;
		CustomFontTextureCache.instance.fontSize = FONT_SIZE;
	}

	@Override
	public void render(BlockCRTStationName1.BlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
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
		if (!(state.getBlock() instanceof BlockCRTStationName1)) {
			return;
		}
		final Direction facing = state.get(BlockCRTStationName1.FACING);
		final int shadingColor = RenderRouteBase.getShadingColor(facing, 0xFFFFFFFF);

		// 站名/站色：优先通过已配置的站台 ID 关联到车站；未配置站台（刷子未选择）时，
		// 站名牌自身坐标不在 blockPosToStation 中，改为按邻近方块就近归属车站，
		// 保证未选择站台时也能渲染站名/站色（无线路信息，只显示站色+站名）。
		final long savedPlatformId = entity.getPlatformId();
		Station station = savedPlatformId != 0 ? ClientData.DATA_CACHE.platformIdToStation.get(savedPlatformId) : null;
		if (station == null) {
			station = findStationByPosition(pos);
		}
		final String stationName = station != null ? station.name : "";
		final int stationColor = station != null ? station.color : -1;

		// 线路数据：由方块实体基于自身已保存的站台 ID 与线路编号解析
		final ResolvedRouteData resolved = entity.getResolvedData(stationColor);
		final int themeColor = resolved.themeColor();
		final int routeColor = resolved.routeColor();
		final String routeNumber = resolved.routeNumber();

		// 站台编号
		String platformNumber = "";
		if (savedPlatformId != 0) {
			final Platform platform = ClientData.DATA_CACHE.platformIdMap.get(savedPlatformId);
			if (platform != null) {
				platformNumber = platform.name;
			}
		}

		final float drawAspect = (BG_WIDTH - TEXT_MARGIN * 2) / (BG_HEIGHT - TEXT_MARGIN * 2);
		final float textX = (-BG_WIDTH / 2.0F + TEXT_MARGIN) * TEXT_SCALE;
		final float textY = (-BG_HEIGHT / 2.0F + TEXT_MARGIN) * TEXT_SCALE;
		final float textWidth = (BG_WIDTH - TEXT_MARGIN * 2) * TEXT_SCALE;
		final float textHeight = (BG_HEIGHT - TEXT_MARGIN * 2) * TEXT_SCALE;

		final Identifier textureId = CustomFontTextureCache.instance.getSignTexture(stationName, themeColor, routeColor, routeNumber, platformNumber, drawAspect, FONT_TYPE);

		// 状态诊断日志：仅当解析结果变化时记录一次，方便排查线路/站台数据是否被解析到
		final String diagnosticKey = pos.toShortString() + "|facing=" + facing + "|station=" + stationName
				+ "|platformId=" + savedPlatformId + "|route='" + routeNumber + "'|platNum='" + platformNumber + "'"
				+ "|theme=#" + Integer.toHexString(themeColor) + "|routeColor=#" + Integer.toHexString(routeColor)
				+ "|texture=" + textureId;
		if (!diagnosticKey.equals(lastDiagnosticKey)) {
			lastDiagnosticKey = diagnosticKey;
			Init.LOGGER.info("RenderCRTStationName1: {}", diagnosticKey);
		}

		if (!stationName.isEmpty() && textureId.getPath().contains("nanbin_empty_fallback")) {
			Init.LOGGER.info("RenderCRTStationName1 STILL-FALLBACK: pos={} station='{}' textureId={}", pos.toShortString(), stationName, textureId);
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

	/**
	 * 未配置站台时，按站名牌所在坐标归属车站。
	 * 站名牌自身坐标不在 {@link mtr.data.DataCache#blockPosToStation} 中，
	 * 因此遍历所有车站，取第一个覆盖站名牌坐标的车站区域（AreaBase.inArea）。结果按坐标缓存。
	 */
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
	public boolean rendersOutsideBoundingBox(BlockCRTStationName1.BlockEntity entity) {
		return true;
	}
}