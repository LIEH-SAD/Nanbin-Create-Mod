package com.Nanbin.client.FiltersGroup;

import com.Nanbin.ItemsGroup.ItemsGroup;
import com.Nanbin.client.Screen.NanbinConfigScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import ziyue.filters.Filter;
import ziyue.filters.FilterBuilder;

public class FiltersMenu {

    public static void init(){
        FilterBuilder.setReservedButton(ItemsGroup.CRT.creativeModeTab, Text.translatable("Menu.nanbin.about"), button -> MinecraftClient.getInstance().setScreen(new NanbinConfigScreen()));
        //FilterBuilder.setReservedButton(ItemsGroup.NRT.creativeModeTab, Text.translatable("Menu.nanbin.about"), button -> MinecraftClient.getInstance().setScreen(new NanbinConfigScreen()));
        FilterBuilder.setReservedButton(ItemsGroup.CITY_BUILDING_BLOCKS.creativeModeTab, Text.translatable("Menu.nanbin.about"), button -> MinecraftClient.getInstance().setScreen(new NanbinConfigScreen()));
        //FilterBuilder.setReservedButton(ItemsGroup.USING_STATION_BUILDING_BLOCKS.creativeModeTab, Text.translatable("Menu.nanbin.about"), button -> MinecraftClient.getInstance().setScreen(new NanbinConfigScreen()));
        //FilterBuilder.setReservedButton(ItemsGroup.USING_RAILWAY_BUILD.creativeModeTab, Text.translatable("Menu.nanbin.about"), button -> MinecraftClient.getInstance().setScreen(new NanbinConfigScreen()));
    }
}
