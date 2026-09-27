package com.Nanbin.neoforge;

import com.Nanbin.Init;
import com.Nanbin.InitClient;
import com.Nanbin.mapping.PlatformHolder;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = Init.MOD_ID, dist = Dist.CLIENT)
public final class NanbinNeoForgeClient {

	/**
	 * NeoForge 没有 ModMenu，配置界面的入口是官方的 {@code IConfigScreenFactory}：
	 * 注册之后「模组列表 → 本模组 → Config」按钮就会自动出现。
	 * <p>
	 * 只能放在客户端专属类里 —— {@code IConfigScreenFactory} 引用原版客户端类，
	 * 放到通用/服务端类里会让专用服务器启动时崩。
	 */
	public NanbinNeoForgeClient(net.neoforged.fml.ModContainer modContainer) {
		PlatformHolder.set(new NeoForgePlatform());
		InitClient.init();
		// 每 tick 驱动一次「不能在构造期做」的客户端初始化（filters 集成），成功一次即停
		NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> InitClient.onClientTick());
		modContainer.registerExtensionPoint(net.neoforged.neoforge.client.gui.IConfigScreenFactory.class,
				(container, parent) -> new com.Nanbin.client.Screen.NanbinConfigScreen(parent));
	}
}
