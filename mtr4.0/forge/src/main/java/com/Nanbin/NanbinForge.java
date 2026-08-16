package com.Nanbin;

import com.Nanbin.client.Screen.NanbinConfigScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Init.MOD_ID)
public class NanbinForge {
    public NanbinForge(FMLJavaModLoadingContext context) {
        context.getModEventBus().addListener(NanbinClientForge::onClientSetup);
        Init.init();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> InitClient::initRendering);
        context.registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(parentScreen -> new NanbinConfigScreen())
        );
    }
}
