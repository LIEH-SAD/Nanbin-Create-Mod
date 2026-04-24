package com.Nanbin.Items;

import net.minecraft.item.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import static com.Nanbin.nanbin.LOGGER;

public class Items {
    public static final Item TRASH = new Item(new Item.Settings().group(ItemGroup.MISC));
    public static final Item EMETIC = new Item(new Item.Settings().group(ItemGroup.FOOD).food((new FoodComponent.Builder()).hunger(-1000).saturationModifier(0.0F).alwaysEdible().build()));

    public static void onInitialize() {
        LOGGER.info("[Nanbin Create Mod] Registering Items...");

        Registry.register(Registry.ITEM, new Identifier("nanbin", "trash"), TRASH);
        Registry.register(Registry.ITEM, new Identifier("nanbin", "emetic"), EMETIC);
    }
}
