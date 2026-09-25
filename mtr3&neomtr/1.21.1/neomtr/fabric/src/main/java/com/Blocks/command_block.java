package com.Blocks;

import com.nanbin;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class command_block {
    public static final Block CRT_OLD_WALL1 = new Block(AbstractBlock.Settings.create().hardness(16.0f));
    public static final Block CRT_OLD_WALL2 = new Block(AbstractBlock.Settings.create().hardness(16.0f));
    public static final Block NANBIN_WHITE_BLOCK = new Block(AbstractBlock.Settings.create().hardness(16.0f));
    public static final Block NANBIN_RED_BLOCK = new Block(AbstractBlock.Settings.create().hardness(16.0f));
    public static final Block NANBIN_YELLOW_BLOCK = new Block(AbstractBlock.Settings.create().hardness(16.0f));
    public static final Block NANBIN_GREEN_BLOCK = new Block(AbstractBlock.Settings.create().hardness(16.0f));
    public static final Block NANBIN_BLUE_BLOCK = new Block(AbstractBlock.Settings.create().hardness(16.0f));
    public static final Block NANBIN_PURPLE_BLOCK = new Block(AbstractBlock.Settings.create().hardness(16.0f));
    public static final Block NANBIN_PINK_BLOCK = new Block(AbstractBlock.Settings.create().hardness(16.0f));
    public static final Block LIGHT_RED_BLOCK = new Block(AbstractBlock.Settings.create().hardness(16.0f));
    public static final Block LIGHT_YELLOW_BLOCK = new Block(AbstractBlock.Settings.create().hardness(16.0f));
    public static final Block LIGHT_GREEN_BLOCK = new Block(AbstractBlock.Settings.create().hardness(16.0f));
    public static final Block LIGHT_BLUE_BLOCK = new Block(AbstractBlock.Settings.create().hardness(16.0f));
    public static final Block LIGHT_PURPLE_BLOCK = new Block(AbstractBlock.Settings.create().hardness(16.0f));
    public static final Block LIGHT_PINK_BLOCK = new Block(AbstractBlock.Settings.create().hardness(16.0f));
    public static final Block CRT_RED_WALL_BLOCK = new Block(AbstractBlock.Settings.create().hardness(16.0f));
    public static final Block CRT_YELLOW_WALL_BLOCK = new Block(AbstractBlock.Settings.create().hardness(16.0f));
    public static final Block CRT_GREEN_WALL_BLOCK = new Block(AbstractBlock.Settings.create().hardness(16.0f));
    public static final Block CRT_BLUE_WALL_BLOCK = new Block(AbstractBlock.Settings.create().hardness(16.0f));
    public static final Block CRT_PURPLE_WALL_BLOCK = new Block(AbstractBlock.Settings.create().hardness(16.0f));
    public static final Block CRT_PINK_WALL_BLOCK = new Block(AbstractBlock.Settings.create().hardness(16.0f));
    public static final Block CRT_GRADIENT_RED_WALL_BLOCK = new Block(AbstractBlock.Settings.create().hardness(16.0f));
    public static final Block CRT_GRADIENT_YELLOW_WALL_BLOCK = new Block(AbstractBlock.Settings.create().hardness(16.0f));
    public static final Block CRT_GRADIENT_GREEN_WALL_BLOCK = new Block(AbstractBlock.Settings.create().hardness(16.0f));
    public static final Block CRT_GRADIENT_BLUE_WALL_BLOCK = new Block(AbstractBlock.Settings.create().hardness(16.0f));
    public static final Block CRT_GRADIENT_PURPLE_WALL_BLOCK = new Block(AbstractBlock.Settings.create().hardness(16.0f));
    public static final Block CRT_GRADIENT_PINK_WALL_BLOCK = new Block(AbstractBlock.Settings.create().hardness(16.0f));
    public static final Block BLACK_MARBLE = new Block(AbstractBlock.Settings.create().hardness(16.0f));
    public static final Block WHITE_MARBLE = new Block(AbstractBlock.Settings.create().hardness(16.0f));
    public static final Block CEMENT = new Block(AbstractBlock.Settings.create().hardness(16.0f));


    private static <T extends Block> T register(String path, T block) {
        // 统一调整为 先Block后Item 顺序
        Registry.register(Registries.BLOCK, Identifier.of("nanbin", "crt_old_wall_1"), CRT_OLD_WALL1);
        Registry.register(Registries.ITEM, Identifier.of("nanbin", "crt_old_wall_1"), new BlockItem(CRT_OLD_WALL1, new Item.Settings()));

        Registry.register(Registries.BLOCK, Identifier.of("nanbin", "crt_old_wall_2"), CRT_OLD_WALL2);
        Registry.register(Registries.ITEM, Identifier.of("nanbin", "crt_old_wall_2"), new BlockItem(CRT_OLD_WALL2, new Item.Settings()));

        Registry.register(Registries.BLOCK, Identifier.of("nanbin", "white_block"), NANBIN_WHITE_BLOCK);
        Registry.register(Registries.ITEM, Identifier.of("nanbin", "white_block"), new BlockItem(NANBIN_WHITE_BLOCK, new Item.Settings()));

        Registry.register(Registries.BLOCK, Identifier.of("nanbin", "red_block"), NANBIN_RED_BLOCK);
        Registry.register(Registries.ITEM, Identifier.of("nanbin", "red_block"), new BlockItem(NANBIN_RED_BLOCK, new Item.Settings()));

        Registry.register(Registries.BLOCK, Identifier.of("nanbin", "yellow_block"), NANBIN_YELLOW_BLOCK);
        Registry.register(Registries.ITEM, Identifier.of("nanbin", "yellow_block"), new BlockItem(NANBIN_YELLOW_BLOCK, new Item.Settings()));

        Registry.register(Registries.BLOCK, Identifier.of("nanbin", "green_block"), NANBIN_GREEN_BLOCK);
        Registry.register(Registries.ITEM, Identifier.of("nanbin", "green_block"), new BlockItem(NANBIN_GREEN_BLOCK, new Item.Settings()));

        Registry.register(Registries.BLOCK, Identifier.of("nanbin", "blue_block"), NANBIN_BLUE_BLOCK);
        Registry.register(Registries.ITEM, Identifier.of("nanbin", "blue_block"), new BlockItem(NANBIN_BLUE_BLOCK, new Item.Settings()));

        Registry.register(Registries.BLOCK, Identifier.of("nanbin", "purple_block"), NANBIN_PURPLE_BLOCK);
        Registry.register(Registries.ITEM, Identifier.of("nanbin", "purple_block"), new BlockItem(NANBIN_PURPLE_BLOCK, new Item.Settings()));

        Registry.register(Registries.BLOCK, Identifier.of("nanbin", "pink_block"), NANBIN_PINK_BLOCK);
        Registry.register(Registries.ITEM, Identifier.of("nanbin", "pink_block"), new BlockItem(NANBIN_PINK_BLOCK, new Item.Settings()));

        Registry.register(Registries.BLOCK, Identifier.of("nanbin", "light_red_block"), LIGHT_RED_BLOCK);
        Registry.register(Registries.ITEM, Identifier.of("nanbin", "light_red_block"), new BlockItem(LIGHT_RED_BLOCK, new Item.Settings()));

        Registry.register(Registries.BLOCK, Identifier.of("nanbin", "light_yellow_block"), LIGHT_YELLOW_BLOCK);
        Registry.register(Registries.ITEM, Identifier.of("nanbin", "light_yellow_block"), new BlockItem(LIGHT_YELLOW_BLOCK, new Item.Settings()));

        Registry.register(Registries.BLOCK, Identifier.of("nanbin", "light_green_block"), LIGHT_GREEN_BLOCK);
        Registry.register(Registries.ITEM, Identifier.of("nanbin", "light_green_block"), new BlockItem(LIGHT_GREEN_BLOCK, new Item.Settings()));

        Registry.register(Registries.BLOCK, Identifier.of("nanbin", "light_blue_block"), LIGHT_BLUE_BLOCK);
        Registry.register(Registries.ITEM, Identifier.of("nanbin", "light_blue_block"), new BlockItem(LIGHT_BLUE_BLOCK, new Item.Settings()));

        Registry.register(Registries.BLOCK, Identifier.of("nanbin", "light_purple_block"), LIGHT_PURPLE_BLOCK);
        Registry.register(Registries.ITEM, Identifier.of("nanbin", "light_purple_block"), new BlockItem(LIGHT_PURPLE_BLOCK, new Item.Settings()));

        Registry.register(Registries.BLOCK, Identifier.of("nanbin", "light_pink_block"), LIGHT_PINK_BLOCK);
        Registry.register(Registries.ITEM, Identifier.of("nanbin", "light_pink_block"), new BlockItem(LIGHT_PINK_BLOCK, new Item.Settings()));

        Registry.register(Registries.BLOCK, Identifier.of("nanbin", "crt_red_wall_block"), CRT_RED_WALL_BLOCK);
        Registry.register(Registries.ITEM, Identifier.of("nanbin", "crt_red_wall_block"), new BlockItem(CRT_RED_WALL_BLOCK, new Item.Settings()));

        Registry.register(Registries.BLOCK, Identifier.of("nanbin", "crt_yellow_wall_block"), CRT_YELLOW_WALL_BLOCK);
        Registry.register(Registries.ITEM, Identifier.of("nanbin", "crt_yellow_wall_block"), new BlockItem(CRT_YELLOW_WALL_BLOCK, new Item.Settings()));

        Registry.register(Registries.BLOCK, Identifier.of("nanbin", "crt_green_wall_block"), CRT_GREEN_WALL_BLOCK);
        Registry.register(Registries.ITEM, Identifier.of("nanbin", "crt_green_wall_block"), new BlockItem(CRT_GREEN_WALL_BLOCK, new Item.Settings()));

        Registry.register(Registries.BLOCK, Identifier.of("nanbin", "crt_blue_wall_block"), CRT_BLUE_WALL_BLOCK);
        Registry.register(Registries.ITEM, Identifier.of("nanbin", "crt_blue_wall_block"), new BlockItem(CRT_BLUE_WALL_BLOCK, new Item.Settings()));

        Registry.register(Registries.BLOCK, Identifier.of("nanbin", "crt_purple_wall_block"), CRT_PURPLE_WALL_BLOCK);
        Registry.register(Registries.ITEM, Identifier.of("nanbin", "crt_purple_wall_block"), new BlockItem(CRT_PURPLE_WALL_BLOCK, new Item.Settings()));

        Registry.register(Registries.BLOCK, Identifier.of("nanbin", "crt_pink_wall_block"), CRT_PINK_WALL_BLOCK);
        Registry.register(Registries.ITEM, Identifier.of("nanbin", "crt_pink_wall_block"), new BlockItem(CRT_PINK_WALL_BLOCK, new Item.Settings()));

        Registry.register(Registries.BLOCK, Identifier.of("nanbin", "crt_gradient_red_wall_block"), CRT_GRADIENT_RED_WALL_BLOCK);
        Registry.register(Registries.ITEM, Identifier.of("nanbin", "crt_gradient_red_wall_block"), new BlockItem(CRT_GRADIENT_RED_WALL_BLOCK, new Item.Settings()));

        Registry.register(Registries.BLOCK, Identifier.of("nanbin", "crt_gradient_yellow_wall_block"), CRT_GRADIENT_YELLOW_WALL_BLOCK);
        Registry.register(Registries.ITEM, Identifier.of("nanbin", "crt_gradient_yellow_wall_block"), new BlockItem(CRT_GRADIENT_YELLOW_WALL_BLOCK, new Item.Settings()));

        Registry.register(Registries.BLOCK, Identifier.of("nanbin", "crt_gradient_green_wall_block"), CRT_GRADIENT_GREEN_WALL_BLOCK);
        Registry.register(Registries.ITEM, Identifier.of("nanbin", "crt_gradient_green_wall_block"), new BlockItem(CRT_GRADIENT_GREEN_WALL_BLOCK, new Item.Settings()));

        Registry.register(Registries.BLOCK, Identifier.of("nanbin", "crt_gradient_blue_wall_block"), CRT_GRADIENT_BLUE_WALL_BLOCK);
        Registry.register(Registries.ITEM, Identifier.of("nanbin", "crt_gradient_blue_wall_block"), new BlockItem(CRT_GRADIENT_BLUE_WALL_BLOCK, new Item.Settings()));

        Registry.register(Registries.BLOCK, Identifier.of("nanbin", "crt_gradient_purple_wall_block"), CRT_GRADIENT_PURPLE_WALL_BLOCK);
        Registry.register(Registries.ITEM, Identifier.of("nanbin", "crt_gradient_purple_wall_block"), new BlockItem(CRT_GRADIENT_PURPLE_WALL_BLOCK, new Item.Settings()));

        Registry.register(Registries.BLOCK, Identifier.of("nanbin", "crt_gradient_pink_wall_block"), CRT_GRADIENT_PINK_WALL_BLOCK);
        Registry.register(Registries.ITEM, Identifier.of("nanbin", "crt_gradient_pink_wall_block"), new BlockItem(CRT_GRADIENT_PINK_WALL_BLOCK, new Item.Settings()));

        Registry.register(Registries.BLOCK, Identifier.of("nanbin", "black_marble"), BLACK_MARBLE);
        Registry.register(Registries.ITEM, Identifier.of("nanbin", "black_marble"), new BlockItem(BLACK_MARBLE, new Item.Settings()));

        Registry.register(Registries.BLOCK, Identifier.of("nanbin", "white_marble"), WHITE_MARBLE);
        Registry.register(Registries.ITEM, Identifier.of("nanbin", "white_marble"), new BlockItem(WHITE_MARBLE, new Item.Settings()));

        Registry.register(Registries.BLOCK, Identifier.of("nanbin", "cement"), CEMENT);
        Registry.register(Registries.ITEM, Identifier.of("nanbin", "cement"), new BlockItem(CEMENT, new Item.Settings()));

        return block;
    }

    public static void initialize() {
        register("dummy", CRT_OLD_WALL1);
        // 初始化方法（空实现，注册逻辑在类加载时执行）}
    }
}