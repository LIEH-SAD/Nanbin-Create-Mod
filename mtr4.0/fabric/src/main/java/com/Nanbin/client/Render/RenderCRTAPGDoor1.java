package com.Nanbin.client.Render;

import com.Nanbin.InitClient;
import com.Nanbin.Registry.RegBlock.*;
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
import org.mtr.mod.block.*;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.data.ArrivalsCacheClient;
import org.mtr.mod.data.IGui;
import org.mtr.mod.render.MainRenderer;
import org.mtr.mod.render.QueuedRenderLayer;
import org.mtr.mod.render.StoredMatrixTransformations;

import java.util.HashMap;
import java.util.Map;

public class RenderCRTAPGDoor1<T extends BlockCRTAPGDoor1.BlockEntityBase> extends BlockEntityRenderer<T> implements IGui, IBlock {
    private final int type;
    private static final ModelAPGDoorBottom CRT_APG_BOTTOM_1 = new ModelAPGDoorBottom();
    private static final ModelAPGDoorLight CRT_APG_LIGHT_1 = new ModelAPGDoorLight();
    private static final ModelAPGDoorGround CRT_APG_GROUND_LIGHT_1 = new ModelAPGDoorGround();
    private static final ModelSingleCube CRT_APG_DOOR_LOCKED_1 = new ModelSingleCube(6, 6, 5, 20, 1, 3, 3, 0);

    private static final double ARRIVAL_WARNING_BLINK_PERIOD = 40.0;
    private static final double DOOR_MOVING_BLINK_PERIOD = 20.0;
    private static final long ARRIVAL_WARNING_TIME_MS = 60000;
    private static final int PLATFORM_SEARCH_RANGE = 3;

    private final Map<BlockPos, Double> previousOpenMap = new HashMap<>();

    public RenderCRTAPGDoor1(Argument dispatcher, int type) {
        super(dispatcher);
        this.type = type;
    }

    private boolean isTrainApproaching(BlockPos doorPos) {
        try {
            final MinecraftClientData clientData = MinecraftClientData.getInstance();
            final LongAVLTreeSet platformIds = new LongAVLTreeSet();

            for (final Platform platform : clientData.platformIdMap.values()) {
                final Position midPos = platform.getMidPosition();
                final int dx = Math.abs((int) midPos.getX() - doorPos.getX());
                final int dz = Math.abs((int) midPos.getZ() - doorPos.getZ());
                if (dx <= PLATFORM_SEARCH_RANGE && dz <= PLATFORM_SEARCH_RANGE) {
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
            InitClient.LOGGER.error("[CRTAPGDoor1] isTrainApproaching error", e);
        }
        return false;
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
            double open = Math.min(entity.getDoorValue(), this.type >= 3 ? (double) 0.75F : (double) 1.0F);
            StoredMatrixTransformations storedMatrixTransformations = new StoredMatrixTransformations((double) 0.5F + (double) entity.getPos2().getX(), (double) entity.getPos2().getY(), (double) 0.5F + (double) entity.getPos2().getZ());
            storedMatrixTransformations.add((graphicsHolderNew) -> {
                graphicsHolderNew.rotateYDegrees(-facing.asRotation());
                graphicsHolderNew.rotateXDegrees(180.0F);
            });
            StoredMatrixTransformations storedMatrixTransformationsLight = storedMatrixTransformations.copy();
            StoredMatrixTransformations storedMatrixTransformationsGround = new StoredMatrixTransformations((double) 0.5F + (double) entity.getPos2().getX(), (double) entity.getPos2().getY(), (double) 0.5F + (double) entity.getPos2().getZ());
            storedMatrixTransformationsGround.add((graphicsHolderNew) -> {
                graphicsHolderNew.rotateYDegrees(-facing.asRotation());
            });
            switch (this.type) {
                case 0:
                case 2:
                    final boolean isTrainApproaching = isTrainApproaching(blockPos);
                    final boolean isDoorFullyOpen = open >= (this.type >= 3 ? 0.75 : 1.0) - 0.01;
                    final double prevOpen = previousOpenMap.getOrDefault(blockPos, -1.0);
                    final boolean isDoorMoving = prevOpen >= 0 && Math.abs(open - prevOpen) > 0.001;
                    previousOpenMap.put(blockPos, open);

                    if (half && side) {
                        Block block = world.getBlockState(blockPos.offset(facing.rotateYClockwise())).getBlock();
                        if (block.data instanceof BlockCRTAPGGlass1 || block.data instanceof BlockCRTAPGGlassEnd1) {
                            String lightTexture;
                            QueuedRenderLayer renderLayer;
                            if (isDoorFullyOpen) {
                                lightTexture = "green";
                                renderLayer = QueuedRenderLayer.LIGHT_TRANSLUCENT;
                            } else if (isDoorMoving) {
                                long gameTime = world.getTime();
                                boolean blinkState = (gameTime % (long) DOOR_MOVING_BLINK_PERIOD) < (DOOR_MOVING_BLINK_PERIOD / 2.0);
                                lightTexture = blinkState ? "off" : "green";
                                renderLayer = QueuedRenderLayer.LIGHT_TRANSLUCENT;
                            } else if (isTrainApproaching) {
                                lightTexture = "off";
                                renderLayer = QueuedRenderLayer.EXTERIOR;
                            } else {
                                lightTexture = "off";
                                renderLayer = QueuedRenderLayer.EXTERIOR;
                            }

                            MainRenderer.scheduleRender(new Identifier(String.format("nanbin:textures/block/crt_apg_door_light_%s_old.png", lightTexture)), false, renderLayer, (graphicsHolderNew, offset) -> {
                                storedMatrixTransformationsLight.transform(graphicsHolderNew, offset);
                                graphicsHolderNew.translate((double) -0.50F, (double) 0.0F, (double) 0.0F);
                                graphicsHolderNew.scale(0.5F, 1.0F, 1.0F);
                                CRT_APG_LIGHT_1.render(graphicsHolderNew, light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
                                graphicsHolderNew.pop();
                            });
                        }
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
                            } else if (isTrainApproaching) {
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
                                CRT_APG_GROUND_LIGHT_1.render(graphicsHolderNew, light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
                                graphicsHolderNew.pop();
                            });
                        }
                    }
            }

            storedMatrixTransformations.add((matricesNew) -> matricesNew.translate(open * (double)(side ? -1 : 1), (double)0.0F, (double)0.0F));
            switch (this.type) {
                case 0:
                case 2:
                    if (!half) {
                        MainRenderer.scheduleRender(new Identifier("nanbin:textures/block/crt_apg_door_1_bottom_" + (side ? "right" : "left") + ".png"), false, QueuedRenderLayer.EXTERIOR, (graphicsHolderNew, offset) -> {
                            storedMatrixTransformations.transform(graphicsHolderNew, offset);
                            CRT_APG_BOTTOM_1.render(graphicsHolderNew, light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
                            graphicsHolderNew.pop();
                        });
                    }
                    if (half && !unlocked) {
                        MainRenderer.scheduleRender(new Identifier("mtr", "textures/block/sign/door_not_in_use.png"), false, QueuedRenderLayer.EXTERIOR, (graphicsHolderNew, offset) -> {
                            storedMatrixTransformations.transform(graphicsHolderNew, offset);
                            CRT_APG_DOOR_LOCKED_1.render(graphicsHolderNew, light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
                            graphicsHolderNew.pop();
                        });
                    }
                    break;
                case 4:
                    if (IBlock.getStatePropertySafe(world, blockPos, TripleHorizontalBlock.CENTER)) {
                        break;
                    }

                    storedMatrixTransformations.add((matricesNew) -> matricesNew.translate(side ? (double)0.5F : (double)-0.5F, (double)0.0F, (double)0.0F));
            }
        }
    }

        private static class ModelAPGDoorLight extends EntityModelExtension<EntityAbstractMapping> {
            private final ModelPartExtension bone = this.createModelPart();

            private ModelAPGDoorLight() {
                super(8, 8);
                this.bone.setTextureUVOffset(0, 4).addCuboid(-8.0F, -1.5F, -5.2F, 4, 1, 1, 0.05F, false);
                this.buildModel();
            }

            public void render(GraphicsHolder graphicsHolder, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
                this.bone.render(graphicsHolder, 0.0F, 0.0F, 0.0F, packedLight, packedOverlay);
            }

            public void setAngles2(EntityAbstractMapping entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
            }
        }

    /**
     * Ground lightning for 1st-gen APG door (flash).
     */
    private static class ModelAPGDoorGround extends EntityModelExtension<EntityAbstractMapping> {
        private final ModelPartExtension bone = this.createModelPart();

        private ModelAPGDoorGround() {
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

    private static class ModelAPGDoorBottom extends EntityModelExtension<EntityAbstractMapping> {
        private final ModelPartExtension bone = this.createModelPart();

        private ModelAPGDoorBottom() {
            super(34, 27);
            this.bone.setTextureUVOffset(0, 0).addCuboid(-8.0F, -17.0F, -7.0F, 16, 16, 1, 0.0F, false);
            this.bone.setTextureUVOffset(0, 17).addCuboid(-8.0F, -7.0F, -8.0F, 16, 6, 1, 0.0F, false);
            ModelPartExtension cube = this.bone.addChild();
            cube.setPivot(0.0F, -5.0F, -8.0F);
            cube.setRotation(-0.7854F, 0.0F, 0.0F);
            cube.setTextureUVOffset(0, 24).addCuboid(-8.0F, -3.0F, -2.0F, 16, 2, 1, 0.0F, false);
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