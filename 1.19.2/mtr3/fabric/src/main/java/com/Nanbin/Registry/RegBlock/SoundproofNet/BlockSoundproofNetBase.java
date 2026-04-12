package com.Nanbin.Registry.RegBlock.SoundproofNet;

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
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import java.util.stream.Stream;

public class BlockSoundproofNetBase extends Block {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

    public BlockSoundproofNetBase() {
        super(FabricBlockSettings.of(Material.METAL).strength(2.0F, 6.0F).nonOpaque());
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH));
    }
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

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient()) {
            Blocks.SOUNDPROOFNET.onBreak(world, pos, state, player);
        }
        super.onBreak(world, pos, state, player);
    }

    // 重写方法
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

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}