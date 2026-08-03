package com.Nanbin.Registry.RegBlock.TallFence;

import com.Nanbin.Blocks.Blocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Stream;

public class BlockMetalFence extends Block {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

    private static final VoxelShape METALFENCE_SOUTH = Stream.of(
            Block.createCuboidShape(15, 0, 0, 16, 16, 2),
            Block.createCuboidShape(0, 0, 0, 1, 16, 2),
            Block.createCuboidShape(1, 0, 0, 15, 16, 1)
    ).reduce((v1, v2) -> VoxelShapes.combineAndSimplify(v1, v2, BooleanBiFunction.OR)).get();
    private static final VoxelShape METALFENCE_WEST = Block.createCuboidShape(14, 0, 0, 16, 16, 16);
    private static final VoxelShape METALFENCE_NORTH =Stream.of(
            Block.createCuboidShape(15, 0, 14, 16, 16, 16),
            Block.createCuboidShape(0, 0, 14, 1, 16, 16),
            Block.createCuboidShape(1, 0, 15, 15, 16, 16)
    ).reduce((v1, v2) -> VoxelShapes.combineAndSimplify(v1, v2, BooleanBiFunction.OR)).get();
    private static final VoxelShape METALFENCE_EAST = Block.createCuboidShape(0, 0, 0, 2, 16, 16);

    public BlockMetalFence(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.NORTH));
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        World world = context.getWorld();
        if (world.isClient()) return null;

        BlockPos clickPos = context.getBlockPos();
        Direction facing = context.getPlayerFacing().getOpposite();
        if (facing.getAxis().isVertical()) facing = Direction.NORTH;

        boolean canPlace = true;
        for (int i = 0; i < 4; i++) {
            if (!world.getBlockState(clickPos.up(i)).canReplace(context)) {
                canPlace = false;
                break;
            }
        }

        if (canPlace) {
            for (int i = 0; i < 3; i++) {
                world.setBlockState(clickPos.up(i), this.getDefaultState().with(FACING, facing), 3);
            }
            world.setBlockState(clickPos.up(3), Blocks.METALFENCE_TOP.get().getDefaultState().with(FACING, facing), 3);
            return this.getDefaultState().with(FACING, facing);
        }
        return null;
    }

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (world.isClient()) return;

        BlockPos bottomPos = pos;
        while (true) {
            BlockPos check = bottomPos.down();
            if (world.getBlockState(check).getBlock() instanceof BlockMetalFence || world.getBlockState(check).getBlock() instanceof BlockMetalFenceTop) {
                bottomPos = check;
            } else break;
        }

        BlockPos topPos = pos;
        while (true) {
            BlockPos check = topPos.up();
            if (world.getBlockState(check).getBlock() instanceof BlockMetalFence || world.getBlockState(check).getBlock() instanceof BlockMetalFenceTop) {
                topPos = check;
            } else break;
        }

        BlockPos current = bottomPos;
        while (true) {
            if (world.getBlockState(current).getBlock() instanceof BlockMetalFence || world.getBlockState(current).getBlock() instanceof BlockMetalFenceTop) {
                world.setBlockState(current, net.minecraft.block.Blocks.AIR.getDefaultState(), 3);
                if (!player.isCreative()) {
                    dropStack(world, current, new net.minecraft.item.ItemStack(Blocks.METALFENCE.get()));
                }
            }
            if (current.equals(topPos)) break;
            current = current.up();
        }
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
        Direction dir = state.get(Properties.HORIZONTAL_FACING);
        return switch (dir) {
            case NORTH -> METALFENCE_NORTH;
            case SOUTH -> METALFENCE_SOUTH;
            case EAST -> METALFENCE_EAST;
            case WEST -> METALFENCE_WEST;
            default -> METALFENCE_NORTH;
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
        return this.getOutlineShape(state, world, pos, ctx);
    }

    public void appendTooltip(ItemStack itemStack, BlockView blockGetter, List<Text> tooltip, TooltipContext tooltipFlag) {
        tooltip.add(mtr.mappings.Text.translatable("tooltip.nanbin.block.tall_fence", new Object[0]).setStyle(Style.EMPTY.withColor(Formatting.GRAY)));
    }
}