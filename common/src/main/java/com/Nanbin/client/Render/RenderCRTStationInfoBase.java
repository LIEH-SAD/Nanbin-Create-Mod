package com.Nanbin.client.Render;

import com.Nanbin.Registry.RegBlock.BlockCRTStationInfoBase;
import com.Nanbin.client.Drawing.CustomFontTextureCache;
import com.Nanbin.client.Drawing.CustomFontTextureCache.FontType;
import com.Nanbin.client.Drawing.WebImageCache;
import com.Nanbin.mapping.DirectionHelper;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.mtr.MTR;
import org.mtr.MTRClient;
import org.mtr.block.IBlock;
import org.mtr.client.MinecraftClientData;
import org.mtr.core.data.Position;
import org.mtr.core.data.Route;
import org.mtr.core.data.SimplifiedRoute;
import org.mtr.core.data.Station;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import org.mtr.render.BlockEntityRendererExtension;
import org.mtr.render.MainRenderer;
import org.mtr.render.QueuedRenderLayer;
import org.mtr.render.RenderRouteBase;
import org.mtr.render.StoredMatrixTransformations;
import org.mtr.tool.Drawing;

import java.util.List;

/**
 * CRT 站台信息屏渲染器的公共基类。
 * <p>
 * 源项目里单面板（391 行）与双面板（352 行）结构相同，只有「画几面、用哪一面数据」不同，
 * 所以这里把一面条带的绘制抽成 {@link #renderFace}，由子类决定调用几次、传 {@code front}
 * 还是只传 {@code true}。
 * <p>
 * 4.1 适配要点：
 * <ul>
 *     <li>{@code BlockEntityRenderer}（4.0 映射层）→ {@link BlockEntityRendererExtension}；
 *     render 签名换成 {@code (T, MatrixStack, VertexConsumerProvider, ClientWorld, ClientPlayerEntity, float, int, int)}，
 *     且栈原点已在方块中心 {@code (blockX+0.5, blockY, blockZ+0.5)}。</li>
 *     <li>逐格标志绘制：4.0 用 {@code RenderCRTRailwaySign.drawSignCRT(..., DrawTexture, ...)} + 延后渲染；
 *     4.1 的 {@code SignResource.render} 需要 {@code VertexConsumerProvider}，延后回调里拿不到，
 *     所以改成在 render 钩子里同步调用 {@link RenderCRTRailwaySign#drawSignCell}。</li>
 *     <li>{@code entity.getUrl()/getSignIds()/getSelectedIds()/getRouteNumbers()/isFlip()} 在 4.1 都带
 *     {@code front} 参数（双面板正反面各存一份）。</li>
 * </ul>
 */
public abstract class RenderCRTStationInfoBase extends BlockEntityRendererExtension<BlockCRTStationInfoBase.BlockEntityBase> implements IBlock {

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
	private static final float RECT_LENGTH = 0.9F;
	private static final float RECT_HEIGHT = 0.1F;
	private static final float RECT_EDGE_GAP = 0.2F;
	private static final float RIGHT_EDGE_GAP = 0.2F;

	/** 正面线路图：固定贴图 route_map.png，强制 4:3（宽 1.6F × 高 1.2F） */
	private static final Identifier ROUTE_MAP_IDENTIFIER = Identifier.of("mtr", "textures/texture/route_map.png");
	private static final float ROUTE_MAP_LEFT = -1.5F + 0.2F;
	private static final float ROUTE_MAP_TOP = 0.25F;
	private static final float ROUTE_MAP_WIDTH = 1.6F;
	private static final float ROUTE_MAP_HEIGHT = 1.2F;
	/** 网络图片区：紧贴线路图右缘，高与线路图相同，宽 1.0F */
	private static final float WEB_IMAGE_LEFT = ROUTE_MAP_LEFT + ROUTE_MAP_WIDTH;
	private static final float WEB_IMAGE_TOP = ROUTE_MAP_TOP;
	private static final float WEB_IMAGE_WIDTH = 1.0F;
	private static final float WEB_IMAGE_HEIGHT = 1.2F;
	/** 线路色实心矩形：紧贴线路图下方 */
	private static final float LINE_RECT_LEFT = -1.5F + 0.3F;
	private static final float LINE_RECT_TOP = ROUTE_MAP_TOP + ROUTE_MAP_HEIGHT;
	private static final float LINE_RECT_WIDTH = 0.3F;
	private static final float LINE_RECT_HEIGHT = 0.7F;
	/** 双格指示牌内容：紧贴线路色矩形右边，每格 0.3F、格子间无间隔，两层从矩形顶对齐，透明背景黑色文字 */
	private static final float DOUBLE_SIGN_LEFT = LINE_RECT_LEFT + LINE_RECT_WIDTH;
	private static final float DOUBLE_SIGN_TOP = LINE_RECT_TOP;
	private static final float DOUBLE_SIGN_CELL = 0.3F;
	/** 上一站中英文字号（比主站名小） */
	private static final int PREV_FONT_SIZE = 28;
	private static final float PREV_MAX_WIDTH = 1.0F;
	private static final float PREV_MAX_HEIGHT = 0.18F;

	/** MTR 的 Render 加载早于 BlockEntity，且字体纹理有缓存，需定时强制刷新 */
	private static final long REFRESH_INTERVAL_MS = 1000L;

	private long lastRefreshTime = 0;

	@Override
	public void render(BlockCRTStationInfoBase.BlockEntityBase entity, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, float tickDelta, int light, int overlay) {
		final long now = System.currentTimeMillis();
		if (now - lastRefreshTime >= REFRESH_INTERVAL_MS) {
			lastRefreshTime = now;
			CustomFontTextureCache.instance.clearFittedTextureCache();
		}

		final BlockPos pos = entity.getPos();
		final BlockState state = clientWorld.getBlockState(pos);
		if (!(state.getBlock() instanceof BlockCRTStationInfoBase)) {
			return;
		}

		// 只在最左上 UPPER 方块渲染，避免 9 个方块重复绘制
		final IBlock.EnumSide side = IBlock.getStatePropertySafe(state, IBlock.SIDE_EXTENDED);
		final IBlock.DoubleBlockHalf half = IBlock.getStatePropertySafe(state, IBlock.HALF);
		if (side != IBlock.EnumSide.LEFT || half != IBlock.DoubleBlockHalf.UPPER) {
			return;
		}

		final Direction facing = IBlock.getStatePropertySafe(state, Properties.HORIZONTAL_FACING);
		final int shadingColor = RenderRouteBase.getShadingColor(facing, 0xFFFFFFFF);
		final Direction right = facing.rotateYClockwise();

		// 条带中心：左上 UPPER 方块中心向 rotateYClockwise 方向偏移 1 格（3 块宽的中间），y 为该半块中心 0.25
		final double bandCenterX = pos.getX() + 0.5 + right.getOffsetX();
		final double bandCenterY = pos.getY() + 0.25;
		final double bandCenterZ = pos.getZ() + 0.5 + right.getOffsetZ();

		if (onlyFront()) {
			renderFace(entity, matrixStack, vertexConsumerProvider, pos, facing, bandCenterX, bandCenterY, bandCenterZ, shadingColor, light, true, true);
		} else {
			// 双面板：正面朝 +facing，背面朝 -facing
			renderFace(entity, matrixStack, vertexConsumerProvider, pos, facing, bandCenterX, bandCenterY, bandCenterZ, shadingColor, light, true, true);
			renderFace(entity, matrixStack, vertexConsumerProvider, pos, facing, bandCenterX, bandCenterY, bandCenterZ, shadingColor, light, false, true);
		}
	}

	/** 子类决定只画正面（单面板）还是正反两面（双面板）。 */
	protected abstract boolean onlyFront();

	/**
	 * 画一面条带。
	 *
	 * @param front      取哪一面（双面板各存一份）的数据
	 * @param flipFacing 背面需要再绕 Y 轴转 180°
	 */
	private void renderFace(BlockCRTStationInfoBase.BlockEntityBase entity, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, BlockPos pos, Direction facing, double bandCenterX, double bandCenterY, double bandCenterZ, int shadingColor, int light, boolean front, boolean flipFacing) {
		final StoredMatrixTransformations matrix = new StoredMatrixTransformations(bandCenterX, bandCenterY, bandCenterZ);
		matrix.add(matrices -> {
			Drawing.rotateYDegrees(matrices, -DirectionHelper.asRotation(facing));
			if (!flipFacing) {
				Drawing.rotateYDegrees(matrices, 180);
			}
			Drawing.rotateZDegrees(matrices, 180);
			matrices.translate(0, 0, Z_FACE);
		});

		// 本站名
		final Station station = findStationForRender(pos);
		final String stationName = station != null ? station.getName() : org.mtr.generated.lang.TranslationProvider.GUI_MTR_UNTITLED.getString();
		final String previousStationName = findPreviousStationName(entity, front);
		final String nextStationName = findNextStationName(entity, front);
		final int lineColor = findLineColor(entity, front);

		// 灰色矩形 + 本站名 + 上一站名 + 右侧线路指示合成一张纹理，一次绘制
		final Identifier textureId = CustomFontTextureCache.instance.getStationInfoBandTexture(
				stationName, previousStationName, nextStationName, lineColor, entity.isFlip(front),
				FONT_TYPE, FONT_SIZE, LATIN_FONT_RATIO, GAP_RATIO, TEXT_COLOR, RECT_COLOR,
				BAND_WIDTH, BAND_HEIGHT,
				RECT_EDGE_GAP, RECT_LENGTH, RECT_HEIGHT, RIGHT_EDGE_GAP, MAX_MARGIN,
				PREV_FONT_SIZE, PREV_MAX_WIDTH, PREV_MAX_HEIGHT
		);

		scheduleQuad(textureId, matrix, -BAND_WIDTH / 2.0F, -BAND_HEIGHT / 2.0F, BAND_WIDTH, BAND_HEIGHT, shadingColor, light, false);

		// 正面线路图：固定贴图 4:3，以最左列中间层方块左上角向右偏移 0.2F 为起点，向下画，发光
		scheduleQuad(ROUTE_MAP_IDENTIFIER, matrix, ROUTE_MAP_LEFT, ROUTE_MAP_TOP, ROUTE_MAP_WIDTH, ROUTE_MAP_HEIGHT, -1, SignDrawing.defaultLight(), false);

		// 网络图片区：URL 非空时异步下载并渲染（发光）；未下载完成前不画
		final String imageUrl = entity.getUrl(front);
		if (!imageUrl.isEmpty()) {
			WebImageCache.instance.request(imageUrl);
			final Identifier webTexture = WebImageCache.instance.get(imageUrl);
			if (webTexture != null) {
				scheduleQuad(webTexture, matrix, WEB_IMAGE_LEFT, WEB_IMAGE_TOP, WEB_IMAGE_WIDTH, WEB_IMAGE_HEIGHT, -1, SignDrawing.defaultLight(), false);
			}
		}

		// 线路色实心矩形：紧贴线路图下方，用纯色纹理拉伸绘制
		scheduleQuad(CustomFontTextureCache.instance.getSolidColorTexture(lineColor), matrix, LINE_RECT_LEFT, LINE_RECT_TOP, LINE_RECT_WIDTH, LINE_RECT_HEIGHT, shadingColor, light, false);

		// 双格指示牌内容：紧贴线路色矩形右边，每格 0.3F，两层（行 0/1）从矩形顶对齐，透明背景黑色文字。
		// 4.1：SignResource.render 需要 VertexConsumerProvider，所以这一块必须同步画（不能用延后回调）。
		renderSignCells(matrix, entity, matrixStack, vertexConsumerProvider, pos, facing, bandCenterX, bandCenterY, bandCenterZ, front, flipFacing);
	}

	private void renderSignCells(StoredMatrixTransformations matrix, BlockCRTStationInfoBase.BlockEntityBase entity, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, BlockPos pos, Direction facing, double bandCenterX, double bandCenterY, double bandCenterZ, boolean front, boolean flipFacing) {
		final String[][] signIds = entity.getSignIds(front);
		if (signIds == null) {
			return;
		}

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
			if (!allEmpty) {
				break;
			}
		}
		if (allEmpty) {
			return;
		}

		// 把实时矩阵栈摆到与上面 StoredMatrixTransformations 相同的位置：
		// 栈原点已在方块中心，所以这里只需要相对偏移。
		matrixStack.push();
		matrixStack.translate(bandCenterX - pos.getX() - 0.5, bandCenterY - pos.getY(), bandCenterZ - pos.getZ() - 0.5);
		Drawing.rotateYDegrees(matrixStack, -DirectionHelper.asRotation(facing));
		if (!flipFacing) {
			Drawing.rotateYDegrees(matrixStack, 180);
		}
		Drawing.rotateZDegrees(matrixStack, 180);
		matrixStack.translate(0, 0, Z_FACE);

		final List<LongAVLTreeSet> selectedIds = entity.getSelectedIds(front);
		final LongAVLTreeSet[] selectedIdsArray = new LongAVLTreeSet[signIds.length];
		for (int i = 0; i < signIds.length; i++) {
			selectedIdsArray[i] = i < selectedIds.size() ? selectedIds.get(i) : new LongAVLTreeSet();
		}

		for (int i = 0; i < signIds.length; i++) {
			final String[] lineIds = signIds[i];
			if (lineIds == null) {
				continue;
			}
			if (RenderCRTRailwaySign.isJSStyleLine(lineIds)) {
				// 整行 JS 样式：交给脚本（脚本内部走延后渲染，与 JS 引擎的绘制原语一致）。
				// 必须用「面矩阵」matrix —— 它已经包含 Z_FACE（画到模型表面之外）；
				// 4.0 传的也是这个矩阵。曾经这里另新建了一个不含 Z_FACE 的矩阵，
				// 结果 JS 内容被画在方块内部，完全看不见。
				RenderCRTRailwaySign.renderJSStyleLine(com.Nanbin.client.JavaScriptSupport.JSSignConfig.getStyleScriptId(lineIds), matrix, pos, lineIds,
						selectedIdsArray[i], entity.getRouteNumbers(front), facing, 0, DOUBLE_SIGN_CELL, DOUBLE_SIGN_LEFT, DOUBLE_SIGN_TOP + i * DOUBLE_SIGN_CELL, true, buildRouteNumberMap(entity, front));
				continue;
			}
			// 4.0 的 renderCustomTextCRT 走的是「本项目字体 + 透明底 + 居中文字」，
			// 所以这里用 transparentBlack 语义（与 4.0 传给 drawSignCRT 的参数一致）。
			RenderCRTRailwaySign.drawSignLine(matrixStack, vertexConsumerProvider, SignDrawing.immediate(matrixStack, vertexConsumerProvider),
					pos, lineIds, selectedIdsArray[i], entity.getRouteNumbers(front), DOUBLE_SIGN_CELL,
					DOUBLE_SIGN_LEFT, DOUBLE_SIGN_TOP + i * DOUBLE_SIGN_CELL, true);
		}

		matrixStack.pop();
	}

	private java.util.Map<Long, String> buildRouteNumberMap(BlockCRTStationInfoBase.BlockEntityBase entity, boolean front) {
		final java.util.Map<Long, String> routeNumberMap = new java.util.HashMap<>();
		try {
			for (final LongAVLTreeSet set : entity.getSelectedIds(front)) {
				for (final long platformId : set) {
					final String number = resolveRouteNumber(entity, front, platformId);
					if (!number.isEmpty()) {
						routeNumberMap.put(platformId, number);
					}
				}
			}
		} catch (Exception ignored) {
		}
		return routeNumberMap;
	}

	/**
	 * 站台对应的线路编号：优先用客户端线路数据里的真实编号
	 * （见 {@link SignRouteData#getRouteNumberForPlatform}），
	 * 其次是方块实体里同步过来的编号（4.0 那套服务端解析 + NBT 同步，本移植未启用）。
	 */
	private String resolveRouteNumber(BlockCRTStationInfoBase.BlockEntityBase entity, boolean front, long platformId) {
		final String live = SignRouteData.getRouteNumberForPlatform(entity.getPos(), platformId);
		if (!live.isEmpty()) {
			return live;
		}
		final String synced = entity.getRouteNumber(platformId, front);
		return synced == null ? "" : synced;
	}

	private static void scheduleQuad(Identifier textureId, StoredMatrixTransformations matrix, float x, float y, float width, float height, int color, int light, boolean priority) {
		MainRenderer.scheduleRender(textureId, priority, QueuedRenderLayer.EXTERIOR, (matrices, vertexConsumer, offset) -> {
			matrix.transform(matrices, offset);
			new Drawing(matrices, vertexConsumer)
					.setVerticesWH(x, y, width, height)
					.setUv(0, 0, 1, 1)
					.setColor(color)
					.setLight(light)
					.draw();
			matrices.pop();
		});
	}

	private LongAVLTreeSet getFirstLineSelected(BlockCRTStationInfoBase.BlockEntityBase entity, boolean front) {
		final List<LongAVLTreeSet> selectedIds = entity.getSelectedIds(front);
		if (selectedIds == null || selectedIds.isEmpty()) {
			return null;
		}
		return selectedIds.get(0);
	}

	/** 根据保存的站台选择（selectedIds 第 0 行）确定线路，取该线路当前站的前一站站名。 */
	private String findPreviousStationName(BlockCRTStationInfoBase.BlockEntityBase entity, boolean front) {
		try {
			final LongAVLTreeSet firstLineSelected = getFirstLineSelected(entity, front);
			if (firstLineSelected == null || firstLineSelected.isEmpty()) {
				return null;
			}
			final long platformId = firstLineSelected.firstLong();
			for (final SimplifiedRoute route : MinecraftClientData.getInstance().simplifiedRoutes) {
				final int index = route.getPlatformIndex(platformId);
				if (index > 0) {
					return route.getPlatforms().get(index - 1).getStationName();
				} else if (index == 0 && route.getCircularState() != Route.CircularState.NONE && !route.getPlatforms().isEmpty()) {
					// 环线：首站的前一站是末站
					return route.getPlatforms().get(route.getPlatforms().size() - 1).getStationName();
				}
			}
		} catch (Exception e) {
			LOG_ERR("failed to resolve previous station", e);
		}
		return null;
	}

	/** 取该线路当前站的后一站站名；选中平台不在任何线路、或位于线路末站（且非环线）时返回 null。 */
	private String findNextStationName(BlockCRTStationInfoBase.BlockEntityBase entity, boolean front) {
		try {
			final LongAVLTreeSet firstLineSelected = getFirstLineSelected(entity, front);
			if (firstLineSelected == null || firstLineSelected.isEmpty()) {
				return null;
			}
			final long platformId = firstLineSelected.firstLong();
			for (final SimplifiedRoute route : MinecraftClientData.getInstance().simplifiedRoutes) {
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
			LOG_ERR("failed to resolve next station", e);
		}
		return null;
	}

	/** 解析选中平台所在线路的颜色（ARGB）；无线路时返回灰色。 */
	private int findLineColor(BlockCRTStationInfoBase.BlockEntityBase entity, boolean front) {
		try {
			final LongAVLTreeSet firstLineSelected = getFirstLineSelected(entity, front);
			if (firstLineSelected == null || firstLineSelected.isEmpty()) {
				return RECT_COLOR;
			}
			final long platformId = firstLineSelected.firstLong();
			for (final SimplifiedRoute route : MinecraftClientData.getInstance().simplifiedRoutes) {
				if (route.getPlatformIndex(platformId) >= 0) {
					return route.getColor() | 0xFF000000;
				}
			}
		} catch (Exception e) {
			LOG_ERR("failed to resolve line color", e);
		}
		return RECT_COLOR;
	}

	private static void LOG_ERR(String message, Exception e) {
		com.Nanbin.Init.LOGGER.error("[RenderCRTStationInfo] " + message, e);
	}

	/** 优先精确匹配方块所在站区域；其次找最近的站台所属站；最后取全图最近的站。 */
	private static Station findStationForRender(BlockPos blockPos) {
		final Station exact = MTRClient.findStation(blockPos);
		if (exact != null) {
			return exact;
		}
		final Station[] closeStation = {null};
		MTRClient.findClosePlatform(blockPos, 256, platform -> closeStation[0] = platform.area);
		if (closeStation[0] != null) {
			return closeStation[0];
		}
		final Position position = MTR.blockPosToPosition(blockPos);
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
	public boolean rendersOutsideBoundingBox(BlockCRTStationInfoBase.BlockEntityBase entity) {
		return true;
	}
}
