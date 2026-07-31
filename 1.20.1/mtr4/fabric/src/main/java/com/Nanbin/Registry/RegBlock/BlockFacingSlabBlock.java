package com.Nanbin.Registry.RegBlock;

import org.mtr.mapping.holder.BlockSettings;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.ItemPlacementContext;
import org.mtr.mapping.holder.Property;
import org.mtr.mapping.mapper.DirectionHelper;
import org.mtr.mapping.mapper.SlabBlockExtension;
import org.mtr.mapping.tool.HolderBase;

import java.util.List;

public class BlockFacingSlabBlock extends SlabBlockExtension {

    public BlockFacingSlabBlock(BlockSettings settings) {
        super(settings);
    }

    @Override
    public void addBlockProperties(List<HolderBase<?>> properties) {
        super.addBlockProperties(properties);
        properties.add(DirectionHelper.FACING);
    }

    @Override
    public BlockState getPlacementState2(ItemPlacementContext ctx) {
        return super.getPlacementState2(ctx).with(new Property<>(DirectionHelper.FACING.data), ctx.getPlayerFacing().getOpposite().data);
    }
}
