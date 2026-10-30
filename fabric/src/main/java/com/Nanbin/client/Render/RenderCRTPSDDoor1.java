package com.Nanbin.client.Render;

import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockEntityRenderer;
import org.mtr.mapping.mapper.EntityModelExtension;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.ModelPartExtension;
import org.mtr.mod.block.BlockPSDAPGDoorBase;
import org.mtr.mod.block.IBlock;
import org.mtr.mod.data.IGui;
import org.mtr.mod.render.MainRenderer;
import org.mtr.mod.render.QueuedRenderLayer;
import org.mtr.mod.render.StoredMatrixTransformations;

public class RenderCRTPSDDoor1<T extends BlockPSDAPGDoorBase.BlockEntityBase> extends BlockEntityRenderer<T> implements IGui, IBlock {
    private static final ModelSingleCube MODEL_PSD = new ModelSingleCube(36, 18, 0, 0, 0, 16, 16, 2);
    private static final ModelSingleCube MODEL_PSD_END_LEFT_1 = new ModelSingleCube(20, 18, 0, 0, 0, 8, 16, 2);
    private static final ModelSingleCube MODEL_PSD_END_RIGHT_1 = new ModelSingleCube(20, 18, 8, 0, 0, 8, 16, 2);
    private static final ModelSingleCube MODEL_PSD_END_LEFT_2 = new ModelSingleCube(20, 18, 8, 0, 2, 8, 16, 2);
    private static final ModelSingleCube MODEL_PSD_END_RIGHT_2 = new ModelSingleCube(20, 18, 0, 0, 2, 8, 16, 2);
    private static final ModelSingleCube MODEL_PSD_LIGHT_LEFT = new ModelSingleCube(16, 16, 0, -2, 6, 1, 1, 1);
    private static final ModelSingleCube MODEL_PSD_LIGHT_RIGHT = new ModelSingleCube(16, 16, 15, -2, 6, 1, 1, 1);
    private static final ModelSingleCube MODEL_PSD_DOOR_LOCKED = new ModelSingleCube(6, 6, 5, 6, 1, 6, 6, 0);

    public RenderCRTPSDDoor1(BlockEntityRenderer.Argument dispatcher) {
        super(dispatcher);
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
            StoredMatrixTransformations storedMatrixTransformations = new StoredMatrixTransformations((double)0.5F + (double)entity.getPos2().getX(), (double)entity.getPos2().getY(), (double)0.5F + (double)entity.getPos2().getZ());
            storedMatrixTransformations.add((graphicsHolderNew) -> {
                graphicsHolderNew.rotateYDegrees(-facing.asRotation());
                graphicsHolderNew.rotateXDegrees(180.0F);
            });
            StoredMatrixTransformations storedMatrixTransformationsLight = storedMatrixTransformations.copy();
            if (half) {
                MainRenderer.scheduleRender(new Identifier(String.format("nanbin:textures/block/crt_door_light_%s.png", open > (double)0.0F ? "on" : "off")), false, open > (double)0.0F ? QueuedRenderLayer.LIGHT : QueuedRenderLayer.EXTERIOR, (graphicsHolderNew, offset) -> {
                    storedMatrixTransformationsLight.transform(graphicsHolderNew, offset);
                    (side ? MODEL_PSD_LIGHT_RIGHT : MODEL_PSD_LIGHT_LEFT).render(graphicsHolderNew, light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
                    graphicsHolderNew.pop();
                });
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
