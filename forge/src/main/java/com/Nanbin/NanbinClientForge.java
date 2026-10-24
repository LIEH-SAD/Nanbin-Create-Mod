package com.Nanbin;

import com.Nanbin.client.Screen.NanbinConfigScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public final class NanbinClientForge {

	private NanbinClientForge() {
	}

	static void init(FMLJavaModLoadingContext context) {
		context.registerExtensionPoint(
				ConfigScreenHandler.ConfigScreenFactory.class,
				() -> new ConfigScreenHandler.ConfigScreenFactory(parentScreen -> new NanbinConfigScreen())
		);
		InitClient.initRendering();
	}

	@Mod.EventBusSubscriber(modid = Init.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
	public static final class ClientEvents {
		@SubscribeEvent
		public static void onClientSetup(final FMLClientSetupEvent event) {
			event.enqueueWork(InitClient::initMenuAndRenderLayers);
		}
	}
}
