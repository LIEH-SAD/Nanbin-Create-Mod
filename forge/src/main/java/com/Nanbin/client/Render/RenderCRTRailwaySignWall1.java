package com.Nanbin.client.Render;

import com.Nanbin.Registry.RegBlock.BlockCRTRailwaySignWall1;
import com.Nanbin.client.Drawing.CustomFontTextureCache;
import com.Nanbin.client.JavaScriptSupport.JSSignConfig;
import org.mtr.core.data.SimplifiedRoute;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectBidirectionalIterator;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockEntityRenderer;
import org.mtr.mapping.mapper.DirectionHelper;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mod.block.IBlock;
import org.mtr.mod.client.IDrawing;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.render.MainRenderer;
import org.mtr.mod.render.QueuedRenderLayer;
import org.mtr.mod.render.RenderRailwaySign;
import org.mtr.mod.render.StoredMatrixTransformations;
import org.mtr.mod.resource.SignResource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RenderCRTRailwaySignWall1 extends BlockEntityRenderer<BlockCRTRailwaySignWall1.BlockEntity> {

	private static final Identifier WHITE_TEXTURE = new Identifier("mtr", "textures/block/white.png");
	// 每个方块放两格内容（格宽 0.5），内容区高度 0.5 块，单格即 0.5 × 0.5。
	private static final int CELLS_PER_BLOCK = BlockCRTRailwaySignWall1.CELLS_PER_BLOCK;
	private static final float CELL_WIDTH = 1.0F / CELLS_PER_BLOCK;
	private static final float CONTENT_HEIGHT = 0.5F;
	private static final double BASE_Y = 0.75D;
	// 内容相对方块中心的偏移：x -0.5 让局部 x=0 落在左端方块的西缘，z 让内容贴在模型黑色面板（+Z 面 z=2px）前方。
	// 每块两格（各 0.5 宽），整条牌的内容宽度 = 方块数 × 1.0。
	private static final float CONTENT_OFFSET_X = -0.5F;
	private static final float CONTENT_OFFSET_Z = 0.36875F;
	private static final long REFRESH_INTERVAL_MS = 1000L;
	private long lastRefreshTime = 0L;

	public RenderCRTRailwaySignWall1(BlockEntityRenderer.Argument dispatcher) {
		super(dispatcher);
	}

	@Override
	public boolean rendersOutsideBoundingBox2(BlockCRTRailwaySignWall1.BlockEntity entity) {
		return true;
	}

	@Override
	public void render(BlockCRTRailwaySignWall1.BlockEntity entity, float tickDelta, GraphicsHolder graphicsHolder, int light, int overlay) {
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
		if (!BlockCRTRailwaySignWall1.isWallSign(state)) {
			return;
		}
		// 整条牌的内容只由左端方块的实体绘制，其余格直接跳过。
		if (IBlock.getStatePropertySafe(state, BlockCRTRailwaySignWall1.LEFT)) {
			return;
		}

		final Direction facing = IBlock.getStatePropertySafe(state, DirectionHelper.FACING);
		final BlockView view = BlockView.cast(world);
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
		final com.Nanbin.signage.crt.CRTSignLayout layout = com.Nanbin.signage.crt.CRTSignLayout.fromJson(signLayoutJson);
		if (allEmpty && layout == null) {
			return;
		}

		final int[] cellBackgroundColors = new int[cellCount];
		int firstBackground = 0;
		for (int i = 0; i < cellCount; i++) {
			if (signIds[i] != null && !signIds[i].isEmpty()) {
				final SignResource sign = RenderRailwaySign.getSign(signIds[i]);
				if (sign != null && sign.getBackgroundColor() != 0) {
					cellBackgroundColors[i] = sign.getBackgroundColor();
					if (firstBackground == 0) {
						firstBackground = sign.getBackgroundColor();
					}
				}
			}
		}

		final boolean isJSStyle = JSSignConfig.getStyleScriptId(signIds) != null;
		final int backgroundColor = (firstBackground != 0 ? firstBackground : (isJSStyle ? 0 : 0xFFFFFF)) | 0xFF000000;
		final LongAVLTreeSet selectedIds = entity.getSelectedIds();
		final String[] routeNumbers = entity.getRouteNumbers();

		final StoredMatrixTransformations storedMatrixTransformations = new StoredMatrixTransformations(pos.getX() + 0.5D, pos.getY() + BASE_Y, pos.getZ() + 0.5D);
		storedMatrixTransformations.add(graphicsHolderNew -> {
			graphicsHolderNew.rotateYDegrees(-facing.asRotation());
			graphicsHolderNew.rotateZDegrees(180.0F);
			graphicsHolderNew.translate(CONTENT_OFFSET_X, 0.0D, CONTENT_OFFSET_Z);
		});

		graphicsHolder.push();
		graphicsHolder.translate(pos.getX() + 0.5D, pos.getY() + BASE_Y, pos.getZ() + 0.5D);
		graphicsHolder.rotateYDegrees(-facing.asRotation());
		graphicsHolder.rotateZDegrees(180.0F);
		graphicsHolder.translate(CONTENT_OFFSET_X, 0.0D, CONTENT_OFFSET_Z);
		try {
			// 自由布局（网页编辑器模型）：有布局数据时整条牌由布局绘制类烘焙成一张贴图来画
			if (layout != null) {
				try {
					final com.Nanbin.client.JavaScriptSupport.JSSignTextureBaker.Baked baked = com.Nanbin.client.Drawing.CRTSignLayoutPainter.bake(layout,
							new com.Nanbin.client.signage.CRTSignDataResolver(entity::getRouteNumber), "crt_layout_wall:" + pos.toShortString(), signLayoutJson.hashCode());
					if (baked != null) {
						final float plateWidth = (float) cellCount * CELL_WIDTH;
						final float plateHeight = CONTENT_HEIGHT;
						final float designWidth = Math.max(1.0F, baked.width);
						final float designHeight = Math.max(1.0F, baked.height);
						final float fitScale = Math.min(plateWidth / designWidth, plateHeight / designHeight);
						final float drawWidth = designWidth * fitScale;
						final float drawHeight = designHeight * fitScale;
						final float drawX = (plateWidth - drawWidth) / 2.0F;
						final float drawY = (plateHeight - drawHeight) / 2.0F;
						final int layoutBackground = layout.backgroundColor | 0xFF000000;
						MainRenderer.scheduleRender(WHITE_TEXTURE, false, QueuedRenderLayer.LIGHT_TRANSLUCENT, (graphicsHolderNew, offset) -> {
							storedMatrixTransformations.transform(graphicsHolderNew, offset);
							IDrawing.drawTexture(graphicsHolderNew, 0.0F, 0.0F, 0.003125F, plateWidth, plateHeight, 0.003125F, facing, layoutBackground, GraphicsHolder.getDefaultLight());
							graphicsHolderNew.pop();
						});
						MainRenderer.scheduleRender(baked.identifier, true, QueuedRenderLayer.LIGHT_TRANSLUCENT, (graphicsHolderNew, offset) -> {
							storedMatrixTransformations.transform(graphicsHolderNew, offset);
							IDrawing.drawTexture(graphicsHolderNew, drawX, drawY, drawWidth, drawHeight, 0.0F, 0.0F, 1.0F, 1.0F, facing, -1, GraphicsHolder.getDefaultLight());
							graphicsHolderNew.pop();
						});
						return;
					}
				} catch (Throwable e) {
					// 布局渲染失败回退旧格子渲染
					com.Nanbin.Init.LOGGER.warn("CRT wall sign layout render failed at {}, falling back to cell rendering", pos.toShortString(), e);
				}
			}

			// 牌面底板：与旧牌一致放在不写深度的 LIGHT_TRANSLUCENT，并先于内容绘制。
			MainRenderer.scheduleRender(WHITE_TEXTURE, false, QueuedRenderLayer.LIGHT_TRANSLUCENT, (graphicsHolderNew, offset) -> {
				storedMatrixTransformations.transform(graphicsHolderNew, offset);
				IDrawing.drawTexture(graphicsHolderNew, 0.0F, 0.0F, 0.003125F, (float) cellCount * CELL_WIDTH, CONTENT_HEIGHT, 0.003125F, facing, backgroundColor, GraphicsHolder.getDefaultLight());
				graphicsHolderNew.pop();
			});

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
				RenderCRTRailwaySign.renderJSStyleLine(styleScriptId, storedMatrixTransformations, pos, signIds, selectedIds, routeNumbers, facing, jsBackgroundColor, CONTENT_HEIGHT, 0.0F, 0.0F, false, routeNumberMap, (float) cellCount * CELL_WIDTH);
			} else {
				// 与铁路指示牌共用同一套绘制类；墙挂牌每格 1.0 宽、内容高 0.5
				final com.Nanbin.client.Drawing.CRTSignView wallView = new com.Nanbin.client.Drawing.CRTSignView(signIds, selectedIds, routeNumbers, entity::getRouteNumber, null)
						.geometry(CONTENT_HEIGHT, CELL_WIDTH, facing)
						.cellOverrides(null, cellBackgroundColors);
				com.Nanbin.client.Drawing.CRTSignDrawing.draw(wallView, graphicsHolder, storedMatrixTransformations, false);
			}
		} finally {
			graphicsHolder.pop();
		}
	}
}
