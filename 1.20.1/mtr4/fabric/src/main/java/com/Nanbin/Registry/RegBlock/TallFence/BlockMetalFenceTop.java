package com.Nanbin.Registry.RegBlock.TallFence;

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

    // 新增：联动破坏逻辑
    @Override
    public void onStateReplaced2(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        // 只有当方块不是被同类型方块替换时，才触发破坏
        if (!state.isOf(newState.getBlock())) {
            BlockPos downPos = pos.down(); // 获取下方一格位置
            BlockState downState = world.getBlockState(downPos);

            // 如果下方是 MetalFENCE 方块，则破坏它
            if (downState.getBlock().data instanceof BlockMetalFence) {
                world.removeBlock(downPos, false); // false 表示不播放破坏音效和粒子（可选，改为 true 则播放）
            }
        }

        super.onStateReplaced2(state, world, pos, newState, moved);
    }
}
