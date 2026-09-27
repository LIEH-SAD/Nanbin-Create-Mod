package com.Nanbin.client.FiltersGroup;

import com.Nanbin.ItemsGroup.ItemsGroup;
import com.Nanbin.client.Screen.NanbinConfigScreen;
import com.Nanbin.mapping.FilterBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemGroup;
import net.minecraft.text.Text;

/**
 * 往创作标签页右下角挂一个「关于」按钮，点开本项目的配置界面。
 * <p>
 * 4.1 适配：{@code CreativeModeTabHolder} → 原版 {@link ItemGroup}；
 * {@code MinecraftClient.getInstance().openScreen(new Screen(...))} → {@code setScreen(...)}；
 * 原版没有无参界面的问题也不存在（{@link NanbinConfigScreen} 已经有无参构造）。
 */
public class FiltersMenu {

	/**
	 * 往创作标签页右下角挂「关于」按钮。前置条件由 {@link FiltersGroup#isReady()} 保证。
	 */
	public static void init() {
		final ItemGroup crtTab = ItemsGroup.CRT_TAB == null ? null : ItemsGroup.CRT_TAB.tryGet();
		final ItemGroup cityTab = ItemsGroup.CITY_BUILDING_BLOCKS_TAB == null ? null : ItemsGroup.CITY_BUILDING_BLOCKS_TAB.tryGet();
		if (crtTab == null || cityTab == null) {
			return;
		}

		FilterBuilder.setReservedButton(crtTab, Text.translatable("Menu.nanbin.about"), button -> openConfigScreen());
		FilterBuilder.setReservedButton(cityTab, Text.translatable("Menu.nanbin.about"), button -> openConfigScreen());
	}

	private static void openConfigScreen() {
		final MinecraftClient client = MinecraftClient.getInstance();
		if (client != null) {
			client.setScreen(new NanbinConfigScreen());
		}
	}
}
