package com.Nanbin.client.Render;

import com.Nanbin.Registry.RegBlock.BlockCRTStationName1;
import com.Nanbin.Registry.RegBlock.BlockCRTStationName1.BlockEntity.ResolvedRouteData;
import com.Nanbin.client.Drawing.CustomFontTextureCache;
import com.Nanbin.client.Drawing.CustomFontTextureCache.FontType;
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
import net.minecraft.util.math.MathHelper;
import org.joml.Quaternionf;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

/**
 * CRT 站名牌（样式 1）渲染器：在单块薄板站名牌的正面绘制车站名/线路信息。
 * 数据来源：站名/站色取自 {@link ClientData#DATA_CACHE}（按方块坐标归属车站），
 * 线路颜色/编号由 {@link com.Nanbin.client.RouteMap.RouteMapGenerator} 解析，文字贴图由
 * {@link CustomFontTextureCache#getSignTexture} 生成。
 */
public class RenderCRTStationName1 implements BlockEntityRenderer<BlockCRTStationName1.BlockEntity> {

	private static final float Z_FROM_CENTER = 0.459375F;
	private static final float BG_WIDTH = 1.4F;
	private static final float BG_HEIGHT = 1.6F;
	private static final float TEXT_MARGIN = 0.05F;
	private static final float TEXT_SCALE = 0.6F;

	private static final boolean USE_CUSTOM_FONT = true;
	private static final boolean WHITE_BACKGROUND = false;

	// ---- 本站名牌渲染数据：顶条 + 底条 + 正色圆 ----
	private static final FontType FONT_TYPE = FontType.SOURCE_HAN;
	private static final int FONT_SIZE = 85;
	private static final FontType FONT_TYPE_2 = FontType.SOURCE_SANS_3; // 圆内文字字体（仅纯数字用 Source Sans 3，其余用主字体 FONT_TYPE）
	private static final float TOP_BAR_END = 0.10F;       // 顶条底缘（相对 H）
	private static final float TEXT_END = 0.55F;          // 文字区底缘（相对 H）
	private static final float MIDDLE_BAR_START = 0F;     // 无中间条
	private static final float MIDDLE_BAR_END = 0F;
	private static final float MIDDLE_BAR_WIDTH = 0F;
	private static final float BOTTOM_BAR_START = 0.70F;  // 底条上缘（相对 H）
	// ---- 文字排版参数 ----
	private static final float LATIN_FONT_RATIO = 0.5F;    // 拉丁行字号 = FONT_SIZE * ratio
	private static final float GAP_RATIO = 0.02F;          // 中/拉行距 = FONT_SIZE * ratio
	private static final float VERTICAL_BIAS = 0.65F;      // 文字块垂直偏置
	private static final float EXTRA_OFFSET_RATIO = 0F; // 文字块额外下移 = H * ratio
	private static final float TEXT_MARGIN_DEFAULT = 0.04F;
	private static final float TEXT_MARGIN_MEDIUM = 0.12F;
	private static final float TEXT_MARGIN_SHORT = 0.20F;
	// ---- 圆参数 ----
	private static final float CIRCLE_CENTER_Y_OFFSET = 0F;   // 圆心相对基准位置的下移量 = H * ratio
	private static final float CIRCLE_RADIUS_H_RATIO = 0.10F; // 圆半径上限 = H * ratio
	private static final float CIRCLE_RADIUS_W_RATIO = 0.14F; // 圆半径上限 = W * ratio
	private static final float STROKE_H_RATIO = 0.006F;       // 描边宽 = max(H * ratio, 2)
	private static final float CIRCLE_FONT_RATIO = 0.35F;     // 圆内字号 = FONT_SIZE * ratio
	private static final boolean INVERTED_CIRCLE = false;

	/** MTR 的 Render 加载早于 BlockEntity，且字体纹理有缓存，需定时强制刷新 */
	private static final long REFRESH_INTERVAL_MS = 1000L;

	private long lastRefreshTime = 0;

	/** 未选中站台时的就近车站查找缓存（方块坐标 -> 车站 id，0 表示未找到），随 1 秒刷新一起清空 */
	private final Map<Long, Long> stationSearchCache = new HashMap<>();

	/** 状态诊断日志去重：仅当解析结果变化时记录一次 */
	private String lastDiagnosticKey = "";

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
		final int themeColor = resolved.routeColor();
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

		final Identifier textureId;
		if (USE_CUSTOM_FONT) {
			textureId = CustomFontTextureCache.instance.getSignTexture(stationName, themeColor, routeColor, routeNumber, platformNumber, drawAspect, FONT_TYPE, FONT_SIZE, FONT_TYPE_2, TOP_BAR_END, TEXT_END, MIDDLE_BAR_START, MIDDLE_BAR_END, MIDDLE_BAR_WIDTH, BOTTOM_BAR_START, LATIN_FONT_RATIO, GAP_RATIO, VERTICAL_BIAS, EXTRA_OFFSET_RATIO, TEXT_MARGIN_DEFAULT, TEXT_MARGIN_MEDIUM, TEXT_MARGIN_SHORT, CIRCLE_CENTER_Y_OFFSET, CIRCLE_RADIUS_H_RATIO, CIRCLE_RADIUS_W_RATIO, STROKE_H_RATIO, CIRCLE_FONT_RATIO, INVERTED_CIRCLE, WHITE_BACKGROUND);
		} else {
			textureId = CustomFontTextureCache.instance.getSignTexture(stationName, themeColor, routeColor, routeNumber, platformNumber, drawAspect, FONT_TYPE);
		}

		// 状态诊断日志：仅当解析结果变化时记录一次，方便排查线路/站台数据是否被解析到
		final String diagnosticKey = pos.toShortString() + "|facing=" + facing + "|station=" + stationName
				+ "|platformId=" + savedPlatformId + "|route='" + routeNumber + "'|platNum='" + platformNumber + "'"
				+ "|theme=#" + Integer.toHexString(themeColor) + "|routeColor=#" + Integer.toHexString(routeColor)
				+ "|texture=" + textureId;
		if (!diagnosticKey.equals(lastDiagnosticKey)) {
			lastDiagnosticKey = diagnosticKey;
		}

		final StoredMatrixTransformations baseMatrix = new StoredMatrixTransformations();
		baseMatrix.add(graphics -> {
			graphics.translate(0.5, 0.5, 0.5);
			graphics.multiply(new Quaternionf().rotationY(MathHelper.RADIANS_PER_DEGREE * -facing.asRotation()));
			graphics.multiply(new Quaternionf().rotationZ(MathHelper.RADIANS_PER_DEGREE * 180.0F));
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
	public boolean rendersOutsideBoundingBox(BlockCRTStationName1.BlockEntity entity) {
		return true;
	}
}