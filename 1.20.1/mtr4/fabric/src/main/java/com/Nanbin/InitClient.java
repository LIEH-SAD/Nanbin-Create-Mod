package com.Nanbin;

import com.Nanbin.client.FiltersGroup.FiltersGroup;
import com.Nanbin.client.FiltersGroup.FiltersMenu;
import com.Nanbin.client.Registry.BlockColor;
import com.Nanbin.client.Registry.BlockEntityRender;
import com.Nanbin.client.Registry.RenderLayerReg;
import com.Nanbin.mappingFabric.Registry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;

public final class InitClient {
    public static final Logger LOGGER = LogManager.getLogger("Nanbin Create Mod");

    public static void init() {
        // Initialize MTR client-side packet registry (for C2S packet sending)
        Registry.setupClientPackets("packet");
        Registry.initClient();

        long startTime = System.currentTimeMillis();
        Map<String, Runnable> ClientinitSteps = new LinkedHashMap<>();

        ClientinitSteps.put("BlockEntityRender", BlockEntityRender::init);
        ClientinitSteps.put("FiltersGroup", FiltersGroup::init);
        ClientinitSteps.put("FiltersMenu", FiltersMenu::init);
        ClientinitSteps.put("BlockColor", BlockColor::init);
        ClientinitSteps.put("RenderLayer", RenderLayerReg::init);

        int currentStep = 1;

        for (Map.Entry<String, Runnable> step : ClientinitSteps.entrySet()) {
            LOGGER.info("Nanbin Create Mod is registering , Please wait... {} ({}/{})", step.getKey(), currentStep, ClientinitSteps.size());
            step.getValue().run();
            currentStep++;
        }
    }
}