package com.Items;

import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

// 遵循Java命名规范，类名首字母大写且使用驼峰命名
public class Track {

    // 私有化构造方法，防止实例化
    private Track() {}

    // 定义垃圾物品（指定物品组，方便在创造模式物品栏找到）
    public static final Item TRASH = new Item(new Item.Settings());

    /**
     * 通用的物品注册方法
     * @param path 物品注册路径（ID）
     * @param item 要注册的物品实例
     * @return 注册后的物品实例
     * @param <T> 物品类型
     */
    private static <T extends Item> T register(String path, T item) {
        // 使用传入的参数进行注册，而不是硬编码
        Identifier id = Identifier.of("nanbin", path);
        return Registry.register(Registries.ITEM, id, item);
    }

    /**
     * 初始化方法 - 在这里注册所有物品
     * 需在mod主类中调用此方法
     */
    public static void initialize() {
        // 注册垃圾物品
        register("trash", TRASH);

        // 如果需要将垃圾设置为燃料（可选）
        FuelRegistry.INSTANCE.add(TRASH, 100); // 100 ticks = 5秒燃烧时间
    }
}