package com.Nanbin.client.Render;

import com.Nanbin.Init;
import com.Nanbin.Registry.RegBlock.BlockCRTAPGDoor1;
import com.Nanbin.Registry.RegBlock.BlockCRTAPGGlass1;
import com.Nanbin.Registry.RegBlock.BlockCRTAPGGlassEnd1;
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
 * CRT 第一代矮屏蔽门渲染器：绘制门页（随开合平移）、门顶指示灯（关门行程闪烁 / 全开常绿 /
 * 列车接近闪烁）以及站台面地灯（站台为 CRT 站台时）。
 */
public class RenderCRTAPGDoor1 implements BlockEntityRenderer<BlockCRTAPGDoor1.BlockEntity> {

	private static final ModelAPGDoorBottom CRT_APG_BOTTOM_1 = new ModelAPGDoorBottom();
	private static final ModelAPGDoorLight CRT_APG_LIGHT_1 = new ModelAPGDoorLight();
	private static final ModelAPGDoorGround CRT_APG_GROUND_LIGHT_1 = new ModelAPGDoorGround();
	private static final ModelSingleCube CRT_APG_DOOR_LOCKED_1 = new ModelSingleCube(6, 6, 5, 20, 1, 3, 3, 0);

	private static final double ARRIVAL_WARNING_BLINK_PERIOD = 40.0;
	private static final double DOOR_MOVING_BLINK_PERIOD = 20.0;
	private static final long ARRIVAL_WARNING_TIME_MS = 60000;
	private static final int PLATFORM_SEARCH_RANGE = 3;

	/** 上一帧的门开合度，用于判断门是否正在运动（运动时指示灯闪烁）。 */
	private final Map<BlockPos, Double> previousOpenMap = new HashMap<>();

	public RenderCRTAPGDoor1(BlockEntityRendererFactory.Context context) {
	}

	@Override
	public void render(BlockCRTAPGDoor1.BlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
		final World world = entity.getWorld();
		if (world == null) {
			return;
		}
		final BlockPos blockPos = entity.getPos();
		if (!(world.getBlockState(blockPos).getBlock() instanceof BlockCRTAPGDoor1)) {
			return;
		}

		final Direction facing = IBlock.getStatePropertySafe(world, blockPos, BlockPSDAPGDoorBase.FACING);
		final boolean side = IBlock.getStatePropertySafe(world, blockPos, BlockPSDAPGDoorBase.SIDE) == IBlock.EnumSide.RIGHT;
		final boolean half = IBlock.getStatePropertySafe(world, blockPos, BlockPSDAPGDoorBase.HALF) == DoubleBlockHalf.UPPER;
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

		// 门顶指示灯：贴在右门页上方的玻璃旁
		if (half && side) {
			final Block neighbour = world.getBlockState(blockPos.offset(facing.rotateYClockwise())).getBlock();
			if (neighbour instanceof BlockCRTAPGGlass1 || neighbour instanceof BlockCRTAPGGlassEnd1) {
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
				drawModel(matrices, vertexConsumers, baseMatrix, blockTexture("crt_apg_door_light_" + lightTexture + "_old.png"), CRT_APG_LIGHT_1, translucent, light, overlay, graphics -> {
					graphics.translate(-0.5F, 0.0F, 0.0F);
					graphics.scale(0.5F, 1.0F, 1.0F);
				});
			}
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
				drawModel(matrices, vertexConsumers, groundMatrix, blockTexture(groundTexture), CRT_APG_GROUND_LIGHT_1, translucent, light, overlay, null);
			}
		}

		// 门页：随开合度沿门框方向平移
		final StoredMatrixTransformations doorMatrix = baseMatrix.copy();
		doorMatrix.add(graphics -> graphics.translate(open * (side ? -1 : 1), 0.0, 0.0));

		if (!half) {
			drawModel(matrices, vertexConsumers, doorMatrix, blockTexture("crt_apg_door_1_bottom_" + (side ? "right" : "left") + ".png"), CRT_APG_BOTTOM_1, false, light, overlay, null);
		}
		if (half && !unlocked) {
			drawModel(matrices, vertexConsumers, doorMatrix, new Identifier("mtr", "textures/block/sign/door_not_in_use.png"), CRT_APG_DOOR_LOCKED_1, false, light, overlay, null);
		}
	}

	@Override
	public boolean rendersOutsideBoundingBox(BlockCRTAPGDoor1.BlockEntity entity) {
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

	private static void drawModel(MatrixStack matrices, VertexConsumerProvider vertexConsumers, StoredMatrixTransformations matrix, Identifier texture, Model model, boolean translucent, int light, int overlay, Consumer<MatrixStack> decoration) {
		matrix.transform(matrices);
		if (decoration != null) {
			decoration.accept(matrices);
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
			Init.LOGGER.error("[CRTAPGDoor1] isTrainApproaching error", e);
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

	/** 门顶指示灯：门框上方的小灯条（含可旋转的遮光片）。 */
	private static class ModelAPGDoorLight extends Model {
		private final ModelMapper bone;

		private ModelAPGDoorLight() {
			super(RenderLayer::getEntityCutout);
			final ModelDataWrapper modelDataWrapper = new ModelDataWrapper(this, 8, 8);
			bone = new ModelMapper(modelDataWrapper);
			bone.texOffs(0, 4).addBox(-8.0F, -1.5F, -5.2F, 4, 1, 1, 0.05F, false);
			final ModelMapper cube = new ModelMapper(modelDataWrapper);
			cube.setPos(0.0F, -9.05F, -4.95F);
			bone.addChild(cube);
			cube.setRotationAngle(0.3927F, 0.0F, 0.0F);
			cube.texOffs(0, 0).addBox(-0.5F, 0.05F, -3.05F, 1, 1, 3, 0.05F, false);
			modelDataWrapper.setModelPart(8, 8);
			bone.setModelPart();
		}

		@Override
		public void render(MatrixStack matrices, net.minecraft.client.render.VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha) {
			bone.render(matrices, vertices, 0.0F, 0.0F, 0.0F, light, overlay);
		}
	}

	/** 站台面地灯：贴地的方形光带。 */
	private static class ModelAPGDoorGround extends Model {
		private final ModelMapper bone;

		private ModelAPGDoorGround() {
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

	/** 门页：底下一段门板 + 顶部斜面板。 */
	private static class ModelAPGDoorBottom extends Model {
		private final ModelMapper bone;

		private ModelAPGDoorBottom() {
			super(RenderLayer::getEntityCutout);
			final ModelDataWrapper modelDataWrapper = new ModelDataWrapper(this, 34, 27);
			bone = new ModelMapper(modelDataWrapper);
			bone.texOffs(0, 0).addBox(-8.0F, -17.0F, -7.0F, 16, 16, 1, 0.0F, false);
			bone.texOffs(0, 17).addBox(-8.0F, -7.0F, -8.0F, 16, 6, 1, 0.0F, false);
			final ModelMapper cube = new ModelMapper(modelDataWrapper);
			cube.setPos(0.0F, -5.0F, -8.0F);
			bone.addChild(cube);
			cube.setRotationAngle(-0.7854F, 0.0F, 0.0F);
			cube.texOffs(0, 24).addBox(-8.0F, -3.0F, -2.0F, 16, 2, 1, 0.0F, false);
			modelDataWrapper.setModelPart(34, 27);
			bone.setModelPart();
		}

		@Override
		public void render(MatrixStack matrices, net.minecraft.client.render.VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha) {
			bone.render(matrices, vertices, 0.0F, 0.0F, 0.0F, light, overlay);
		}
	}

	/** 单个方块大小的立方体模型（用于「此门停用」提示牌等）。 */
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
