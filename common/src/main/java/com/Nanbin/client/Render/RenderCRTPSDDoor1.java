package com.Nanbin.client.Render;

import com.Nanbin.Init;
import com.Nanbin.Registry.RegBlock.BlockCRTPSDDoor1;
import com.Nanbin.Registry.RegBlock.BlockCRTPlatform;
import mtr.MTRClient;
import mtr.block.BlockPSDAPGDoorBase;
import mtr.block.IBlock;
import mtr.client.ClientData;
import mtr.data.Platform;
import mtr.data.ScheduleEntry;
import mtr.mappings.ModelDataWrapper;
import mtr.mappings.ModelMapper;
import mtr.mappings.UtilitiesClient;
import mtr.render.MoreRenderLayers;
import mtr.render.StoredMatrixTransformations;
import net.minecraft.block.Block;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * CRT 第一代高屏蔽门（PSD）渲染器：绘制门页（随开合平移）、门顶指示灯（关门行程闪烁 / 全开常绿 /
 * 列车接近闪烁）以及站台面地灯（站台为 CRT 站台时）。门体外观与 MTR 的 psd_door 一代一致，
 * 贴图换成本模组的 crt_psd_door_* 系列。
 *
 * <p>MTR 4 → 3.6.3 适配：{@code BlockEntityRenderer#render(T, float, GraphicsHolder, int, int)} →
 * 原版 {@code render(T, float, MatrixStack, VertexConsumerProvider, int, int)}；
 * {@code MainRenderer.scheduleRender} → {@link MoreRenderLayers} + {@link Model#render} 直接绘制；
 * {@code entity.getDoorValue()} → {@code entity.getOpen(MTRClient.getLastFrameDuration())}。</p>
 */
public class RenderCRTPSDDoor1 implements BlockEntityRenderer<BlockCRTPSDDoor1.BlockEntity> {

	private static final ModelSingleCube MODEL_PSD = new ModelSingleCube(36, 18, 0, 0, 0, 16, 16, 2);
	private static final ModelSingleCube MODEL_PSD_END_LEFT_1 = new ModelSingleCube(20, 18, 0, 0, 0, 8, 16, 2);
	private static final ModelSingleCube MODEL_PSD_END_RIGHT_1 = new ModelSingleCube(20, 18, 8, 0, 0, 8, 16, 2);
	private static final ModelSingleCube MODEL_PSD_END_LEFT_2 = new ModelSingleCube(20, 18, 8, 0, 2, 8, 16, 2);
	private static final ModelSingleCube MODEL_PSD_END_RIGHT_2 = new ModelSingleCube(20, 18, 0, 0, 2, 8, 16, 2);
	private static final ModelSingleCube MODEL_PSD_LIGHT_LEFT = new ModelSingleCube(16, 16, 0, -2, 6, 1, 1, 1);
	private static final ModelSingleCube MODEL_PSD_LIGHT_RIGHT = new ModelSingleCube(16, 16, 15, -2, 6, 1, 1, 1);
	private static final ModelSingleCube MODEL_PSD_DOOR_LOCKED = new ModelSingleCube(6, 6, 5, 6, 1, 6, 6, 0);
	private static final ModelGroundLight MODEL_GROUND_LIGHT = new ModelGroundLight();

	private static final double ARRIVAL_WARNING_BLINK_PERIOD = 40.0;
	private static final double DOOR_MOVING_BLINK_PERIOD = 20.0;
	private static final long ARRIVAL_WARNING_TIME_MS = 60000;
	private static final int PLATFORM_SEARCH_RANGE = 3;

	private final Map<BlockPos, Double> previousOpenMap = new HashMap<>();

	public RenderCRTPSDDoor1(BlockEntityRendererFactory.Context context) {
	}

	@Override
	public void render(BlockCRTPSDDoor1.BlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
		final World world = entity.getWorld();
		if (world == null) {
			return;
		}
		final BlockPos blockPos = entity.getPos();
		if (!(world.getBlockState(blockPos).getBlock() instanceof BlockCRTPSDDoor1)) {
			return;
		}

		final Direction facing = IBlock.getStatePropertySafe(world, blockPos, BlockPSDAPGDoorBase.FACING);
		final boolean side = IBlock.getStatePropertySafe(world, blockPos, BlockPSDAPGDoorBase.SIDE) == IBlock.EnumSide.RIGHT;
		final boolean half = IBlock.getStatePropertySafe(world, blockPos, BlockPSDAPGDoorBase.HALF) == DoubleBlockHalf.UPPER;
		final boolean end = IBlock.getStatePropertySafe(world, blockPos, BlockPSDAPGDoorBase.END);
		final boolean unlocked = IBlock.getStatePropertySafe(world, blockPos, BlockPSDAPGDoorBase.UNLOCKED);
		final double open = Math.min(entity.getOpen(MTRClient.getLastFrameDuration()), 1.0);

		final StoredMatrixTransformations baseMatrix = translateToBlock(facing, true);
		final StoredMatrixTransformations groundMatrix = translateToBlock(facing, false);

		final boolean isTrainApproaching = isTrainApproaching(blockPos);
		final boolean isDoorFullyOpen = open >= 1.0 - 0.01;
		final double prevOpen = previousOpenMap.getOrDefault(blockPos, -1.0);
		final boolean isDoorMoving = prevOpen >= 0 && Math.abs(open - prevOpen) > 0.001;
		previousOpenMap.put(blockPos, open);
		final long gameTime = world.getTime();

		// 门顶指示灯
		if (half) {
			final String lightTexture;
			final boolean translucent;
			if (isDoorFullyOpen) {
				lightTexture = "green";
				translucent = true;
			} else if (isDoorMoving) {
				lightTexture = gameTime % (long) DOOR_MOVING_BLINK_PERIOD < DOOR_MOVING_BLINK_PERIOD / 2.0 ? "off" : "green";
				translucent = true;
			} else {
				lightTexture = "off";
				translucent = false;
			}
			drawModel(matrices, vertexConsumers, baseMatrix, blockTexture("crt_door_light_" + lightTexture + ".png"), side ? MODEL_PSD_LIGHT_RIGHT : MODEL_PSD_LIGHT_LEFT, translucent, light, overlay);
		}

		// 站台面地灯：仅当门下方为 CRT 站台方块时绘制
		if (!half) {
			final Block blockBelow = world.getBlockState(blockPos.down()).getBlock();
			if (blockBelow instanceof BlockCRTPlatform) {
				final String groundLightTexture;
				final boolean translucent;
				if (isDoorFullyOpen) {
					groundLightTexture = "green";
					translucent = true;
				} else if (isDoorMoving) {
					groundLightTexture = "off";
					translucent = false;
				} else if (isTrainApproaching) {
					groundLightTexture = gameTime % (long) ARRIVAL_WARNING_BLINK_PERIOD < ARRIVAL_WARNING_BLINK_PERIOD / 2.0 ? "off" : "yellow";
					translucent = true;
				} else {
					groundLightTexture = "off";
					translucent = false;
				}
				final String groundTexture = "crt_ground_light_" + groundLightTexture + (side ? ".png" : "_flipped.png");
				drawModel(matrices, vertexConsumers, groundMatrix, blockTexture(groundTexture), MODEL_GROUND_LIGHT, translucent, light, overlay);
			}
		}

		// 端部玻璃段：第二层门页（关门时隐藏在门后）
		if (end) {
			drawModelTranslated(matrices, vertexConsumers, baseMatrix, blockTexture("crt_psd_door_end_" + (half ? "top" : "bottom") + "_" + (side ? "right" : "left") + "_2_1.png"), side ? MODEL_PSD_END_RIGHT_2 : MODEL_PSD_END_LEFT_2, false, light, overlay, open / 2.0 * (side ? -1 : 1));
		}

		// 门页：随开合度沿门框方向平移
		final String doorTexture = end
				? "crt_psd_door_end_" + (half ? "top" : "bottom") + "_" + (side ? "right" : "left") + "_1_1.png"
				: "crt_psd_door_" + (half ? "top" : "bottom") + "_" + (side ? "right" : "left") + "_1.png";
		drawModelTranslated(matrices, vertexConsumers, baseMatrix, blockTexture(doorTexture), end ? (side ? MODEL_PSD_END_RIGHT_1 : MODEL_PSD_END_LEFT_1) : MODEL_PSD, false, light, overlay, open * (side ? -1 : 1));

		// 「此门停用」锁定标记
		if (half && !unlocked) {
			drawModelTranslated(matrices, vertexConsumers, baseMatrix, new Identifier("mtr", "textures/block/sign/door_not_in_use.png"), MODEL_PSD_DOOR_LOCKED, false, light, overlay, 0.0, end ? (side ? 0.25 : -0.25) : 0.0);
		}
	}

	@Override
	public boolean rendersOutsideBoundingBox(BlockCRTPSDDoor1.BlockEntity entity) {
		return true;
	}

	private static StoredMatrixTransformations translateToBlock(Direction facing, boolean flipVertical) {
		final StoredMatrixTransformations matrix = new StoredMatrixTransformations();
		matrix.add(graphics -> {
			// vanilla 的 BlockEntityRenderDispatcher 已将 matrices 平移到本方块坐标原点，因此用相对坐标
			graphics.translate(0.5, 0.0, 0.5);
			UtilitiesClient.rotateYDegrees(graphics, -facing.asRotation());
			if (flipVertical) {
				UtilitiesClient.rotateXDegrees(graphics, 180.0F);
			}
		});
		return matrix;
	}

	private static void drawModel(MatrixStack matrices, VertexConsumerProvider vertexConsumers, StoredMatrixTransformations matrix, Identifier texture, Model model, boolean translucent, int light, int overlay) {
		drawModelTranslated(matrices, vertexConsumers, matrix, texture, model, translucent, light, overlay, 0.0, 0.0);
	}

	private static void drawModelTranslated(MatrixStack matrices, VertexConsumerProvider vertexConsumers, StoredMatrixTransformations matrix, Identifier texture, Model model, boolean translucent, int light, int overlay, double translateX) {
		drawModelTranslated(matrices, vertexConsumers, matrix, texture, model, translucent, light, overlay, translateX, 0.0);
	}

	private static void drawModelTranslated(MatrixStack matrices, VertexConsumerProvider vertexConsumers, StoredMatrixTransformations matrix, Identifier texture, Model model, boolean translucent, int light, int overlay, double translateX, double translateY) {
		matrix.transform(matrices);
		if (translateX != 0.0 || translateY != 0.0) {
			matrices.translate(translateX, translateY, 0.0);
		}
		final RenderLayer renderLayer = translucent ? MoreRenderLayers.getLight(texture, true) : MoreRenderLayers.getExterior(texture);
		model.render(matrices, vertexConsumers.getBuffer(renderLayer), light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
		matrices.pop();
	}

	private static Identifier blockTexture(String fileName) {
		return new Identifier(Init.MOD_ID, "textures/block/" + fileName);
	}

	/** 判断门口附近是否有列车即将到站（用于地灯闪烁提示）。 */
	private boolean isTrainApproaching(BlockPos doorPos) {
		try {
			final long now = System.currentTimeMillis();
			long earliestArrival = Long.MAX_VALUE;
			for (final Platform platform : ClientData.PLATFORMS) {
				if (isNearPlatform(doorPos, platform)) {
					final Set<ScheduleEntry> entries = ClientData.SCHEDULES_FOR_PLATFORM.get(platform.id);
					if (entries != null) {
						for (final ScheduleEntry entry : entries) {
							final long timeUntilArrival = entry.arrivalMillis - now;
							if (timeUntilArrival > 0 && timeUntilArrival < earliestArrival) {
								earliestArrival = timeUntilArrival;
							}
						}
					}
				}
			}
			return earliestArrival <= ARRIVAL_WARNING_TIME_MS;
		} catch (Exception e) {
			Init.LOGGER.error("[CRTPSDDoor1] isTrainApproaching error", e);
		}
		return false;
	}

	/** 判断门是否位于站台沿线（按站台两端坐标的 XZ 包围盒判定，含 PLATFORM_SEARCH_RANGE 裕量）。 */
	private static boolean isNearPlatform(BlockPos doorPos, Platform platform) {
		final BlockPos position1 = platform.getMidPos();
		if (position1 == null) {
			return false;
		}
		final BlockPos position2 = platform.getOtherPosition(position1);
		if (position2 == null) {
			return false;
		}
		return doorPos.getX() >= Math.min(position1.getX(), position2.getX()) - PLATFORM_SEARCH_RANGE
				&& doorPos.getX() <= Math.max(position1.getX(), position2.getX()) + PLATFORM_SEARCH_RANGE
				&& doorPos.getZ() >= Math.min(position1.getZ(), position2.getZ()) - PLATFORM_SEARCH_RANGE
				&& doorPos.getZ() <= Math.max(position1.getZ(), position2.getZ()) + PLATFORM_SEARCH_RANGE;
	}

	/** 贴地光带模型。 */
	private static class ModelGroundLight extends Model {
		private final ModelMapper bone;

		private ModelGroundLight() {
			super(RenderLayer::getEntityCutout);
			final ModelDataWrapper modelDataWrapper = new ModelDataWrapper(this, 16, 16);
			bone = new ModelMapper(modelDataWrapper);
			bone.texOffs(0, 0).addBox(-8.0F, 0.0F, -8.0F, 16, 0, 16, 0.0F, false);
			modelDataWrapper.setModelPart(16, 16);
			bone.setModelPart();
		}

		@Override
		public void render(MatrixStack matrices, net.minecraft.client.render.VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha) {
			bone.render(matrices, vertices, 0.0F, 0.0F, 0.0F, light, overlay);
		}
	}

	/** 单个方块大小的立方体模型。 */
	private static class ModelSingleCube extends Model {
		private final ModelMapper cube;

		private ModelSingleCube(int textureWidth, int textureHeight, int x, int y, int z, int length, int height, int depth) {
			super(RenderLayer::getEntityCutout);
			final ModelDataWrapper modelDataWrapper = new ModelDataWrapper(this, textureWidth, textureHeight);
			cube = new ModelMapper(modelDataWrapper);
			cube.texOffs(0, 0).addBox(x - 8.0F, y - 16.0F, z - 8.0F, length, height, depth, 0.0F, false);
			modelDataWrapper.setModelPart(textureWidth, textureHeight);
			cube.setModelPart();
		}

		@Override
		public void render(MatrixStack matrices, net.minecraft.client.render.VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha) {
			cube.render(matrices, vertices, 0.0F, 0.0F, 0.0F, light, overlay);
		}
	}
}
