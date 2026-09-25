package com.Blocks;

import com.nanbin;
import mtr.block.BlockPlatform;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class crt_platform {
    public static final Block CRT_PLATFORM = register("crt_platform", new BlockPlatform(AbstractBlock.Settings.create().requiresTool().strength(2.0F).nonOpaque(), true));

    private static <T extends Block> T register(String path, T block) {
        // 注册方块
        Registry.register(Registries.BLOCK, Identifier.of("nanbin", path), block);
        // 注册方块物品
        Registry.register(Registries.ITEM,  Identifier.of("nanbin", path),
                new BlockItem(block, new Item.Settings()));
        return block;
    }

    public static void initialize() {
        // 初始化方法（空实现，注册逻辑在类加载时执行）
    }
}
