package com.Nanbin;

import com.Nanbin.Blocks.Blocks;
import com.Nanbin.Items.Items;
import com.Nanbin.ItemsGroup.ItemsGroup;
import com.Nanbin.Registry.SoundEvents;
import com.Nanbin.entity.BlockEntityTypes;
import com.Nanbin.mapping.Registry;
import com.Nanbin.packet.PacketHandler;
import org.apache.http.config.RegistryBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;

public final class Init {
    public static final String MOD_ID = "nanbin";
    public static final Logger LOGGER = LogManager.getLogger("Nanbin Create Mod");
    public static final Registry REGISTRY = new Registry();
    public static final String VERSION = "3.0";
    public static final String VERSION_DATA = "26805";
    public static final String DESIGNED_MTR_VERSION = "3.2.0";
    public static final String FINAL_VERSION = VERSION + "." + VERSION_DATA + " + MTR" + DESIGNED_MTR_VERSION;

    public static void init() {
        LOGGER.info("Hello Nanbin!");

        PacketHandler.registerServerReceivers();

        long startTime = System.currentTimeMillis();
        Map<String, Runnable> initSteps = new LinkedHashMap<>();

        initSteps.put("ItemsGroup", ItemsGroup::init);
        initSteps.put("SoundEvents", SoundEvents::init);
        initSteps.put("Blocks", Blocks::init);
        initSteps.put("BlockEntityTypes", BlockEntityTypes::init);
        initSteps.put("Items", Items::init);

        int currentStep = 1;
        for (Map.Entry<String, Runnable> step : initSteps.entrySet()) {
            LOGGER.info("Nanbin Create Mod is registering , Please wait... {} ({}/{})", step.getKey(), currentStep, initSteps.size());
            step.getValue().run();
            currentStep++;
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        LOGGER.info("Nanbin Create Mod has successfully registered in {} ms.", duration);
    }
}
