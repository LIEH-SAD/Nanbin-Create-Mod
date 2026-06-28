package com.Nanbin;

import com.Nanbin.client.ItemsGroup.FiltersGroup;
import com.Nanbin.client.ItemsGroup.FiltersMenu;
import com.Nanbin.client.Registry.RenderLayerReg;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;

public final class InitClient {
    public static final Logger LOGGER = LogManager.getLogger("Nanbin Create Mod");

    public static void init() {
        long startTime = System.currentTimeMillis();
        Map<String, Runnable> ClientinitSteps = new LinkedHashMap<>();

        ClientinitSteps.put("FiltersGroup", FiltersGroup::init);
        ClientinitSteps.put("FiltersMenu", FiltersMenu::init);
        ClientinitSteps.put("RenderLayer", RenderLayerReg::init);

        int currentStep = 1;

        for (Map.Entry<String, Runnable> step : ClientinitSteps.entrySet()) {
            LOGGER.info("Nanbin Create Mod is registering , Please wait... {} ({}/{})", step.getKey(), currentStep, ClientinitSteps.size());
            step.getValue().run();
            currentStep++;
        }
    }
}