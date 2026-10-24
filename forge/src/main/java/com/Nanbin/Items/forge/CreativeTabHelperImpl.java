package com.Nanbin.Items.forge;

import mtr.CreativeModeTabs;
import net.minecraft.item.Item;

public final class CreativeTabHelperImpl {
    private CreativeTabHelperImpl() {
    }

    public static void addToMtrCoreTab(Item item) {
        mtr.Registry.registerCreativeModeTab(CreativeModeTabs.CORE.resourceLocation, item);
    }
}
