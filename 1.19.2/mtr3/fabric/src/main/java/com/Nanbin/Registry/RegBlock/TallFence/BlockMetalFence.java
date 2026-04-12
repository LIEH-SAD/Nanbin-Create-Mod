package com.Nanbin.Registry.RegBlock.TallFence;

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

/*
               —————       ——
              |     |     |  |
              |  |   |    |  |
              |  | |  |   |  |
              |  |  |  |  |  |
              |  |   |  | |  |
              |  |    |  ||  |
              |  |     |  |  | anbin  提醒您
               ——       ——————

               此内容由     A           I       生成

 */
public class BlockMetalFence extends Block {
    // 1. 必须注册FACING属性（修复崩溃点2）
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    // 朝北形状（原始形状：-7, 0, 5, 23, 28, 11）
    private static final VoxelShape METALFENCE_SOUTH = Stream.of(
            Block.createCuboidShape(15, 0, 0, 16, 16, 2),
            Block.createCuboidShape(0, 0, 0, 1, 16, 2),
            Block.createCuboidShape(1, 0, 0, 15, 16, 1)
    ).reduce((v1, v2) -> VoxelShapes.combineAndSimplify(v1, v2, BooleanBiFunction.OR)).get();

    // 朝东形状（90度旋转）
    private static final VoxelShape METALFENCE_WEST = Block.createCuboidShape(14, 0, 0, 16, 16, 16);
    // 朝南形状（180度旋转）
    private static final VoxelShape METALFENCE_NORTH =Stream.of(
            Block.createCuboidShape(15, 0, 14, 16, 16, 16),
            Block.createCuboidShape(0, 0, 14, 1, 16, 16),
            Block.createCuboidShape(1, 0, 15, 15, 16, 16)
    ).reduce((v1, v2) -> VoxelShapes.combineAndSimplify(v1, v2, BooleanBiFunction.OR)).get();

    // 朝西形状（270度旋转）
    private static final VoxelShape METALFENCE_EAST = Block.createCuboidShape(0, 0, 0, 2, 16, 16);
    // 构造函数
    public BlockMetalFence(Settings settings) {
        super(settings);
        // 设置默认朝向为北
        setDefaultState(getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.NORTH));
    }

    public BlockMetalFence() {
        super(FabricBlockSettings.of(Material.METAL).strength(2.0F, 6.0F).nonOpaque());
        this.setDefaultState(this.getStateManager().getDefaultState().with(FACING, Direction.NORTH));
    }

    // 2. 放置时生成竖直四格（修复崩溃点3：用注册类的实例）
    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        World world = context.getWorld();
        if (world.isClient()) return null; // 仅服务端执行（修复崩溃点4）

        BlockPos clickPos = context.getBlockPos();
        Direction facing = context.getPlayerFacing().getOpposite();
        if (facing.getAxis().isVertical()) facing = Direction.NORTH;

        // 检测四格空间
        boolean canPlace = true;
        for (int i = 0; i < 4; i++) {
            if (!world.getBlockState(clickPos.up(i)).canReplace(context)) {
                canPlace = false;
                break;
            }
        }

        if (canPlace) {
            // 下3格：普通方块
            for (int i = 0; i < 3; i++) {
                world.setBlockState(clickPos.up(i), this.getDefaultState().with(FACING, facing), 3);
            }
            // 第4格：用注册类的顶部方块实例（修复崩溃点3）
            world.setBlockState(clickPos.up(3), Blocks.METALFENCE_TOP.getDefaultState().with(FACING, facing), 3);
            return this.getDefaultState().with(FACING, facing);
        }
        return null;
    }

    // 3. 修复onBreak无限递归（修复崩溃点1：移除super.onBreak，直接setBlockToAir）
    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (world.isClient()) return;

        // 找四格的真实底部
        BlockPos bottomPos = pos;
        while (true) {
            BlockPos check = bottomPos.down();
            if (world.getBlockState(check).getBlock() instanceof BlockMetalFence || world.getBlockState(check).getBlock() instanceof BlockMetalFenceTop) {
                bottomPos = check;
            } else break;
        }

        // 找四格的真实顶部
        BlockPos topPos = pos;
        while (true) {
            BlockPos check = topPos.up();
            if (world.getBlockState(check).getBlock() instanceof BlockMetalFence || world.getBlockState(check).getBlock() instanceof BlockMetalFenceTop) {
                topPos = check;
            } else break;
        }

        // 直接setBlockToAir，避免调用breakBlock触发递归（修复崩溃点1）
        BlockPos current = bottomPos;
        while (true) {
            if (world.getBlockState(current).getBlock() instanceof BlockMetalFence || world.getBlockState(current).getBlock() instanceof BlockMetalFenceTop) {
                world.setBlockState(current, net.minecraft.block.Blocks.AIR.getDefaultState(), 3);
                if (!player.isCreative()) {
                    dropStack(world, current, new net.minecraft.item.ItemStack(Blocks.METALFENCE));
                }
            }
            if (current.equals(topPos)) break;
            current = current.up();
        }
    }

    // 必须注册FACING属性（修复崩溃点2）
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    // 外观形状（参考VerticalSlabBlock的switch写法）
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

    //碰撞箱
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
        return this.getOutlineShape(state, world, pos, ctx);
    }
}