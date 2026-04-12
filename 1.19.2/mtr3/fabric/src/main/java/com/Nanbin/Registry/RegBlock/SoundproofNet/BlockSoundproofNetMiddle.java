package com.Nanbin.Registry.RegBlock.SoundproofNet;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
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

public class BlockSoundproofNetMiddle extends HorizontalFacingBlock {
    // 1. 预定义四个朝向的形状（基于新的单段形状）
    // 朝北形状（原始形状：-7, 0, 5, 23, 28, 11）
    private static final VoxelShape SOUNDPROOFNETUP_SHAPE_NORTH = Stream.of(
            Block.createCuboidShape(1, 13, 0, 15, 14, 16),
            Block.createCuboidShape(0, 13, 0, 1, 14, 16),
            Block.createCuboidShape(15, 13, 0, 16, 14, 16)
    ).reduce((v1, v2) -> VoxelShapes.combineAndSimplify(v1, v2, BooleanBiFunction.OR)).get();

    // 朝东形状（90度旋转：x/z坐标按1.19.2规则转换）
    private static final VoxelShape SOUNDPROOFNETUP_SHAPE_EAST = Stream.of(
            Block.createCuboidShape(1, 13, 0, 15, 14, 16),
            Block.createCuboidShape(0, 13, 0, 1, 14, 16),
            Block.createCuboidShape(15, 13, 0, 16, 14, 16)
    ).reduce((v1, v2) -> VoxelShapes.combineAndSimplify(v1, v2, BooleanBiFunction.OR)).get();

    // 朝南形状（180度旋转）
    private static final VoxelShape SOUNDPROOFNETUP_SHAPE_SOUTH = Stream.of(
            Block.createCuboidShape(1, 13, 0, 15, 14, 16),
            Block.createCuboidShape(0, 13, 0, 1, 14, 16),
            Block.createCuboidShape(15, 13, 0, 16, 14, 16)
    ).reduce((v1, v2) -> VoxelShapes.combineAndSimplify(v1, v2, BooleanBiFunction.OR)).get();

    // 朝西形状（270度旋转）
    private static final VoxelShape SOUNDPROOFNETUP_SHAPE_WEST = Stream.of(
            Block.createCuboidShape(1, 13, 0, 15, 14, 16),
            Block.createCuboidShape(0, 13, 0, 1, 14, 16),
            Block.createCuboidShape(15, 13, 0, 16, 14, 16)
    ).reduce((v1, v2) -> VoxelShapes.combineAndSimplify(v1, v2, BooleanBiFunction.OR)).get();

    // 构造函数
    public BlockSoundproofNetMiddle(Settings settings) {
        super(settings);
        // 设置默认朝向为北
        setDefaultState(getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.NORTH));
    }

    // 注册朝向属性
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(Properties.HORIZONTAL_FACING);
    }

    // 1.19.2 兼容的放置朝向逻辑
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        Direction playerFacing = ctx.getPlayerFacing();
        // 过滤垂直方向，只保留水平朝向
        Direction horizontalFacing = playerFacing.getAxis().isHorizontal() ? playerFacing : Direction.NORTH;
        // 朝向玩家的反方向
        return this.getDefaultState().with(Properties.HORIZONTAL_FACING, horizontalFacing.getOpposite());
    }

    // 外观形状（参考VerticalSlabBlock的switch写法）
    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
        Direction dir = state.get(Properties.HORIZONTAL_FACING);
        return switch (dir) {
            case NORTH -> SOUNDPROOFNETUP_SHAPE_NORTH;
            case SOUTH -> SOUNDPROOFNETUP_SHAPE_SOUTH;
            case EAST -> SOUNDPROOFNETUP_SHAPE_EAST;
            case WEST -> SOUNDPROOFNETUP_SHAPE_WEST;
            default -> SOUNDPROOFNETUP_SHAPE_NORTH;
        };
    }

    // 碰撞形状（和外观形状保持一致）
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
        return this.getOutlineShape(state, world, pos, ctx);
    }
}
