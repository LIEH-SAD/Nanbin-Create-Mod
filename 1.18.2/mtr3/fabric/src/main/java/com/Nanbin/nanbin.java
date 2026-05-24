package com.Nanbin;

import com.Nanbin.Blocks.Blocks;
import com.Nanbin.Items.Items;
import com.Nanbin.ItemsGroup.ItemsGroup;
import com.Nanbin.Registry.NanbinSoundEvents;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class nanbin implements ModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("Nanbin Create Mod");

    @Override
    public void onInitialize() {
        LOGGER.info("[Nanbin Create Mod] Loading...");

        Blocks.onInitialize();
        Items.onInitialize();
        ItemsGroup.onInitialize();
        NanbinSoundEvents.onInitialize();


        //最终报告
        LOGGER.info("[Nanbin Create Mod] All tasks had been finished!");
    }
}

