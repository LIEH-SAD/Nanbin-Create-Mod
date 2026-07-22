package com.Nanbin.Registry.RegBlock;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mtr.mapping.holder.BlockSettings;
import org.mtr.mapping.holder.BlockView;
import org.mtr.mapping.holder.ItemStack;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.holder.TextFormatting;
import org.mtr.mapping.holder.TooltipContext;
import org.mtr.mapping.mapper.BlockExtension;
import org.mtr.mod.Blocks;
import org.mtr.mod.generated.lang.TranslationProvider;

public class BlockStationColor extends BlockExtension {
    public BlockStationColor() {
        this(Blocks.createDefaultBlockSettings(false));
    }

    protected BlockStationColor(BlockSettings blockSettings) {
        super(blockSettings);
    }

    public void addTooltips(ItemStack stack, @Nullable BlockView world, List<MutableText> tooltip, TooltipContext options) {
        tooltip.add(TranslationProvider.TOOLTIP_MTR_STATION_COLOR.getMutableText(new Object[0]).formatted(TextFormatting.DARK_GRAY));
    }
}
