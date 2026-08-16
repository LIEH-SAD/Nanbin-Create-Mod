package com.Nanbin;

import com.Nanbin.client.FiltersGroup.FiltersGroup;
import com.Nanbin.client.FiltersGroup.FiltersMenu;
import com.Nanbin.client.Registry.BlockColor;
import com.Nanbin.client.Registry.BlockEntityRender;
import com.Nanbin.client.Registry.RenderLayerReg;
import com.Nanbin.mapping.Registry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mtr.mod.data.ArrivalsCacheClient;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.mtr.mod.InitClient.REGISTRY_CLIENT;

public final class InitClient {
    public static final Logger LOGGER = LogManager.getLogger("Nanbin Create Mod");

    public static void init() {
        initRendering();
        initMenuAndRenderLayers();
    }

    public static void initRendering() {
        // Initialize MTR client-side packet registry (for C2S packet sending)
        Registry.setupClientPackets("packet");
        Registry.initClient();

        Map<String, Runnable> ClientinitSteps = new LinkedHashMap<>();

        ClientinitSteps.put("BlockEntityRender", BlockEntityRender::init);
        ClientinitSteps.put("BlockColor", BlockColor::init);

        runClientInitSteps(ClientinitSteps);
    }

    public static void initMenuAndRenderLayers() {
        Map<String, Runnable> ClientinitSteps = new LinkedHashMap<>();

        ClientinitSteps.put("FiltersGroup", FiltersGroup::init);
        ClientinitSteps.put("FiltersMenu", FiltersMenu::init);
        ClientinitSteps.put("RenderLayer", RenderLayerReg::init);

        runClientInitSteps(ClientinitSteps);
    }

    private static void runClientInitSteps(Map<String, Runnable> ClientinitSteps) {
        int currentStep = 1;

        for (Map.Entry<String, Runnable> step : ClientinitSteps.entrySet()) {
            LOGGER.info("Nanbin Create Mod is registering , Please wait... {} ({}/{})", step.getKey(), currentStep, ClientinitSteps.size());
            step.getValue().run();
            currentStep++;
        }
    }
}