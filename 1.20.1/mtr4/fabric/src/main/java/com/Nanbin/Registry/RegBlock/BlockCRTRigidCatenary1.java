package com.Nanbin.Registry.RegBlock;

import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockExtension;

import javax.annotation.Nonnull;

public class BlockCRTRigidCatenary1 extends BlockExtension {

    public BlockCRTRigidCatenary1(BlockSettings blockSettings) {
        super(blockSettings);
    }

    @Nonnull
    public VoxelShape getOutlineShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return getStationPoleShape();
    }

    public static VoxelShape getStationPoleShape() {
        return Block.createCuboidShape((double)6.0F, (double)0.0F, (double)6.0F, (double)10.0F, (double)16.0F, (double)10.0F);
    }
}
