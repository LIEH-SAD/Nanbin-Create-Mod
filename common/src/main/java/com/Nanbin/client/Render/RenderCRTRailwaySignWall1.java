package com.Nanbin.client.Render;

import com.Nanbin.Init;
import com.Nanbin.Registry.RegBlock.BlockCRTRailwaySignWall1;
import com.Nanbin.client.CRTRailwaySignEditor.legacy.CRTLegacySignDrawing;
import com.Nanbin.client.CRTRailwaySignEditor.legacy.CRTLegacySignView;
import com.Nanbin.client.CRTRailwaySignEditor.render.CRTJsLayoutBake;
import com.Nanbin.client.CRTRailwaySignEditor.render.CRTSignDataResolver;
import com.Nanbin.client.CRTRailwaySignEditor.render.CRTSignLayoutPainter;
import com.Nanbin.client.Drawing.CustomFontTextureCache;
import com.Nanbin.client.JavaScriptSupport.JSDrawSink;
import com.Nanbin.client.JavaScriptSupport.JSSignConfig;
import com.Nanbin.client.JavaScriptSupport.JSSignTextureBaker;
import com.Nanbin.mapping.GraphicsHolder;
import com.Nanbin.signage.crt.CRTSignLayout;
import it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import mtr.block.IBlock;
import mtr.client.ClientData;
import mtr.client.CustomResources;
import mtr.client.IDrawing;
import mtr.data.Station;
import mtr.render.MoreRenderLayers;
import mtr.render.RenderRailwaySign;
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
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 墙挂铁路指示牌渲染基类：内容几何与光照做成可覆盖字段，{@link RenderCRTRailwaySignWall2} 继承后单独调自己的模型。
 *
 * <p>MTR 4 → 3.6.3 适配：{@code BlockEntityRenderer} → 原版接口（matrices + vertexConsumers）；
 * {@code MainRenderer.scheduleRender} → 用 {@link MoreRenderLayers} + {@link IDrawing} 直接绘制；
 * {@code SignResource} → {@link CustomResources.CustomSign}；{@code getWorld2/getPos2} → {@code getWorld/getPos}；
 * 车站查询用 MTR 3 的 {@link ClientData}（来源的 ClientPacketHelper.findStationForSelectionCached 在目标侧尚未提供）。</p>
 */
public class RenderCRTRailwaySignWall1<T extends BlockCRTRailwaySignWall1.BlockEntity> implements BlockEntityRenderer<T> {

	private static final Identifier WHITE_TEXTURE = new Identifier("mtr", "textures/block/white.png");
	// 每个方块放两格内容（格宽 0.5），内容区高度 0.5 块，单格即 0.5 × 0.5。
	private static final int CELLS_PER_BLOCK = BlockCRTRailwaySignWall1.CELLS_PER_BLOCK;
	protected float cellWidth = 1.0F / CELLS_PER_BLOCK;
	protected float contentHeight = 0.5F;
	protected double baseY = 0.75D;
	// 内容相对方块中心的偏移：x -0.5 让局部 x=0 落在左端方块的西缘，z 让内容贴在模型黑色面板（+Z 面 z=2px）前方。
	// 每块两格（各 0.5 宽），整条牌的内容宽度 = 方块数 × 1.0。
	protected float contentOffsetX = -0.5F;
	protected float contentOffsetZ = 0.36875F;
	private static final long REFRESH_INTERVAL_MS = 1000L;
	private long lastRefreshTime = 0L;

	public RenderCRTRailwaySignWall1(BlockEntityRendererFactory.Context context) {
	}

	// 牌面内容的光照：默认全亮（自发光），子类可改为跟随世界光照。
	protected int contentLight(int worldLight) {
		return GraphicsHolder.getDefaultLight();
	}

	@Override
	public boolean rendersOutsideBoundingBox(T entity) {
		return true;
	}

	@Override
	public void render(T entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
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
		if (!BlockCRTRailwaySignWall1.isWallSign(state)) {
			return;
		}
		// 整条牌的内容只由左端方块的实体绘制，其余格直接跳过。
		if (IBlock.getStatePropertySafe(state, BlockCRTRailwaySignWall1.LEFT)) {
			return;
		}

		final Direction facing = IBlock.getStatePropertySafe(state, net.minecraft.block.HorizontalFacingBlock.FACING);
		final BlockView view = world;
		final List<BlockPos> run = BlockCRTRailwaySignWall1.collectRun(view, pos);
		if (run.isEmpty()) {
			return;
		}

		final int cellCount = run.size() * CELLS_PER_BLOCK;
		final String[] signIds = new String[cellCount];
		boolean allEmpty = true;
		for (int i = 0; i < cellCount; i++) {
			final BlockCRTRailwaySignWall1.BlockEntity cellEntity = BlockCRTRailwaySignWall1.getBlockEntity(view, run.get(i / CELLS_PER_BLOCK));
			if (cellEntity != null) {
				signIds[i] = cellEntity.getSignId(i % CELLS_PER_BLOCK);
				if (signIds[i] != null && !signIds[i].isEmpty()) {
					allEmpty = false;
				}
			}
		}
		// 自由布局（存在整条牌起始方块实体上）：新编辑器创建的牌可能没有旧 signIds，不能因"全空"提前返回
		final String signLayoutJson = entity.getSignLayout();
		final CRTSignLayout layout = CRTSignLayout.fromJson(signLayoutJson);
		if (allEmpty && layout == null) {
			return;
		}

		final int[] cellBackgroundColors = new int[cellCount];
		int firstBackground = 0;
		for (int i = 0; i < cellCount; i++) {
			if (signIds[i] != null && !signIds[i].isEmpty()) {
				final CustomResources.CustomSign sign = RenderRailwaySign.getSign(signIds[i]);
				if (sign != null && sign.backgroundColor != 0) {
					cellBackgroundColors[i] = sign.backgroundColor;
					if (firstBackground == 0) {
						firstBackground = sign.backgroundColor;
					}
				}
			}
		}

		final boolean isJSStyle = JSSignConfig.getStyleScriptId(signIds) != null;
		final int backgroundColor = (firstBackground != 0 ? firstBackground : (isJSStyle ? 0 : 0xFFFFFF)) | 0xFF000000;
		final LongAVLTreeSet selectedIds = entity.getSelectedIds();
		final String[] routeNumbers = entity.getRouteNumbers();
		final int contentLight = contentLight(light);

		final GraphicsHolder graphicsHolder = new GraphicsHolder(matrices, vertexConsumers);
		graphicsHolder.push();
		// vanilla 的 BlockEntityRenderDispatcher 已将 matrices 平移到本方块坐标原点，因此用相对坐标。
		graphicsHolder.translate(0.5D, baseY, 0.5D);
		graphicsHolder.rotateYDegrees(-facing.asRotation());
		graphicsHolder.rotateZDegrees(180.0F);
		graphicsHolder.translate(contentOffsetX, 0.0D, contentOffsetZ);
		try {
			// 自由布局（网页编辑器模型）：有布局数据时整条牌由布局绘制类烘焙成一张贴图来画
			if (layout != null) {
				try {
					// 车站上下文用于出口信息条（EXIT_INFO）的目的地查询
					final CRTSignDataResolver wallLayoutData = new CRTSignDataResolver(entity::getRouteNumber, findStationAt(pos));
					final boolean jsStyle = CRTJsLayoutBake.isJsStyle(layout);
					final JSSignTextureBaker.Baked baked = jsStyle
							? CRTJsLayoutBake.bake(layout, wallLayoutData, "crt_layout_wall:" + pos.toShortString(), signLayoutJson.hashCode(), facing, cellCount)
							: CRTSignLayoutPainter.bake(layout, wallLayoutData, "crt_layout_wall:" + pos.toShortString(), signLayoutJson.hashCode());
					if (baked != null) {
						final float plateWidth = (float) cellCount * cellWidth;
						final float plateHeight = contentHeight;
						// 不能用 Math.max(1.0F, ...) 夹取：JS 样式的烘焙尺寸是世界单位（高只有 0.5），
						// 被抬到 1.0 会让 fitScale 变成 0.5，表现为「内容缩小居中、四周留白」。
						final float designWidth = Math.max(1.0E-4F, baked.width);
						final float designHeight = Math.max(1.0E-4F, baked.height);
						final float fitScale = Math.min(plateWidth / designWidth, plateHeight / designHeight);
						final float drawWidth = designWidth * fitScale;
						final float drawHeight = designHeight * fitScale;
						final float drawX = (plateWidth - drawWidth) / 2.0F;
						final float drawY = (plateHeight - drawHeight) / 2.0F;
						final int layoutBackground = layout.backgroundColor | 0xFF000000;
						drawTexturedRectZ(matrices, vertexConsumers, WHITE_TEXTURE, 0.0F, 0.0F, 0.003125F, plateWidth, plateHeight, facing, layoutBackground, contentLight, false);
						drawTexturedRectZ(matrices, vertexConsumers, baked.identifier, drawX, drawY, 0.0F, drawWidth, drawHeight, facing, -1, contentLight, true);
						return;
					}

					if (jsStyle) {
						// JS 样式烘焙失败：只画牌面底色，不回退旧格子渲染（与编辑器预览一致）
						final float jsPlateWidth = (float) cellCount * cellWidth;
						final int layoutBackground = layout.backgroundColor | 0xFF000000;
						drawTexturedRectZ(matrices, vertexConsumers, WHITE_TEXTURE, 0.0F, 0.0F, 0.003125F, jsPlateWidth, contentHeight, facing, layoutBackground, contentLight, false);
						return;
					}
				} catch (Throwable e) {
					// 布局渲染失败回退旧格子渲染
					Init.LOGGER.warn("CRT wall sign layout render failed at {}, falling back to cell rendering", pos.toShortString(), e);
				}
			}

			// 牌面底板：与旧牌一致放在不写深度的 LIGHT_TRANSLUCENT，并先于内容绘制。
			drawTexturedRectZ(matrices, vertexConsumers, WHITE_TEXTURE, 0.0F, 0.0F, 0.003125F, (float) cellCount * cellWidth, contentHeight, facing, backgroundColor, contentLight, false);

			final String styleScriptId = JSSignConfig.getStyleScriptId(signIds);
			if (styleScriptId != null) {
				final Map<Long, String> routeNumberMap = new HashMap<>();
				selectedIds.longStream().forEach(color -> {
					final String number = entity.getRouteNumber(color);
					if (number != null && !number.isEmpty()) {
						routeNumberMap.put(color, number);
					}
				});
				final int jsBackgroundColor = cellBackgroundColors[0] != 0 ? cellBackgroundColors[0] : 0;
				RenderCRTRailwaySign.renderJSStyleLine(styleScriptId, matrices, vertexConsumers, pos, signIds, selectedIds, routeNumbers, facing, jsBackgroundColor, contentHeight, 0.0F, 0.0F, false, routeNumberMap);
			} else {
				// 与铁路指示牌共用同一套绘制类；墙挂牌每格 1.0 宽、内容高 0.5。
				// 目标侧 RenderCRTRailwaySign 的 GraphicsHolder 兼容入口忽略 smt，直接用真实 matrices 绘制，
				// 所以这里把当前上下文登记后传 null 的 SMT。
				final CRTLegacySignView wallView = new CRTLegacySignView(signIds, selectedIds, routeNumbers, entity::getRouteNumber, null)
						.geometry(contentHeight, cellWidth, facing)
						.cellOverrides(null, cellBackgroundColors);
				RenderCRTRailwaySign.setCompatContext(matrices, vertexConsumers);
				CRTLegacySignDrawing.draw(wallView, graphicsHolder, null, false);
			}
		} finally {
			graphicsHolder.pop();
		}
	}

	/** 用当前矩阵栈直接绘制一块带 z 偏移的贴图（来源的 MainRenderer.scheduleRender 在目标侧的等价写法）。 */
	private static void drawTexturedRectZ(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Identifier textureId, float x, float y, float z, float width, float height, Direction facing, int color, int light, boolean translucent) {
		matrices.push();
		matrices.translate(0.0F, 0.0F, z);
		final RenderLayer renderLayer = translucent ? MoreRenderLayers.getLight(textureId, true) : MoreRenderLayers.getLight(textureId, false);
		final VertexConsumer vertexConsumer = vertexConsumers.getBuffer(renderLayer);
		IDrawing.drawTexture(matrices, vertexConsumer, x, y, width, height, 0.0F, 0.0F, 1.0F, 1.0F, facing, color, light);
		matrices.pop();
	}

	/** 按方块坐标就近查找所属车站（MTR 3 客户端数据缓存）。 */
	@Nullable
	private static Station findStationAt(BlockPos pos) {
		final Station direct = ClientData.DATA_CACHE.blockPosToStation.get(pos);
		if (direct != null) {
			return direct;
		}
		for (final Station station : ClientData.STATIONS) {
			if (station.inArea(pos.getX(), pos.getZ())) {
				return station;
			}
		}
		return null;
	}
}
