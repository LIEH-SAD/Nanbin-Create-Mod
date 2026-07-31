package com.Nanbin.Registry.RegBlock;

import com.Nanbin.mappingFabric.TranslationProvider;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockExtension;
import org.mtr.mod.Blocks;

import javax.annotation.Nullable;
import java.util.List;

public class BlockStationColor extends BlockExtension {
    public BlockStationColor() {
        this(Blocks.createDefaultBlockSettings(false));
    }

    protected BlockStationColor(BlockSettings blockSettings) {
        super(blockSettings);
    }

    public void addTooltips(ItemStack stack, @Nullable BlockView world, List<MutableText> tooltip, TooltipContext options) {
        tooltip.add(TranslationProvider.TOOLTIP_STATION_COLOR.getMutableText(new Object[0]).formatted(TextFormatting.DARK_GRAY));
    }
}
