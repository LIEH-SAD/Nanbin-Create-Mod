package com.Nanbin.fabric;

import com.Nanbin.client.Screen.NanbinConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * ModMenu 配置界面入口（只有 Fabric 有这个 Mod，NeoForge 侧见 {@code NanbinNeoForgeClient}
 * 注册的 {@code IConfigScreenFactory}）。
 * <p>
 * modmenu 是 {@code modCompileOnly}：没装 ModMenu 时 fabric-loader 不会加载这个类，
 * 所以不会出现 NoClassDefFoundError。
 */
public class ModmenuConfig implements ModMenuApi {

	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return NanbinConfigScreen::new;
	}
}
