package com.Nanbin.Registry.RegBlock;

import org.mtr.mapping.holder.*;
import org.mtr.mapping.tool.HolderBase;
import org.mtr.mod.Blocks;
import org.mtr.mod.block.BlockDirectionalDoubleBlockBase;
import org.mtr.mod.block.IBlock;

import javax.annotation.Nonnull;
import java.util.List;

public class BlockGlassFence extends BlockDirectionalDoubleBlockBase {

    public BlockGlassFence() {
        super(Blocks.createDefaultBlockSettings(true).nonOpaque());
    }

    @Nonnull
    public VoxelShape getOutlineShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        Direction facing = IBlock.getStatePropertySafe(state, FACING);
        return IBlock.getStatePropertySafe(state, HALF) == DoubleBlockHalf.UPPER ? IBlock.getVoxelShapeByDirection((double)0.0F, (double)0.0F, (double)0.0F, (double)16.0F, (double)3.0F, (double)3.0F, facing) : IBlock.getVoxelShapeByDirection((double)0.0F, (double)0.0F, (double)0.0F, (double)16.0F, (double)16.0F, (double)3.0F, facing);
    }

    @Nonnull
    public VoxelShape getCollisionShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        Direction facing = IBlock.getStatePropertySafe(state, FACING);
        return VoxelShapes.union(this.getOutlineShape2(state, world, pos, context), IBlock.getVoxelShapeByDirection((double)0.0F, (double)0.0F, (double)0.0F, (double)16.0F, (double)8.0F, (double)3.0F, facing));
    }

    @Nonnull
    public VoxelShape getCameraCollisionShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.empty();
    }

    public void addBlockProperties(List<HolderBase<?>> properties) {
        properties.add(FACING);
        properties.add(HALF);
    }
}
