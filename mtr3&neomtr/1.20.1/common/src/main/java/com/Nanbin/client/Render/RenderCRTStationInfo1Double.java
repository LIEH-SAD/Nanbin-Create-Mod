package com.Nanbin.client.Render;

import com.Nanbin.Init;
import com.Nanbin.Registry.RegBlock.BlockCRTStationInfo1;
import com.Nanbin.Registry.RegBlock.BlockCRTStationInfo1Double;
import com.Nanbin.client.Drawing.CustomFontTextureCache;
import com.Nanbin.client.Drawing.CustomFontTextureCache.FontType;
import com.Nanbin.client.Drawing.WebImageCache;
import com.Nanbin.client.JavaScriptSupport.JSSignConfig;
import it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import mtr.block.IBlock;
import mtr.client.ClientData;
import mtr.client.IDrawing;
import mtr.data.Route;
import mtr.data.Station;
import mtr.mappings.UtilitiesClient;
import mtr.render.MoreRenderLayers;
import mtr.render.RenderRouteBase;
import mtr.render.StoredMatrixTransformations;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CRT 站台信息屏（双面）渲染器：正面与反面各自绘制顶部条带、线路图、网络图片、
 * 线路色矩形与 2 行 × 7 格指示牌内容。只在最左上（LEFT + UPPER）方块上绘制一次。
 */
public class RenderCRTStationInfo1Double implements BlockEntityRenderer<BlockCRTStationInfo1Double.BlockEntity> {

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

	private static final int MAX_LIGHT = 15728880;

	private static final long REFRESH_INTERVAL_MS = 1000L;

	private long lastRefreshTime = 0;

	public RenderCRTStationInfo1Double(BlockEntityRendererFactory.Context context) {
	}

	@Override
	public void render(BlockCRTStationInfo1Double.BlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
		try {
			renderInner(entity, matrices, vertexConsumers, light, overlay);
		} catch (Exception e) {
			Init.LOGGER.error("RenderCRTStationInfo1Double: render failed at {}", entity.getPos(), e);
		}
	}

	private void renderInner(BlockCRTStationInfo1Double.BlockEntity entity, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
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
		if (!(state.getBlock() instanceof BlockCRTStationInfo1Double)) {
			return;
		}

		final IBlock.EnumSide side = IBlock.getStatePropertySafe(state, IBlock.SIDE_EXTENDED);
		if (side != IBlock.EnumSide.LEFT || IBlock.getStatePropertySafe(state, IBlock.HALF) != DoubleBlockHalf.UPPER) {
			return;
		}

		final Direction facing = IBlock.getStatePropertySafe(state, BlockCRTStationInfo1.FACING);
		final Direction right = facing.rotateYClockwise();

		renderSide(entity, pos, facing, right, true, matrices, vertexConsumers, light);
		renderSide(entity, pos, facing, right, false, matrices, vertexConsumers, light);
	}

	private void renderSide(BlockCRTStationInfo1Double.BlockEntity entity, BlockPos pos, Direction facing, Direction right, boolean front, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
		final Direction renderFacing = front ? facing : facing.getOpposite();
		final int shadingColor = RenderRouteBase.getShadingColor(renderFacing, 0xFFFFFFFF);

		final StoredMatrixTransformations baseMatrix = new StoredMatrixTransformations();
		baseMatrix.add(graphics -> {
			graphics.translate(0.5 + right.getOffsetX(), 0.25, 0.5 + right.getOffsetZ());
			UtilitiesClient.rotateYDegrees(graphics, -renderFacing.asRotation());
			UtilitiesClient.rotateZDegrees(graphics, 180.0F);
		});

		final StoredMatrixTransformations matrix = baseMatrix.copy();
		matrix.add(graphics -> graphics.translate(0, 0, Z_FACE));

		final Station station = findStationForRender(pos);
		final String stationName = station != null ? station.name : "";
		final String previousStationName = findPreviousStationName(entity, front);
		final String nextStationName = findNextStationName(entity, front);
		final int lineColor = findLineColor(entity, front);

		final Identifier bandTexture = CustomFontTextureCache.instance.getStationInfoBandTexture(
				stationName, previousStationName, nextStationName, lineColor, entity.isFlip(front),
				FONT_TYPE, FONT_SIZE, LATIN_FONT_RATIO, GAP_RATIO, TEXT_COLOR, RECT_COLOR,
				BAND_WIDTH, BAND_HEIGHT,
				RECT_EDGE_GAP, RECT_LENGTH, RECT_HEIGHT, RIGHT_EDGE_GAP, MAX_MARGIN,
				PREV_FONT_SIZE, PREV_MAX_WIDTH, PREV_MAX_HEIGHT
		);
		drawTexture(matrices, vertexConsumers, matrix, bandTexture, false, false, -BAND_WIDTH / 2.0F, -BAND_HEIGHT / 2.0F, BAND_WIDTH, BAND_HEIGHT, renderFacing, shadingColor, light);

		drawTexture(matrices, vertexConsumers, matrix, ROUTE_MAP_IDENTIFIER, true, false, ROUTE_MAP_LEFT, ROUTE_MAP_TOP, ROUTE_MAP_WIDTH, ROUTE_MAP_HEIGHT, renderFacing, -1, MAX_LIGHT);

		final String imageUrl = entity.getUrl(front);
		if (imageUrl != null && !imageUrl.isEmpty()) {
			WebImageCache.instance.request(imageUrl);
			final Identifier webTexture = WebImageCache.instance.get(imageUrl);
			if (webTexture != null) {
				drawTexture(matrices, vertexConsumers, matrix, webTexture, true, false, WEB_IMAGE_LEFT, WEB_IMAGE_TOP, WEB_IMAGE_WIDTH, WEB_IMAGE_HEIGHT, renderFacing, -1, MAX_LIGHT);
			}
		}

		drawTexture(matrices, vertexConsumers, matrix, CustomFontTextureCache.instance.getSolidColorTexture(lineColor), false, false, LINE_RECT_LEFT, LINE_RECT_TOP, LINE_RECT_WIDTH, LINE_RECT_HEIGHT, renderFacing, shadingColor, light);

		final String[][] signIds = entity.getSignIds(front);
		if (signIds == null || allEmpty(signIds)) {
			return;
		}

		final List<LongAVLTreeSet> selectedIds = entity.getSelectedIds(front);
		matrix.transform(matrices);
		try {
			for (int i = 0; i < signIds.length; i++) {
				final String[] lineIds = signIds[i];
				final LongAVLTreeSet lineSelected = i < selectedIds.size() ? selectedIds.get(i) : new LongAVLTreeSet();
				final String lineStyleScriptId = JSSignConfig.getStyleScriptId(lineIds);
				if (lineStyleScriptId != null) {
					final Map<Long, String> routeNumberMap = new HashMap<>();
					for (final long platformId : lineSelected) {
						final String number = entity.getRouteNumber(platformId, front);
						if (number != null && !number.isEmpty()) {
							routeNumberMap.put(platformId, number);
						}
					}
					RenderCRTRailwaySign.renderJSStyleLine(lineStyleScriptId, matrices, vertexConsumers, pos, lineIds, lineSelected, entity.getRouteNumbers(front), renderFacing, 0, DOUBLE_SIGN_CELL, DOUBLE_SIGN_LEFT, DOUBLE_SIGN_TOP + i * DOUBLE_SIGN_CELL, true, routeNumberMap);
					continue;
				}
				for (int j = 0; j < lineIds.length; j++) {
					final String signId = lineIds[j];
					if (signId == null) {
						continue;
					}
					final float cellX = DOUBLE_SIGN_LEFT + j * DOUBLE_SIGN_CELL;
					final float cellY = DOUBLE_SIGN_TOP + i * DOUBLE_SIGN_CELL;
					RenderCRTRailwaySign.drawSignCRT(matrices, vertexConsumers, pos, signId, cellX, cellY, DOUBLE_SIGN_CELL,
							DOUBLE_SIGN_CELL, DOUBLE_SIGN_CELL, lineSelected, entity.getRouteNumbers(front), renderFacing, 0,
							(textureId, x, y, size, flipTexture) -> drawTexturedRect(matrices, vertexConsumers, textureId, x, y, size, size, flipTexture, renderFacing),
							true, lineIds, 16777215);
				}
			}
		} finally {
			matrices.pop();
		}
	}

	private static boolean allEmpty(String[][] signIds) {
		for (final String[] lineIds : signIds) {
			if (lineIds != null) {
				for (final String id : lineIds) {
					if (id != null && !id.isEmpty()) {
						return false;
					}
				}
			}
		}
		return true;
	}

	private static void drawTexture(MatrixStack matrices, VertexConsumerProvider vertexConsumers, StoredMatrixTransformations matrix, Identifier texture, boolean lightLayer, boolean translucent, float x, float y, float width, float height, Direction facing, int color, int light) {
		matrix.transform(matrices);
		final RenderLayer renderLayer = lightLayer ? MoreRenderLayers.getLight(texture, translucent) : MoreRenderLayers.getExterior(texture);
		IDrawing.drawTexture(matrices, vertexConsumers.getBuffer(renderLayer), x, y, width, height, 0F, 0F, 1F, 1F, facing, color, light);
		matrices.pop();
	}

	private static void drawTexturedRect(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Identifier textureId, float x, float y, float width, float height, boolean flip, Direction facing) {
		final VertexConsumer vertexConsumer = vertexConsumers.getBuffer(MoreRenderLayers.getLight(textureId, true));
		IDrawing.drawTexture(matrices, vertexConsumer, x, y, width, height, flip ? 1.0F : 0.0F, 0.0F, flip ? 0.0F : 1.0F, 1.0F, facing, -1, MAX_LIGHT);
	}

	private static String findNextStationName(BlockCRTStationInfo1Double.BlockEntity entity, boolean front) {
		final Route route = findFirstSelectedRoute(entity, front);
		if (route == null) {
			return null;
		}
		final int index = route.getPlatformIdIndex(getFirstLineSelected(entity, front).firstLong());
		if (index < 0) {
			return null;
		}
		if (index + 1 < route.platformIds.size()) {
			return getStationName(route.platformIds.get(index + 1).platformId);
		}
		if (route.circularState != Route.CircularState.NONE && !route.platformIds.isEmpty()) {
			return getStationName(route.platformIds.get(0).platformId);
		}
		return null;
	}

	private static String findPreviousStationName(BlockCRTStationInfo1Double.BlockEntity entity, boolean front) {
		final Route route = findFirstSelectedRoute(entity, front);
		if (route == null) {
			return null;
		}
		final int index = route.getPlatformIdIndex(getFirstLineSelected(entity, front).firstLong());
		if (index > 0) {
			return getStationName(route.platformIds.get(index - 1).platformId);
		}
		if (index == 0 && route.circularState != Route.CircularState.NONE && !route.platformIds.isEmpty()) {
			return getStationName(route.platformIds.get(route.platformIds.size() - 1).platformId);
		}
		return null;
	}

	private static int findLineColor(BlockCRTStationInfo1Double.BlockEntity entity, boolean front) {
		final Route route = findFirstSelectedRoute(entity, front);
		return route == null ? RECT_COLOR : route.color | 0xFF000000;
	}

	private static Route findFirstSelectedRoute(BlockCRTStationInfo1Double.BlockEntity entity, boolean front) {
		final LongAVLTreeSet firstLineSelected = getFirstLineSelected(entity, front);
		if (firstLineSelected.isEmpty()) {
			return null;
		}
		final long platformId = firstLineSelected.firstLong();
		for (final Route route : ClientData.ROUTES) {
			if (route.getPlatformIdIndex(platformId) >= 0) {
				return route;
			}
		}
		return null;
	}

	private static LongAVLTreeSet getFirstLineSelected(BlockCRTStationInfo1Double.BlockEntity entity, boolean front) {
		final List<LongAVLTreeSet> selectedIds = entity.getSelectedIds(front);
		if (selectedIds == null || selectedIds.isEmpty()) {
			return new LongAVLTreeSet();
		}
		return selectedIds.get(0);
	}

	private static String getStationName(long platformId) {
		final Station station = ClientData.DATA_CACHE.platformIdToStation.get(platformId);
		return station == null ? null : station.name;
	}

	private static Station findStationForRender(BlockPos blockPos) {
		final Station direct = ClientData.DATA_CACHE.blockPosToStation.get(blockPos);
		if (direct != null) {
			return direct;
		}
		for (final Station station : ClientData.STATIONS) {
			if (station.inArea(blockPos.getX(), blockPos.getZ())) {
				return station;
			}
		}
		return null;
	}

	@Override
	public boolean rendersOutsideBoundingBox(BlockCRTStationInfo1Double.BlockEntity entity) {
		return true;
	}
}
