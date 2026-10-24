package com.Nanbin.mapping;

import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import ziyue.filters.Filter;

import java.util.function.Supplier;

/**
 * 创作标签页筛选（ziyue.filters）的薄适配层。
 * <p>
 * 4.0 用的是 MTR 映射层的 {@code CreativeModeTabHolder}（包着原版标签页）+
 * {@code org.mtr.mapping.holder.MutableText}/{@code ItemStack}；
 * 4.1 没有这层包装，所以这里直接收原版类型：
 * {@code CreativeModeTabHolder.creativeModeTab} → {@link ItemGroup}，
 * {@code MutableText.data} → {@link Text}，{@code ItemStack.data} → {@link ItemStack}。
 * <p>
 * <b>跨加载器说明：</b>filters 的 fabric / neoforge 两个变体暴露的是<b>同一套</b>
 * {@code ziyue.filters.FilterBuilder} 静态方法（含 {@code ResourceKey<CreativeModeTab>} 与
 * {@code CreativeModeTab} 两个重载），只是 MC 类型按各自映射命名。common 只编译一次、
 * 由 Architectury 转换器按 MC 映射改写描述符，因此这里写原版类型即可两端通用。
 */
public interface FilterBuilder {

	/** 注册一个筛选分类。 */
	static Filter registerFilter(ItemGroup itemGroup, Text filterName, Supplier<ItemStack> filterIcon) {
		return ziyue.filters.FilterBuilder.registerFilter(itemGroup, filterName, filterIcon);
	}

	/** 给某个标签页设置右下角的自定义按钮（本项目用来打开配置界面）。 */
	static void setReservedButton(ItemGroup itemGroup, Text tooltip, ButtonWidget.PressAction onPress) {
		ziyue.filters.FilterBuilder.setReservedButton(itemGroup, tooltip, onPress);
	}
}
