package com.Nanbin.client.Render;

import com.Nanbin.Init;
import com.Nanbin.Registry.RegBlock.BlockCRTStationInfo1;
import com.Nanbin.client.Drawing.CustomFontTextureCache;
import com.Nanbin.client.Drawing.CustomFontTextureCache.FontType;
import com.Nanbin.client.Drawing.WebImageCache;
import com.Nanbin.client.JavaScriptSupport.JSSignConfig;
import it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import mtr.block.IBlock;
import mtr.client.ClientData;
import mtr.client.IDrawing;
import mtr.data.Platform;
import mtr.data.Route;
import mtr.data.Station;
import mtr.mappings.UtilitiesClient;
import mtr.render.MoreRenderLayers;
import mtr.render.RenderRailwaySign;
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
 * CRT 站台信息屏（单面）渲染器：顶部条带（灰色矩形 + 本站名 + 上一站名 + 线路指示）、
 * 线路图、网络图片、线路色矩形与 2 行 × 7 格指示牌内容。
 * 只在最左上（LEFT + UPPER）方块上绘制一次，避免 9 个方块重复渲染。
 */
public class RenderCRTStationInfo1 implements BlockEntityRenderer<BlockCRTStationInfo1.BlockEntity> {

	private static final FontType FONT_TYPE = FontType.ALIBABA;
	private static final int FONT_SIZE = 60;
	/** 英文行字号 = 中文行字号 × ratio，英文要比中文小很多。 */
	private static final float LATIN_FONT_RATIO = 0.45F;
	/** 本站名中/拉两行间距 = fontSize × ratio */
	private static final float GAP_RATIO = 0.04F;
	private static final int TEXT_COLOR = 0xFF000000;

	/** 顶部条带：3 块宽 × 0.5 块高（UPPER 半块 y 0..8） */
	private static final float BAND_WIDTH = 3.0F;
	private static final float BAND_HEIGHT = 0.5F;
	private static final float MAX_MARGIN = 0.06F;

	/** 绘制面 Z 偏移（正面）。面板碰撞箱 z 为 6..10，半厚 2px = 0.125，文字画在模型表面之外 */
	private static final float Z_FACE = -(0.125F + 0.003125F);

	// ---- 左侧半格灰色实心矩形 + 上一站名 ----
	private static final int RECT_COLOR = 0xFF929498;
	/** 矩形长 0.9F，宽 0.1F（9:1），左缘距模型边缘 0.2F */
	private static final float RECT_LENGTH = 0.9F;
	private static final float RECT_HEIGHT = 0.1F;
	private static final float RECT_EDGE_GAP = 0.2F;
	/** 右侧线路指示（矩形+等腰三角+下一站名）整体距条带右缘 0.2F */
	private static final float RIGHT_EDGE_GAP = 0.2F;

	/** 正面线路图：固定贴图 route_map.png，强制 4:3（宽 1.6F × 高 1.2F） */
	private static final Identifier ROUTE_MAP_IDENTIFIER = new Identifier("mtr", "textures/texture/route_map.png");
	private static final float ROUTE_MAP_LEFT = -1.5F + 0.2F;
	private static final float ROUTE_MAP_TOP = 0.25F;
	private static final float ROUTE_MAP_WIDTH = 1.6F;
	private static final float ROUTE_MAP_HEIGHT = 1.2F;
	/** 网络图片区：紧贴线路图右缘，高与线路图相同，宽 1.0F */
	private static final float WEB_IMAGE_LEFT = ROUTE_MAP_LEFT + ROUTE_MAP_WIDTH;
	private static final float WEB_IMAGE_TOP = ROUTE_MAP_TOP;
	private static final float WEB_IMAGE_WIDTH = 1.0F;
	private static final float WEB_IMAGE_HEIGHT = 1.2F;
	/** 线路色实心矩形：紧贴线路图下方，0.3F 宽 × 0.7F 高 */
	private static final float LINE_RECT_LEFT = -1.5F + 0.3F;
	private static final float LINE_RECT_TOP = ROUTE_MAP_TOP + ROUTE_MAP_HEIGHT;
	private static final float LINE_RECT_WIDTH = 0.3F;
	private static final float LINE_RECT_HEIGHT = 0.7F;
	/** 双格指示牌内容：紧贴线路色矩形右边，每格 0.3F、格子间无间隔 */
	private static final float DOUBLE_SIGN_LEFT = LINE_RECT_LEFT + LINE_RECT_WIDTH;
	private static final float DOUBLE_SIGN_TOP = LINE_RECT_TOP;
	private static final float DOUBLE_SIGN_CELL = 0.3F;
	/** 上一站中英文字号（比主站名小） */
	private static final int PREV_FONT_SIZE = 28;
	private static final float PREV_MAX_WIDTH = 1.0F;
	private static final float PREV_MAX_HEIGHT = 0.18F;

	private static final int MAX_LIGHT = 15728880;

	/** MTR 的 Render 加载早于 BlockEntity，且字体纹理有缓存，需定时强制刷新 */
	private static final long REFRESH_INTERVAL_MS = 1000L;

	private long lastRefreshTime = 0;

	public RenderCRTStationInfo1(BlockEntityRendererFactory.Context context) {
	}

	@Override
	public void render(BlockCRTStationInfo1.BlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
		try {
			renderInner(entity, matrices, vertexConsumers, light, overlay);
		} catch (Exception e) {
			Init.LOGGER.error("RenderCRTStationInfo1: render failed at {}", entity.getPos(), e);
		}
	}

	private void renderInner(BlockCRTStationInfo1.BlockEntity entity, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
		// 字体纹理被缓存不会随数据变化自动失效，每秒强制刷新一次
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
		if (!(state.getBlock() instanceof BlockCRTStationInfo1)) {
			return;
		}

		// 只在最左上 UPPER 方块渲染，避免 9 个方块重复绘制
		final IBlock.EnumSide side = IBlock.getStatePropertySafe(state, IBlock.SIDE_EXTENDED);
		if (side != IBlock.EnumSide.LEFT || IBlock.getStatePropertySafe(state, IBlock.HALF) != DoubleBlockHalf.UPPER) {
			return;
		}

		final Direction facing = IBlock.getStatePropertySafe(state, BlockCRTStationInfo1.FACING);
		final int shadingColor = RenderRouteBase.getShadingColor(facing, 0xFFFFFFFF);

		// 条带中心：左上 UPPER 方块中心向 rotateYClockwise 方向偏移 1 格（3 块宽的中间），y 为该半块中心
		// vanilla 的 BlockEntityRenderDispatcher 已将 matrices 平移到本方块坐标原点，因此用相对坐标
		final Direction right = facing.rotateYClockwise();
		final StoredMatrixTransformations baseMatrix = new StoredMatrixTransformations();
		baseMatrix.add(graphics -> {
			graphics.translate(0.5 + right.getOffsetX(), 0.25, 0.5 + right.getOffsetZ());
			UtilitiesClient.rotateYDegrees(graphics, -facing.asRotation());
			UtilitiesClient.rotateZDegrees(graphics, 180.0F);
		});

		final StoredMatrixTransformations matrix = baseMatrix.copy();
		matrix.add(graphics -> graphics.translate(0, 0, Z_FACE));

		// 本站名 / 上一站 / 下一站 / 线路色
		final Station station = findStationForRender(pos);
		final String stationName = station != null ? station.name : "";
		final String previousStationName = findPreviousStationName(entity);
		final String nextStationName = findNextStationName(entity);
		final int lineColor = findLineColor(entity);

		final Identifier bandTexture = CustomFontTextureCache.instance.getStationInfoBandTexture(
				stationName, previousStationName, nextStationName, lineColor, entity.isFlip(),
				FONT_TYPE, FONT_SIZE, LATIN_FONT_RATIO, GAP_RATIO, TEXT_COLOR, RECT_COLOR,
				BAND_WIDTH, BAND_HEIGHT,
				RECT_EDGE_GAP, RECT_LENGTH, RECT_HEIGHT, RIGHT_EDGE_GAP, MAX_MARGIN,
				PREV_FONT_SIZE, PREV_MAX_WIDTH, PREV_MAX_HEIGHT
		);
		drawTexture(matrices, vertexConsumers, matrix, bandTexture, false, false, -BAND_WIDTH / 2.0F, -BAND_HEIGHT / 2.0F, BAND_WIDTH, BAND_HEIGHT, facing, shadingColor, light);

		// 正面线路图：固定贴图 4:3，发光绘制
		drawTexture(matrices, vertexConsumers, matrix, ROUTE_MAP_IDENTIFIER, true, false, ROUTE_MAP_LEFT, ROUTE_MAP_TOP, ROUTE_MAP_WIDTH, ROUTE_MAP_HEIGHT, facing, -1, MAX_LIGHT);

		// 网络图片区：URL 非空时异步下载并渲染（下载完成前保持透明）
		final String imageUrl = entity.getUrl();
		if (imageUrl != null && !imageUrl.isEmpty()) {
			WebImageCache.instance.request(imageUrl);
			final Identifier webTexture = WebImageCache.instance.get(imageUrl);
			if (webTexture != null) {
				drawTexture(matrices, vertexConsumers, matrix, webTexture, true, false, WEB_IMAGE_LEFT, WEB_IMAGE_TOP, WEB_IMAGE_WIDTH, WEB_IMAGE_HEIGHT, facing, -1, MAX_LIGHT);
			}
		}

		// 线路色实心矩形
		drawTexture(matrices, vertexConsumers, matrix, CustomFontTextureCache.instance.getSolidColorTexture(lineColor), false, false, LINE_RECT_LEFT, LINE_RECT_TOP, LINE_RECT_WIDTH, LINE_RECT_HEIGHT, facing, shadingColor, light);

		// 2 行 × 7 格指示牌内容
		final String[][] signIds = entity.getSignIds();
		if (signIds == null || allEmpty(signIds)) {
			return;
		}

		final List<LongAVLTreeSet> selectedIds = entity.getSelectedIds();
		matrix.transform(matrices);
		try {
			for (int i = 0; i < signIds.length; i++) {
				final String[] lineIds = signIds[i];
				final LongAVLTreeSet lineSelected = i < selectedIds.size() ? selectedIds.get(i) : new LongAVLTreeSet();
				// 整行 JS 样式：第 0 格为样式标记时整行由脚本渲染
				final String lineStyleScriptId = JSSignConfig.getStyleScriptId(lineIds);
				if (lineStyleScriptId != null) {
					final Map<Long, String> routeNumberMap = new HashMap<>();
					for (final long platformId : lineSelected) {
						final String number = entity.getRouteNumber(platformId);
						if (number != null && !number.isEmpty()) {
							routeNumberMap.put(platformId, number);
						}
					}
					RenderCRTRailwaySign.renderJSStyleLine(lineStyleScriptId, matrices, vertexConsumers, pos, lineIds, lineSelected, entity.getRouteNumbers(), facing, 0, DOUBLE_SIGN_CELL, DOUBLE_SIGN_LEFT, DOUBLE_SIGN_TOP + i * DOUBLE_SIGN_CELL, true, routeNumberMap);
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
							DOUBLE_SIGN_CELL, DOUBLE_SIGN_CELL, lineSelected, entity.getRouteNumbers(), facing, 0,
							(textureId, x, y, size, flipTexture) -> drawTexturedRect(matrices, vertexConsumers, textureId, x, y, size, size, flipTexture, facing),
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

	/** 根据第 0 行的站台选择确定线路，取该线路当前站的后一站站名（环线时末站回到首站）。 */
	private static String findNextStationName(BlockCRTStationInfo1.BlockEntity entity) {
		final Route route = findFirstSelectedRoute(entity);
		if (route == null) {
			return null;
		}
		final long platformId = getFirstLineSelected(entity).firstLong();
		final int index = route.getPlatformIdIndex(platformId);
		if (index < 0) {
			return null;
		}
		if (index + 1 < route.platformIds.size()) {
			return getStationName(route.platformIds.get(index + 1).platformId);
		}
		if (route.circularState != Route.CircularState.NONE && !route.platformIds.isEmpty()) {
			// 环线：末站的后一站是首站
			return getStationName(route.platformIds.get(0).platformId);
		}
		return null;
	}

	/** 根据第 0 行的站台选择确定线路，取该线路当前站的前一站站名（环线时首站回到末站）。 */
	private static String findPreviousStationName(BlockCRTStationInfo1.BlockEntity entity) {
		final Route route = findFirstSelectedRoute(entity);
		if (route == null) {
			return null;
		}
		final long platformId = getFirstLineSelected(entity).firstLong();
		final int index = route.getPlatformIdIndex(platformId);
		if (index > 0) {
			return getStationName(route.platformIds.get(index - 1).platformId);
		}
		if (index == 0 && route.circularState != Route.CircularState.NONE && !route.platformIds.isEmpty()) {
			// 环线：首站的前一站是末站
			return getStationName(route.platformIds.get(route.platformIds.size() - 1).platformId);
		}
		return null;
	}

	/** 解析选中平台所在线路的颜色（ARGB）；无线路时返回灰色。 */
	private static int findLineColor(BlockCRTStationInfo1.BlockEntity entity) {
		final Route route = findFirstSelectedRoute(entity);
		return route == null ? RECT_COLOR : route.color | 0xFF000000;
	}

	private static Route findFirstSelectedRoute(BlockCRTStationInfo1.BlockEntity entity) {
		final LongAVLTreeSet firstLineSelected = getFirstLineSelected(entity);
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

	private static LongAVLTreeSet getFirstLineSelected(BlockCRTStationInfo1.BlockEntity entity) {
		final List<LongAVLTreeSet> selectedIds = entity.getSelectedIds();
		if (selectedIds == null || selectedIds.isEmpty()) {
			return new LongAVLTreeSet();
		}
		return selectedIds.get(0);
	}

	private static String getStationName(long platformId) {
		final Station station = ClientData.DATA_CACHE.platformIdToStation.get(platformId);
		return station == null ? null : station.name;
	}

	/** 优先精确匹配方块所在站区域（MTR 车站包围盒）；找不到返回 null。 */
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
	public boolean rendersOutsideBoundingBox(BlockCRTStationInfo1.BlockEntity entity) {
		return true;
	}
}
