package com.Nanbin.Registry;

import com.Nanbin.Blocks.Blocks;
import com.Nanbin.Items.Items;
import com.Nanbin.ItemsGroup.ItemsGroup;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.Nanbin.nanbin.LOGGER;

public class Init {
    /*
    这里提供了方块快捷注册、方块物品注册的方法，且此类用于一键完成注册和Logger
    */
    public static final DeferredRegister.Items ITEM_REGISTER = DeferredRegister.createItems(Init.MOD_ID);

    public static final String MOD_ID = "nanbin";

    public static void init(IEventBus bus) {
        //记录开始
        long startTime = System.currentTimeMillis();
        LOGGER.info("Nanbin Create Mod started registry at {}.", startTime);

        //注册开始
        LOGGER.info("Nanbin Create Mod is registering , Please wait... Blocks /{}ms", System.currentTimeMillis());
        Blocks.BLOCKS.register(bus);
        ITEM_REGISTER.register(bus);

        LOGGER.info("Nanbin Create Mod is registering , Please wait... Items /{}ms", System.currentTimeMillis());
        Items.ITEMS.register(bus);

        LOGGER.info("Nanbin Create Mod is registering , Please wait... ItemsGroup /{}ms", System.currentTimeMillis());
        ItemsGroup.CREATIVE_MODE_TABS.register(bus);

        //注册结束
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        LOGGER.info("Nanbin Create Mod has successfully registered in {} ms.", duration);
    }
}