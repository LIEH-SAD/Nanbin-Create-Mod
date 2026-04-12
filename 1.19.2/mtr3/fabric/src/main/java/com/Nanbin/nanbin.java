package com.Nanbin;

import com.Nanbin.Blocks.Blocks;
import com.Nanbin.Blocks.PlatformDoors;
import com.Nanbin.Items.Items;
import com.Nanbin.ItemsGroup.ItemsGroup;
import com.Nanbin.Registry.NanbinSoundEvents;
import net.fabricmc.api.ModInitializer;

import static com.mojang.text2speech.Narrator.LOGGER;

public class nanbin implements ModInitializer {

    @Override
    public void onInitialize() {
        LOGGER.info("[Nanbin Create Mod] Loading...");

        Blocks.onInitialize();
        Items.onInitialize();
        ItemsGroup.onInitialize();
        NanbinSoundEvents.onInitialize();

        //一些独特方块
        PlatformDoors.onInitialize();

        //最终报告
        LOGGER.info("[Nanbin Create Mod] All tasks had been finished!");
    }
}

