package com.Nanbin.Items;

import com.Nanbin.Blocks.Blocks;
import com.Nanbin.ItemsGroup.ItemsGroup;
import com.Nanbin.Registry.RegItem.ItemCRTAPG1;
import com.Nanbin.Registry.RegItem.ItemCRTAPG2;
import com.Nanbin.Registry.RegItem.ItemPhone;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.Item;
import org.mtr.mapping.registry.ItemRegistryObject;
import org.mtr.mod.CreativeModeTabs;
import org.mtr.mod.Init;

import static com.Nanbin.Init.MOD_ID;


public class Items {
    public static final ItemRegistryObject PHONE;
    public final static ItemRegistryObject CRT_APG_DOOR_1;
    public final static ItemRegistryObject CRT_APG_GLASS_1;
    public final static ItemRegistryObject CRT_APG_GLASS_END_1;
    public final static ItemRegistryObject CRT_APG_DOOR_2;
    public final static ItemRegistryObject CRT_APG_GLASS_2;
    public final static ItemRegistryObject CRT_APG_GLASS_END_2;

    static {
        PHONE = Init.REGISTRY.registerItem(new Identifier(MOD_ID, "phone"), (itemSettings) -> new Item(new ItemPhone(itemSettings)), CreativeModeTabs.CORE);
        CRT_APG_DOOR_1 = Init.REGISTRY.registerItem(new Identifier(MOD_ID, "crt_apg_door_1"), (itemSettings) ->  new Item(new ItemCRTAPG1(Blocks.CRT_APG_DOOR_1, itemSettings)), ItemsGroup.CRT);
        CRT_APG_GLASS_1 = Init.REGISTRY.registerItem(new Identifier(MOD_ID, "crt_apg_glass_1"), (itemSettings) ->  new Item(new ItemCRTAPG1(Blocks.CRT_APG_GLASS_1, itemSettings)), ItemsGroup.CRT);
        CRT_APG_GLASS_END_1 = Init.REGISTRY.registerItem(new Identifier(MOD_ID, "crt_apg_glass_end_1"), (itemSettings) ->  new Item(new ItemCRTAPG1(Blocks.CRT_APG_GLASS_END_1, itemSettings)), ItemsGroup.CRT);
        CRT_APG_DOOR_2 = Init.REGISTRY.registerItem(new Identifier(MOD_ID, "crt_apg_door_2"), (itemSettings) ->  new Item(new ItemCRTAPG2(Blocks.CRT_APG_DOOR_2, itemSettings)), ItemsGroup.CRT);
        CRT_APG_GLASS_2 = Init.REGISTRY.registerItem(new Identifier(MOD_ID, "crt_apg_glass_2"), (itemSettings) ->  new Item(new ItemCRTAPG2(Blocks.CRT_APG_GLASS_2, itemSettings)), ItemsGroup.CRT);
        CRT_APG_GLASS_END_2 = Init.REGISTRY.registerItem(new Identifier(MOD_ID, "crt_apg_glass_end_2"), (itemSettings) ->  new Item(new ItemCRTAPG2(Blocks.CRT_APG_GLASS_END_2, itemSettings)), ItemsGroup.CRT);
    }

    public static void init() {}
}
