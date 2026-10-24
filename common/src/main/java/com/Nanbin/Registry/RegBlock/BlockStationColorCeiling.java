package com.Nanbin.Registry.RegBlock;

import com.Nanbin.mapping.BlockExtension;
import com.Nanbin.mapping.TranslationProvider;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import org.mtr.block.IBlock;

import java.util.List;

public class BlockStationColorCeiling extends BlockExtension {

	public static final Property<Direction> FACING = Properties.HORIZONTAL_FACING;

	public BlockStationColorCeiling(AbstractBlock.Settings settings) {
		super(settings);
	}

	@Override
	public BlockState getPlacementState2(ItemPlacementContext ctx) {
		return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing());
	}

	@Override
	public VoxelShape getOutlineShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return IBlock.getVoxelShapeByDirection(0, 0, 0, 16, 5, 16, IBlock.getStatePropertySafe(state, FACING));
	}

	@Override
	public void addTooltips(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(TranslationProvider.TOOLTIP_STATION_COLOR.getMutableText().formatted(Formatting.DARK_GRAY));
	}

	@Override
	public void addBlockProperties(List<Property<?>> properties) {
		properties.add(FACING);
	}
}
