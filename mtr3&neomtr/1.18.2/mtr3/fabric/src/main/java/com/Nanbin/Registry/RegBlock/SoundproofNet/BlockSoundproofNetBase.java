package com.Nanbin.Registry.RegBlock.SoundproofNet;

import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.*;
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

import java.util.stream.Stream;

public class BlockSoundproofNetBase extends HorizontalFacingBlock {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

    // 加在类里
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

    // 构造函数
    public BlockSoundproofNetBase(Settings settings) {
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
            case NORTH -> SOUNDPROOFNET_SHAPE_NORTH;
            case SOUTH -> SOUNDPROOFNET_SHAPE_SOUTH;
            case EAST -> SOUNDPROOFNET_SHAPE_EAST;
            case WEST -> SOUNDPROOFNET_SHAPE_WEST;
            default -> SOUNDPROOFNET_SHAPE_NORTH;
        };
    }

    // 碰撞形状（和外观形状保持一致）
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
        return this.getOutlineShape(state, world, pos, ctx);
    }}