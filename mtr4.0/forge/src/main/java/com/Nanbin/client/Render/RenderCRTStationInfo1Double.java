package com.Nanbin.client.Render;

import com.Nanbin.InitClient;
import com.Nanbin.Registry.RegBlock.BlockCRTStationInfo1Double;
import com.Nanbin.client.Drawing.CustomFontTextureCache;
import com.Nanbin.client.Drawing.CustomFontTextureCache.FontType;
import com.Nanbin.client.Drawing.WebImageCache;
import com.Nanbin.client.JavaScriptSupport.JSSignConfig;
import org.mtr.core.data.Position;
import org.mtr.core.data.Route;
import org.mtr.core.data.SimplifiedRoute;
import org.mtr.core.data.Station;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockEntityRenderer;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mod.block.IBlock;
import org.mtr.mod.client.IDrawing;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.data.IGui;
import org.mtr.mod.generated.lang.TranslationProvider;
import org.mtr.mod.render.MainRenderer;
import org.mtr.mod.render.QueuedRenderLayer;
import org.mtr.mod.render.RenderRouteBase;
import org.mtr.mod.render.StoredMatrixTransformations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * StationInfo2 双面版本渲染器。正面和背面各自独立渲染，数据不共用。
 */
public class RenderCRTStationInfo1Double extends BlockEntityRenderer<BlockCRTStationInfo1Double.BlockEntity> implements IBlock, IGui, IDrawing {

	private static final FontType FONT_TYPE = FontType.ALIBABA;
	private static final int FONT_SIZE = 60;
	private static final float LATIN_FONT_RATIO = 0.45F;
	private static final float GAP_RATIO = 0.04F;
	private static final int TEXT_COLOR = 0xFF000000;

	private static final float BAND_WIDTH = 3.0F;
	private static final float BAND_HEIGHT = 0.5F;
	private static final float MAX_MARGIN = 0.06F;

	private static final float Z_FACE = -(0.125F + 0.003125F);

	private static final int RECT_COLOR = 0xFF929498;
	private static final float RECT_LENGTH = 0.9F;
	private static final float RECT_HEIGHT = 0.1F;
	private static final float RECT_EDGE_GAP = 0.2F;
	private static final float RIGHT_EDGE_GAP = 0.2F;

	private static final Identifier ROUTE_MAP_IDENTIFIER = new Identifier("mtr", "textures/texture/route_map.png");
	private static final float ROUTE_MAP_LEFT = -1.5F + 0.2F;
	private static final float ROUTE_MAP_TOP = 0.25F;
	private static final float ROUTE_MAP_WIDTH = 1.6F;
	private static final float ROUTE_MAP_HEIGHT = 1.2F;
	private static final float WEB_IMAGE_LEFT = ROUTE_MAP_LEFT + ROUTE_MAP_WIDTH;
	private static final float WEB_IMAGE_TOP = ROUTE_MAP_TOP;
	private static final float WEB_IMAGE_WIDTH = 1.0F;
	private static final float WEB_IMAGE_HEIGHT = 1.2F;
	private static final float LINE_RECT_LEFT = -1.5F + 0.3F;
	private static final float LINE_RECT_TOP = ROUTE_MAP_TOP + ROUTE_MAP_HEIGHT;
	private static final float LINE_RECT_WIDTH = 0.3F;
	private static final float LINE_RECT_HEIGHT = 0.7F;
	private static final float DOUBLE_SIGN_LEFT = LINE_RECT_LEFT + LINE_RECT_WIDTH;
	private static final float DOUBLE_SIGN_TOP = LINE_RECT_TOP;
	private static final float DOUBLE_SIGN_CELL = 0.3F;
	private static final int PREV_FONT_SIZE = 28;
	private static final float PREV_MAX_WIDTH = 1.0F;
	private static final float PREV_MAX_HEIGHT = 0.18F;

	private static final long REFRESH_INTERVAL_MS = 1000L;

	private long lastRefreshTime = 0;

	public RenderCRTStationInfo1Double(Argument dispatcher) {
		super(dispatcher);
	}

	@Override
	public void render(BlockCRTStationInfo1Double.BlockEntity entity, float tickDelta, GraphicsHolder graphicsHolder, int light, int overlay) {
		final long now = System.currentTimeMillis();
		if (now - lastRefreshTime >= REFRESH_INTERVAL_MS) {
			lastRefreshTime = now;
			CustomFontTextureCache.instance.clearFittedTextureCache();
		}

		final World world = entity.getWorld2();
		if (world == null) {
			return;
		}
		final BlockPos pos = entity.getPos2();
		final BlockState state = world.getBlockState(pos);
		if (!(state.getBlock().data instanceof BlockCRTStationInfo1Double)) {
			return;
		}

		final IBlock.EnumSide side = IBlock.getStatePropertySafe(state, IBlock.SIDE_EXTENDED);
		final IBlock.DoubleBlockHalf half = IBlock.getStatePropertySafe(state, IBlock.HALF);
		if (side != IBlock.EnumSide.LEFT || half != IBlock.DoubleBlockHalf.UPPER) {
			return;
		}

		final Direction facing = IBlock.getStatePropertySafe(state, BlockCRTStationInfo1Double.FACING);
		final Direction right = facing.rotateYClockwise();

		renderSide(entity, pos, facing, right, true, graphicsHolder, light);
		renderSide(entity, pos, facing, right, false, graphicsHolder, light);
	}

	private void renderSide(BlockCRTStationInfo1Double.BlockEntity entity, BlockPos pos, Direction facing, Direction right, boolean front, GraphicsHolder graphicsHolder, int light) {
		final Direction renderFacing = front ? facing : facing.getOpposite();
		final int shadingColor = RenderRouteBase.getShadingColor(renderFacing, 0xFFFFFFFF);

		final StoredMatrixTransformations baseMatrix = new StoredMatrixTransformations(
				pos.getX() + 0.5 + right.getOffsetX() * 1.0,
				pos.getY() + 0.25,
				pos.getZ() + 0.5 + right.getOffsetZ() * 1.0
		);
		baseMatrix.add(graphics -> {
			graphics.rotateYDegrees(-renderFacing.asRotation());
			graphics.rotateZDegrees(180);
		});

		final StoredMatrixTransformations matrix = baseMatrix.copy();
		matrix.add(graphics -> graphics.translate(0, 0, Z_FACE));

		final Station station = findStationForRender(pos);
		final String stationName = station != null ? station.getName() : TranslationProvider.GUI_MTR_UNTITLED.getString();

		final String previousStationName = findPreviousStationName(entity, front);
		final String nextStationName = findNextStationName(entity, front);
		final int lineColor = findLineColor(entity, front);

		final Identifier textureId = CustomFontTextureCache.instance.getStationInfoBandTexture(
				stationName, previousStationName, nextStationName, lineColor, entity.isFlip(front),
				FONT_TYPE, FONT_SIZE, LATIN_FONT_RATIO, GAP_RATIO, TEXT_COLOR, RECT_COLOR,
				BAND_WIDTH, BAND_HEIGHT,
				RECT_EDGE_GAP, RECT_LENGTH, RECT_HEIGHT, RIGHT_EDGE_GAP, MAX_MARGIN,
				PREV_FONT_SIZE, PREV_MAX_WIDTH, PREV_MAX_HEIGHT
		);

		MainRenderer.scheduleRender(textureId, false, QueuedRenderLayer.EXTERIOR, (graphicsHolderNew, offset) -> {
			matrix.transform(graphicsHolderNew, offset);
			IDrawing.drawTexture(graphicsHolderNew, -BAND_WIDTH / 2.0F, -BAND_HEIGHT / 2.0F, BAND_WIDTH, BAND_HEIGHT, 0F, 0F, 1F, 1F, renderFacing, shadingColor, light);
			graphicsHolderNew.pop();
		});

		MainRenderer.scheduleRender(ROUTE_MAP_IDENTIFIER, false, QueuedRenderLayer.LIGHT, (graphicsHolderNew, offset) -> {
			matrix.transform(graphicsHolderNew, offset);
			IDrawing.drawTexture(graphicsHolderNew, ROUTE_MAP_LEFT, ROUTE_MAP_TOP, ROUTE_MAP_WIDTH, ROUTE_MAP_HEIGHT, 0F, 0F, 1F, 1F, renderFacing, -1, GraphicsHolder.getDefaultLight());
			graphicsHolderNew.pop();
		});

		final String imageUrl = entity.getUrl(front);
		if (!imageUrl.isEmpty()) {
			WebImageCache.instance.request(imageUrl);
			final Identifier webTexture = WebImageCache.instance.get(imageUrl);
			if (webTexture != null) {
				MainRenderer.scheduleRender(webTexture, false, QueuedRenderLayer.LIGHT, (graphicsHolderNew, offset) -> {
					matrix.transform(graphicsHolderNew, offset);
					IDrawing.drawTexture(graphicsHolderNew, WEB_IMAGE_LEFT, WEB_IMAGE_TOP, WEB_IMAGE_WIDTH, WEB_IMAGE_HEIGHT, 0F, 0F, 1F, 1F, renderFacing, -1, GraphicsHolder.getDefaultLight());
					graphicsHolderNew.pop();
				});
			}
		}

		MainRenderer.scheduleRender(CustomFontTextureCache.instance.getSolidColorTexture(lineColor), false, QueuedRenderLayer.EXTERIOR, (graphicsHolderNew, offset) -> {
			matrix.transform(graphicsHolderNew, offset);
			IDrawing.drawTexture(graphicsHolderNew, LINE_RECT_LEFT, LINE_RECT_TOP, LINE_RECT_WIDTH, LINE_RECT_HEIGHT, 0F, 0F, 1F, 1F, renderFacing, shadingColor, light);
			graphicsHolderNew.pop();
		});

		final String[][] signIds = entity.getSignIds(front);
		if (signIds != null) {
			// Skip rendering when all cells are empty
			boolean allEmpty = true;
			for (final String[] lineIds : signIds) {
				if (lineIds != null) {
					for (final String id : lineIds) {
						if (id != null && !id.isEmpty()) {
							allEmpty = false;
							break;
						}
					}
				}
				if (!allEmpty) break;
			}
			if (allEmpty) {
				return;
			}

			final List<LongAVLTreeSet> selectedIds = entity.getSelectedIds(front);
			graphicsHolder.push();
			graphicsHolder.translate(pos.getX() + 0.5 + right.getOffsetX(), pos.getY() + 0.25, pos.getZ() + 0.5 + right.getOffsetZ());
			graphicsHolder.rotateYDegrees(-renderFacing.asRotation());
			graphicsHolder.rotateZDegrees(180);
			graphicsHolder.translate(0, 0, Z_FACE);
			for (int i = 0; i < signIds.length; i++) {
				final LongAVLTreeSet lineSelected = i < selectedIds.size() ? selectedIds.get(i) : new LongAVLTreeSet();
				final String[] lineIds = signIds[i];
				// 整行 JS 样式：第 0 格为样式标记时整行由脚本渲染
				final String lineStyleScriptId = JSSignConfig.getStyleScriptId(lineIds);
				if (lineStyleScriptId != null) {
					final Map<Long, String> routeNumberMap = new HashMap<>();
					for (final long platformId : lineSelected) {
						final String number = entity.getRouteNumber(platformId, front);
						if (number != null && !number.isEmpty()) {
							routeNumberMap.put(platformId, number);
						}
					}
					RenderCRTRailwaySign.renderJSStyleLine(lineStyleScriptId, matrix, pos, lineIds, lineSelected, entity.getRouteNumbers(front), renderFacing, 0, DOUBLE_SIGN_CELL, DOUBLE_SIGN_LEFT, DOUBLE_SIGN_TOP + i * DOUBLE_SIGN_CELL, true, routeNumberMap);
					continue;
				}
				for (int j = 0; j < lineIds.length; j++) {
					final String signId = lineIds[j];
					if (signId == null) {
						continue;
					}
					RenderCRTRailwaySign.drawSignCRT(
							graphicsHolder,
							matrix,
							pos,
							signId,
							DOUBLE_SIGN_LEFT + j * DOUBLE_SIGN_CELL,
							DOUBLE_SIGN_TOP + i * DOUBLE_SIGN_CELL,
							DOUBLE_SIGN_CELL,
							DOUBLE_SIGN_CELL,
							DOUBLE_SIGN_CELL,
							lineSelected,
							entity.getRouteNumbers(front),
							renderFacing,
							0,
							(textureId2, x2, y2, size2, flipTexture2) -> MainRenderer.scheduleRender(textureId2, true, QueuedRenderLayer.LIGHT_TRANSLUCENT, (graphicsHolderNew, offset) -> {
								matrix.transform(graphicsHolderNew, offset);
								IDrawing.drawTexture(graphicsHolderNew, x2, y2, size2, size2, flipTexture2 ? 1 : 0, 0, flipTexture2 ? 0 : 1, 1, renderFacing, -1, GraphicsHolder.getDefaultLight());
								graphicsHolderNew.pop();
							}),
							true,
							lineIds,
							16777215
					);
				}
			}
			graphicsHolder.pop();
		}
	}

	private String findNextStationName(BlockCRTStationInfo1Double.BlockEntity entity, boolean front) {
		try {
			final LongAVLTreeSet firstLineSelected = getFirstLineSelected(entity, front);
			if (firstLineSelected == null || firstLineSelected.isEmpty()) {
				return null;
			}
			final long platformId = firstLineSelected.firstLong();
			final MinecraftClientData clientData = MinecraftClientData.getInstance();
			for (final SimplifiedRoute route : clientData.simplifiedRoutes) {
				final int index = route.getPlatformIndex(platformId);
				if (index >= 0) {
					if (index + 1 < route.getPlatforms().size()) {
						return route.getPlatforms().get(index + 1).getStationName();
					} else if (route.getCircularState() != Route.CircularState.NONE && !route.getPlatforms().isEmpty()) {
						return route.getPlatforms().get(0).getStationName();
					}
					return null;
				}
			}
		} catch (Exception e) {
			InitClient.LOGGER.error("[RenderCRTStationInfo2]: failed to resolve next station", e);
		}
		return null;
	}

	private int findLineColor(BlockCRTStationInfo1Double.BlockEntity entity, boolean front) {
		try {
			final LongAVLTreeSet firstLineSelected = getFirstLineSelected(entity, front);
			if (firstLineSelected == null || firstLineSelected.isEmpty()) {
				return RECT_COLOR;
			}
			final long platformId = firstLineSelected.firstLong();
			final MinecraftClientData clientData = MinecraftClientData.getInstance();
			for (final SimplifiedRoute route : clientData.simplifiedRoutes) {
				if (route.getPlatformIndex(platformId) >= 0) {
					return route.getColor() | 0xFF000000;
				}
			}
		} catch (Exception e) {
			InitClient.LOGGER.error("[RenderCRTStationInfo2]: failed to resolve line color", e);
		}
		return RECT_COLOR;
	}

	private LongAVLTreeSet getFirstLineSelected(BlockCRTStationInfo1Double.BlockEntity entity, boolean front) {
		final List<LongAVLTreeSet> selectedIds = entity.getSelectedIds(front);
		if (selectedIds == null || selectedIds.isEmpty()) {
			return null;
		}
		return selectedIds.get(0);
	}

	private String findPreviousStationName(BlockCRTStationInfo1Double.BlockEntity entity, boolean front) {
		try {
			final LongAVLTreeSet firstLineSelected = getFirstLineSelected(entity, front);
			if (firstLineSelected == null || firstLineSelected.isEmpty()) {
				return null;
			}
			final long platformId = firstLineSelected.firstLong();
			final MinecraftClientData clientData = MinecraftClientData.getInstance();
			for (final SimplifiedRoute route : clientData.simplifiedRoutes) {
				final int index = route.getPlatformIndex(platformId);
				if (index > 0) {
					return route.getPlatforms().get(index - 1).getStationName();
				} else if (index == 0 && route.getCircularState() != Route.CircularState.NONE && !route.getPlatforms().isEmpty()) {
					return route.getPlatforms().get(route.getPlatforms().size() - 1).getStationName();
				}
			}
		} catch (Exception e) {
			InitClient.LOGGER.error("[RenderCRTStationInfo2]: failed to resolve previous station", e);
		}
		return null;
	}

	private Station findStationForRender(BlockPos blockPos) {
		final Station exact = org.mtr.mod.InitClient.findStation(blockPos);
		if (exact != null) {
			return exact;
		}
		final Station[] closeStation = {null};
		org.mtr.mod.InitClient.findClosePlatform(blockPos, 256, platform -> closeStation[0] = platform.area);
		if (closeStation[0] != null) {
			return closeStation[0];
		}
		final Position position = org.mtr.mod.Init.blockPosToPosition(blockPos);
		Station nearest = null;
		long nearestDistance = Long.MAX_VALUE;
		for (final Station station : MinecraftClientData.getInstance().stations) {
			final long distance = station.getCenter().manhattanDistance(position);
			if (distance < nearestDistance) {
				nearestDistance = distance;
				nearest = station;
			}
		}
		return nearest;
	}

	@Override
	public boolean rendersOutsideBoundingBox2(BlockCRTStationInfo1Double.BlockEntity entity) {
		return true;
	}
}