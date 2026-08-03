package com.Nanbin.Registry.RegBlock;

import mtr.block.BlockDirectionalDoubleBlockBase;
import mtr.block.IBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

import static net.minecraft.util.shape.VoxelShapes.union;

public class BlockGlassFenceConnect extends BlockDirectionalDoubleBlockBase {
    public static final IntProperty NUMBER = IntProperty.of("number", 1, 7);

    public BlockGlassFenceConnect(Settings settings) {
        super(settings);
    }

    public VoxelShape getOutlineShape(BlockState state, BlockView blockGetter, BlockPos pos, ShapeContext collisionContext) {
        Direction facing = (Direction) IBlock.getStatePropertySafe(state, FACING);
        return IBlock.getStatePropertySafe(state, HALF) != DoubleBlockHalf.UPPER ?
                union(IBlock.getVoxelShapeByDirection(0, 0, 0, 16, 16, 2, facing), IBlock.getVoxelShapeByDirection(14, 0, 2, 16, 16, 16, facing)) : union(IBlock.getVoxelShapeByDirection(0, 0, 0, 16, 3, 2, facing), IBlock.getVoxelShapeByDirection(14, 0, 2, 16, 3, 16, facing));
    }

    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        Direction facing = (Direction)IBlock.getStatePropertySafe(state, FACING);
        return union(this.getOutlineShape(state, world, pos, context), union(IBlock.getVoxelShapeByDirection(0, 0, 0, 16, 24, 2, facing), IBlock.getVoxelShapeByDirection(14, 0, 2, 16, 24, 16, facing)));
    }

    public VoxelShape getCameraCollisionShape(BlockState blockState, BlockView blockGetter, BlockPos blockPos, ShapeContext collisionContext) {
        return VoxelShapes.empty();
    }

    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{FACING, HALF, NUMBER});
    }

    protected BlockState getAdditionalState(BlockPos pos, Direction facing) {
        return (BlockState)this.getDefaultState().with(NUMBER, getNumber(pos, facing));
    }

    private static int getNumber(BlockPos pos, Direction facing) {
        int x = (pos.getX() % 7 + 7) % 7;
        int z = (pos.getZ() % 7 + 7) % 7;
        return facing != Direction.NORTH && facing != Direction.EAST ? (-x - z) % 7 + 7 : (x + z) % 7 + 1;
    }
}
