package com.Nanbin.client.Render;

import com.Nanbin.InitClient;
import com.Nanbin.Registry.RegBlock.BlockCRTStationInfo1;
import com.Nanbin.client.Drawing.CustomFontTextureCache;
import com.Nanbin.client.Drawing.CustomFontTextureCache.FontType;
import com.Nanbin.client.Drawing.WebImageCache;
import org.mtr.core.data.Position;
import org.mtr.core.data.Route;
import org.mtr.core.data.SimplifiedRoute;
import org.mtr.core.data.Station;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockEntityRenderer;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mod.Init;
import org.mtr.mod.block.IBlock;
import org.mtr.mod.client.IDrawing;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.data.IGui;
import org.mtr.mod.generated.lang.TranslationProvider;
import org.mtr.mod.render.MainRenderer;
import org.mtr.mod.render.QueuedRenderLayer;
import org.mtr.mod.render.RenderRouteBase;
import org.mtr.mod.render.StoredMatrixTransformations;

import java.util.List;

/**
 * StationInfo1 顶部 0.5F 单面站名渲染器。
 * 只在结构的最左上 UPPER 方块渲染一次：把灰色矩形 + 本站名（中文 + 英文）+ 线路上一站名
 * 合成一张纹理后，绘制在 3 块宽 × 0.5 块高的顶部条带上。
 * 中文用阿里巴巴字体大号单行（只能等比缩放），英文用很小的字号并可换行成最多两行后整体等比缩放。
 */
public class RenderCRTStationInfo1 extends BlockEntityRenderer<BlockCRTStationInfo1.BlockEntity> implements IBlock, IGui, IDrawing {

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
	/** 矩形长 0.9F，宽 0.1F（9:1），左缘距模型边缘 0.2F（原 0.1F 再右移一格 0.1F） */
	private static final float RECT_LENGTH = 0.9F;
	private static final float RECT_HEIGHT = 0.1F;
	private static final float RECT_EDGE_GAP = 0.2F;
	/** 右侧线路指示（矩形+等腰三角+下一站名）整体距条带右缘 0.2F */
	private static final float RIGHT_EDGE_GAP = 0.2F;

	/** 正面线路图：固定贴图 route_map.png，强制 4:3（宽 1.6F × 高 1.2F） */
	private static final Identifier ROUTE_MAP_IDENTIFIER = new Identifier("mtr", "textures/texture/route_map.png");
	/** 以最左边一列、中间层方块的左上角（条带坐标 x=-1.5，中间层顶部=局部 y=+0.25）向右偏移 0.2F 为左上角，向下绘制 */
	private static final float ROUTE_MAP_LEFT = -1.5F + 0.2F;
	private static final float ROUTE_MAP_TOP = 0.25F;
	private static final float ROUTE_MAP_WIDTH = 1.6F;
	private static final float ROUTE_MAP_HEIGHT = 1.2F;
	/** 网络图片区：紧贴线路图右缘（-1.3 + 1.6 = 0.3），高与线路图相同，宽 1.0F */
	private static final float WEB_IMAGE_LEFT = ROUTE_MAP_LEFT + ROUTE_MAP_WIDTH;
	private static final float WEB_IMAGE_TOP = ROUTE_MAP_TOP;
	private static final float WEB_IMAGE_WIDTH = 1.0F;
	private static final float WEB_IMAGE_HEIGHT = 1.2F;
	/** 线路色实心矩形：紧贴线路图下方，左缘距结构最左侧 0.3F（-1.5 + 0.3），向下绘制 0.3F 宽 × 0.7F 高 */
	private static final float LINE_RECT_LEFT = -1.5F + 0.3F;
	private static final float LINE_RECT_TOP = ROUTE_MAP_TOP + ROUTE_MAP_HEIGHT;
	private static final float LINE_RECT_WIDTH = 0.3F;
	private static final float LINE_RECT_HEIGHT = 0.7F;
	/** 双格指示牌内容：紧贴线路色矩形右边（左缘 = 矩形右缘），每格 0.3F、格子间无间隔，两层从矩形顶对齐，透明背景黑色文字 */
	private static final float DOUBLE_SIGN_LEFT = LINE_RECT_LEFT + LINE_RECT_WIDTH;
	private static final float DOUBLE_SIGN_TOP = LINE_RECT_TOP;
	private static final float DOUBLE_SIGN_CELL = 0.3F;
	/** 上一站中英文字号（比主站名小） */
	private static final int PREV_FONT_SIZE = 28;
	/** 上一站文字最大显示宽度（世界单位） */
	private static final float PREV_MAX_WIDTH = 1.0F;
	/** 上一站文字最大显示高度（世界单位，向下） */
	private static final float PREV_MAX_HEIGHT = 0.18F;

	/** MTR 的 Render 加载早于 BlockEntity，且字体纹理有缓存，需定时强制刷新 */
	private static final long REFRESH_INTERVAL_MS = 1000L;

	private long lastRefreshTime = 0;

	public RenderCRTStationInfo1(Argument dispatcher) {
		super(dispatcher);
	}

	@Override
	public void render(BlockCRTStationInfo1.BlockEntity entity, float tickDelta, GraphicsHolder graphicsHolder, int light, int overlay) {
		// MTR 的 Render 加载早于 BlockEntity，且字体纹理被缓存不会随数据变化自动失效，每秒强制刷新一次
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
		if (!(state.getBlock().data instanceof BlockCRTStationInfo1)) {
			return;
		}

		// 只在最左上 UPPER 方块渲染，避免 9 个方块重复绘制
		final IBlock.EnumSide side = IBlock.getStatePropertySafe(state, IBlock.SIDE_EXTENDED);
		final IBlock.DoubleBlockHalf half = IBlock.getStatePropertySafe(state, IBlock.HALF);
		if (side != IBlock.EnumSide.LEFT || half != IBlock.DoubleBlockHalf.UPPER) {
			return;
		}

		final Direction facing = IBlock.getStatePropertySafe(state, BlockCRTStationInfo1.FACING);
		final int shadingColor = RenderRouteBase.getShadingColor(facing, 0xFFFFFFFF);

		// 条带中心：左上 UPPER 方块中心向 rotateYClockwise 方向偏移 1 格（3 块宽的中间），y 为该半块中心 0.25
		final Direction right = facing.rotateYClockwise();
		final StoredMatrixTransformations baseMatrix = new StoredMatrixTransformations(
				pos.getX() + 0.5 + right.getOffsetX() * 1.0,
				pos.getY() + 0.25,
				pos.getZ() + 0.5 + right.getOffsetZ() * 1.0
		);
		baseMatrix.add(graphics -> {
			graphics.rotateYDegrees(-facing.asRotation());
			graphics.rotateZDegrees(180);
		});

		// 单面：只画正面
		final StoredMatrixTransformations matrix = baseMatrix.copy();
		matrix.add(graphics -> graphics.translate(0, 0, Z_FACE));

		// 本站名
		final Station station = findStationForRender(pos);
		final String stationName = station != null ? station.getName() : TranslationProvider.GUI_MTR_UNTITLED.getString();

		// 上一站名 / 下一站名（依据保存的站台选择确定线路，取该线路当前站的前/后一站）
		final String previousStationName = findPreviousStationName(entity);
		final String nextStationName = findNextStationName(entity);
		final int lineColor = findLineColor(entity);

		// 灰色矩形 + 本站名 + 上一站名 + 右侧线路指示合成一张纹理，一次绘制
		final Identifier textureId = CustomFontTextureCache.instance.getStationInfoBandTexture(
				stationName, previousStationName, nextStationName, lineColor, entity.isFlip(),
				FONT_TYPE, FONT_SIZE, LATIN_FONT_RATIO, GAP_RATIO, TEXT_COLOR, RECT_COLOR,
				BAND_WIDTH, BAND_HEIGHT,
				RECT_EDGE_GAP, RECT_LENGTH, RECT_HEIGHT, RIGHT_EDGE_GAP, MAX_MARGIN,
				PREV_FONT_SIZE, PREV_MAX_WIDTH, PREV_MAX_HEIGHT
		);

		MainRenderer.scheduleRender(textureId, false, QueuedRenderLayer.EXTERIOR, (graphicsHolderNew, offset) -> {
			matrix.transform(graphicsHolderNew, offset);
			IDrawing.drawTexture(graphicsHolderNew, -BAND_WIDTH / 2.0F, -BAND_HEIGHT / 2.0F, BAND_WIDTH, BAND_HEIGHT, 0F, 0F, 1F, 1F, facing, shadingColor, light);
			graphicsHolderNew.pop();
		});

		// 正面线路图：固定贴图 4:3（1.6F × 1.2F），以最左列中间层方块左上角向右偏移 0.2F 为起点，向下画，发光。
		// （drawTexture 的 y 是纹理顶部，图像向下延伸 HEIGHT，故 y=ROUTE_MAP_TOP 覆盖 [TOP, TOP+HEIGHT]）
		MainRenderer.scheduleRender(ROUTE_MAP_IDENTIFIER, false, QueuedRenderLayer.LIGHT, (graphicsHolderNew, offset) -> {
			matrix.transform(graphicsHolderNew, offset);
			IDrawing.drawTexture(graphicsHolderNew, ROUTE_MAP_LEFT, ROUTE_MAP_TOP, ROUTE_MAP_WIDTH, ROUTE_MAP_HEIGHT, 0F, 0F, 1F, 1F, facing, -1, GraphicsHolder.getDefaultLight());
			graphicsHolderNew.pop();
		});

		// 网络图片区：紧贴线路图右边，宽 2.6F × 高 1.2F，与线路图同高。URL 非空时异步下载并渲染（发光）。
		// 未下载完成前不画（保持透明），下载完成后自动出现。
		final String imageUrl = entity.getUrl();
		if (!imageUrl.isEmpty()) {
			WebImageCache.instance.request(imageUrl);
			final Identifier webTexture = WebImageCache.instance.get(imageUrl);
			if (webTexture != null) {
				MainRenderer.scheduleRender(webTexture, false, QueuedRenderLayer.LIGHT, (graphicsHolderNew, offset) -> {
					matrix.transform(graphicsHolderNew, offset);
					IDrawing.drawTexture(graphicsHolderNew, WEB_IMAGE_LEFT, WEB_IMAGE_TOP, WEB_IMAGE_WIDTH, WEB_IMAGE_HEIGHT, 0F, 0F, 1F, 1F, facing, -1, GraphicsHolder.getDefaultLight());
					graphicsHolderNew.pop();
				});
			}
		}

		// 线路色实心矩形：紧贴线路图下方（左缘 -1.2，顶 1.45），0.3F × 0.7F，用纯色纹理拉伸绘制
		MainRenderer.scheduleRender(CustomFontTextureCache.instance.getSolidColorTexture(lineColor), false, QueuedRenderLayer.EXTERIOR, (graphicsHolderNew, offset) -> {
			matrix.transform(graphicsHolderNew, offset);
			IDrawing.drawTexture(graphicsHolderNew, LINE_RECT_LEFT, LINE_RECT_TOP, LINE_RECT_WIDTH, LINE_RECT_HEIGHT, 0F, 0F, 1F, 1F, facing, shadingColor, light);
			graphicsHolderNew.pop();
		});

		// 双格指示牌内容：紧贴线路色矩形右边（左缘 -0.9，顶 1.45），每格 0.2F、格子间无间隔，
		// 两层对应双层指示牌（行 0/1），透明背景、黑色文字。每格按 signId 复用 drawSignCRT 渲染。
		final String[][] signIds = entity.getSignIds();
		if (signIds != null) {
			final List<LongAVLTreeSet> selectedIds = entity.getSelectedIds();
			graphicsHolder.push();
			graphicsHolder.translate(pos.getX() + 0.5 + right.getOffsetX(), pos.getY() + 0.25, pos.getZ() + 0.5 + right.getOffsetZ());
			graphicsHolder.rotateYDegrees(-facing.asRotation());
			graphicsHolder.rotateZDegrees(180);
			graphicsHolder.translate(0, 0, Z_FACE);
			for (int i = 0; i < signIds.length; i++) {
				final LongAVLTreeSet lineSelected = i < selectedIds.size() ? selectedIds.get(i) : new LongAVLTreeSet();
				for (int j = 0; j < signIds[i].length; j++) {
					final String signId = signIds[i][j];
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
							entity.getRouteNumbers(),
							facing,
							0,
							(textureId2, x2, y2, size2, flipTexture2) -> MainRenderer.scheduleRender(textureId2, true, QueuedRenderLayer.LIGHT_TRANSLUCENT, (graphicsHolderNew, offset) -> {
								matrix.transform(graphicsHolderNew, offset);
								IDrawing.drawTexture(graphicsHolderNew, x2, y2, size2, size2, flipTexture2 ? 1 : 0, 0, flipTexture2 ? 0 : 1, 1, facing, -1, GraphicsHolder.getDefaultLight());
								graphicsHolderNew.pop();
							}),
							true
					);
				}
			}
			graphicsHolder.pop();
		}
	}

	/**
	 * 根据保存的站台选择（selectedIds 第 0 行）确定线路，取该线路当前站的前一站站名。
	 * 选中平台不在任何线路、或位于线路首站（且非环线）时返回 null。
	 */
	/**
	 * 根据保存的站台选择（selectedIds 第 0 行）确定线路，取该线路当前站的后一站站名。
	 * 选中平台不在任何线路、或位于线路末站（且非环线）时返回 null。
	 */
	private String findNextStationName(BlockCRTStationInfo1.BlockEntity entity) {
		try {
			final LongAVLTreeSet firstLineSelected = getFirstLineSelected(entity);
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
						// 环线：末站的后一站是首站
						return route.getPlatforms().get(0).getStationName();
					}
					return null;
				}
			}
		} catch (Exception e) {
			InitClient.LOGGER.error("[RenderCRTStationInfo1]: failed to resolve next station", e);
		}
		return null;
	}

	/** 解析选中平台所在线路的颜色（RGB）；无线路时返回灰色。 */
	private int findLineColor(BlockCRTStationInfo1.BlockEntity entity) {
		try {
			final LongAVLTreeSet firstLineSelected = getFirstLineSelected(entity);
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
			InitClient.LOGGER.error("[RenderCRTStationInfo1]: failed to resolve line color", e);
		}
		return RECT_COLOR;
	}

	private LongAVLTreeSet getFirstLineSelected(BlockCRTStationInfo1.BlockEntity entity) {
		final List<LongAVLTreeSet> selectedIds = entity.getSelectedIds();
		if (selectedIds == null || selectedIds.isEmpty()) {
			return null;
		}
		return selectedIds.get(0);
	}

	private String findPreviousStationName(BlockCRTStationInfo1.BlockEntity entity) {
		try {
			final LongAVLTreeSet firstLineSelected = getFirstLineSelected(entity);
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
					// 环线：首站的前一站是末站
					return route.getPlatforms().get(route.getPlatforms().size() - 1).getStationName();
				}
			}
		} catch (Exception e) {
			InitClient.LOGGER.error("[RenderCRTStationInfo1]: failed to resolve previous station", e);
		}
		return null;
	}

	/** 优先精确匹配方块所在站区域；其次找最近的站台所属站；最后取全图最近的站。 */
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
		final Position position = Init.blockPosToPosition(blockPos);
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
	public boolean rendersOutsideBoundingBox2(BlockCRTStationInfo1.BlockEntity entity) {
		return true;
	}
}
