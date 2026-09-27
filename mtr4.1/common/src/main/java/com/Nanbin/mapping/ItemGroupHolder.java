package com.Nanbin.mapping;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 创作模式标签页的抽象。
 * <p>
 * 内容（要放进哪个标签页）在 common 里声明；真正的标签页注册交给平台层，
 * 因为 Fabric 用 fabric-api 的 {@code FabricItemGroup}，NeoForge 用 {@code DeferredRegister}。
 */
public final class ItemGroupHolder {

	private final String path;
	private final Supplier<ItemStack> icon;
	private final List<ObjectHolder<? extends Item>> entries = new ArrayList<>();

	public ItemGroupHolder(String path, Supplier<ItemStack> icon) {
		this.path = path;
		this.icon = icon;
	}

	public String getPath() {
		return path;
	}

	/** 标签页图标，惰性求值（标签页定义早于方块注册）。 */
	public ItemStack getIcon() {
		return icon.get();
	}

	public void addItem(ObjectHolder<? extends Item> item) {
		entries.add(item);
	}

	public List<ObjectHolder<? extends Item>> getEntries() {
		return entries;
	}
}
