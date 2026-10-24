
package com.Nanbin.ItemsGroup;

import com.Nanbin.Blocks.Blocks;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.ItemConvertible;
import org.mtr.mapping.holder.ItemStack;
import org.mtr.mapping.registry.CreativeModeTabHolder;
import org.mtr.mod.Items;

import static com.Nanbin.Init.MOD_ID;
import static com.Nanbin.Init.REGISTRY;

public class ItemsGroup {
    public static final CreativeModeTabHolder CITY_BUILDING_BLOCKS;
    public static final CreativeModeTabHolder USING_STATION_BUILDING_BLOCKS;
    public static final CreativeModeTabHolder USING_RAILWAY_BUILD;
    public static final CreativeModeTabHolder CRT;
    public static final CreativeModeTabHolder NRT;
    //public static final CreativeModeTabHolder CDM;

    static {
        CITY_BUILDING_BLOCKS = REGISTRY.createCreativeModeTabHolder(new Identifier(MOD_ID, "city_building_blocks"), () -> new ItemStack(new ItemConvertible(Blocks.LIGHT_GREEN_BLOCK.get().data)));
        USING_STATION_BUILDING_BLOCKS = REGISTRY.createCreativeModeTabHolder(new Identifier(MOD_ID, "using_station_building_blocks"), () -> new ItemStack(new ItemConvertible(Blocks.LOGO.get().data)));
        USING_RAILWAY_BUILD = REGISTRY.createCreativeModeTabHolder(new Identifier(MOD_ID, "using_railway_build"), () -> new ItemStack(new ItemConvertible(Items.RAIL_CONNECTOR_300_ONE_WAY.get().data)));
        CRT = REGISTRY.createCreativeModeTabHolder(new Identifier(MOD_ID, "crt_building_blocks"), () -> new ItemStack(new ItemConvertible(Blocks.CRT_LOGO.get().data)));
        NRT = REGISTRY.createCreativeModeTabHolder(new Identifier(MOD_ID, "nrt_building_blocks"), () -> new ItemStack(new ItemConvertible(Blocks.NRT_TICKET_1_EXIT.get().data)));
        //CDM = REGISTRY.createCreativeModeTabHolder(new Identifier(MOD_ID, "chengdu_metro_building_blocks"), () -> new ItemStack(new ItemConvertible(Blocks.CRT_LOGO.get().data)));
    }

    public static void init() {}
}
