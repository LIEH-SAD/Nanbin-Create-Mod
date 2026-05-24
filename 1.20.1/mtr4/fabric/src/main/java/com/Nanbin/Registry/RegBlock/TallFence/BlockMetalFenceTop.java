package com.Nanbin.Registry.RegBlock.TallFence;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class BlockMetalFenceTop extends Block {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

    private static final VoxelShape SHAPE_NORTH = Block.createCuboidShape(0, 0, 10, 16, 16, 16);
    private static final VoxelShape SHAPE_SOUTH = Block.createCuboidShape(0, 0, 0, 16, 16, 6);
    private static final VoxelShape SHAPE_EAST = Block.createCuboidShape(0, 0, 0, 6, 16, 16);
    private static final VoxelShape SHAPE_WEST = Block.createCuboidShape(10, 0, 0, 16, 16, 16);

    public BlockMetalFenceTop(Settings settings) {
        super(settings);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
        return switch (state.get(FACING)) {
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case EAST -> SHAPE_EAST;
            case WEST -> SHAPE_WEST;
            default -> SHAPE_NORTH;
        };
    }

    // 新增：联动破坏逻辑
    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        // 只有当方块不是被同类型方块替换时，才触发破坏
        if (!state.isOf(newState.getBlock())) {
            BlockPos downPos = pos.down(); // 获取下方一格位置
            BlockState downState = world.getBlockState(downPos);

            // 如果下方是 MetalFENCE 方块，则破坏它
            if (downState.getBlock() instanceof BlockMetalFence) {
                world.removeBlock(downPos, false); // false 表示不播放破坏音效和粒子（可选，改为 true 则播放）
            }
        }

        super.onStateReplaced(state, world, pos, newState, moved);
    }
}