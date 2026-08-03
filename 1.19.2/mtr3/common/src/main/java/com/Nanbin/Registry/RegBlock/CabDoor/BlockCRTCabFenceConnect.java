package com.Nanbin.Registry.RegBlock.CabDoor;

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

public class BlockCRTCabFenceConnect extends BlockDirectionalDoubleBlockBase {
    public static final IntProperty NUMBER = IntProperty.of("number", 1, 7);

    public BlockCRTCabFenceConnect(Settings settings) {
        super(settings);
    }

    public VoxelShape getOutlineShape(BlockState state, BlockView blockGetter, BlockPos pos, ShapeContext collisionContext) {
        Direction facing = (Direction) IBlock.getStatePropertySafe(state, FACING);
        VoxelShape shape1 = IBlock.getVoxelShapeByDirection(0, 0, 0, 16, 16, 3, facing);
        VoxelShape shape2 = IBlock.getVoxelShapeByDirection(0, 0, 2, 3, 16, 16, facing);
        return VoxelShapes.union(shape1, shape2);
    }

    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        Direction facing = (Direction)IBlock.getStatePropertySafe(state, FACING);
        VoxelShape shape1 = IBlock.getVoxelShapeByDirection(0, 0, 0, 16, 16, 3, facing);
        VoxelShape shape2 = IBlock.getVoxelShapeByDirection(0, 0, 2, 3, 16, 16, facing);
        return union(shape1, shape2);
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
