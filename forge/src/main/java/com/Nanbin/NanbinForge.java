package com.Nanbin;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Init.MOD_ID)
public class NanbinForge {
	public NanbinForge(FMLJavaModLoadingContext context) {
		Init.init();
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> NanbinClientForge.init(context));
	}
}
