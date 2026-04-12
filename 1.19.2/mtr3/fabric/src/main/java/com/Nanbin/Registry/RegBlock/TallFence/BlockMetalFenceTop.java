package com.Nanbin.Registry.RegBlock.TallFence;

import com.Nanbin.Blocks.Blocks;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Material;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class BlockMetalFenceTop extends Block {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    // 朝北形状（原始形状：-7, 0, 5, 23, 28, 11）
    private static final VoxelShape METALFENCE_TOP_SOUTH = Block.createCuboidShape(0, 0, 0, 16, 16, 6);

    // 朝东形状（90度旋转）
    private static final VoxelShape METALFENCE_TOP_WEST = Block.createCuboidShape(10, 0, 0, 16, 16, 16);
    // 朝南形状（180度旋转）
    private static final VoxelShape METALFENCE_TOP_NORTH =Block.createCuboidShape(0, 0, 10, 16, 16, 16);

    // 朝西形状（270度旋转）
    private static final VoxelShape METALFENCE_TOP_EAST = Block.createCuboidShape(0, 0, 0, 6, 16, 16);
    // 构造函数
    public BlockMetalFenceTop(Settings settings) {
        super(settings);
        // 设置默认朝向为北
        setDefaultState(getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.NORTH));
    }
    // 修复1：构造器名改为和类名一致
    public BlockMetalFenceTop() {
        // 修复2：材质改为IRON（顶部方块更硬）
        super(FabricBlockSettings.of(Material.METAL).strength(5.0F, 10.0F).nonOpaque());
        this.setDefaultState(this.getStateManager().getDefaultState().with(FACING, Direction.NORTH));
    }

    // 补：复用普通方块的删除逻辑（避免破坏时崩溃）
    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        // 直接调用普通方块的删除逻辑（确保四格同步删）
        Blocks.METALFENCE.onBreak(world, pos, state, player);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    // 外观形状（参考VerticalSlabBlock的switch写法）
    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
        Direction dir = state.get(Properties.HORIZONTAL_FACING);
        return switch (dir) {
            case NORTH -> METALFENCE_TOP_NORTH;
            case SOUTH -> METALFENCE_TOP_SOUTH;
            case EAST -> METALFENCE_TOP_EAST;
            case WEST -> METALFENCE_TOP_WEST;
            default -> METALFENCE_TOP_NORTH;
        };
    }

    //碰撞箱
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
        return this.getOutlineShape(state, world, pos, ctx);
    }
}