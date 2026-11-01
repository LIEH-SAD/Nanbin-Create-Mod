package com.Nanbin.Registry.RegBlock;

import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockExtension;
import org.mtr.mapping.mapper.DirectionHelper;
import org.mtr.mapping.tool.HolderBase;
import org.mtr.mod.block.IBlock;

import java.util.List;

public class BlockGreenFenceTop extends BlockExtension {
    public static final DirectionProperty FACING = DirectionHelper.FACING;

    private static final VoxelShape SHAPE_NORTH = Block.createCuboidShape(0, 0, 10, 16, 16, 16);
    private static final VoxelShape SHAPE_SOUTH = Block.createCuboidShape(0, 0, 0, 16, 16, 6);
    private static final VoxelShape SHAPE_EAST = Block.createCuboidShape(0, 0, 0, 6, 16, 16);
    private static final VoxelShape SHAPE_WEST = Block.createCuboidShape(10, 0, 0, 16, 16, 16);

    public BlockGreenFenceTop(BlockSettings settings) {
        super(settings);
    }

    @Override
    public void addBlockProperties(List<HolderBase<?>> properties) {
        properties.add(DirectionHelper.FACING);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
        Direction direction = IBlock.getStatePropertySafe(state, DirectionHelper.FACING);
        switch (direction) {
            case NORTH:
                return SHAPE_NORTH;
            case SOUTH:
                return SHAPE_SOUTH;
            case EAST:
                return SHAPE_EAST;
            case WEST:
                return SHAPE_WEST;
            default:
                return SHAPE_NORTH;
        }
    }

    @Override
    public void onStateReplaced2(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            BlockPos downPos = pos.down();
            BlockState downState = world.getBlockState(downPos);

            if (downState.getBlock().data instanceof BlockGreenFence) {
                world.removeBlock(downPos, false);
            }
        }

        super.onStateReplaced2(state, world, pos, newState, moved);
    }
}
