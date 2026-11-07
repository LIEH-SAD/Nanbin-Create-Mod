package com.Nanbin.mapping;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import ziyue.filters.Filter;

import java.util.function.Supplier;

/**
 * MTR 4.0 {@code com.Nanbin.mapping.FilterBuilder} 的 Architectury/1.20.1 版本。
 *
 * <p>来源把 MTR 4 的 {@code CreativeModeTabHolder} 和 {@code MutableText} holder 拆包后转交
 * {@code ziyue.filters.FilterBuilder}；目标侧物品栏是 Architectury 的
 * {@link RegistrySupplier}&lt;{@link ItemGroup}&gt;，文本就是原版 {@link MutableText}，
 * 因此这里只差一层 {@code RegistrySupplier.get()} 拆包。</p>
 */
public interface FilterBuilder {

	static Filter registerFilter(RegistrySupplier<ItemGroup> creativeModeTab, MutableText filterName, Supplier<ItemStack> filterIcon) {
		return ziyue.filters.FilterBuilder.registerFilter(creativeModeTab.get(), filterName, filterIcon);
	}

	static void setReservedButton(RegistrySupplier<ItemGroup> creativeModeTab, MutableText tooltip, ButtonWidget.PressAction onPress) {
		ziyue.filters.FilterBuilder.setReservedButton(creativeModeTab.get(), tooltip, onPress);
	}
}
