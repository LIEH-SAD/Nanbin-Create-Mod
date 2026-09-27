package com.Nanbin.fabric;

import com.Nanbin.Init;
import com.Nanbin.InitClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;

public class NanbinClientFabric implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		MtrCustomResourceReloadFix.register();
		InitClient.init();
		// 每 tick 驱动一次「不能在构造期做」的客户端初始化（filters 集成），成功一次即停
		ClientTickEvents.END_CLIENT_TICK.register(client -> InitClient.onClientTick());
	}
}
