package com.Nanbin.Client;

import com.Nanbin.Client.Registry.FiltersGroup;
import com.Nanbin.Client.Registry.RenderLayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

import static com.Nanbin.Registry.Init.MOD_ID;

@EventBusSubscriber(value = Dist.CLIENT, modid = MOD_ID)
public class nanbinClient {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
       RenderLayer.init();
       FiltersGroup.init();
    }
}