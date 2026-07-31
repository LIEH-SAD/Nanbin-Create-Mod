package com.Nanbin.Registry.RegBlock;

import org.mtr.mapping.holder.BlockSettings;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.ItemPlacementContext;
import org.mtr.mapping.holder.Property;
import org.mtr.mapping.mapper.BlockExtension;
import org.mtr.mapping.mapper.DirectionHelper;
import org.mtr.mapping.tool.HolderBase;

import java.util.List;

public class BlockFacingBlock extends BlockExtension {
    public BlockFacingBlock(BlockSettings settings) {
        super(settings);
    }

    @Override
    public void addBlockProperties(List<HolderBase<?>> properties) {
        properties.add(DirectionHelper.FACING);
    }

    @Override
    public BlockState getPlacementState2(ItemPlacementContext ctx) {
        return this.getDefaultState2().with(new Property<>(DirectionHelper.FACING.data), ctx.getPlayerFacing().getOpposite().data);
    }
}
