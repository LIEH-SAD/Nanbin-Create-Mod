package com.Nanbin;

import com.Nanbin.client.FiltersGroup.FiltersGroup;
import com.Nanbin.client.FiltersGroup.FiltersMenu;
import com.Nanbin.client.Registry.BlockColor;
import com.Nanbin.client.Registry.BlockEntityRender;
import com.Nanbin.client.Registry.RenderLayerReg;
import com.Nanbin.packet.ClientPacketHelper;
import com.Nanbin.packet.PacketOpenScreen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;

public class InitClient {
    private static final Logger LOGGER = LogManager.getLogger("Nanbin Create Mod");

    public static void init() {
        initRendering();
        initMenuAndRenderLayers();
    }
    public static void initRendering() {
        // 客户端注册 S2C 包接收器

        Map<String, Runnable> ClientinitSteps = new LinkedHashMap<>();

        ClientinitSteps.put("PacketOpenScreen", PacketOpenScreen::registerClientReceivers);
        ClientinitSteps.put("BlockEntityRender", BlockEntityRender::init);
        ClientinitSteps.put("BlockColor", BlockColor::init);

        runClientInitSteps(ClientinitSteps);
    }

    /**
     * Registers creative-tab filters and block render layers.
     * These access blocks directly, so they must run after blocks are registered.
     */
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
