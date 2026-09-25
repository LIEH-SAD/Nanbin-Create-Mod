package com.Nanbin.Registry.RegBlock;

import net.minecraft.block.FenceBlock;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.BlockView;

import java.util.List;

public class BlockTempFence extends FenceBlock {
    public BlockTempFence(Settings settings) {
        super(settings);
    }

    public void appendTooltip(ItemStack itemStack, BlockView blockGetter, List<Text> tooltip, TooltipContext tooltipFlag) {
        tooltip.add(mtr.mappings.Text.translatable("tooltip.nanbin.block.crt_temp_fence_1", new Object[0]).setStyle(Style.EMPTY.withColor(Formatting.GRAY)));
    }
}
