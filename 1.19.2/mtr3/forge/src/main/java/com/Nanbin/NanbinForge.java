package com.Nanbin;

import com.Nanbin.client.Screen.NanbinConfigScreen;
import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Init.MOD_ID)
public final class NanbinForge {
    public NanbinForge() {
        EventBuses.registerModEventBus(Init.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        Init.init();
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(parentScreen -> new NanbinConfigScreen(parentScreen))
        );
    }

    @Mod.EventBusSubscriber(modid = Init.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ClientEvents {
        @SubscribeEvent
        public static void onClientSetup(final FMLClientSetupEvent event) {
            event.enqueueWork(InitClient::init);
        }
    }
}