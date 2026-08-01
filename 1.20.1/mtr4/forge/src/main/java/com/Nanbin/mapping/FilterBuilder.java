package com.Nanbin.mapping;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import org.mtr.mapping.holder.ItemStack;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.holder.PressAction;
import org.mtr.mapping.registry.BlockRegistryObject;
import org.mtr.mapping.registry.CreativeModeTabHolder;
import org.mtr.mapping.registry.ItemRegistryObject;
import ziyue.filters.Filter;

import java.util.function.Supplier;

/**
 * @since 1.0.0-beta-1
 */

public interface FilterBuilder
{
    static Filter registerFilter(CreativeModeTabHolder creativeModeTab, MutableText filterName, Supplier<ItemStack> filterIcon) {
        return ziyue.filters.FilterBuilder.registerFilter(getCreativeModeTabKey(creativeModeTab), filterName.data, () -> filterIcon.get().data);
    }

    static Filter registerUncategorizedItemsFilter(CreativeModeTabHolder creativeModeTab) {
        return ziyue.filters.FilterBuilder.registerUncategorizedItemsFilter(getCreativeModeTabKey(creativeModeTab));
    }

    static void filtersVisibility(CreativeModeTabHolder creativeModeTab, boolean visible) {
        ziyue.filters.FilterBuilder.filtersVisibility(getCreativeModeTabKey(creativeModeTab), visible);
    }

    static void setReservedButton(CreativeModeTabHolder creativeModeTab, MutableText tooltip, PressAction onPress) {
        ziyue.filters.FilterBuilder.setReservedButton(getCreativeModeTabKey(creativeModeTab), tooltip.data, onPress);
    }

    static void addBlocks(Filter filter, BlockRegistryObject... blocks) {
        for (BlockRegistryObject block : blocks) {
            filter.addItems(block.get().asItem().data);
        }
    }

    static void addItems(Filter filter, ItemRegistryObject... items) {
        for (ItemRegistryObject item : items) {
            filter.addItems(item.get().data);
        }
    }

    private static ResourceKey<CreativeModeTab> getCreativeModeTabKey(CreativeModeTabHolder creativeModeTab) {
        return ResourceKey.create(Registries.CREATIVE_MODE_TAB, creativeModeTab.identifier);
    }
}
