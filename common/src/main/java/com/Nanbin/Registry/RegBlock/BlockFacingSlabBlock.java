package com.Nanbin.Registry.RegBlock;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.SlabBlock;
import net.minecraft.util.math.Direction;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;

public class BlockFacingSlabBlock extends SlabBlock {

	public static final Property<Direction> FACING = Properties.HORIZONTAL_FACING;

	public BlockFacingSlabBlock(AbstractBlock.Settings settings) {
		super(settings);
	}

	@Override
	protected void appendProperties(StateManager.Builder<net.minecraft.block.Block, BlockState> builder) {
		super.appendProperties(builder);
		builder.add(FACING);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		final BlockState state = super.getPlacementState(ctx);
		return (state == null ? getDefaultState() : state).with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
	}
}
