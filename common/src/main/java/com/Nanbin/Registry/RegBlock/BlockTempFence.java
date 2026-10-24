package com.Nanbin.Registry.RegBlock;

import com.Nanbin.mapping.TranslationProvider;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.FenceBlock;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class BlockTempFence extends FenceBlock {

	public BlockTempFence(AbstractBlock.Settings settings) {
		super(settings);
	}

	@Override
	public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(TranslationProvider.BLOCK_TEMP_FENCE.getMutableText().formatted(Formatting.DARK_GRAY));
	}
}
