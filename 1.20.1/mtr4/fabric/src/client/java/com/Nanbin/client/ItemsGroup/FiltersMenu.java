package com.Nanbin.client.ItemsGroup;

import com.Nanbin.ItemsGroup.ItemsGroup;
import com.Nanbin.client.Menu.NanbinConfigScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import ziyue.filters.Filter;
import ziyue.filters.FilterBuilder;

public class FiltersMenu {
    public static Filter CRT_OPTION;
    public static Filter NRT_OPTION;
    public static Filter UCC_OPTION;
    public static Filter USC_OPTION;
    public static Filter URC_OPTION;

    public static void init(){
        FilterBuilder.setReservedButton(ItemsGroup.CRT.creativeModeTab, Text.translatable("Menu.nanbin.about"), button -> MinecraftClient.getInstance().setScreen(new NanbinConfigScreen()));
        //FilterBuilder.setReservedButton(ItemsGroup.CRT.creativeModeTab, Text.translatable("Menu.nanbin.about"), button -> MinecraftClient.getInstance().setScreen(new NanbinConfigScreen()));
        FilterBuilder.setReservedButton(ItemsGroup.CITY_BUILDING_BLOCKS.creativeModeTab, Text.translatable("Menu.nanbin.about"), button -> MinecraftClient.getInstance().setScreen(new NanbinConfigScreen()));
        //FilterBuilder.setReservedButton(ItemsGroup.USING_STATION_BUILDING_BLOCKS.creativeModeTab, Text.translatable("Menu.nanbin.about"), button -> MinecraftClient.getInstance().setScreen(new NanbinConfigScreen()));
        //FilterBuilder.setReservedButton(ItemsGroup.USING_RAILWAY_BUILD.creativeModeTab, Text.translatable("Menu.nanbin.about"), button -> MinecraftClient.getInstance().setScreen(new NanbinConfigScreen()));
    }
}
