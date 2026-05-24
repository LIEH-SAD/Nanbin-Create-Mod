package com.Items;

import net.minecraft.component.type.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

// 催吐剂物品注册类
public class Emetic {
    // 私有化构造方法，防止类被实例化
    private Emetic() {}

    // 定义催吐剂物品：设置食物属性（可食用、饱和度0、始终可食用），并指定物品组
    public static final Item EMETIC = new Item(
            new Item.Settings()
                    .food((new FoodComponent.Builder())
                            .saturationModifier(0.0F)  // 饱和度0
                            .alwaysEdible()             // 始终可食用（无视饥饿值）
                            .build())
    );

    /**
     * 通用物品注册方法（私有化，仅内部使用）
     * @param path 物品注册的路径（ID）
     * @param item 要注册的物品实例
     * @return 注册后的物品实例
     * @param <T> 物品类型
     */
    private static <T extends Item> T register(String path, T item) {
        // 使用传入的path和item参数注册，而非硬编码
        Identifier itemId = Identifier.of("nanbin", path);
        return Registry.register(Registries.ITEM, itemId, item);
    }

    /**
     * 初始化方法：注册所有物品
     * 需在Mod主类的onInitialize()中调用此方法
     */
    public static void initialize() {
        // 注册催吐剂物品，注册ID与物品名称匹配（emetic）
        register("emetic", EMETIC);
    }
}