package com.Nanbin.client.Render;

import com.Nanbin.InitClient;
import com.Nanbin.Registry.RegBlock.BlockCRTAPGDoor2;
import com.Nanbin.Registry.RegBlock.BlockCRTAPGGlass2;
import com.Nanbin.Registry.RegBlock.BlockCRTAPGGlassEnd2;
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
import org.mtr.mod.block.*;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.data.ArrivalsCacheClient;
import org.mtr.mod.data.IGui;
import org.mtr.mod.render.MainRenderer;
import org.mtr.mod.render.QueuedRenderLayer;
import org.mtr.mod.render.StoredMatrixTransformations;

import java.util.HashMap;
import java.util.Map;

public class RenderCRTAPGDoor2<T extends BlockCRTAPGDoor2.BlockEntityBase> extends BlockEntityRenderer<T> implements IGui, IBlock {
    private final int type;
    private static final ModelSingleCube CRT_APG_TOP_2 = new ModelSingleCube(34, 9, 0, 8, 1, 16, 8, 1);
    private static final ModelAPGDoorBottom CRT_APG_BOTTOM_2 = new ModelAPGDoorBottom();
    private static final ModelAPGDoorLight CRT_APG_LIGHT_2 = new ModelAPGDoorLight();
    private static final ModelAPGDoorGround CRT_APG_GROUND_LIGHT_2 = new ModelAPGDoorGround();
    private static final ModelSingleCube CRT_APG_DOOR_LOCKED_2 = new ModelSingleCube(6, 6, 5, 10, 1, 6, 6, 0);

    private static final double ARRIVAL_WARNING_BLINK_PERIOD = 40.0;
    private static final double DOOR_MOVING_BLINK_PERIOD = 20.0;
    private static final long ARRIVAL_WARNING_TIME_MS = 60000;
    private static final int PLATFORM_SEARCH_RANGE = 3;

    private final Map<BlockPos, Double> previousOpenMap = new HashMap<>();

    public RenderCRTAPGDoor2(Argument dispatcher, int type) {
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
            InitClient.LOGGER.error("[CRTAPGDoor2] isTrainApproaching error", e);
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
                    if (half) {
                        Block block = world.getBlockState(blockPos.offset(side ? facing.rotateYClockwise() : facing.rotateYCounterclockwise())).getBlock();
                        //穷举实例（两个MTR的门用来防呆）
                        if (block.data instanceof BlockCRTAPGGlass2 || block.data instanceof BlockCRTAPGGlassEnd2 || block.data instanceof BlockAPGGlass || block.data instanceof BlockAPGGlassEnd) {
                            final boolean isTrainApproaching = isTrainApproaching(blockPos);
                            final boolean isDoorFullyOpen = open >= (this.type >= 3 ? 0.75 : 1.0) - 0.01;
                            final double prevOpen = previousOpenMap.getOrDefault(blockPos, -1.0);
                            final boolean isDoorMoving = prevOpen >= 0 && Math.abs(open - prevOpen) > 0.001;
                            previousOpenMap.put(blockPos, open);

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
                                long gameTime = world.getTime();
                                boolean blinkState = (gameTime % (long) ARRIVAL_WARNING_BLINK_PERIOD) < (ARRIVAL_WARNING_BLINK_PERIOD / 2.0);
                                lightTexture = blinkState ? "off" : "yellow";
                                renderLayer = QueuedRenderLayer.LIGHT_TRANSLUCENT;
                            } else {
                                lightTexture = "off";
                                renderLayer = QueuedRenderLayer.EXTERIOR;
                            }

                            MainRenderer.scheduleRender(new Identifier(String.format("nanbin:textures/block/crt_apg_door_light_%s.png", lightTexture)), false, renderLayer, (graphicsHolderNew, offset) -> {
                                storedMatrixTransformationsLight.transform(graphicsHolderNew, offset);
                                graphicsHolderNew.translate(side ? (double) -0.50F : (double) 0.50F, (double) 0.0F, (double) 0.0F);
                                graphicsHolderNew.scale(0.5F, 1.0F, 1.0F);
                                CRT_APG_LIGHT_2.render(graphicsHolderNew, light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
                                graphicsHolderNew.pop();
                            });
                        }
                    }
                    if (!half) {
                        // Use different textures for left/right sides to avoid flipping issues
                        // Demand Users Choose CRT Platform to avoid textures error
                        Block block = world.getBlockState(blockPos.offset(Direction.DOWN)).getBlock();
                        if (block.data instanceof BlockCRTPlatform) {
                            String groundTexture = side ? "crt_ground_light_off.png" : "crt_ground_light_off_flipped.png";
                            MainRenderer.scheduleRender(new Identifier("nanbin:textures/block/" + groundTexture), false, QueuedRenderLayer.EXTERIOR, (graphicsHolderNew, offset) -> {
                                storedMatrixTransformationsGround.transform(graphicsHolderNew, offset);
                                CRT_APG_GROUND_LIGHT_2.render(graphicsHolderNew, light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
                                graphicsHolderNew.pop();
                            });
                        }
                    }
            }

            storedMatrixTransformations.add((matricesNew) -> matricesNew.translate(open * (double)(side ? -1 : 1), (double)0.0F, (double)0.0F));
            switch (this.type) {
                case 0:
                case 2:
                    MainRenderer.scheduleRender(new Identifier(String.format("nanbin:textures/block/crt_apg_door_2_%s_%s.png", half ? "top" : "bottom", side ? "right" : "left")), false, QueuedRenderLayer.EXTERIOR, (graphicsHolderNew, offset) -> {
                        storedMatrixTransformations.transform(graphicsHolderNew, offset);
                        ((EntityModelExtension)(half ? CRT_APG_TOP_2 : CRT_APG_BOTTOM_2)).render(graphicsHolderNew, light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
                        graphicsHolderNew.pop();
                    });
                    if (half && !unlocked) {
                        MainRenderer.scheduleRender(new Identifier("mtr", "textures/block/sign/door_not_in_use.png"), false, QueuedRenderLayer.EXTERIOR, (graphicsHolderNew, offset) -> {
                            storedMatrixTransformations.transform(graphicsHolderNew, offset);
                            CRT_APG_DOOR_LOCKED_2.render(graphicsHolderNew, light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
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
                this.bone.setTextureUVOffset(0, 4).addCuboid(-0.5F, -9.0F, -7.0F, 1, 1, 3, 0.05F, false);
                ModelPartExtension cube = this.bone.addChild();
                cube.setPivot(0.0F, -9.05F, -4.95F);
                cube.setRotation(0.3927F, 0.0F, 0.0F);
                cube.setTextureUVOffset(0, 0).addCuboid(-0.5F, 0.05F, -3.05F, 1, 1, 3, 0.05F, false);
                this.buildModel();
            }

            public void render(GraphicsHolder graphicsHolder, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
                this.bone.render(graphicsHolder, 0.0F, 0.0F, 0.0F, packedLight, packedOverlay);
            }

            public void setAngles2(EntityAbstractMapping entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
            }
        }

    /**
     * Ground lightning for 2nd-gen APG door (no flash).
     * A flat 1x1 block square (16x1x16 pixels) rendered on the floor at the bottom of the door.
     * Texture file is reserved at nanbin:textures/block/crt_apg_ground_light.png.
     */
    private static class ModelAPGDoorGround extends EntityModelExtension<EntityAbstractMapping> {
        private final ModelPartExtension bone = this.createModelPart();

        private ModelAPGDoorGround() {
            super(16, 16);
            this.bone.setTextureUVOffset(0, 0).addCuboid(-8.0F, 0.0F, -8.0F, 16, 1, 16, 0.0F, false);
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
            this.bone.setTextureUVOffset(0, 0).addCuboid(-8.0F, -16.0F, -7.0F, 16, 16, 1, 0.0F, false);
            this.bone.setTextureUVOffset(0, 17).addCuboid(-8.0F, -6.0F, -8.0F, 16, 6, 1, 0.0F, false);
            ModelPartExtension cube = this.bone.addChild();
            cube.setPivot(0.0F, -6.0F, -8.0F);
            cube.setRotation(-0.7854F, 0.0F, 0.0F);
            cube.setTextureUVOffset(0, 24).addCuboid(-8.0F, -2.0F, 0.0F, 16, 2, 1, 0.0F, false);
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