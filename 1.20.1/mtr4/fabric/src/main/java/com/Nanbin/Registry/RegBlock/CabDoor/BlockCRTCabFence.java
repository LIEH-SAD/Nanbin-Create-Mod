package com.Nanbin.Registry.RegBlock.CabDoor;

import org.mtr.mapping.holder.*;
import org.mtr.mapping.tool.HolderBase;
import org.mtr.mod.Blocks;
import org.mtr.mod.block.BlockDirectionalDoubleBlockBase;
import org.mtr.mod.block.IBlock;

import javax.annotation.Nonnull;
import java.util.List;

public class BlockCRTCabFence extends BlockDirectionalDoubleBlockBase {

    public BlockCRTCabFence() {
        super(Blocks.createDefaultBlockSettings(true).nonOpaque());
    }

    @Nonnull
    public VoxelShape getOutlineShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return IBlock.getVoxelShapeByDirection(0, 0, 0, 16, 16, 3, IBlock.getStatePropertySafe(state, FACING));
    }

    @Nonnull
    public VoxelShape getCollisionShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.union(this.getOutlineShape2(state, world, pos, context),
                IBlock.getVoxelShapeByDirection(0, 0, 0, 16, 16, 3, IBlock.getStatePropertySafe(state, FACING)));
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
