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

public class BlockCRTLogo extends HorizontalFacingBlock {
    private static final VoxelShape CRT_SHAPE_NORTH = Block.createCuboidShape(-7, 0, 5, 23, 28, 11);

    private static final VoxelShape CRT_SHAPE_EAST = Block.createCuboidShape(5, 0, 16 - 23, 11, 28, 16 - (-7));

    private static final VoxelShape CRT_SHAPE_SOUTH = Block.createCuboidShape(16 - 23, 0, 16 - 11, 16 - (-7), 28, 16 - 5);

    private static final VoxelShape CRT_SHAPE_WEST = Block.createCuboidShape(16 - 11, 0, -7, 16 - 5, 28, 23);

    public BlockCRTLogo(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.NORTH));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(Properties.HORIZONTAL_FACING);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        Direction playerFacing = ctx.getPlayerFacing();
        Direction horizontalFacing = playerFacing.getAxis().isHorizontal() ? playerFacing : Direction.NORTH;
        return this.getDefaultState().with(Properties.HORIZONTAL_FACING, horizontalFacing.getOpposite());
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
        Direction dir = state.get(Properties.HORIZONTAL_FACING);
        return switch (dir) {
            case NORTH -> CRT_SHAPE_NORTH;
            case SOUTH -> CRT_SHAPE_SOUTH;
            case EAST -> CRT_SHAPE_EAST;
            case WEST -> CRT_SHAPE_WEST;
            default -> CRT_SHAPE_NORTH;
        };
    }

    // 碰撞形状
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
        return this.getOutlineShape(state, world, pos, ctx);
    }
}