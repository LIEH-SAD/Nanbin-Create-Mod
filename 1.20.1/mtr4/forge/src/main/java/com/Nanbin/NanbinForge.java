package com.Nanbin;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Init.MOD_ID)
public class NanbinForge {
    public final net.minecraftforge.eventbus.api.IEventBus MOD_BUS;
    public NanbinForge(FMLJavaModLoadingContext context) {
        MOD_BUS = context.getModEventBus();
        MOD_BUS.addListener(NanbinClientForge::onClientSetup);
        Init.init();
    }
}
