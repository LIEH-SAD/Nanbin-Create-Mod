package com.Nanbin.client.FiltersGroup;

import com.Nanbin.ItemsGroup.ItemsGroup;
import com.Nanbin.client.Screen.NanbinConfigScreen;
import com.Nanbin.mappingFabric.FilterBuilder;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.Screen;
import org.mtr.mapping.mapper.TextHelper;

public class FiltersMenu {

    public static void init(){
        FilterBuilder.setReservedButton(ItemsGroup.CRT, TextHelper.translatable("Menu.nanbin.about"), button -> MinecraftClient.getInstance().openScreen(new Screen(new NanbinConfigScreen())));
        //FilterBuilder.setReservedButton(ItemsGroup.NRT, TextHelper.translatable("Menu.nanbin.about"), button -> MinecraftClient.getInstance().openScreen(new Screen(new NanbinConfigScreen())));
        FilterBuilder.setReservedButton(ItemsGroup.CITY_BUILDING_BLOCKS, TextHelper.translatable("Menu.nanbin.about"), button -> MinecraftClient.getInstance().openScreen(new Screen(new NanbinConfigScreen())));
        //FilterBuilder.setReservedButton(ItemsGroup.USING_STATION_BUILDING_BLOCKS, TextHelper.translatable("Menu.nanbin.about"), button -> MinecraftClient.getInstance().openScreen(new Screen(new NanbinConfigScreen())));
        //FilterBuilder.setReservedButton(ItemsGroup.USING_RAILWAY_BUILD, TextHelper.translatable("Menu.nanbin.about"), button -> MinecraftClient.getInstance().openScreen(new Screen(new NanbinConfigScreen())));
    }
}
