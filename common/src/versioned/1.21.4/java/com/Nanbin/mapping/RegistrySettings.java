package com.Nanbin.mapping;

import net.minecraft.block.AbstractBlock;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

/**
 * 方块 / 物品的 Settings 工厂（1.21.4 版），取代 4.0 由 MTR 包装层代劳的部分。
 * <p>
 * 1.21.4 起原版要求 Settings 必须带上注册键，否则创建 Item/BlockItem 时直接抛
 * {@code NullPointerException: Item id not set}（{@code Item.Settings.getTranslationKey} 里的
 * {@code Objects.requireNonNull(registryKey, "Item id not set")}），方块则是「Block id not set」。
 * <p>
 * 这里必须<b>直接调用</b> {@code registryKey} / {@code useBlockPrefixedTranslationKey}，
 * 不能像以前那样用反射按方法名去找：开发环境跑的是 Yarn 命名的 Minecraft，按名字找得到；
 * 但发布后的 Fabric 环境用的是 intermediary 命名的 Minecraft（{@code registryKey} 在方块那边变成
 * {@code method_63505} 这类名字，字符串不参与重映射），反射必然返回 null，
 * Settings 因此拿不到注册键，游戏一进 {@code Blocks} 的静态初始化就崩「Block id not set」。
 * 直接把方法写进字节码，remap 时会跟着一起改名，开发与发布两端都正确。
 * <p>
 * 1.21.1 没有这套 API，对应实现见 {@code common/src/versioned/1.21.1}。
 */
public final class RegistrySettings {

	private RegistrySettings() {
	}

	public static AbstractBlock.Settings createBlock(Identifier identifier) {
		return AbstractBlock.Settings.create().registryKey(RegistryKey.of(RegistryKeys.BLOCK, identifier));
	}

	public static Item.Settings createItem(Identifier identifier) {
		return new Item.Settings().registryKey(RegistryKey.of(RegistryKeys.ITEM, identifier));
	}

	/**
	 * 方块物品用的 Settings。
	 * <p>
	 * 1.21.4 起物品的翻译键默认加 {@code item.} 前缀，而方块物品在 lang 里一直是
	 * {@code block.<命名空间>.<路径>}，所以要显式调这个方法把它改回方块前缀，
	 * 才能不改 lang 文件就恢复翻译。1.21.1 的 {@code BlockItem} 自己就用方块的翻译键，不需要。
	 */
	public static Item.Settings createBlockItem(Identifier identifier) {
		return createItem(identifier).useBlockPrefixedTranslationKey();
	}
}
