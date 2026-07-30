package com.Nanbin.Items;

import com.Nanbin.Registry.RegItem.ItemPhone;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.Item;
import org.mtr.mapping.holder.ItemSettings;
import org.mtr.mapping.registry.ItemRegistryObject;
import org.mtr.mod.CreativeModeTabs;
import org.mtr.mod.Init;

import static com.Nanbin.Init.MOD_ID;


public class Items {
    public static final ItemRegistryObject PHONE;
    public static final ItemRegistryObject IC_CARD;

    static {
        PHONE = Init.REGISTRY.registerItem(new Identifier(MOD_ID, "phone"), (itemSettings) -> new Item(new ItemPhone(itemSettings)), CreativeModeTabs.CORE);
        IC_CARD = Init.REGISTRY.registerItem(new Identifier(MOD_ID, "ic_card"), (itemSettings) ->  new Item(itemSettings), CreativeModeTabs.CORE);
    }

    public static void init() {
        }
}
