package com.Nanbin.Items.fabric;

import mtr.CreativeModeTabs;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;

public final class CreativeTabHelperImpl {
    private CreativeTabHelperImpl() {
    }

    public static void addToMtrCoreTab(Item item) {
        ItemGroupEvents.MODIFY_ENTRIES_ALL.register((group, entries) -> {
            if (group == CreativeModeTabs.CORE.get()) {
                entries.add(item);
            }
        });
    }
}
