package com.Nanbin.Registry.RegBlock;

import mtr.block.BlockDirectionalDoubleBlockBase;
import mtr.block.IBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlockCRTRigidCatenary1 extends BlockDirectionalDoubleBlockBase {

    public BlockCRTRigidCatenary1() {
        super(Properties.of().noOcclusion());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = IBlock.getStatePropertySafe(state, FACING);
        VoxelShape shape1 = IBlock.getVoxelShapeByDirection(6, 0, 0, 10, 11, 16, facing);
        VoxelShape shape2 = IBlock.getVoxelShapeByDirection(7.5F, 11, 12.5F, 8.5F, 16, 13.5F, facing);
        return Shapes.or(shape1, shape2);
    }

}