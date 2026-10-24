package com.Nanbin.mapping;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import org.mtr.mapping.holder.ItemStack;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.holder.PressAction;
import org.mtr.mapping.registry.CreativeModeTabHolder;
import ziyue.filters.Filter;

public interface FilterBuilder {
    static Filter registerFilter(CreativeModeTabHolder creativeModeTab, MutableText filterName, Supplier<ItemStack> filterIcon) {
        return ziyue.filters.FilterBuilder.registerFilter(getCreativeModeTabKey(creativeModeTab), (Component)filterName.data, () -> (net.minecraft.world.item.ItemStack)((ItemStack)filterIcon.get()).data);
    }

    static void setReservedButton(CreativeModeTabHolder creativeModeTab, MutableText tooltip, PressAction onPress) {
        ziyue.filters.FilterBuilder.setReservedButton(getCreativeModeTabKey(creativeModeTab), (Component)tooltip.data, onPress);
    }

    private static ResourceKey<CreativeModeTab> getCreativeModeTabKey(CreativeModeTabHolder creativeModeTab) {
        return ResourceKey.create(Registries.CREATIVE_MODE_TAB, creativeModeTab.identifier);
    }
}
