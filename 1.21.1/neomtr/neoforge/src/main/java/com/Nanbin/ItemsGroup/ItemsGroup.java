package com.Nanbin.ItemsGroup;

import com.Nanbin.Blocks.Blocks;
import mtr.Items;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

import static com.Nanbin.Registry.Init.MOD_ID;

public class ItemsGroup {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    public static final Supplier<CreativeModeTab> CRT =
            CREATIVE_MODE_TABS.register("crt", () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(Blocks.CRT_TICKET_MACHINE_1.get()))
                    .title(Component.translatable("itemGroup.nanbin.crt_building_blocks"))
                    .displayItems((displayParameters, output) -> {
                        //象征性注册1-2个得了
                        output.accept(new ItemStack(Blocks.CRT_PLATFORM.get()));
                    })
                    .build());

    public static final Supplier<CreativeModeTab> CITY_BUILDING_BLOCKS =
            CREATIVE_MODE_TABS.register("city_building_blocks", () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(Blocks.LIGHT_GREEN_BLOCK.get()))
                    .title(Component.translatable("itemGroup.nanbin.city_building_blocks"))
                    .displayItems((displayParameters, output) -> {
                        output.accept(new ItemStack(Blocks.NANBIN_WHITE_BLOCK.get()));
                    })
                    .build());

    public static final Supplier<CreativeModeTab> USING_STATION_BUILDING_BLOCKS =
            CREATIVE_MODE_TABS.register("using_station_building_blocks", () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(Blocks.CRT_TICKET_MACHINE_1.get()))
                    .title(Component.translatable("itemGroup.nanbin.using_station_building_blocks"))
                    .displayItems((displayParameters, output) -> {
                    })
                    .build());

    public static final Supplier<CreativeModeTab> USING_RAILWAY_BUILD =
            CREATIVE_MODE_TABS.register("using_railway_build", () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(Items.RAIL_CONNECTOR_300_ONE_WAY.get()))
                    .title(Component.translatable("itemGroup.nanbin.using_railway_build"))
                    .displayItems((displayParameters, output) -> {
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}