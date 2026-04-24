package com.Nanbin.Registry.RegBlock.TallFence;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;


import java.util.stream.Stream;

import static com.Nanbin.Blocks.Blocks.GREENFENCE_TOP;



public class BlockGreenFence extends Block {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

    private static final VoxelShape GREENFENCE_SOUTH = Stream.of(
            Block.createCuboidShape(15, 0, 0, 16, 16, 2),
            Block.createCuboidShape(0, 0, 0, 1, 16, 2),
            Block.createCuboidShape(1, 0, 0, 15, 16, 1)
    ).reduce((v1, v2) -> VoxelShapes.combineAndSimplify(v1, v2, BooleanBiFunction.OR)).get();

    private static final VoxelShape GREENFENCE_WEST = Block.createCuboidShape(14, 0, 0, 16, 16, 16);
    private static final VoxelShape GREENFENCE_NORTH = Stream.of(
            Block.createCuboidShape(15, 0, 14, 16, 16, 16),
            Block.createCuboidShape(0, 0, 14, 1, 16, 16),
            Block.createCuboidShape(1, 0, 15, 15, 16, 16)
    ).reduce((v1, v2) -> VoxelShapes.combineAndSimplify(v1, v2, BooleanBiFunction.OR)).get();
    private static final VoxelShape GREENFENCE_EAST = Block.createCuboidShape(0, 0, 0, 2, 16, 16);

    public BlockGreenFence(Settings settings) {
        super(settings);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        return this.getDefaultState()
                .with(FACING, context.getHorizontalPlayerFacing().getOpposite());
    }

    // ========== 修正后的放置联动逻辑 ==========
    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.onPlaced(world, pos, state, placer, stack);

        if (world.isClient) return;

        BlockPos bottom = pos;
        while (world.getBlockState(bottom.down()).getBlock() instanceof BlockGreenFence ||
                world.getBlockState(bottom.down()).getBlock() instanceof BlockGreenFenceTop) {
            bottom = bottom.down();
        }

        Direction facing = state.get(FACING);
        for (int i = 0; i <= 3; i++) {
            BlockPos targetPos = bottom.up(i);
            BlockState targetState;

            if (i < 3) {
                targetState = this.getDefaultState().with(FACING, facing);
            } else {
                targetState = GREENFENCE_TOP.get().data.getDefaultState().with(BlockGreenFenceTop.FACING, facing);
            }

            BlockState existingState = world.getBlockState(targetPos);
            if (existingState.isAir() ||
                    existingState.getBlock() instanceof BlockGreenFence ||
                    existingState.getBlock() instanceof BlockGreenFenceTop) {
                world.setBlockState(targetPos, targetState, Block.NOTIFY_ALL);
            }
        }
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        super.onStateReplaced(state, world, pos, newState, moved);

        if (world.isClient) return;
        if (state.isOf(newState.getBlock())) return;

        destroyWholeFence(world, pos);
    }

    private void destroyWholeFence(World world, BlockPos pos) {
        BlockPos bottom = pos;
        while (world.getBlockState(bottom.down()).getBlock() instanceof BlockGreenFence ||
                world.getBlockState(bottom.down()).getBlock() instanceof BlockGreenFenceTop) {
            bottom = bottom.down();
        }

        for (int i = 0; i < 4; i++) {
            BlockPos target = bottom.up(i);
            world.breakBlock(target, false);
        }
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
        return switch (state.get(FACING)) {
            case NORTH -> GREENFENCE_NORTH;
            case SOUTH -> GREENFENCE_SOUTH;
            case EAST -> GREENFENCE_EAST;
            case WEST -> GREENFENCE_WEST;
            default -> GREENFENCE_NORTH;
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
        return getOutlineShape(state, world, pos, ctx);
    }
}