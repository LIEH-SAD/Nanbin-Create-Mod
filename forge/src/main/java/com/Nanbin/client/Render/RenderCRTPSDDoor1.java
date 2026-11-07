package com.Nanbin.client.Render;

import com.Nanbin.InitClient;
import com.Nanbin.Registry.RegBlock.BlockCRTPlatform;
import org.mtr.core.data.Platform;
import org.mtr.core.data.Position;
import org.mtr.core.operation.ArrivalResponse;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockEntityRenderer;
import org.mtr.mapping.mapper.EntityModelExtension;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.ModelPartExtension;
import org.mtr.mod.block.BlockPSDAPGDoorBase;
import org.mtr.mod.block.IBlock;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.data.ArrivalsCacheClient;
import org.mtr.mod.data.IGui;
import org.mtr.mod.render.MainRenderer;
import org.mtr.mod.render.QueuedRenderLayer;
import org.mtr.mod.render.StoredMatrixTransformations;

import java.util.HashMap;
import java.util.Map;

public class RenderCRTPSDDoor1<T extends BlockPSDAPGDoorBase.BlockEntityBase> extends BlockEntityRenderer<T> implements IGui, IBlock {
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

    public RenderCRTPSDDoor1(BlockEntityRenderer.Argument dispatcher) {
        super(dispatcher);
    }

    private boolean isTrainApproaching(BlockPos doorPos) {
        try {
            final MinecraftClientData clientData = MinecraftClientData.getInstance();
            final LongAVLTreeSet platformIds = new LongAVLTreeSet();

            for (final Platform platform : clientData.platformIdMap.values()) {
                if (isNearPlatform(doorPos, platform)) {
                    platformIds.add(platform.getId());
                }
            }

            if (platformIds.isEmpty()) {
                return false;
            }

            final ObjectArrayList<ArrivalResponse> arrivals = ArrivalsCacheClient.INSTANCE.requestArrivals(platformIds);
            final long millisOffset = ArrivalsCacheClient.INSTANCE.getMillisOffset();
            final long now = System.currentTimeMillis();

            long earliestArrival = Long.MAX_VALUE;
            for (final ArrivalResponse response : arrivals) {
                final long timeUntilArrival = response.getArrival() - millisOffset - now;
                if (timeUntilArrival > 0 && timeUntilArrival < earliestArrival) {
                    earliestArrival = timeUntilArrival;
                }
            }

            if (earliestArrival < Long.MAX_VALUE) {
                return earliestArrival <= ARRIVAL_WARNING_TIME_MS;
            }
        } catch (Exception e) {
            InitClient.LOGGER.error("[CRTPSDDoor1] isTrainApproaching error", e);
        }
        return false;
    }

    private boolean isNearPlatform(BlockPos doorPos, Platform platform) {
        final Position position1 = platform.getRandomPosition();
        final Position position2 = platform.getOtherPosition(position1);
        final long minX = Math.min(position1.getX(), position2.getX()) - PLATFORM_SEARCH_RANGE;
        final long maxX = Math.max(position1.getX(), position2.getX()) + PLATFORM_SEARCH_RANGE;
        final long minZ = Math.min(position1.getZ(), position2.getZ()) - PLATFORM_SEARCH_RANGE;
        final long maxZ = Math.max(position1.getZ(), position2.getZ()) + PLATFORM_SEARCH_RANGE;
        return doorPos.getX() >= minX && doorPos.getX() <= maxX && doorPos.getZ() >= minZ && doorPos.getZ() <= maxZ;
    }

    public void render(T entity, float tickDelta, GraphicsHolder graphicsHolder, int light, int overlay) {
        World world = entity.getWorld2();
        if (world != null) {
            entity.tick(tickDelta);
            BlockPos blockPos = entity.getPos2();
            Direction facing = IBlock.getStatePropertySafe(world, blockPos, BlockPSDAPGDoorBase.FACING);
            boolean side = IBlock.getStatePropertySafe(world, blockPos, BlockPSDAPGDoorBase.SIDE) == EnumSide.RIGHT;
            boolean half = IBlock.getStatePropertySafe(world, blockPos, BlockPSDAPGDoorBase.HALF) == DoubleBlockHalf.UPPER;
            boolean end = IBlock.getStatePropertySafe(world, blockPos, BlockPSDAPGDoorBase.END);
            boolean unlocked = IBlock.getStatePropertySafe(world, blockPos, BlockPSDAPGDoorBase.UNLOCKED);
            double open = Math.min(entity.getDoorValue(), (double)1.0F);
            final boolean isDoorFullyOpen = open >= 1.0 - 0.01;
            final double prevOpen = previousOpenMap.getOrDefault(blockPos, -1.0);
            final boolean isDoorMoving = prevOpen >= 0 && Math.abs(open - prevOpen) > 0.001;
            previousOpenMap.put(blockPos, open);
            StoredMatrixTransformations storedMatrixTransformations = new StoredMatrixTransformations((double)0.5F + (double)entity.getPos2().getX(), (double)entity.getPos2().getY(), (double)0.5F + (double)entity.getPos2().getZ());
            storedMatrixTransformations.add((graphicsHolderNew) -> {
                graphicsHolderNew.rotateYDegrees(-facing.asRotation());
                graphicsHolderNew.rotateXDegrees(180.0F);
            });
            StoredMatrixTransformations storedMatrixTransformationsLight = storedMatrixTransformations.copy();
            StoredMatrixTransformations storedMatrixTransformationsGround = new StoredMatrixTransformations((double)0.5F + (double)entity.getPos2().getX(), (double)entity.getPos2().getY(), (double)0.5F + (double)entity.getPos2().getZ());
            storedMatrixTransformationsGround.add((graphicsHolderNew) -> graphicsHolderNew.rotateYDegrees(-facing.asRotation()));
            if (half) {
                String lightTexture;
                QueuedRenderLayer lightRenderLayer;
                if (isDoorFullyOpen) {
                    lightTexture = "green";
                    lightRenderLayer = QueuedRenderLayer.LIGHT_TRANSLUCENT;
                } else if (isDoorMoving) {
                    long gameTime = world.getTime();
                    boolean blinkState = (gameTime % (long) DOOR_MOVING_BLINK_PERIOD) < (DOOR_MOVING_BLINK_PERIOD / 2.0);
                    lightTexture = blinkState ? "off" : "green";
                    lightRenderLayer = QueuedRenderLayer.LIGHT_TRANSLUCENT;
                } else {
                    lightTexture = "off";
                    lightRenderLayer = QueuedRenderLayer.EXTERIOR;
                }
                MainRenderer.scheduleRender(new Identifier(String.format("nanbin:textures/block/crt_door_light_%s.png", lightTexture)), false, lightRenderLayer, (graphicsHolderNew, offset) -> {
                    storedMatrixTransformationsLight.transform(graphicsHolderNew, offset);
                    (side ? MODEL_PSD_LIGHT_RIGHT : MODEL_PSD_LIGHT_LEFT).render(graphicsHolderNew, light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
                    graphicsHolderNew.pop();
                });
            }

            if (!half) {
                Block block = world.getBlockState(blockPos.offset(Direction.DOWN)).getBlock();
                if (block.data instanceof BlockCRTPlatform) {
                    String groundLightTexture;
                    QueuedRenderLayer groundRenderLayer;
                    if (isDoorFullyOpen) {
                        groundLightTexture = "green";
                        groundRenderLayer = QueuedRenderLayer.LIGHT_TRANSLUCENT;
                    } else if (isDoorMoving) {
                        groundLightTexture = "off";
                        groundRenderLayer = QueuedRenderLayer.EXTERIOR;
                    } else if (isTrainApproaching(blockPos)) {
                        long gameTime = world.getTime();
                        boolean blinkState = (gameTime % (long) ARRIVAL_WARNING_BLINK_PERIOD) < (ARRIVAL_WARNING_BLINK_PERIOD / 2.0);
                        groundLightTexture = blinkState ? "off" : "yellow";
                        groundRenderLayer = QueuedRenderLayer.LIGHT_TRANSLUCENT;
                    } else {
                        groundLightTexture = "off";
                        groundRenderLayer = QueuedRenderLayer.EXTERIOR;
                    }
                    String groundTexture = side ? String.format("crt_ground_light_%s.png", groundLightTexture) : String.format("crt_ground_light_%s_flipped.png", groundLightTexture);
                    MainRenderer.scheduleRender(new Identifier("nanbin:textures/block/" + groundTexture), false, groundRenderLayer, (graphicsHolderNew, offset) -> {
                        storedMatrixTransformationsGround.transform(graphicsHolderNew, offset);
                        MODEL_GROUND_LIGHT.render(graphicsHolderNew, light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
                        graphicsHolderNew.pop();
                    });
                }
            }

            if (end) {
                MainRenderer.scheduleRender(new Identifier(String.format("nanbin:textures/block/crt_psd_door_end_%s_%s_2_1.png", half ? "top" : "bottom", side ? "right" : "left")), false, QueuedRenderLayer.EXTERIOR, (graphicsHolderNew, offset) -> {
                    storedMatrixTransformationsLight.transform(graphicsHolderNew, offset);
                    graphicsHolderNew.translate(open / (double)2.0F * (double)(side ? -1 : 1), (double)0.0F, (double)0.0F);
                    (side ? MODEL_PSD_END_RIGHT_2 : MODEL_PSD_END_LEFT_2).render(graphicsHolderNew, light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
                    graphicsHolderNew.pop();
                });
            }

            storedMatrixTransformations.add((matricesNew) -> matricesNew.translate(open * (double)(side ? -1 : 1), (double)0.0F, (double)0.0F));
            if (end) {
                MainRenderer.scheduleRender(new Identifier(String.format("nanbin:textures/block/crt_psd_door_end_%s_%s_1_1.png", half ? "top" : "bottom", side ? "right" : "left")), false, QueuedRenderLayer.EXTERIOR, (graphicsHolderNew, offset) -> {
                    storedMatrixTransformations.transform(graphicsHolderNew, offset);
                    (side ? MODEL_PSD_END_RIGHT_1 : MODEL_PSD_END_LEFT_1).render(graphicsHolderNew, light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
                    graphicsHolderNew.pop();
                });
            } else {
                MainRenderer.scheduleRender(new Identifier(String.format("nanbin:textures/block/crt_psd_door_%s_%s_1.png", half ? "top" : "bottom", side ? "right" : "left")), false, QueuedRenderLayer.EXTERIOR, (graphicsHolderNew, offset) -> {
                    storedMatrixTransformations.transform(graphicsHolderNew, offset);
                    MODEL_PSD.render(graphicsHolderNew, light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
                    graphicsHolderNew.pop();
                });
            }

            if (half && !unlocked) {
                MainRenderer.scheduleRender(new Identifier("mtr", "textures/block/sign/door_not_in_use.png"), false, QueuedRenderLayer.EXTERIOR, (graphicsHolderNew, offset) -> {
                    storedMatrixTransformations.transform(graphicsHolderNew, offset);
                    if (end) {
                        graphicsHolderNew.translate(side ? (double)0.25F : (double)-0.25F, (double)0.0F, (double)0.0F);
                    }

                    MODEL_PSD_DOOR_LOCKED.render(graphicsHolderNew, light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
                    graphicsHolderNew.pop();
                });
            }

        }
    }

    public boolean rendersOutsideBoundingBox2(T blockEntity) {
        return true;
    }

    private static class ModelGroundLight extends EntityModelExtension<EntityAbstractMapping> {
        private final ModelPartExtension bone = this.createModelPart();

        private ModelGroundLight() {
            super(16, 16);
            this.bone.setTextureUVOffset(0, 0).addCuboid(-8.0F, 0.0F, -8.0F, 16, 0, 16, 0.0F, false);
            this.buildModel();
        }

        public void render(GraphicsHolder graphicsHolder, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
            this.bone.render(graphicsHolder, 0.0F, 0.0F, 0.0F, packedLight, packedOverlay);
        }

        public void setAngles2(EntityAbstractMapping entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        }
    }

    private static class ModelSingleCube extends EntityModelExtension<EntityAbstractMapping> {
        private final ModelPartExtension cube = this.createModelPart();

        private ModelSingleCube(int textureWidth, int textureHeight, int x, int y, int z, int length, int height, int depth) {
            super(textureWidth, textureHeight);
            this.cube.setTextureUVOffset(0, 0).addCuboid((float)(x - 8), (float)(y - 16), (float)(z - 8), length, height, depth, 0.0F, false);
            this.buildModel();
        }

        public void render(GraphicsHolder graphicsHolder, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
            this.cube.render(graphicsHolder, 0.0F, 0.0F, 0.0F, packedLight, packedOverlay);
        }

        public void setAngles2(EntityAbstractMapping entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        }
    }
}
