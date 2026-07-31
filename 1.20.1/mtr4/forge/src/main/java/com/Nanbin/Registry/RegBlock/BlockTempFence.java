package com.Nanbin.Registry.RegBlock;

import com.Nanbin.mappingForge.TranslationProvider;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.FenceBlockExtension;

import javax.annotation.Nullable;
import java.util.List;

public class BlockTempFence extends FenceBlockExtension {
    public BlockTempFence(BlockSettings settings) {
        super(settings);
    }

    public void addTooltips(ItemStack stack, @Nullable BlockView world, List<MutableText> tooltip, TooltipContext options) {
        tooltip.add(TranslationProvider.BLOCK_TEMP_FENCE.getMutableText(new Object[0]).formatted(TextFormatting.DARK_GRAY));
    }
}
