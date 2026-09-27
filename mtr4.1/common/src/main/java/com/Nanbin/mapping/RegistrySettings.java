package com.Nanbin.mapping;

import net.minecraft.block.AbstractBlock;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.lang.reflect.Method;

/**
 * 方块 / 物品的 Settings 工厂（取代 4.0 由 MTR 包装层代劳的部分）。
 * <p>
 * 1.21.4 起原版要求 Settings 必须带上注册键，否则创建 Item/BlockItem 时直接抛
 * {@code NullPointerException: Item id not set}（{@code Item.Settings.getTranslationKey} 里
 * {@code Objects.requireNonNull(registryKey, "Item id not set")}），方块则是「Block id not set」。
 * 1.21.1 没有这个要求、也没有这个方法，所以这里在类初始化时解析一次，
 * 有就设置、没有就跳过 —— 同一份源码两个版本都能用。
 */
public final class RegistrySettings {

	private static final Method BLOCK_REGISTRY_KEY_METHOD = findMethod(AbstractBlock.Settings.class, "registryKey", RegistryKey.class);
	private static final Method ITEM_REGISTRY_KEY_METHOD = findMethod(Item.Settings.class, "registryKey", RegistryKey.class);

	/**
	 * 1.21.4 起物品的翻译键默认加 {@code item.} 前缀（{@code Item.Settings} 里带
	 * {@code ITEM_PREFIXED_TRANSLATION_KEY}），而方块物品在 lang 里一直是 {@code block.<命名空间>.<路径>}，
	 * 所以要显式调这个方法把它改回方块前缀，才能不改 lang 文件就恢复翻译。
	 * 1.21.1 的 {@code BlockItem} 自己就返回方块的翻译键，没有这个方法，跳过即可。
	 */
	private static final Method ITEM_BLOCK_PREFIXED_KEY_METHOD = findMethod(Item.Settings.class, "useBlockPrefixedTranslationKey");

	private RegistrySettings() {
	}

	private static Method findMethod(Class<?> settingsClass, String name, Class<?>... parameterTypes) {
		try {
			return settingsClass.getMethod(name, parameterTypes);
		} catch (NoSuchMethodException e) {
			// 1.21.1 及更早版本没有该方法
			return null;
		}
	}

	public static AbstractBlock.Settings createBlock(Identifier identifier) {
		final AbstractBlock.Settings settings = AbstractBlock.Settings.create();
		apply(BLOCK_REGISTRY_KEY_METHOD, settings, RegistryKey.of(RegistryKeys.BLOCK, identifier));
		return settings;
	}

	public static Item.Settings createItem(Identifier identifier) {
		final Item.Settings settings = new Item.Settings();
		apply(ITEM_REGISTRY_KEY_METHOD, settings, RegistryKey.of(RegistryKeys.ITEM, identifier));
		return settings;
	}

	/** 方块物品用的 Settings：翻译键固定落在 {@code block.<命名空间>.<路径>} 上。 */
	public static Item.Settings createBlockItem(Identifier identifier) {
		final Item.Settings settings = createItem(identifier);
		apply(ITEM_BLOCK_PREFIXED_KEY_METHOD, settings);
		return settings;
	}

	private static void apply(Method method, Object settings, Object... arguments) {
		if (method == null) {
			return;
		}
		try {
			method.invoke(settings, arguments);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("无法设置 Settings 的注册键或翻译键: " + settings, e);
		}
	}
}
