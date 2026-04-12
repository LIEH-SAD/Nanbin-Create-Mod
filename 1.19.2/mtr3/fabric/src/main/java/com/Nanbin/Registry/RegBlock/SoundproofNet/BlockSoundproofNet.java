package com.Nanbin.Registry.RegBlock.SoundproofNet;

import com.Nanbin.Blocks.Blocks;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Material;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public class BlockSoundproofNet extends Block {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

    private static final VoxelShape  SOUNDPROOFNET_SHAPE_SOUTH = Stream.of(
            Block.createCuboidShape(15, 0, 0, 16, 16, 2),
            Block.createCuboidShape(0, 0, 0, 1, 16, 2),
            Block.createCuboidShape(1, 0, 0, 15, 16, 1)
    ).reduce((v1, v2) -> VoxelShapes.combineAndSimplify(v1, v2, BooleanBiFunction.OR)).get();

    private static final VoxelShape  SOUNDPROOFNET_SHAPE_NORTH = Stream.of(
            Block.createCuboidShape(15, 0, 14, 16, 16, 16),
            Block.createCuboidShape(0, 0, 14, 1, 16, 16),
            Block.createCuboidShape(1, 0, 15, 15, 16, 16)
    ).reduce((v1, v2) -> VoxelShapes.combineAndSimplify(v1, v2, BooleanBiFunction.OR)).get();

    private static final VoxelShape  SOUNDPROOFNET_SHAPE_WEST = Stream.of(
            Block.createCuboidShape(14, 0, 15, 16, 16, 16),
            Block.createCuboidShape(14, 0, 0, 16, 16, 1),
            Block.createCuboidShape(15, 0, 1, 16, 16, 15)
    ).reduce((v1, v2) -> VoxelShapes.combineAndSimplify(v1, v2, BooleanBiFunction.OR)).get();

    private static final VoxelShape  SOUNDPROOFNET_SHAPE_EAST = Stream.of(
            Block.createCuboidShape(0, 0, 15, 2, 16, 16),
            Block.createCuboidShape(0, 0, 0, 2, 16, 1),
            Block.createCuboidShape(0, 0, 1, 1, 16, 15)
    ).reduce((v1, v2) -> VoxelShapes.combineAndSimplify(v1, v2, BooleanBiFunction.OR)).get();

    public BlockSoundproofNet() {
        super(FabricBlockSettings.of(Material.METAL).strength(2.0F, 6.0F).nonOpaque());
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH));
    }

    // 放置6格逻辑（不变）
    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        World world = context.getWorld();
        if (world.isClient()) return null;

        BlockPos pos = context.getBlockPos();
        Direction facing = context.getPlayerFacing().getOpposite();
        if (facing.getAxis().isVertical()) facing = Direction.NORTH;

        // 检查6格空间
        for (int i = 0; i < 8; i++) {
            if (!world.getBlockState(pos.up(i)).canReplace(context)) {
                return null;
            }
        }

        // 从上到下：top → base  → glass → base
        world.setBlockState(pos.up(7), Blocks.SOUNDPROOFNET_TOP.getDefaultState().with(FACING, facing), 3);
        world.setBlockState(pos.up(6), Blocks.SOUNDPROOFNET_BASE.getDefaultState().with(FACING, facing), 3);
        world.setBlockState(pos.up(5), Blocks.SOUNDPROOFNET_BASE.getDefaultState().with(FACING, facing), 3);
        world.setBlockState(pos.up(4), Blocks.SOUNDPROOFNET_BASE.getDefaultState().with(FACING, facing), 3);
        world.setBlockState(pos.up(3), Blocks.SOUNDPROOFNET_BASE.getDefaultState().with(FACING, facing), 3);
        world.setBlockState(pos.up(2), Blocks.SOUNDPROOFNET_BASE.getDefaultState().with(FACING, facing), 3);
        world.setBlockState(pos.up(1), Blocks.SOUNDPROOFNET_GLASS.getDefaultState().with(FACING, facing), 3);
        world.setBlockState(pos.up(0), Blocks.SOUNDPROOFNET.getDefaultState().with(FACING, facing), 3);

        return getDefaultState().with(FACING, facing);
    }

    // ========== 核心修复：删除逻辑（彻底去掉递归） ==========
    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        // 1. 只在服务端执行
        if (world.isClient()) return;

        // ========== 修复：找最底部的逻辑（关键改这里） ==========
        BlockPos bottomPos = pos;
        // 最多向下找9格（防止无限循环），确保找到真正的最底部
        for (int downStep = 0; downStep < 8; downStep++) {
            BlockPos nextCheck = bottomPos.down(1);
            Block nextBlock = world.getBlockState(nextCheck).getBlock();

            // 只要下一格是我们的方块，就继续往下找
            if (nextBlock == Blocks.SOUNDPROOFNET_BASE
                    || nextBlock == Blocks.SOUNDPROOFNET_TOP
                    || nextBlock == Blocks.SOUNDPROOFNET_GLASS
                    || nextBlock == Blocks.SOUNDPROOFNET
            ) {
                bottomPos = nextCheck;
            } else {
                // 不是目标方块 → 停止找，此时bottomPos就是真正的最底部
                break;
            }
        }

        for (int upStep = 0; upStep < 8; upStep++) {
            BlockPos currentPos = bottomPos.up(upStep);
            Block currentBlock = world.getBlockState(currentPos).getBlock();

            // 只要是我们的方块，就设为空气
            if (currentBlock == Blocks.SOUNDPROOFNET_BASE
                    || currentBlock == Blocks.SOUNDPROOFNET_TOP
                    || currentBlock == Blocks.SOUNDPROOFNET_GLASS
                    || currentBlock == Blocks.SOUNDPROOFNET) {
                world.setBlockState(currentPos, net.minecraft.block.Blocks.AIR.getDefaultState(), 3);
                // 只在最底部掉落1个物品（非创造模式）
                if (!player.isCreative() && upStep == 0) {
                    dropStack(world, currentPos, new net.minecraft.item.ItemStack(Blocks.SOUNDPROOFNET_BASE));
                }
            }
        }

        super.onBreak(world, pos, state, player);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        Direction dir = state.get(Properties.HORIZONTAL_FACING);
        return switch (dir) {
            case NORTH -> SOUNDPROOFNET_SHAPE_NORTH;
            case SOUTH -> SOUNDPROOFNET_SHAPE_SOUTH;
            case EAST -> SOUNDPROOFNET_SHAPE_EAST;
            case WEST -> SOUNDPROOFNET_SHAPE_WEST;
            default -> SOUNDPROOFNET_SHAPE_NORTH;
        };
    }
}