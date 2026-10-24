package com.Nanbin.Items;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.item.Item;

public final class CreativeTabHelper {
    private CreativeTabHelper() {
    }

    @ExpectPlatform
    public static void addToMtrCoreTab(Item item) {
        throw new AssertionError();
    }
}
