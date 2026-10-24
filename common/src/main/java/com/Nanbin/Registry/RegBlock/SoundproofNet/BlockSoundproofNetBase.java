package com.Nanbin.Registry.RegBlock.SoundproofNet;

import mtr.block.IBlock;
import mtr.mappings.BlockDirectionalMapper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

public class BlockSoundproofNetBase extends BlockDirectionalMapper {
    public BlockSoundproofNetBase(Settings settings) {
        super(settings);
    }

    public VoxelShape getOutlineShape(BlockState state, BlockView blockGetter, BlockPos pos, ShapeContext collisionContext) {
        VoxelShape shape1 = IBlock.getVoxelShapeByDirection(15, 0, 0, 16, 16, 2, (Direction)state.get(FACING));
        VoxelShape shape2 = IBlock.getVoxelShapeByDirection(0, 0, 0, 1, 16, 2, (Direction)state.get(FACING));
        VoxelShape shape3 = IBlock.getVoxelShapeByDirection(1, 0, 0, 15, 16, 1, (Direction)state.get(FACING));
        return VoxelShapes.union(shape1, shape2, shape3);
    }

    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return (BlockState)this.getDefaultState().with(FACING, ctx.getPlayerFacing());
    }

    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{FACING});
    }
}
