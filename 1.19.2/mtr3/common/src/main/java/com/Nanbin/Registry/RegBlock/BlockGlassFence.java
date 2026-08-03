package com.Nanbin.Registry.RegBlock;

import mtr.block.BlockDirectionalDoubleBlockBase;
import mtr.block.IBlock;
import net.minecraft.block.AbstractBlock;
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

public class BlockGlassFence extends BlockDirectionalDoubleBlockBase {
    public static final IntProperty NUMBER = IntProperty.of("number", 1, 7);

    public BlockGlassFence(AbstractBlock.Settings settings) {
        super(settings);
    }

    public VoxelShape getOutlineShape(BlockState state, BlockView blockGetter, BlockPos pos, ShapeContext collisionContext) {
        Direction facing = (Direction) IBlock.getStatePropertySafe(state, FACING);
        return IBlock.getStatePropertySafe(state, HALF) == DoubleBlockHalf.UPPER ? IBlock.getVoxelShapeByDirection((double)0.0F, (double)0.0F, (double)0.0F, (double)16.0F, (double)3.0F, (double)3.0F, facing) : IBlock.getVoxelShapeByDirection((double)0.0F, (double)0.0F, (double)0.0F, (double)16.0F, (double)16.0F, (double)3.0F, facing);
    }

    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        Direction facing = (Direction)IBlock.getStatePropertySafe(state, FACING);
        return VoxelShapes.union(this.getOutlineShape(state, world, pos, context), IBlock.getVoxelShapeByDirection((double)0.0F, (double)0.0F, (double)0.0F, (double)16.0F, (double)8.0F, (double)3.0F, facing));
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
