package com.Nanbin.client.Render;

import com.Nanbin.InitClient;
import com.Nanbin.Registry.RegBlock.BlockOrdinaryStationName;
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

import static org.mtr.mod.InitClient.findStation;


public class RenderOrdinaryStationName extends BlockEntityRenderer<BlockOrdinaryStationName.BlockEntity> implements IBlock, IGui, IDrawing {

	private static final float Z_FROM_CENTER = 0.459375F;
	private static final float BG_WIDTH = 1.5F;
	private static final float BG_HEIGHT = 1.7F;
	private static final float TEXT_MARGIN = 0.05F;
	private static final float TEXT_SCALE = 0.6F;

	private static final boolean USE_CUSTOM_FONT = true;

	// ---- CRT 告示牌渲染数据：白底黑字，ALIBABA 字体，等比缩放 ----
	private static final FontType FONT_TYPE = FontType.ALIBABA;
	private static final FontType FONT_TYPE_2 = FontType.SOURCE_SANS_3; // 圆内文字字体（仅纯数字用 Source Sans 3，其余用主字体 FONT_TYPE）
	private static final boolean WHITE_BACKGROUND = true;          // 默认白色背景
	private static final int FONT_SIZE = 81;
	private static final float TOP_BAR_END = 0F;          // 无顶条（白底）
	private static final float TEXT_END = 0.62F;          // 文字区底缘（相对 H）
	private static final float MIDDLE_BAR_START = 0F;     // 无中间条
	private static final float MIDDLE_BAR_END = 0F;
	private static final float MIDDLE_BAR_WIDTH = 0F;
	private static final float BOTTOM_BAR_START = 1F;     // 无底条
	// ---- 文字排版参数：翻译小一些、汉文不动、间距统一，超框优先等比缩放 ----
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

	public RenderOrdinaryStationName(Argument dispatcher) {
		super(dispatcher);
	}

	@Override
	public void render(BlockOrdinaryStationName.BlockEntity entity, float tickDelta, GraphicsHolder graphicsHolder, int light, int overlay) {
		// 定时强制刷新字体纹理缓存，确保站名、间距等显示最新效果
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

	private void renderSide(StoredMatrixTransformations base, World world, BlockPos pos, BlockState state, Direction facing, String stationName, int stationColor, int shadingColor, int light, BlockOrdinaryStationName.BlockEntity entity) {
		final StoredMatrixTransformations matrix = base.copy();
		matrix.add(graphics -> graphics.translate(0, 0, Z_FROM_CENTER));

		// Resolve route/platform data from saved platform ID
		String platformNumber = "";

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
			InitClient.LOGGER.error("RenderCRTOrdinaryRailwaySign: Error resolving platform name at {}", pos.toShortString(), e);
		}

		// Texture coordinates
		final float drawAspect = (BG_WIDTH - TEXT_MARGIN * 2) / (BG_HEIGHT - TEXT_MARGIN * 2);
		final float textX = (-BG_WIDTH / 2.0F + TEXT_MARGIN) * TEXT_SCALE;
		final float textY = (-BG_HEIGHT / 2.0F + TEXT_MARGIN) * TEXT_SCALE;
		final float textWidth = (BG_WIDTH - TEXT_MARGIN * 2) * TEXT_SCALE;
		final float textHeight = (BG_HEIGHT - TEXT_MARGIN * 2) * TEXT_SCALE;

		final Identifier textureId;
		if (USE_CUSTOM_FONT) {
			textureId = CustomFontTextureCache.instance.getSignTexture(stationName, themeColor, routeColor, routeNumber, platformNumber, drawAspect, FONT_TYPE, FONT_SIZE, FONT_TYPE_2, TOP_BAR_END, TEXT_END, MIDDLE_BAR_START, MIDDLE_BAR_END, MIDDLE_BAR_WIDTH, BOTTOM_BAR_START, LATIN_FONT_RATIO, GAP_RATIO, VERTICAL_BIAS, EXTRA_OFFSET_RATIO, TEXT_MARGIN_DEFAULT, TEXT_MARGIN_MEDIUM, TEXT_MARGIN_SHORT, CIRCLE_CENTER_Y_OFFSET, CIRCLE_RADIUS_H_RATIO, CIRCLE_RADIUS_W_RATIO, STROKE_H_RATIO, CIRCLE_FONT_RATIO, INVERTED_CIRCLE, WHITE_BACKGROUND);
		} else {
			textureId = DynamicTextureCache.instance.getStationName(stationName, BG_WIDTH).identifier;
		}

		MainRenderer.scheduleRender(textureId, false, QueuedRenderLayer.EXTERIOR, (graphicsHolder, offset) -> {
			matrix.transform(graphicsHolder, offset);
			IDrawing.drawTexture(graphicsHolder, textX, textY, textWidth, textHeight, 0F, 0F, 1F, 1F, facing, shadingColor, light);
			graphicsHolder.pop();
		});
	}
}