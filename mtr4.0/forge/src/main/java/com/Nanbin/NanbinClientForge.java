package com.Nanbin;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = Init.MOD_ID)
public class NanbinClientForge {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // Block colors and block entity renderers were already queued at mod construction
        // time (InitClient.initRendering), so only register the steps that need blocks,
        // which are guaranteed to be registered by now.
        InitClient.initMenuAndRenderLayers();
    }
}
