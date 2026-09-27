package com.Nanbin.neoforge;

import com.Nanbin.Init;
import com.Nanbin.mapping.PlatformHolder;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(Init.MOD_ID)
public final class NanbinNeoForge {

	public NanbinNeoForge(IEventBus modEventBus) {
		final NeoForgePlatform platform = new NeoForgePlatform();
		PlatformHolder.set(platform);
		Init.init();
		platform.register(modEventBus);
		// NeoForge 的方块实体类型是延迟注册的，构造期还取不到实例，必须等到注册全部完成之后
		// 才能把 CRT 方块补进 MTR 告示牌类型的可接受方块集合（否则 NeoForge 的
		// BlockEntity#validateSupports 会在放置告示牌时直接崩客户端）。
		modEventBus.addListener(net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent.class, event ->
				com.Nanbin.entity.BlockEntityTypes.ensureMtrSignTypesAcceptCrtBlocks());
	}
}
