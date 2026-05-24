package com.Nanbin.Registry.RegBlock;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

public class BlockCRTFenceVertical extends HorizontalFacingBlock {
    private static final VoxelShape FENCE_SHAPE_NORTH =
            Block.createCuboidShape(0, 0, 0, 16, 18, 3);

    private static final VoxelShape FENCE_SHAPE_EAST =
            Block.createCuboidShape(13, 0, 16, 29, 18, 19);

    private static final VoxelShape FENCE_SHAPE_SOUTH =
            Block.createCuboidShape(0, 0, 13, 16, 18, 16);

    private static final VoxelShape FENCE_SHAPE_WEST =
            Block.createCuboidShape(0, 0, 16, 16, 18, 19);

    public BlockCRTFenceVertical(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.NORTH));
    }

    // 注册
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(Properties.HORIZONTAL_FACING);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        Direction playerFacing = ctx.getPlayerFacing();
        // 过滤垂直方向，只保留水平朝向
        Direction horizontalFacing = playerFacing.getAxis().isHorizontal() ? playerFacing : Direction.NORTH;
        // 朝向玩家的反方向
        return this.getDefaultState().with(Properties.HORIZONTAL_FACING, horizontalFacing.getOpposite());
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
        Direction dir = state.get(Properties.HORIZONTAL_FACING);
        return switch (dir) {
            case NORTH -> FENCE_SHAPE_NORTH;
            case SOUTH -> FENCE_SHAPE_SOUTH;
            case EAST -> FENCE_SHAPE_EAST;
            case WEST -> FENCE_SHAPE_WEST;
            default -> FENCE_SHAPE_NORTH;
        };
    }

    // 碰撞形状
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
        return this.getOutlineShape(state, world, pos, ctx);
    }
}