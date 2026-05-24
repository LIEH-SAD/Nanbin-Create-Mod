package com.Nanbin.Registry.RegBlock;

import net.minecraft.block.*;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

public class BlockCeiling extends HorizontalFacingBlock {
    private static final VoxelShape CELING_NORTH = Block.createCuboidShape(0, 0, 0, 16, 5, 16);

    private static final VoxelShape CELING_EAST = Block.createCuboidShape(0, 0, 0, 16, 5, 16);

    private static final VoxelShape CELING_SOUTH =Block.createCuboidShape(0, 0, 0, 16, 5, 16);

    private static final VoxelShape CELING_WEST = Block.createCuboidShape(0, 0, 0, 16, 5, 16);

    public BlockCeiling(AbstractBlock.Settings settings) {
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
            case NORTH -> CELING_NORTH;
            case SOUTH -> CELING_SOUTH;
            case EAST -> CELING_EAST;
            case WEST -> CELING_WEST;
            default -> CELING_NORTH;
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
        return this.getOutlineShape(state, world, pos, ctx);
    }
}
