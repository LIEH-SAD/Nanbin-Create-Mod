package com.Nanbin.client.FiltersGroup;

import com.Nanbin.ItemsGroup.ItemsGroup;
import com.Nanbin.client.Screen.NanbinConfigScreen;
import mtr.mappings.Text;
import net.minecraft.client.MinecraftClient;
import ziyue.filters.FilterBuilder;

public class FiltersMenu {

    public static void init(){
        FilterBuilder.setReservedButton(ItemsGroup.CRT.getKey(), Text.translatable("Menu.nanbin.about"), button -> MinecraftClient.getInstance().setScreen(new NanbinConfigScreen()));
        //FilterBuilder.setReservedButton(ItemsGroup.NRT.getKey(), Text.translatable("Menu.nanbin.about"), button -> MinecraftClient.getInstance().openScreen(new Screen(new NanbinConfigScreen())));
        FilterBuilder.setReservedButton(ItemsGroup.CITY_BUILDING_BLOCKS.getKey(), Text.translatable("Menu.nanbin.about"), button -> MinecraftClient.getInstance().setScreen(new NanbinConfigScreen()));
        //FilterBuilder.setReservedButton(ItemsGroup.USING_STATION_BUILDING_BLOCKS.getKey(), Text.translatable("Menu.nanbin.about"), button -> MinecraftClient.getInstance().openScreen(new Screen(new NanbinConfigScreen())));
        //FilterBuilder.setReservedButton(ItemsGroup.USING_RAILWAY_BUILD.getKey(), Text.translatable("Menu.nanbin.about"), button -> MinecraftClient.getInstance().openScreen(new Screen(new NanbinConfigScreen())));
    }
}
