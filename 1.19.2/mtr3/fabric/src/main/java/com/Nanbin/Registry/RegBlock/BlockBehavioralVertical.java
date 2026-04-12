package com.Nanbin.Registry.RegBlock;

import net.minecraft.block.*;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

import java.util.stream.Stream;

public class BlockBehavioralVertical extends HorizontalFacingBlock {
    private static final VoxelShape BEHAVIORALBLOCK__SHAPE_SOUTH =
            Stream.of(
                    Block.createCuboidShape(0, 12, 0, 16, 16, 7),
                    Block.createCuboidShape(0, 0, 4, 4, 12, 16),
                    Block.createCuboidShape(0, 12, 7, 7, 16, 16),
                    Block.createCuboidShape(0, 0, 0, 16, 12, 4)
            ).reduce((v1, v2) -> VoxelShapes.combineAndSimplify(v1, v2, BooleanBiFunction.OR)).get();

    private static final VoxelShape BEHAVIORALBLOCK__SHAPE_EAST =
            Stream.of(
                    Block.createCuboidShape(0, 12, 9, 16, 16, 16),
                    Block.createCuboidShape(0, 0, 0, 4, 12, 12),
                    Block.createCuboidShape(0, 12, 0, 7, 16, 9),
                    Block.createCuboidShape(0, 0, 12, 16, 12, 16)
            ).reduce((v1, v2) -> VoxelShapes.combineAndSimplify(v1, v2, BooleanBiFunction.OR)).get();

    private static final VoxelShape BEHAVIORALBLOCK__SHAPE_NORTH =
            Stream.of(
                    Block.createCuboidShape(0, 12, 9, 16, 16, 16),
                    Block.createCuboidShape(12, 0, 0, 16, 12, 12),
                    Block.createCuboidShape(9, 12, 0, 16, 16, 9),
                    Block.createCuboidShape(0, 0, 12, 16, 12, 16)
            ).reduce((v1, v2) -> VoxelShapes.combineAndSimplify(v1, v2, BooleanBiFunction.OR)).get();

    private static final VoxelShape BEHAVIORALBLOCK__SHAPE_WEST =
            Stream.of(
                    Block.createCuboidShape(0, 12, 0, 16, 16, 7),
                    Block.createCuboidShape(12, 0, 4, 16, 12, 16),
                    Block.createCuboidShape(9, 12, 7, 16, 16, 16),
                    Block.createCuboidShape(0, 0, 0, 16, 12, 4)
            ).reduce((v1, v2) -> VoxelShapes.combineAndSimplify(v1, v2, BooleanBiFunction.OR)).get();

    public BlockBehavioralVertical(AbstractBlock.Settings settings) {
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
        Direction horizontalFacing = playerFacing.getAxis().isHorizontal() ? playerFacing : Direction.NORTH;
        return this.getDefaultState().with(Properties.HORIZONTAL_FACING, horizontalFacing.getOpposite());
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
        Direction dir = state.get(Properties.HORIZONTAL_FACING);
        return switch (dir) {
            case NORTH -> BEHAVIORALBLOCK__SHAPE_NORTH;
            case SOUTH -> BEHAVIORALBLOCK__SHAPE_SOUTH;
            case EAST -> BEHAVIORALBLOCK__SHAPE_EAST;
            case WEST -> BEHAVIORALBLOCK__SHAPE_WEST;
            default -> BEHAVIORALBLOCK__SHAPE_NORTH;
        };
    }

    // 碰撞形状
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
        return this.getOutlineShape(state, world, pos, ctx);
    }

}
