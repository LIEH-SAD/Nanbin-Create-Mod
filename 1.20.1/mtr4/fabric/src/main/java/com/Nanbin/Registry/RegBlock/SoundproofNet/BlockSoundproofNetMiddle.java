package com.Nanbin.Registry.RegBlock.SoundproofNet;

import com.Nanbin.Registry.RegMean.VoxelShapes;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockExtension;
import org.mtr.mapping.mapper.DirectionHelper;
import org.mtr.mapping.tool.HolderBase;
import org.mtr.mod.block.IBlock;

import javax.annotation.Nonnull;
import java.util.List;

public class BlockSoundproofNetMiddle extends BlockExtension implements DirectionHelper {
public BlockSoundproofNetMiddle(BlockSettings blockSettings) {
    super(blockSettings);
}

    public BlockState getPlacementState2(ItemPlacementContext ctx) {
        return this.getDefaultState2().with(new Property<>(FACING.data), ctx.getPlayerFacing().data);
    }

    @Nonnull
    public VoxelShape getOutlineShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        VoxelShape shape1 = IBlock.getVoxelShapeByDirection(1, 13, 0, 15, 14, 16, IBlock.getStatePropertySafe(state, FACING));
        VoxelShape shape2 = IBlock.getVoxelShapeByDirection(0, 13, 0, 1, 14, 16, IBlock.getStatePropertySafe(state, FACING));
        VoxelShape shape3 = IBlock.getVoxelShapeByDirection(15, 13, 0, 16, 14, 16, IBlock.getStatePropertySafe(state, FACING));
        return VoxelShapes.union(shape1, shape2, shape3);
    }

    public void addBlockProperties(List<HolderBase<?>> properties) {
        super.addBlockProperties(properties);
        properties.add(FACING);
    }
}
