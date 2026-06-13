package com.Nanbin.Registry.RegBlock.SoundproofNet;

import com.Nanbin.Registry.RegMean.VoxelShapes;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockExtension;
import org.mtr.mapping.mapper.DirectionHelper;
import org.mtr.mapping.tool.HolderBase;
import org.mtr.mod.block.IBlock;

import javax.annotation.Nonnull;
import java.util.List;


public class BlockSoundproofNetTop extends BlockExtension implements DirectionHelper {
public BlockSoundproofNetTop(BlockSettings blockSettings) {
    super(blockSettings);
}

    public BlockState getPlacementState2(ItemPlacementContext ctx) {
        return this.getDefaultState2().with(new Property<>(FACING.data), ctx.getPlayerFacing().data);
    }

    @Nonnull
    public VoxelShape getOutlineShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        VoxelShape shape1 = IBlock.getVoxelShapeByDirection(0, 12, 0, 16, 14, 16, IBlock.getStatePropertySafe(state, FACING));
        VoxelShape shape2 = IBlock.getVoxelShapeByDirection(0, 0, 0, 16, 14, 2, IBlock.getStatePropertySafe(state, FACING));
       return VoxelShapes.union(shape1, shape2);
    }

    public void addBlockProperties(List<HolderBase<?>> properties) {
        super.addBlockProperties(properties);
        properties.add(FACING);
    }
}