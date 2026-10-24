package com.Nanbin.Registry.RegBlock;

import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockExtension;
import org.mtr.mapping.mapper.DirectionHelper;
import org.mtr.mapping.tool.HolderBase;
import org.mtr.mod.block.IBlock;

import java.util.List;

public class BlockMetalFenceTop extends BlockExtension {
    public static final DirectionProperty FACING = DirectionHelper.FACING;

    private static final VoxelShape SHAPE_NORTH = Block.createCuboidShape(0, 0, 10, 16, 16, 16);
    private static final VoxelShape SHAPE_SOUTH = Block.createCuboidShape(0, 0, 0, 16, 16, 6);
    private static final VoxelShape SHAPE_EAST = Block.createCuboidShape(0, 0, 0, 6, 16, 16);
    private static final VoxelShape SHAPE_WEST = Block.createCuboidShape(10, 0, 0, 16, 16, 16);

    public BlockMetalFenceTop(BlockSettings settings) {
        super(settings);
    }

    @Override
    public void addBlockProperties(List<HolderBase<?>> properties) {
        properties.add(DirectionHelper.FACING);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
        Direction direction = IBlock.getStatePropertySafe(state, DirectionHelper.FACING);
        return switch (direction) {
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case EAST -> SHAPE_EAST;
            case WEST -> SHAPE_WEST;
            default -> SHAPE_NORTH;
        };
    }

    // 联动破坏
    @Override
    public void onStateReplaced2(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            BlockPos downPos = pos.down();
            BlockState downState = world.getBlockState(downPos);

            if (downState.getBlock().data instanceof BlockMetalFence) {
                world.removeBlock(downPos, false); // false = 不播放破坏音效与粒子
            }
        }

        super.onStateReplaced2(state, world, pos, newState, moved);
    }
}
