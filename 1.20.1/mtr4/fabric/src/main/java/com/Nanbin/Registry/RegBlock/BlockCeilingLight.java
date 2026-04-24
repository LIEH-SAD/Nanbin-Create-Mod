package com.Nanbin.Registry.RegBlock;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

public class BlockCeilingLight extends HorizontalFacingBlock {
    public static final BooleanProperty LIT = Properties.LIT;

    private static final VoxelShape CEILING_LIGHT_SHAPE = Block.createCuboidShape(0, 14, 0, 16, 16, 16);

    public BlockCeilingLight(Settings settings) {
        super(settings.luminance(state -> 15));
        setDefaultState(getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.NORTH));
        this.setDefaultState(this.getStateManager().getDefaultState().with(LIT, true));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(Properties.HORIZONTAL_FACING);
        builder.add(LIT);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        Direction playerFacing = ctx.getHorizontalPlayerFacing();
        return this.getDefaultState().with(Properties.HORIZONTAL_FACING, playerFacing.getOpposite());
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
        Direction dir = state.get(Properties.HORIZONTAL_FACING);
        return switch (dir) {
            case NORTH -> CEILING_LIGHT_SHAPE;
            case SOUTH -> CEILING_LIGHT_SHAPE;
            case EAST -> CEILING_LIGHT_SHAPE;
            case WEST -> CEILING_LIGHT_SHAPE;
            default -> CEILING_LIGHT_SHAPE;
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
        return this.getOutlineShape(state, world, pos, ctx);
    }
}