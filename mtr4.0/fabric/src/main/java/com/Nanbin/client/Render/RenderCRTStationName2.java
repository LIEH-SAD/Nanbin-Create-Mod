package com.Nanbin.client.Render;

import com.Nanbin.InitClient;
import com.Nanbin.Registry.RegBlock.BlockCRTStationName2;
import com.Nanbin.client.Drawing.CustomFontTextureCache;
import com.Nanbin.client.Drawing.CustomFontTextureCache.FontType;
import com.Nanbin.client.RouteMap.RouteMapGenerator;
import com.Nanbin.client.RouteMap.RouteMapGenerator.ResolvedRouteData;
import org.mtr.core.data.Platform;
import org.mtr.core.data.Station;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockEntityRenderer;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mod.block.BlockStationNameBase;
import org.mtr.mod.block.IBlock;
import org.mtr.mod.client.DynamicTextureCache;
import org.mtr.mod.client.IDrawing;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.data.IGui;
import org.mtr.mod.generated.lang.TranslationProvider;
import org.mtr.mod.render.MainRenderer;
import org.mtr.mod.render.QueuedRenderLayer;
import org.mtr.mod.render.RenderRouteBase;
import org.mtr.mod.render.StoredMatrixTransformations;

import java.util.HashMap;
import java.util.Map;

import static org.mtr.mod.InitClient.findStation;


public class RenderCRTStationName2 extends BlockEntityRenderer<BlockCRTStationName2.BlockEntity> implements IBlock, IGui, IDrawing {

	private static final float Z_FROM_CENTER = 0.459375F;
	private static final float BG_WIDTH = 1.5F;
	private static final float BG_HEIGHT = 1.7F;
	private static final float TEXT_MARGIN = 0.05F;
	private static final float TEXT_SCALE = 0.6F;

	private static final boolean USE_CUSTOM_FONT = true;
	private static final boolean WHITE_BACKGROUND = false;

	// ---- 本站名牌渲染数据：无顶/底条 + 中间主题色横条 + 反色圆 ----
	private static final FontType FONT_TYPE = FontType.ALIBABA;
	private static final FontType FONT_TYPE_2 = FontType.SOURCE_SANS_3;
	private static final int FONT_SIZE = 81;
	private static final float TOP_BAR_END = 0.0F;           // 顶条底缘（相对 H）
	private static final float TEXT_END = 0.30F;           // 文字区底缘（相对 H）
	private static final float MIDDLE_BAR_START = 0.40F;   	 // 中间条上缘 = 0.50 × 背景高
	private static final float MIDDLE_BAR_END = 0.50F;     // 中间条下缘 = 0.60 × 背景高（高 0.10 × 背景高）
	private static final float MIDDLE_BAR_WIDTH = 0F;      // 中间条宽 = 整宽（<=0 表示整宽）
	private static final float BOTTOM_BAR_START = 1F;      // 底条上缘（相对 H）
	// ---- 文字排版参数 ----
	private static final float LATIN_FONT_RATIO = 0.5F;    // 拉丁行字号 = FONT_SIZE * ratio
	private static final float GAP_RATIO = 0.012F;         // 中/拉行距 = FONT_SIZE * ratio（缩小但不重叠）
	private static final float VERTICAL_BIAS = 0.7F;      // 文字块垂直偏置
	private static final float EXTRA_OFFSET_RATIO = 0.03F; // 文字块额外下移 = H * ratio（0 表示相对上一步下移 0.05）
	private static final float TEXT_MARGIN_DEFAULT = 0.04F;
	private static final float TEXT_MARGIN_MEDIUM = 0.12F;
	private static final float TEXT_MARGIN_SHORT = 0.20F;
	// ---- 圆参数 ----
	private static final float CIRCLE_CENTER_Y_OFFSET = 0.30F; // 圆心在条中心基础上再下移 = H * ratio
	private static final float CIRCLE_RADIUS_H_RATIO = 0.1F;  // 圆半径上限 = H * ratio（1 倍）
	private static final float CIRCLE_RADIUS_W_RATIO = 0.14F; // 圆半径上限 = W * ratio（1.2 倍）
	private static final float STROKE_H_RATIO = 0.006F;        // 描边宽 = max(H * ratio, 2)
	private static final float CIRCLE_FONT_RATIO = 0.35F;      // 圆内字号 = FONT_SIZE * ratio
	private static final boolean INVERTED_CIRCLE = true;

	/** MTR 的 Render 加载早于 BlockEntity，且字体纹理有缓存，需定时强制刷新 */
	private static final long REFRESH_INTERVAL_MS = 1000L;

	private long lastRefreshTime = 0;
	/** 调试：记录每个站名牌最近一次的解析状态 (posKey → stateKey)，仅在状态变化时输出日志 */
	private static final Map<Long, String> lastDebugStates = new HashMap<>();

	public RenderCRTStationName2(Argument dispatcher, boolean showLogo) {
		super(dispatcher);
	}

	@Override
	public void render(BlockCRTStationName2.BlockEntity entity, float tickDelta, GraphicsHolder graphicsHolder, int light, int overlay) {
		// MTR 的 Render 加载早于 BlockEntity，且字体纹理被缓存不会随数据/布局变化自动失效，
		// 因此每秒强制刷新一次缓存，确保站名、间距等显示最新效果。
		final long now = System.currentTimeMillis();
		if (now - lastRefreshTime >= REFRESH_INTERVAL_MS) {
			lastRefreshTime = now;
			CustomFontTextureCache.instance.clearFittedTextureCache();
		}

		final World world = entity.getWorld2();
		if (world == null) return;

		final BlockPos pos = entity.getPos2();
		final BlockState state = world.getBlockState(pos);

		final Direction facing = IBlock.getStatePropertySafe(state, BlockStationNameBase.FACING);
		final int shadingColor = RenderRouteBase.getShadingColor(facing, entity.getColor(state));

		// Centre the transformation on the block, applying yOffset
		final StoredMatrixTransformations baseMatrix = new StoredMatrixTransformations(
				pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5
		);
		baseMatrix.add(graphics -> {
			graphics.rotateYDegrees(-facing.asRotation());
			graphics.rotateZDegrees(180);
		});

		// Resolve station name and colour
		final Station station = findStation(pos);
		final String stationName = station != null ? station.getName() : TranslationProvider.GUI_MTR_UNTITLED.getString();
		final int stationColor = station != null ? station.getColor() : 0;

		// Render once (CRT signs are single-sided)
		renderSide(baseMatrix, world, pos, state, facing, stationName, stationColor, shadingColor, light, entity);
	}

	private void renderSide(StoredMatrixTransformations base, World world, BlockPos pos, BlockState state, Direction facing, String stationName, int stationColor, int shadingColor, int light, BlockCRTStationName2.BlockEntity entity) {
		final StoredMatrixTransformations matrix = base.copy();
		matrix.add(graphics -> graphics.translate(0, 0, Z_FROM_CENTER));

		// Resolve route/platform data from saved platform ID
		String platformNumber = "";
		final long posKey = pos.asLong();

		final long savedPlatformId = entity.getPlatformId();
		// 优先使用服务端同步到方块实体上的真实线路编号（MTR 客户端数据不含 routeNumber）
		final String syncedRouteNumber = entity.getRouteNumber() == null ? "" : entity.getRouteNumber();
		final ResolvedRouteData resolved = RouteMapGenerator.resolveRouteData(savedPlatformId, stationColor, syncedRouteNumber);
		final int themeColor = resolved.themeColor();
		final int routeColor = resolved.routeColor();
		final String routeNumber = resolved.routeNumber();

		try {
			final MinecraftClientData clientData = MinecraftClientData.getInstance();
			final Platform platform = (savedPlatformId == 0) ? null : clientData.platformIdMap.get(savedPlatformId);
			if (platform != null) {
				platformNumber = platform.getName();
			}
		} catch (Exception e) {
			InitClient.LOGGER.error("RenderCRTStationName1: Error resolving platform name at {}", pos.toShortString(), e);
		}

		// Texture coordinates
		final float drawAspect = (BG_WIDTH - TEXT_MARGIN * 2) / (BG_HEIGHT - TEXT_MARGIN * 2);
		final float textX = (-BG_WIDTH / 2.0F + TEXT_MARGIN) * TEXT_SCALE;
		final float textY = (-BG_HEIGHT / 2.0F + TEXT_MARGIN) * TEXT_SCALE;
		final float textWidth = (BG_WIDTH - TEXT_MARGIN * 2) * TEXT_SCALE;
		final float textHeight = (BG_HEIGHT - TEXT_MARGIN * 2) * TEXT_SCALE;

		final Identifier textureId;
		// 数据暂缺时仍渲染（回退到站名/站色），不再跳过渲染
		if (USE_CUSTOM_FONT) {
			// 站名牌 2：反色圆形线路标志 + 中间主题色横条（无顶/底条）
			textureId = CustomFontTextureCache.instance.getSignTexture(stationName, themeColor, routeColor, routeNumber, platformNumber, drawAspect, FONT_TYPE, FONT_SIZE, FONT_TYPE_2, TOP_BAR_END, TEXT_END, MIDDLE_BAR_START, MIDDLE_BAR_END, MIDDLE_BAR_WIDTH, BOTTOM_BAR_START, LATIN_FONT_RATIO, GAP_RATIO, VERTICAL_BIAS, EXTRA_OFFSET_RATIO, TEXT_MARGIN_DEFAULT, TEXT_MARGIN_MEDIUM, TEXT_MARGIN_SHORT, CIRCLE_CENTER_Y_OFFSET, CIRCLE_RADIUS_H_RATIO, CIRCLE_RADIUS_W_RATIO, STROKE_H_RATIO, CIRCLE_FONT_RATIO, INVERTED_CIRCLE, WHITE_BACKGROUND);
		} else {
			textureId = DynamicTextureCache.instance.getStationName(stationName, BG_WIDTH).identifier;
		}

		MainRenderer.scheduleRender(textureId, false, QueuedRenderLayer.EXTERIOR, (graphicsHolder, offset) -> {
			matrix.transform(graphicsHolder, offset);
			IDrawing.drawTexture(graphicsHolder, textX, textY, textWidth, textHeight, 0F, 0F, 1F, 1F, facing, shadingColor, light);
			graphicsHolder.pop();
		});

		// ===== 调试：解析状态变化时输出一次日志（不再每秒刷屏） =====
		final String stationColorHex = String.format("#%06X", stationColor);
		final Platform debugPlatform = (savedPlatformId != 0) ? MinecraftClientData.getInstance().platformIdMap.get(savedPlatformId) : null;

		final boolean debugRouteSynced = !syncedRouteNumber.isEmpty();
		final String debugRouteNum = routeNumber.isEmpty() ? "N/A" : routeNumber;
		final String debugRouteColorHex = String.format("#%06X", routeColor);
		final String platName = (debugPlatform != null) ? debugPlatform.getName() : "N/A";
		final int routesCount = (debugPlatform != null) ? debugPlatform.routes.size() : 0;
		final String stateKey = savedPlatformId + "|" + (debugPlatform != null) + "|" + routesCount + "|" + debugRouteColorHex + "|" + debugRouteNum + "|" + platName + "|" + debugRouteSynced;
	}
}