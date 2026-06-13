package com.Nanbin.Registry.RegBlock;

import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockExtension;
import org.mtr.mapping.mapper.DirectionHelper;
import org.mtr.mapping.tool.HolderBase;
import org.mtr.mod.block.IBlock;

import javax.annotation.Nonnull;
import java.util.List;

public class BlockCRTLogo extends BlockExtension implements DirectionHelper {
    public BlockCRTLogo(BlockSettings blockSettings) {
        super(blockSettings);
    }

    public BlockState getPlacementState2(ItemPlacementContext ctx) {
        return this.getDefaultState2().with(new Property<>(FACING.data), ctx.getPlayerFacing().data);
    }

    @Nonnull
    public VoxelShape getOutlineShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return IBlock.getVoxelShapeByDirection(-7, 0, 5, 23, 28, 11, IBlock.getStatePropertySafe(state, FACING));
    }

    public void addBlockProperties(List<HolderBase<?>> properties) {
        super.addBlockProperties(properties);
        properties.add(FACING);
    }
}