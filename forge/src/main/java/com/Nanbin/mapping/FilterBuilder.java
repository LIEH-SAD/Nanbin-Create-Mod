package com.Nanbin.mapping;

import java.util.function.Supplier;
import org.mtr.mapping.holder.ItemStack;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.holder.PressAction;
import org.mtr.mapping.registry.CreativeModeTabHolder;
import ziyue.filters.Filter;

public interface FilterBuilder {
    static Filter registerFilter(CreativeModeTabHolder creativeModeTab, MutableText filterName, Supplier<ItemStack> filterIcon) {
        //? if <1.17 {
        /*return ziyue.filters.FilterBuilder.registerFilter(creativeModeTab.creativeModeTab, (net.minecraft.util.text.ITextComponent) filterName.data, () -> (net.minecraft.item.ItemStack) ((ItemStack) filterIcon.get()).data);
        *///?} else if <1.19.4 {
        /*return ziyue.filters.FilterBuilder.registerFilter(creativeModeTab.creativeModeTab, (net.minecraft.network.chat.Component) filterName.data, () -> (net.minecraft.world.item.ItemStack) ((ItemStack) filterIcon.get()).data);
        *///?} else if <1.20 {
        /*return ziyue.filters.FilterBuilder.registerFilter(getCreativeModeTab(creativeModeTab), (net.minecraft.network.chat.Component) filterName.data, () -> (net.minecraft.world.item.ItemStack) ((ItemStack) filterIcon.get()).data);
        *///?} else {
        return ziyue.filters.FilterBuilder.registerFilter(getCreativeModeTabKey(creativeModeTab), (net.minecraft.network.chat.Component) filterName.data, () -> (net.minecraft.world.item.ItemStack) ((ItemStack) filterIcon.get()).data);
        //?}
    }

    static void setReservedButton(CreativeModeTabHolder creativeModeTab, MutableText tooltip, PressAction onPress) {
        //? if <1.17 {
        /*ziyue.filters.FilterBuilder.setReservedButton(creativeModeTab.creativeModeTab, (net.minecraft.util.text.ITextComponent) tooltip.data, onPress);
        *///?} else if <1.19.4 {
        /*ziyue.filters.FilterBuilder.setReservedButton(creativeModeTab.creativeModeTab, (net.minecraft.network.chat.Component) tooltip.data, onPress);
        *///?} else if <1.20 {
        /*ziyue.filters.FilterBuilder.setReservedButton(getCreativeModeTab(creativeModeTab), (net.minecraft.network.chat.Component) tooltip.data, onPress);
        *///?} else {
        ziyue.filters.FilterBuilder.setReservedButton(getCreativeModeTabKey(creativeModeTab), (net.minecraft.network.chat.Component) tooltip.data, onPress);
        //?}
    }

    //? if >=1.20 {
    private static net.minecraft.resources.ResourceKey<net.minecraft.world.item.CreativeModeTab> getCreativeModeTabKey(CreativeModeTabHolder creativeModeTab) {
        return net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB, creativeModeTab.identifier);
    }
    //?}

    //? if >=1.19.4 && <1.20 {
    /*private static net.minecraft.world.item.CreativeModeTab getCreativeModeTab(CreativeModeTabHolder creativeModeTab) {
        return net.minecraftforge.common.CreativeModeTabRegistry.getTab(creativeModeTab.identifier);
    }
    *///?}
}
