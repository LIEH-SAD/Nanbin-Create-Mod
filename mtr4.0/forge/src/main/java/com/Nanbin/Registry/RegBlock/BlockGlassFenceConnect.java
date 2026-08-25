package com.Nanbin.Registry.RegBlock;

import org.mtr.mapping.holder.*;
import org.mtr.mapping.tool.HolderBase;
import org.mtr.mod.Blocks;
import org.mtr.mod.block.BlockDirectionalDoubleBlockBase;
import org.mtr.mod.block.IBlock;

import javax.annotation.Nonnull;
import java.util.List;

import static com.Nanbin.mapping.VoxelShapes.union;

public class BlockGlassFenceConnect extends BlockDirectionalDoubleBlockBase {

    public BlockGlassFenceConnect() {
        super(Blocks.createDefaultBlockSettings(true).nonOpaque());
    }

    @Nonnull
    public VoxelShape getOutlineShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        Direction facing = IBlock.getStatePropertySafe(state, FACING);
        return IBlock.getStatePropertySafe(state, HALF) != DoubleBlockHalf.UPPER ?
                union(IBlock.getVoxelShapeByDirection(0, 0, 0, 16, 16, 2, facing), IBlock.getVoxelShapeByDirection(14, 0, 2, 16, 16, 16, facing)) : union(IBlock.getVoxelShapeByDirection(0, 0, 0, 16, 3, 2, facing), IBlock.getVoxelShapeByDirection(14, 0, 2, 16, 3, 16, facing));
    }

    @Nonnull
    public VoxelShape getCollisionShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        Direction facing = IBlock.getStatePropertySafe(state, FACING);
        return VoxelShapes.union(this.getOutlineShape2(state, world, pos, context), union(IBlock.getVoxelShapeByDirection(0, 0, 0, 16, 24, 2, facing), IBlock.getVoxelShapeByDirection(14, 0, 2, 16, 24, 16, facing)));
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
