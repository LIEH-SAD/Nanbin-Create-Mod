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

public class BlockMTRLogo extends HorizontalFacingBlock {
    private static final VoxelShape LOGO_NORTH = Block.createCuboidShape(0, 0, 15.68, 16, 16, 15.98);

    private static final VoxelShape LOGO_EAST = Block.createCuboidShape(0.02, 0, 0, 0.32, 16, 16);

    private static final VoxelShape LOGO_SOUTH = Block.createCuboidShape(0, 0, 0.02, 16, 16, 0.32);

    private static final VoxelShape LOGO_WEST = Block.createCuboidShape(15.68, 0, 0, 15.98, 16, 16);

    public BlockMTRLogo(Settings settings) {
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
            case NORTH -> LOGO_NORTH;
            case SOUTH -> LOGO_SOUTH;
            case EAST -> LOGO_EAST;
            case WEST -> LOGO_WEST;
            default -> LOGO_NORTH;
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
        return this.getOutlineShape(state, world, pos, ctx);
    }
}