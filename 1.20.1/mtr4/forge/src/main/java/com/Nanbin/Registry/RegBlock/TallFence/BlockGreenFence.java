package com.Nanbin.Registry.RegBlock.TallFence;

import com.Nanbin.mapping.TranslationProvider;
import com.Nanbin.mapping.WorldHelper;
import org.jetbrains.annotations.Nullable;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockExtension;
import org.mtr.mapping.mapper.DirectionHelper;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mapping.tool.HolderBase;
import org.mtr.mod.block.IBlock;

import java.util.List;
import java.util.stream.Stream;

import static com.Nanbin.Blocks.Blocks.GREENFENCE_TOP;

public class BlockGreenFence extends BlockExtension {
    public static final DirectionProperty FACING = DirectionHelper.FACING;

    private static final int MAX_FENCE_HEIGHT = 4;        // 总高度4格
    private static final int BOTTOM_SECTION_COUNT = 3;

    private static final VoxelShape BLUEFENCE_SOUTH = createFenceShape(0, 2, 0, 1);
    private static final VoxelShape BLUEFENCE_NORTH = createFenceShape(14, 16, 15, 16);
    private static final VoxelShape BLUEFENCE_WEST = Block.createCuboidShape(14, 0, 0, 16, 16, 16);
    private static final VoxelShape BLUEFENCE_EAST = Block.createCuboidShape(0, 0, 0, 2, 16, 16);

    public BlockGreenFence(BlockSettings settings) {
        super(settings);
    }

    //判断上方4格
    @Nullable
    @Override
    public BlockState getPlacementState2(ItemPlacementContext context) {
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
        if (maxRequiredY > WorldHelper.getTopY(world)) {
            sendPlayerMessage(world, player, "tips.fence_too_high");
            return null;
        }

        return this.getDefaultState2().with(new Property<>(FACING.data), context.getPlayerFacing().getOpposite().data);
    }

    @Override
    public void onPlaced2(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.onPlaced2(world, pos, state, placer, stack);
        if (world.isClient()) return;

        BlockPos bottom = findBottomFencePos(world, pos);
        Direction facing = IBlock.getStatePropertySafe(state, DirectionHelper.FACING);

        for (int i = 0; i < MAX_FENCE_HEIGHT; i++) {
            BlockPos targetPos = bottom.up(i);
            BlockState targetState = createFenceState(facing, i);
            if (canReplace(world.getBlockState(targetPos))) {
                world.setBlockState(targetPos, targetState, 3);
            }
        }
    }

    //联动破坏
    @Override
    public void onStateReplaced2(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        super.onStateReplaced2(state, world, pos, newState, moved);
        if (world.isClient() || isFenceBlock(newState.getBlock())) return;
        destroyWholeFence(world, pos);
    }

    @Override
    public void addBlockProperties(List<HolderBase<?>> properties) {
        properties.add(DirectionHelper.FACING);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
        Direction direction = IBlock.getStatePropertySafe(state, DirectionHelper.FACING);
        return switch (direction) {
            case NORTH -> BLUEFENCE_NORTH;
            case SOUTH -> BLUEFENCE_SOUTH;
            case EAST -> BLUEFENCE_EAST;
            case WEST -> BLUEFENCE_WEST;
            default -> BLUEFENCE_NORTH;
        };
    }

    @Override
    public VoxelShape getCollisionShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
        return getOutlineShape2(state, world, pos, ctx);
    }

    private void sendPlayerMessage(World world, @Nullable PlayerEntity player, String key) {
        if (!world.isClient() && player != null) {
            player.sendMessage(Text.cast(TextHelper.translatable(key)), true);
        }
    }

    private boolean isFenceBlock(Block block) {
        return block.data instanceof BlockGreenFence || block.data instanceof BlockBlueFenceTop;
    }

    private BlockPos findBottomFencePos(World world, BlockPos pos) {
        BlockPos bottom = pos;
        while (bottom.getY() > WorldHelper.getBottomY(world)) {
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
            return this.getDefaultState2().with(new Property<>(FACING.data), facing.data);
        } else {
            Block topBlock = GREENFENCE_TOP.get();
            BlockState topState = topBlock.getDefaultState();
            topState = topState.with(new Property<>(BlockBlueFenceTop.FACING.data), facing.data);
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
        ).reduce((v1, v2) -> VoxelShapes.combineAndSimplify(v1, v2, BooleanBiFunction.getOrMapped())).get();
    }

    public void addTooltips(ItemStack stack, @javax.annotation.Nullable BlockView world, List<MutableText> tooltip, TooltipContext options) {
        tooltip.add(TranslationProvider.BLOCK_TALL_FENCE.getMutableText(new Object[0]).formatted(TextFormatting.DARK_GRAY));
    }
}
