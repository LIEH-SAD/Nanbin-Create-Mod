package com.Nanbin.fabric;

import com.Nanbin.Init;
import com.Nanbin.mapping.PlatformHolder;
import net.fabricmc.api.ModInitializer;

public class NanbinFabric implements ModInitializer {

	@Override
	public void onInitialize() {
		PlatformHolder.set(new FabricPlatform());
		Init.init();
		// Fabric 的注册表是立即生效的，而且 fabric-loader 会先初始化依赖（含 mtr），
		// 所以这里 MTR 的告示牌类型已经可以取到；把 CRT 方块补进它们的可接受方块集合，
		// 免得 NeoForge 侧因为校验失败崩客户端（Fabric 侧没这个校验，但保持两边一致）。
		try {
			com.Nanbin.entity.BlockEntityTypes.ensureMtrSignTypesAcceptCrtBlocks();
		} catch (Exception e) {
			com.Nanbin.Init.LOGGER.warn("MTR 告示牌类型兼容性修补推迟到首次放置告示牌时执行", e);
		}
	}
}
