package com.Nanbin.client.FiltersGroup;

import com.Nanbin.ItemsGroup.ItemsGroup;
import com.Nanbin.client.Screen.NanbinConfigScreen;
import mtr.mappings.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import ziyue.filters.FilterBuilder;

public class FiltersMenu {

    public static void init(){
        FilterBuilder.setReservedButton(ItemsGroup.CRT, Text.translatable("Menu.nanbin.about"), button -> MinecraftClient.getInstance().setScreen(new NanbinConfigScreen()));
        //FilterBuilder.setReservedButton(ItemsGroup.NRT, Text.translatable("Menu.nanbin.about"), button -> MinecraftClient.getInstance().openScreen(new Screen(new NanbinConfigScreen())));
        FilterBuilder.setReservedButton(ItemsGroup.CITY_BUILDING_BLOCKS, Text.translatable("Menu.nanbin.about"), button -> MinecraftClient.getInstance().setScreen(new NanbinConfigScreen()));
        //FilterBuilder.setReservedButton(ItemsGroup.USING_STATION_BUILDING_BLOCKS, Text.translatable("Menu.nanbin.about"), button -> MinecraftClient.getInstance().openScreen(new Screen(new NanbinConfigScreen())));
        //FilterBuilder.setReservedButton(ItemsGroup.USING_RAILWAY_BUILD, Text.translatable("Menu.nanbin.about"), button -> MinecraftClient.getInstance().openScreen(new Screen(new NanbinConfigScreen())));
    }
}
