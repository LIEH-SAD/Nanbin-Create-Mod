package com.Nanbin.Registry.RegBlock;

import com.Nanbin.mapping.BlockExtension;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.Direction;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;

import java.util.List;

public class BlockFacingBlock extends BlockExtension {

	public static final Property<Direction> FACING = Properties.HORIZONTAL_FACING;

	public BlockFacingBlock(AbstractBlock.Settings settings) {
		super(settings);
	}

	@Override
	public void addBlockProperties(List<Property<?>> properties) {
		properties.add(FACING);
	}

	@Override
	public BlockState getPlacementState2(ItemPlacementContext ctx) {
		return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
	}
}
