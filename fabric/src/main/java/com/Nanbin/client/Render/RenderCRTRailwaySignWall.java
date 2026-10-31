package com.Nanbin.client.Render;

import com.Nanbin.Registry.RegBlock.BlockCRTRailwaySignWall;
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

public class RenderCRTRailwaySignWall extends BlockEntityRenderer<BlockCRTRailwaySignWall.BlockEntity> {

	private static final Identifier WHITE_TEXTURE = new Identifier("mtr", "textures/block/white.png");
	// 每格 1 块宽，内容区高度 0.5 块。
	private static final float CELL_WIDTH = 1.0F;
	private static final float CONTENT_HEIGHT = 0.5F;
	private static final double BASE_Y = 0.75D;
	// 内容相对格中心的偏移：x -0.5 让局部 x=0 落在左端方块的西缘，z 让内容贴在模型黑色面板（+Z 面 z=2px）前方。
	private static final float CONTENT_OFFSET_X = -0.5F;
	private static final float CONTENT_OFFSET_Z = 0.36875F;
	private static final long REFRESH_INTERVAL_MS = 1000L;
	private long lastRefreshTime = 0L;

	public RenderCRTRailwaySignWall(BlockEntityRenderer.Argument dispatcher) {
		super(dispatcher);
	}

	@Override
	public boolean rendersOutsideBoundingBox2(BlockCRTRailwaySignWall.BlockEntity entity) {
		return true;
	}

	@Override
	public void render(BlockCRTRailwaySignWall.BlockEntity entity, float tickDelta, GraphicsHolder graphicsHolder, int light, int overlay) {
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
		if (!BlockCRTRailwaySignWall.isWallSign(state)) {
			return;
		}
		// 整条牌的内容只由左端方块的实体绘制，其余格直接跳过。
		if (IBlock.getStatePropertySafe(state, BlockCRTRailwaySignWall.LEFT)) {
			return;
		}

		final Direction facing = IBlock.getStatePropertySafe(state, DirectionHelper.FACING);
		final BlockView view = BlockView.cast(world);
		final List<BlockPos> run = BlockCRTRailwaySignWall.collectRun(view, pos);
		if (run.isEmpty()) {
			return;
		}

		final int cellCount = run.size();
		final String[] signIds = new String[cellCount];
		boolean allEmpty = true;
		for (int i = 0; i < cellCount; i++) {
			final BlockCRTRailwaySignWall.BlockEntity cellEntity = BlockCRTRailwaySignWall.getBlockEntity(view, run.get(i));
			if (cellEntity != null) {
				signIds[i] = cellEntity.getSignId();
				if (signIds[i] != null && !signIds[i].isEmpty()) {
					allEmpty = false;
				}
			}
		}
		if (allEmpty) {
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
				final Map<Long, String> routeNameByColor = new HashMap<>();
				try {
					final ObjectBidirectionalIterator iterator = MinecraftClientData.getInstance().simplifiedRoutes.iterator();
					while (iterator.hasNext()) {
						final SimplifiedRoute route = (SimplifiedRoute) iterator.next();
						routeNameByColor.put((long) route.getColor(), route.getName());
					}
				} catch (Exception ignored) {
				}

				final int globalBg = firstBackground != 0 ? firstBackground : 0xFFFFFF;
				for (int i = 0; i < cellCount; i++) {
					if (signIds[i] != null && !RenderCRTRailwaySign.isRouteNumberV2(signIds[i])) {
						final int cellBg = cellBackgroundColors[i] != 0 ? cellBackgroundColors[i] : 0xFFFFFF;
						final float cellX = CELL_WIDTH * (float) i;
						RenderCRTRailwaySign.drawSignCRT(graphicsHolder, storedMatrixTransformations, pos, signIds[i], cellX, 0.0F, CONTENT_HEIGHT, CELL_WIDTH, RenderRailwaySign.getMaxWidth(signIds, i, false) * 2.0F, RenderRailwaySign.getMaxWidth(signIds, i, true) * 2.0F, selectedIds, routeNumbers, facing, cellBg, (textureId, x, y, size, flipTexture) -> MainRenderer.scheduleRender(textureId, true, QueuedRenderLayer.LIGHT_TRANSLUCENT, (graphicsHolderNew, offset) -> {
							storedMatrixTransformations.transform(graphicsHolderNew, offset);
							IDrawing.drawTexture(graphicsHolderNew, x, y, size, size, flipTexture ? 1.0F : 0.0F, 0.0F, flipTexture ? 0.0F : 1.0F, 1.0F, facing, -1, GraphicsHolder.getDefaultLight());
							graphicsHolderNew.pop();
						}), false, signIds, globalBg);
					}
				}

				RenderCRTRailwaySign.renderRouteNumberV2Regions(storedMatrixTransformations, signIds, selectedIds, entity::getRouteNumber, cell -> null, facing, CONTENT_HEIGHT, CELL_WIDTH, routeNameByColor, cellBackgroundColors);
			}
		} finally {
			graphicsHolder.pop();
		}
	}
}
