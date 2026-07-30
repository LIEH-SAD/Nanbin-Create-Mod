
package com.Nanbin.client.Render;

import com.Nanbin.Registry.RegBlock.BlockPSDTOP;
import org.mtr.mapping.holder.Block;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.MathHelper;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.BlockEntityRenderer;
import org.mtr.mod.block.BlockPSDAPGDoorBase;
import org.mtr.mod.block.BlockPSDAPGGlassEndBase;
import org.mtr.mod.block.BlockPSDTop;
import org.mtr.mod.block.IBlock;
import org.mtr.mod.block.BlockPSDTop.EnumPersistent;
import org.mtr.mod.client.DynamicTextureCache;
import org.mtr.mod.client.IDrawing;
import org.mtr.mod.render.MainRenderer;
import org.mtr.mod.render.QueuedRenderLayer;
import org.mtr.mod.render.RenderRouteBase;
import org.mtr.mod.render.StoredMatrixTransformations;

public class RenderPSDTOP extends RenderRouteBase<BlockPSDTOP.BlockEntity> {
    private static final float END_FRONT_OFFSET = 1.0F / (MathHelper.getSquareRootOfTwoMapped() * 16.0F);
    private static final float BOTTOM_DIAGONAL_OFFSET = ((float)Math.sqrt((double)3.0F) - 1.0F) / 32.0F;
    private static final float ROOT_TWO_SCALED = MathHelper.getSquareRootOfTwoMapped() / 16.0F;
    private static final float BOTTOM_END_DIAGONAL_OFFSET;
    private static final float COLOR_STRIP_START = 0.90625F;
    private static final float COLOR_STRIP_END = 0.9375F;

    public RenderPSDTOP(BlockEntityRenderer.Argument dispatcher) {
        super(dispatcher, 1.95F, 7.5F, 1.5F, 0.125F, true, 3, BlockPSDTop.ARROW_DIRECTION);
    }

    protected RenderRouteBase.RenderType getRenderType(World world, BlockPos pos, BlockState state) {
        BlockPSDTop.EnumPersistent persistent = (BlockPSDTop.EnumPersistent)IBlock.getStatePropertySafe(state, BlockPSDTop.PERSISTENT);
        if (persistent == EnumPersistent.NONE) {
            Block blockBelow = world.getBlockState(pos.down()).getBlock();
            if (blockBelow.data instanceof BlockPSDAPGDoorBase) {
                return RenderType.ARROW;
            } else {
                return !(blockBelow.data instanceof BlockPSDAPGGlassEndBase) ? RenderType.ROUTE : RenderType.NONE;
            }
        } else {
            return persistent == EnumPersistent.ARROW ? RenderType.ARROW : (persistent == EnumPersistent.ROUTE ? RenderType.ROUTE : RenderType.NONE);
        }
    }

    protected void renderAdditionalUnmodified(StoredMatrixTransformations storedMatrixTransformations, BlockState state, Direction facing, int light) {
        boolean airLeft = IBlock.getStatePropertySafe(state, BlockPSDTop.AIR_LEFT);
        boolean airRight = IBlock.getStatePropertySafe(state, BlockPSDTop.AIR_RIGHT);
        boolean persistent = IBlock.getStatePropertySafe(state, BlockPSDTop.PERSISTENT) != EnumPersistent.NONE;
        if ((airLeft || airRight) && !persistent) {
            MainRenderer.scheduleRender(new Identifier("mtr", "textures/block/psd_top.png"), false, QueuedRenderLayer.EXTERIOR, (graphicsHolder, offset) -> {
                storedMatrixTransformations.transform(graphicsHolder, offset);
                if (airLeft) {
                    IDrawing.drawTexture(graphicsHolder, -0.125F, 0.0F, 0.5F, 0.5F, 0.0F, -0.125F, 0.5F, 1.0F, -0.125F, -0.125F, 1.0F, 0.5F, 0.0F, 0.0F, 1.0F, 1.0F, facing, -1, light);
                    IDrawing.drawTexture(graphicsHolder, 0.5F - END_FRONT_OFFSET, 0.0625F, -0.5F - END_FRONT_OFFSET, -0.25F - END_FRONT_OFFSET, 0.0625F, 0.25F - END_FRONT_OFFSET, -0.25F - END_FRONT_OFFSET, 1.0F, 0.25F - END_FRONT_OFFSET, 0.5F - END_FRONT_OFFSET, 1.0F, -0.5F - END_FRONT_OFFSET, 0.0F, 0.0F, 1.0F, 0.9375F, facing.getOpposite(), -1, light);
                    IDrawing.drawTexture(graphicsHolder, 0.5F - BOTTOM_END_DIAGONAL_OFFSET, BOTTOM_DIAGONAL_OFFSET, -0.5F - BOTTOM_END_DIAGONAL_OFFSET, -0.25F - BOTTOM_END_DIAGONAL_OFFSET, BOTTOM_DIAGONAL_OFFSET, 0.25F - BOTTOM_END_DIAGONAL_OFFSET, -0.25F - END_FRONT_OFFSET, 0.0625F, 0.25F - END_FRONT_OFFSET, 0.5F - END_FRONT_OFFSET, 0.0625F, -0.5F - END_FRONT_OFFSET, 0.0F, 0.9375F, 1.0F, 0.96875F, facing.getOpposite(), -1, light);
                    IDrawing.drawTexture(graphicsHolder, 0.5F, 0.0F, -0.5F, -0.25F, 0.0F, 0.25F, -0.25F - BOTTOM_END_DIAGONAL_OFFSET, BOTTOM_DIAGONAL_OFFSET, 0.25F - BOTTOM_END_DIAGONAL_OFFSET, 0.5F - BOTTOM_END_DIAGONAL_OFFSET, BOTTOM_DIAGONAL_OFFSET, -0.5F - BOTTOM_END_DIAGONAL_OFFSET, 0.0F, 0.96875F, 1.0F, 1.0F, facing.getOpposite(), -1, light);
                    IDrawing.drawTexture(graphicsHolder, 0.5F, 0.003125F, -0.125F, -0.125F, 0.003125F, 0.5F, -0.125F, 0.003125F, 0.125F, 0.5F, 0.003125F, -0.5F, 0.125F, 0.125F, 0.1875F, 0.1875F, facing, -1, light);
                    IDrawing.drawTexture(graphicsHolder, 0.5F, 0.996875F, -0.5F, -0.125F, 0.996875F, 0.125F, -0.125F, 0.996875F, 0.5F, 0.5F, 0.996875F, -0.125F, 0.125F, 0.125F, 0.1875F, 0.1875F, Direction.UP, -1, light);
                    IDrawing.drawTexture(graphicsHolder, 0.5F - END_FRONT_OFFSET, 0.996875F, -0.5F - END_FRONT_OFFSET, -0.125F - ROOT_TWO_SCALED, 0.996875F, 0.125F, -0.125F, 0.996875F, 0.125F, 0.5F, 0.996875F, -0.5F, 0.125F, 0.125F, 0.1875F, 0.1875F, Direction.UP, -1, light);
                    IDrawing.drawTexture(graphicsHolder, 0.5F, 0.0625F, -0.5F, 0.5F - END_FRONT_OFFSET, 0.0625F, -0.5F - END_FRONT_OFFSET, 0.5F - END_FRONT_OFFSET, 1.0F, -0.5F - END_FRONT_OFFSET, 0.5F, 1.0F, -0.5F, 0.9375F, 0.0F, 1.0F, 0.9375F, facing, -1, light);
                    IDrawing.drawTexture(graphicsHolder, 0.5F, 0.0F, -0.5F, 0.5F - BOTTOM_END_DIAGONAL_OFFSET, BOTTOM_DIAGONAL_OFFSET, -0.5F - BOTTOM_END_DIAGONAL_OFFSET, 0.5F - END_FRONT_OFFSET, 0.0625F, -0.5F - END_FRONT_OFFSET, 0.5F, 0.0625F, -0.5F, 0.9375F, 0.9375F, 1.0F, 1.0F, facing, -1, light);
                }

                if (airRight) {
                    IDrawing.drawTexture(graphicsHolder, -0.5F, 0.0F, -0.125F, 0.125F, 0.0F, 0.5F, 0.125F, 1.0F, 0.5F, -0.5F, 1.0F, -0.125F, 0.0F, 0.0F, 1.0F, 1.0F, facing, -1, light);
                    IDrawing.drawTexture(graphicsHolder, 0.25F + END_FRONT_OFFSET, 0.0625F, 0.25F - END_FRONT_OFFSET, -0.5F + END_FRONT_OFFSET, 0.0625F, -0.5F - END_FRONT_OFFSET, -0.5F + END_FRONT_OFFSET, 1.0F, -0.5F - END_FRONT_OFFSET, 0.25F + END_FRONT_OFFSET, 1.0F, 0.25F - END_FRONT_OFFSET, 0.0F, 0.0F, 1.0F, 0.9375F, facing.getOpposite(), -1, light);
                    IDrawing.drawTexture(graphicsHolder, 0.25F + BOTTOM_END_DIAGONAL_OFFSET, BOTTOM_DIAGONAL_OFFSET, 0.25F - BOTTOM_END_DIAGONAL_OFFSET, -0.5F + BOTTOM_END_DIAGONAL_OFFSET, BOTTOM_DIAGONAL_OFFSET, -0.5F - BOTTOM_END_DIAGONAL_OFFSET, -0.5F + END_FRONT_OFFSET, 0.0625F, -0.5F - END_FRONT_OFFSET, 0.25F + END_FRONT_OFFSET, 0.0625F, 0.25F - END_FRONT_OFFSET, 0.0F, 0.9375F, 1.0F, 0.96875F, facing.getOpposite(), -1, light);
                    IDrawing.drawTexture(graphicsHolder, 0.25F, 0.0F, 0.25F, -0.5F, 0.0F, -0.5F, -0.5F + BOTTOM_END_DIAGONAL_OFFSET, BOTTOM_DIAGONAL_OFFSET, -0.5F - BOTTOM_END_DIAGONAL_OFFSET, 0.25F + BOTTOM_END_DIAGONAL_OFFSET, BOTTOM_DIAGONAL_OFFSET, 0.25F - BOTTOM_END_DIAGONAL_OFFSET, 0.0F, 0.96875F, 1.0F, 1.0F, facing.getOpposite(), -1, light);
                    IDrawing.drawTexture(graphicsHolder, 0.125F, 0.003125F, 0.5F, -0.5F, 0.003125F, -0.125F, -0.5F, 0.003125F, -0.5F, 0.125F, 0.003125F, 0.125F, 0.125F, 0.125F, 0.1875F, 0.1875F, facing, -1, light);
                    IDrawing.drawTexture(graphicsHolder, 0.125F, 0.996875F, 0.125F, -0.5F, 0.996875F, -0.5F, -0.5F, 0.996875F, -0.125F, 0.125F, 0.996875F, 0.5F, 0.125F, 0.125F, 0.1875F, 0.1875F, Direction.UP, -1, light);
                    IDrawing.drawTexture(graphicsHolder, 0.125F + ROOT_TWO_SCALED, 0.996875F, 0.125F, -0.5F + END_FRONT_OFFSET, 0.996875F, -0.5F - END_FRONT_OFFSET, -0.5F, 0.996875F, -0.5F, 0.125F, 0.996875F, 0.125F, 0.125F, 0.125F, 0.1875F, 0.1875F, Direction.UP, -1, light);
                    IDrawing.drawTexture(graphicsHolder, -0.5F + END_FRONT_OFFSET, 0.0625F, -0.5F - END_FRONT_OFFSET, -0.5F, 0.0625F, -0.5F, -0.5F, 1.0F, -0.5F, -0.5F + END_FRONT_OFFSET, 1.0F, -0.5F - END_FRONT_OFFSET, 0.0F, 0.0F, 0.0625F, 0.9375F, facing, -1, light);
                    IDrawing.drawTexture(graphicsHolder, -0.5F + BOTTOM_END_DIAGONAL_OFFSET, BOTTOM_DIAGONAL_OFFSET, -0.5F - BOTTOM_END_DIAGONAL_OFFSET, -0.5F, 0.0F, -0.5F, -0.5F, 0.0625F, -0.5F, -0.5F + END_FRONT_OFFSET, 0.0625F, -0.5F - END_FRONT_OFFSET, 0.0F, 0.9375F, 0.0625F, 1.0F, facing, -1, light);
                }

                graphicsHolder.pop();
            });
        }
    }

    protected void renderAdditional(StoredMatrixTransformations storedMatrixTransformations, long platformId, BlockState state, int leftBlocks, int rightBlocks, Direction facing, int color, int light) {
        boolean isNotPersistent = IBlock.getStatePropertySafe(state, BlockPSDTop.PERSISTENT) == EnumPersistent.NONE;
        boolean airLeft = isNotPersistent && IBlock.getStatePropertySafe(state, BlockPSDTop.AIR_LEFT);
        boolean airRight = isNotPersistent && IBlock.getStatePropertySafe(state, BlockPSDTop.AIR_RIGHT);
        MainRenderer.scheduleRender(DynamicTextureCache.instance.getColorStrip(platformId).identifier, false, QueuedRenderLayer.EXTERIOR, (graphicsHolder, offset) -> {
            storedMatrixTransformations.transform(graphicsHolder, offset);
            IDrawing.drawTexture(graphicsHolder, airLeft ? 0.625F : 0.0F, 0.90625F, 0.0F, airRight ? 0.375F : 1.0F, 0.9375F, 0.0F, facing, color, light);
            if (airLeft) {
                IDrawing.drawTexture(graphicsHolder, END_FRONT_OFFSET, 0.90625F, -0.625F - END_FRONT_OFFSET, 0.75F + END_FRONT_OFFSET, 0.9375F, 0.125F - END_FRONT_OFFSET, facing, -1, light);
            }

            if (airRight) {
                IDrawing.drawTexture(graphicsHolder, 0.25F - END_FRONT_OFFSET, 0.90625F, 0.125F - END_FRONT_OFFSET, 1.0F - END_FRONT_OFFSET, 0.9375F, -0.625F - END_FRONT_OFFSET, facing, -1, light);
            }

            graphicsHolder.pop();
        });
    }

    protected float getAdditionalOffset(BlockState state) {
        return IBlock.getStatePropertySafe(state, BlockPSDTop.PERSISTENT) == EnumPersistent.NONE ? 0.0F : 0.46875F;
    }

    static {
        BOTTOM_END_DIAGONAL_OFFSET = END_FRONT_OFFSET - BOTTOM_DIAGONAL_OFFSET / MathHelper.getSquareRootOfTwoMapped();
    }
}
