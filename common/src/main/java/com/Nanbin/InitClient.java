package com.Nanbin;

import com.Nanbin.client.ClientData.NanbinClientConfig;
import com.Nanbin.client.CRTRailwaySignEditor.legacy.CRTSignRegistry;
import com.Nanbin.client.Drawing.RouteMapOverride;
import com.Nanbin.client.FiltersGroup.FiltersGroup;
import com.Nanbin.client.FiltersGroup.FiltersMenu;
import com.Nanbin.client.JavaScriptSupport.CRTJsSignLoader;
import com.Nanbin.client.JavaScriptSupport.JSSignConfig;
import com.Nanbin.client.JavaScriptSupport.JSSignLoader;
import com.Nanbin.client.Registry.BlockColor;
import com.Nanbin.client.Registry.BlockEntityRender;
import com.Nanbin.client.Registry.RenderLayerReg;
import com.Nanbin.packet.PacketOpenScreen;
import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.MinecraftClient;
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
        // 自定义导视标志：来源用 CRTSignLoaderMixin 钩 MTR4 的资源重载，MTR3 直接用 CustomResources.registerReloadListener
        ClientinitSteps.put("CRTSignRegistry", CRTSignRegistry::register);
        // JS 指示牌配置：必须在资源重载时加载（客户端初始化阶段 MinecraftClient 尚未创建）
        ClientinitSteps.put("JSSignLoader", JSSignLoader::register);
        // CRT 整牌 JS 样式配置（crt_js_signs_config.json，与上面的旧配置叠加）
        ClientinitSteps.put("CRTJsSignLoader", CRTJsSignLoader::register);
        ClientinitSteps.put("RouteMapOverride", InitClient::initRouteMapOverride);

        runClientInitSteps(ClientinitSteps);

        // 兜底：若首个 tick 时 JS 脚本列表仍为空（资源重载监听器时序/失败），主动补加载一次
        ClientTickEvent.CLIENT_POST.register(client -> ensureJsScriptsLoaded());
    }

    /** JS 脚本配置是否已确认加载。 */
    private static boolean jsScriptsVerified;

    private static void ensureJsScriptsLoaded() {
        if (jsScriptsVerified || MinecraftClient.getInstance() == null) {
            return;
        }
        jsScriptsVerified = true;
        if (JSSignConfig.getAllScripts().isEmpty()) {
            LOGGER.warn("JS sign scripts are still empty after resource reload - reloading now");
            JSSignLoader.reload();
            CRTJsSignLoader.reload();
        }
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
