package com.Nanbin;

import com.Nanbin.client.Screen.NanbinConfigScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public final class NanbinClientForge {

	private NanbinClientForge() {
	}

	static void init(FMLJavaModLoadingContext context) {
		// The config screen extension point differs between Forge versions.
		//? if <1.17 {
		/*net.minecraftforge.fml.ModLoadingContext.get().registerExtensionPoint(net.minecraftforge.fml.ExtensionPoint.CONFIGGUIFACTORY, () -> (minecraft, parentScreen) -> new NanbinConfigScreen());
		*///?} else if <1.18.2 {
		/*net.minecraftforge.fml.ModLoadingContext.get().registerExtensionPoint(net.minecraftforge.fmlclient.ConfigGuiHandler.ConfigGuiFactory.class, () -> new net.minecraftforge.fmlclient.ConfigGuiHandler.ConfigGuiFactory((minecraft, parentScreen) -> new NanbinConfigScreen()));
		*///?} else if <1.19.2 {
		/*net.minecraftforge.fml.ModLoadingContext.get().registerExtensionPoint(net.minecraftforge.client.ConfigGuiHandler.ConfigGuiFactory.class, () -> new net.minecraftforge.client.ConfigGuiHandler.ConfigGuiFactory((minecraft, parentScreen) -> new NanbinConfigScreen()));
		*///?} else {
		context.registerExtensionPoint(net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory.class, () -> new net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory(parentScreen -> new NanbinConfigScreen()));
		//?}
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