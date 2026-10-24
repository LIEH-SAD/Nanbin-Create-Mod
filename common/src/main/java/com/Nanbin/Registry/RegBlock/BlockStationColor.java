package com.Nanbin.Registry.RegBlock;

import mtr.mappings.BlockMapper;
import net.minecraft.block.AbstractBlock;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.BlockView;

import java.util.List;

public class BlockStationColor extends BlockMapper {
    public BlockStationColor(AbstractBlock.Settings settings) {
        super(settings);
    }

    public void appendTooltip(ItemStack itemStack, BlockView blockGetter, List<Text> tooltip, TooltipContext tooltipFlag) {
        tooltip.add(mtr.mappings.Text.translatable("tooltip.mtr.station_color", new Object[0]).setStyle(Style.EMPTY.withColor(Formatting.GRAY)));
    }
}
