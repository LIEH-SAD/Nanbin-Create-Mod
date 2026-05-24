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
import net.minecraft.text.Text;
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

    private static final int MAX_FENCE_HEIGHT = 4;        // 总高度4格
    private static final int BOTTOM_SECTION_COUNT = 3;

    private static final VoxelShape BLUEFENCE_SOUTH = createFenceShape(0, 2, 0, 1);
    private static final VoxelShape BLUEFENCE_NORTH = createFenceShape(14, 16, 15, 16);
    private static final VoxelShape BLUEFENCE_WEST = Block.createCuboidShape(14, 0, 0, 16, 16, 16);
    private static final VoxelShape BLUEFENCE_EAST = Block.createCuboidShape(0, 0, 0, 2, 16, 16);

    public BlockGreenFence(Settings settings) {
        super(settings);
    }

    //判断上方4格
    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        World world = context.getWorld();
        BlockPos placementPos = context.getBlockPos();
        PlayerEntity player = context.getPlayer();

        for (int i = 1; i <= MAX_FENCE_HEIGHT; i++) {
            BlockPos checkPos = placementPos.up(i);
            if (!world.getBlockState(checkPos).isAir()) {
                sendPlayerMessage(world, player, "tips.fence_no_space");
                return null;
            }
        }

        // 世界顶部高度
        int maxRequiredY = placementPos.getY() + MAX_FENCE_HEIGHT - 1;
        if (maxRequiredY > world.getTopY()) {
            sendPlayerMessage(world, player, "tips.fence_too_high");
            return null;
        }

        return this.getDefaultState().with(FACING, context.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.onPlaced(world, pos, state, placer, stack);
        if (world.isClient) return;

        BlockPos bottom = findBottomFencePos(world, pos);
        Direction facing = state.get(FACING);

        for (int i = 0; i < MAX_FENCE_HEIGHT; i++) {
            BlockPos targetPos = bottom.up(i);
            BlockState targetState = createFenceState(facing, i);
            if (canReplace(world.getBlockState(targetPos))) {
                world.setBlockState(targetPos, targetState, Block.NOTIFY_ALL);
            }
        }
    }

    //联动破坏
    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        super.onStateReplaced(state, world, pos, newState, moved);
        if (world.isClient || isFenceBlock(newState.getBlock())) return;
        destroyWholeFence(world, pos);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
        return switch (state.get(FACING)) {
            case NORTH -> BLUEFENCE_NORTH;
            case SOUTH -> BLUEFENCE_SOUTH;
            case EAST -> BLUEFENCE_EAST;
            case WEST -> BLUEFENCE_WEST;
            default -> BLUEFENCE_NORTH;
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
        return getOutlineShape(state, world, pos, ctx);
    }

    private void sendPlayerMessage(World world, @Nullable PlayerEntity player, String key) {
        if (!world.isClient && player != null) {
            player.sendMessage(Text.translatable(key), true);
        }
    }

    private boolean isFenceBlock(Block block) {
        return block instanceof BlockGreenFence || block instanceof BlockBlueFenceTop;
    }

    private BlockPos findBottomFencePos(World world, BlockPos pos) {
        BlockPos bottom = pos;
        while (bottom.getY() > world.getBottomY()) {
            BlockPos down = bottom.down();
            if (isFenceBlock(world.getBlockState(down).getBlock())) {
                bottom = down;
            } else {
                break;
            }
        }
        return bottom;
    }

    private BlockState createFenceState(Direction facing, int heightIndex) {
        if (heightIndex < BOTTOM_SECTION_COUNT) {
            return this.getDefaultState().with(FACING, facing);
        } else {
            Block topBlock = GREENFENCE_TOP.get().data;
            BlockState topState = topBlock.getDefaultState();
            topState = topState.with(BlockBlueFenceTop.FACING, facing);
            return topState;
        }
    }

    private boolean canReplace(BlockState state) {
        return state.isAir() || isFenceBlock(state.getBlock());
    }

    // 销毁
    private void destroyWholeFence(World world, BlockPos pos) {
        BlockPos bottom = findBottomFencePos(world, pos);
        for (int i = 0; i < MAX_FENCE_HEIGHT; i++) {
            world.breakBlock(bottom.up(i), false);
        }
    }

    private static VoxelShape createFenceShape(int zStart, int zEnd, int middleZStart, int middleZEnd) {
        return Stream.of(
                Block.createCuboidShape(15, 0, zStart, 16, 16, zEnd),
                Block.createCuboidShape(0, 0, zStart, 1, 16, zEnd),
                Block.createCuboidShape(1, 0, middleZStart, 15, 16, middleZEnd)
        ).reduce((v1, v2) -> VoxelShapes.combineAndSimplify(v1, v2, BooleanBiFunction.OR)).get();
    }
}