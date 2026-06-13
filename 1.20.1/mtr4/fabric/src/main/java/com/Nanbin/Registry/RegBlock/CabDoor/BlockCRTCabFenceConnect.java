package com.Nanbin.Registry.RegBlock.CabDoor;

import com.Nanbin.Registry.RegMean.VoxelShapes;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.tool.HolderBase;
import org.mtr.mod.Blocks;
import org.mtr.mod.block.BlockDirectionalDoubleBlockBase;
import org.mtr.mod.block.IBlock;

import javax.annotation.Nonnull;
import java.util.List;

public class BlockCRTCabFenceConnect extends BlockDirectionalDoubleBlockBase {
    public BlockCRTCabFenceConnect() {
        super(Blocks.createDefaultBlockSettings(true).nonOpaque());
    }

    @Nonnull
    public VoxelShape getOutlineShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        VoxelShape shape1 = IBlock.getVoxelShapeByDirection(0, 0, 0, 16, 16, 3,IBlock.getStatePropertySafe(state, FACING));
        VoxelShape shape2 = IBlock.getVoxelShapeByDirection(0, 0, 2, 3, 16, 16,IBlock.getStatePropertySafe(state, FACING));
        return VoxelShapes.union(shape1, shape2);
    }

    @Nonnull
    public VoxelShape getCollisionShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        VoxelShape shape1 = IBlock.getVoxelShapeByDirection(0, 0, 0, 16, 16, 3,IBlock.getStatePropertySafe(state, FACING));
        VoxelShape shape2 = IBlock.getVoxelShapeByDirection(0, 0, 2, 3, 16, 16,IBlock.getStatePropertySafe(state, FACING));
        return VoxelShapes.union(shape1, shape2);
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
