package com.Nanbin;

import com.Nanbin.client.ClientData.NanbinClientConfig;
import com.Nanbin.client.Drawing.NanbinSignLoader;
import com.Nanbin.client.Drawing.RouteMapOverride;
import com.Nanbin.client.FiltersGroup.FiltersGroup;
import com.Nanbin.client.FiltersGroup.FiltersMenu;
import com.Nanbin.client.Registry.BlockColor;
import com.Nanbin.client.Registry.BlockEntityRender;
import com.Nanbin.client.Registry.RenderLayerReg;
import com.Nanbin.packet.PacketOpenScreen;
import dev.architectury.event.events.client.ClientTickEvent;
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
        ClientinitSteps.put("NanbinSignLoader", NanbinSignLoader::register);
        ClientinitSteps.put("RouteMapOverride", InitClient::initRouteMapOverride);

        runClientInitSteps(ClientinitSteps);
    }

    /** 每次启动游戏后按配置拉取线路图并覆盖；同时挂上每 tick 的自动恢复。 */
    private static void initRouteMapOverride() {
        RouteMapOverride.apply(NanbinClientConfig.getRouteMapUrl());
        ClientTickEvent.CLIENT_POST.register(client -> RouteMapOverride.tick());
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
