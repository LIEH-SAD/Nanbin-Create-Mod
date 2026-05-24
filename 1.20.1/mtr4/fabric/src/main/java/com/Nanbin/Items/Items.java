package com.Nanbin.Items;

import com.Nanbin.Registry.RegItem.ItemPhone;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.Item;
import org.mtr.mapping.registry.ItemRegistryObject;
import org.mtr.mod.CreativeModeTabs;
import org.mtr.mod.Init;

import static com.Nanbin.Init.MOD_ID;


public class Items {
    public final static ItemRegistryObject PHONE;

    static {
        PHONE = Init.REGISTRY.registerItem(new Identifier(MOD_ID, "phone"), (itemSettings) -> new Item(new ItemPhone(itemSettings)), CreativeModeTabs.CORE);
    }

    public static void init() {
        }
}
