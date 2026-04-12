package com.Nanbin.ItemsGroup;

import com.Nanbin.Blocks.Blocks;
import mtr.Items;
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import static com.mojang.text2speech.Narrator.LOGGER;

public class ItemsGroup {
    public static ItemGroup CITY_BUILDING_BLOCKS = FabricItemGroupBuilder.build(
            new Identifier("nanbin", "city_building_blocks"),
            () -> new ItemStack(Blocks.LIGHT_GREEN_BLOCK));

    public static ItemGroup USING_STATION_BUILDING_BLOCKS= FabricItemGroupBuilder.build(
            new Identifier("nanbin", "using_station_building_blocks"),
            () -> new ItemStack(Blocks.LOGO));

    public static ItemGroup USING_RAILWAY_BUILD = FabricItemGroupBuilder.build(
            new Identifier("nanbin", "using_railway_build"),
            () -> new ItemStack(Items.RAIL_CONNECTOR_300_ONE_WAY.get()));

    public static ItemGroup CRT = FabricItemGroupBuilder.build(
            new Identifier("nanbin", "crt_building_blocks"),
            () -> new ItemStack(Blocks.CRT_LOGO));

    public static void onInitialize() {
        LOGGER.info("[Nanbin Create Mod] Registering ItemsGroup...");
    }
}