package com.Nanbin.ItemsGroup;

import com.Nanbin.Init;
import com.Nanbin.Items.Items;
import dev.architectury.registry.CreativeTabRegistry;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public class ItemsGroup {
    public static final ItemGroup CITY_BUILDING_BLOCKS = CreativeTabRegistry.create(
            new Identifier(Init.MOD_ID, "city_building_blocks"),
            () -> new ItemStack(Items.LIGHT_GREEN_BLOCK.get()));

    public static final ItemGroup USING_STATION_BUILDING_BLOCKS = CreativeTabRegistry.create(
            new Identifier(Init.MOD_ID, "using_station_building_blocks"),
            () -> new ItemStack(Items.LOGO.get()));

    public static final ItemGroup USING_RAILWAY_BUILD = CreativeTabRegistry.create(
            new Identifier(Init.MOD_ID, "using_railway_build"),
            () -> new ItemStack(mtr.Items.RAIL_CONNECTOR_300_ONE_WAY.get()));

    public static final ItemGroup CRT = CreativeTabRegistry.create(
            new Identifier(Init.MOD_ID, "crt_building_blocks"),
            () -> new ItemStack(Items.CRT_LOGO.get()));

    public static final ItemGroup NRT = CreativeTabRegistry.create(
            new Identifier(Init.MOD_ID, "nrt_building_blocks"),
            () -> new ItemStack(Items.NRT_TICKET_1_EXIT.get()));

    public static void init(){}
}
