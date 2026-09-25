package com.Nanbin.Items;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.Nanbin.Registry.Init.MOD_ID;

public class Items {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);

    public static final DeferredItem<Item>
            TEST_ITEM = reg("test_item");

    private static DeferredItem<Item> reg(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties()));
    }
}