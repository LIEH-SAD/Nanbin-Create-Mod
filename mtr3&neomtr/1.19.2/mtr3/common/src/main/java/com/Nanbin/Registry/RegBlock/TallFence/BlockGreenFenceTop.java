package com.Nanbin.Registry.RegBlock.TallFence;

import com.Nanbin.Blocks.Blocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class BlockGreenFenceTop extends Block {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

    private static final VoxelShape GREENFENCE_TOP_SOUTH = Block.createCuboidShape(0, 0, 0, 16, 16, 6);
    private static final VoxelShape GREENFENCE_TOP_WEST = Block.createCuboidShape(10, 0, 0, 16, 16, 16);
    private static final VoxelShape GREENFENCE_TOP_NORTH =Block.createCuboidShape(0, 0, 10, 16, 16, 16);
    private static final VoxelShape GREENFENCE_TOP_EAST = Block.createCuboidShape(0, 0, 0, 6, 16, 16);

    public BlockGreenFenceTop(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.NORTH));
    }

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        Blocks.GREENFENCE.get().onBreak(world, pos, state, player);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
        Direction dir = state.get(Properties.HORIZONTAL_FACING);
        return switch (dir) {
            case NORTH -> GREENFENCE_TOP_NORTH;
            case SOUTH -> GREENFENCE_TOP_SOUTH;
            case EAST -> GREENFENCE_TOP_EAST;
            case WEST -> GREENFENCE_TOP_WEST;
            default -> GREENFENCE_TOP_NORTH;
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
        return this.getOutlineShape(state, world, pos, ctx);
    }
}