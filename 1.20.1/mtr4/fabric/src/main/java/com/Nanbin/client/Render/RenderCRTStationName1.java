package com.Nanbin.client.Render;

import com.Nanbin.Registry.RegBlock.BlockCRTStationName1;
import com.Nanbin.client.Drawing.CustomFontTextureCache;
import com.Nanbin.client.Drawing.CustomFontTextureCache.FontType;
import com.Nanbin.mapping.Registry;
import com.Nanbin.packet.PacketRequestPlatformRouteData;
import com.Nanbin.packet.PacketSyncStationNameData;
import org.mtr.core.data.Platform;
import org.mtr.core.data.Route;
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

import java.util.HashMap;
import java.util.Map;

public class RenderCRTStationName1 extends BlockEntityRenderer<BlockCRTStationName1.BlockEntity> implements IBlock, IGui, IDrawing {

	// From MTR RenderStationNameBase: translate z by (0.5 - zOffset - 0.003125)
	// Then old drawStationName added another -0.0375 in local space
	// With zOffset=0: total Z from center = 0.5 - 0 - 0.003125 - 0.0375 = 0.459375
	private static final float Z_FROM_CENTER = 0.459375F;
	private static final float BG_WIDTH = 1.4F;
	private static final float BG_HEIGHT = 1.6F;
	private static final float TEXT_MARGIN = 0.05F;
	private static final float TEXT_SCALE = 0.6F;

	private static final boolean USE_CUSTOM_FONT = true;

	private static final FontType SELECTED_FONT = FontType.SOURCE_HAN;
	private static final int FONT_SIZE = 100;

	/** 已经向服务端请求过路线数据的 (方块位置→平台ID) 映射，站台变更后会自动重请求 */
	private static final Map<Long, Long> requestedPlatformIds = new HashMap<>();

	static {
		CustomFontTextureCache.instance.selectedFont = SELECTED_FONT;
		CustomFontTextureCache.instance.fontSize = FONT_SIZE;
	}

	public RenderCRTStationName1(BlockEntityRenderer.Argument dispatcher, boolean showLogo) {
		super(dispatcher);
	}

	@Override
	public void render(BlockCRTStationName1.BlockEntity entity, float tickDelta, GraphicsHolder graphicsHolder, int light, int overlay) {
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
		int themeColor = stationColor;
		int routeColor = stationColor;
		String routeNumber = "";
		String platformNumber = "";

		try {
			final long savedPlatformId = entity.getPlatformId();
			final long posKey = pos.asLong();
			Platform platform = null;

			if (savedPlatformId != 0) {
				platform = MinecraftClientData.getInstance().platformIdMap.get(savedPlatformId);
			}

			if (platform != null && !platform.routes.isEmpty()) {
				// 路线数据已就绪（收到 PacketUpdateData 后），同步到服务端持久化
				final Route route = platform.routes.iterator().next();
				themeColor = route.getColor();
				routeColor = route.getColor();
				routeNumber = route.getRouteNumber();
				platformNumber = platform.getName();
				if (entity.getRouteColor() != routeColor
						|| !entity.getRouteNumber().equals(routeNumber)
						|| !entity.getPlatformName().equals(platformNumber)) {
					Registry.sendPacketToServer(new PacketSyncStationNameData(
							entity.getPos2(), routeColor, routeNumber, platformNumber));
				}
				requestedPlatformIds.remove(posKey); // 数据已就绪，清除请求标记使站台切换后能重新请求
			} else if (entity.getRouteColor() != 0) {
				// 使用已保存到 NBT 的路线数据
				themeColor = entity.getRouteColor();
				routeColor = entity.getRouteColor();
				routeNumber = entity.getRouteNumber();
				platformNumber = entity.getPlatformName();
				requestedPlatformIds.remove(posKey); // 数据已就绪，清除请求标记使站台切换后能重新请求
			}

			// 站台 ID 存在但路线数据未就绪（platform 为 null、routes 为空、或缓存已被清空）→ 向服务端请求
			if (savedPlatformId != 0 && routeColor == 0 && platformNumber.isEmpty()) {
				final Long lastPlatformId = requestedPlatformIds.get(posKey);
				if (lastPlatformId == null || lastPlatformId != savedPlatformId) {
					requestedPlatformIds.put(posKey, savedPlatformId);
					Registry.sendPacketToServer(new PacketRequestPlatformRouteData(pos));
				}
			}
		} catch (Exception ignored) {
		}

		// Texture coordinates
		final float drawAspect = (BG_WIDTH - TEXT_MARGIN * 2) / (BG_HEIGHT - TEXT_MARGIN * 2);
		final float textX = (-BG_WIDTH / 2.0F + TEXT_MARGIN) * TEXT_SCALE;
		final float textY = (-BG_HEIGHT / 2.0F + TEXT_MARGIN) * TEXT_SCALE;
		final float textWidth = (BG_WIDTH - TEXT_MARGIN * 2) * TEXT_SCALE;
		final float textHeight = (BG_HEIGHT - TEXT_MARGIN * 2) * TEXT_SCALE;

		final Identifier textureId;
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


	}
}