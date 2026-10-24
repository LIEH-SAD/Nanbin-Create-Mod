package com.Nanbin.mapping;

import net.minecraft.block.AbstractBlock;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;

/**
 * 方块 / 物品的 Settings 工厂（1.21.1 版），取代 4.0 由 MTR 包装层代劳的部分。
 * <p>
 * 1.21.1 的 Settings 不需要注册键，也没有 {@code registryKey} /
 * {@code useBlockPrefixedTranslationKey}（这是 1.21.4 才加的），方块物品的翻译键由
 * {@code BlockItem} 自己取方块的翻译键，所以这里原样创建即可。
 * <p>
 * 1.21.4 需要在 Settings 上设置注册键，写法不同，见 {@code common/src/versioned/1.21.4}。
 */
public final class RegistrySettings {

	private RegistrySettings() {
	}

	public static AbstractBlock.Settings createBlock(Identifier identifier) {
		return AbstractBlock.Settings.create();
	}

	public static Item.Settings createItem(Identifier identifier) {
		return new Item.Settings();
	}

	public static Item.Settings createBlockItem(Identifier identifier) {
		return createItem(identifier);
	}
}
