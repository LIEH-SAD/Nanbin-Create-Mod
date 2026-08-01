package com.Nanbin.client.Render;

import com.Nanbin.Init;
import com.Nanbin.Registry.RegBlock.BlockCRTStationName1;
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


public class RenderCRTStationName1 extends BlockEntityRenderer<BlockCRTStationName1.BlockEntity> implements IBlock, IGui, IDrawing {

	private static final float Z_FROM_CENTER = 0.459375F;
	private static final float BG_WIDTH = 1.4F;
	private static final float BG_HEIGHT = 1.6F;
	private static final float TEXT_MARGIN = 0.05F;
	private static final float TEXT_SCALE = 0.6F;

	private static final boolean USE_CUSTOM_FONT = true;

	private static final FontType SELECTED_FONT = FontType.SOURCE_HAN;
	private static final int FONT_SIZE = 92;
	/** MTR 的 Render 加载早于 BlockEntity，且字体纹理有缓存，需定时强制刷新 */
	private static final long REFRESH_INTERVAL_MS = 1000L;

	private long lastRefreshTime = 0;
	/** 调试：记录每个站名牌最近一次的解析状态 (posKey → stateKey)，仅在状态变化时输出日志 */
	private static final Map<Long, String> lastDebugStates = new HashMap<>();

	static {
		CustomFontTextureCache.instance.selectedFont = SELECTED_FONT;
		CustomFontTextureCache.instance.fontSize = FONT_SIZE;
	}

	public RenderCRTStationName1(Argument dispatcher, boolean showLogo) {
		super(dispatcher);
	}

	@Override
	public void render(BlockCRTStationName1.BlockEntity entity, float tickDelta, GraphicsHolder graphicsHolder, int light, int overlay) {
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

	private void renderSide(StoredMatrixTransformations base, World world, BlockPos pos, BlockState state, Direction facing, String stationName, int stationColor, int shadingColor, int light, BlockCRTStationName1.BlockEntity entity) {
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
			Init.LOGGER.error("RenderCRTStationName1: Error resolving platform name at {}", pos.toShortString(), e);
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
			textureId = CustomFontTextureCache.instance.getSignTexture(stationName, themeColor, routeColor, routeNumber, platformNumber, drawAspect, SELECTED_FONT);
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

		if (!stateKey.equals(lastDebugStates.get(posKey))) {
			lastDebugStates.put(posKey, stateKey);
			Init.LOGGER.info(
				"[CRT-DEBUG] Pos={} | platformId={} | platformFound={} | routesCount={} | " +
				"routeColor={} | routeNumber={} | syncedRouteNumber={} | synced={} | platformName={} | stationName={} | stationColor={} | textureId={}",
				pos.toShortString(),
				savedPlatformId,
				(debugPlatform != null),
				routesCount,
				debugRouteColorHex,
				debugRouteNum,
				syncedRouteNumber,
				debugRouteSynced,
				platName,
				stationName,
				stationColorHex,
				textureId
			);
		}
	}
}