package com.Nanbin.Registry.RegBlock;

import com.Nanbin.mapping.BlockExtension;
import com.Nanbin.mapping.TranslationProvider;
import net.minecraft.block.AbstractBlock;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class BlockStationColor extends BlockExtension {

	public BlockStationColor(AbstractBlock.Settings settings) {
		super(settings);
	}

	@Override
	public void addTooltips(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(TranslationProvider.TOOLTIP_STATION_COLOR.getMutableText().formatted(Formatting.DARK_GRAY));
	}
}
