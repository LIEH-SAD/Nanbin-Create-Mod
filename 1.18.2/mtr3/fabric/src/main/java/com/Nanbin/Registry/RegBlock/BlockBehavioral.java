package com.Nanbin.Registry.RegBlock;

import net.minecraft.block.*;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

public class BlockBehavioral extends HorizontalFacingBlock {
    private static final VoxelShape BEHAVIORALBLOCK_SHAPE_SOUTH = Block.createCuboidShape(0, 0, 0, 16, 16, 5);

    private static final VoxelShape BEHAVIORALBLOCK_SHAPE_EAST = Block.createCuboidShape(0, 0, 0, 5, 16, 16);

    private static final VoxelShape BEHAVIORALBLOCK_SHAPE_NORTH = Block.createCuboidShape(0, 0, 11, 16, 16, 16);

    private static final VoxelShape BEHAVIORALBLOCK_SHAPE_WEST = Block.createCuboidShape(11, 0, 0, 16, 16, 16);

    public BlockBehavioral(AbstractBlock.Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.NORTH));
    }

    // 注册朝向
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

    // 外观形状（参考VerticalSlabBlock的switch写法）
    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
        Direction dir = state.get(Properties.HORIZONTAL_FACING);
        return switch (dir) {
            case NORTH -> BEHAVIORALBLOCK_SHAPE_NORTH;
            case SOUTH -> BEHAVIORALBLOCK_SHAPE_SOUTH;
            case EAST -> BEHAVIORALBLOCK_SHAPE_EAST;
            case WEST -> BEHAVIORALBLOCK_SHAPE_WEST;
            default -> BEHAVIORALBLOCK_SHAPE_NORTH;
        };
    }

    // 碰撞形状
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
        return this.getOutlineShape(state, world, pos, ctx);
    }
}
