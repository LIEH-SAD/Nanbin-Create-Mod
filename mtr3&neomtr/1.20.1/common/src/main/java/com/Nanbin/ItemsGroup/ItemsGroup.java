package com.Nanbin.ItemsGroup;

import com.Nanbin.Init;
import com.Nanbin.Items.Items;
import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;

public class ItemsGroup {
    public static final DeferredRegister<ItemGroup> ITEM_GROUPS = DeferredRegister.create(Init.MOD_ID, RegistryKeys.ITEM_GROUP);

    public static final RegistrySupplier<ItemGroup> CITY_BUILDING_BLOCKS = ITEM_GROUPS.register("city_building_blocks",
            () -> CreativeTabRegistry.create(Text.translatable("itemGroup.nanbin.city_building_blocks"), () -> new ItemStack(Items.LIGHT_GREEN_BLOCK.get())));

    public static final RegistrySupplier<ItemGroup> USING_STATION_BUILDING_BLOCKS = ITEM_GROUPS.register("using_station_building_blocks",
            () -> CreativeTabRegistry.create(Text.translatable("itemGroup.nanbin.using_station_building_blocks"), () -> new ItemStack(Items.LOGO.get())));

    public static final RegistrySupplier<ItemGroup> USING_RAILWAY_BUILD = ITEM_GROUPS.register("using_railway_build",
            () -> CreativeTabRegistry.create(Text.translatable("itemGroup.nanbin.using_railway_build"), () -> new ItemStack(mtr.Items.RAIL_CONNECTOR_300_ONE_WAY.get())));

    public static final RegistrySupplier<ItemGroup> CRT = ITEM_GROUPS.register("crt_building_blocks",
            () -> CreativeTabRegistry.create(Text.translatable("itemGroup.nanbin.crt_building_blocks"), () -> new ItemStack(Items.CRT_LOGO.get())));

    public static final RegistrySupplier<ItemGroup> NRT = ITEM_GROUPS.register("nrt_building_blocks",
            () -> CreativeTabRegistry.create(Text.translatable("itemGroup.nanbin.nrt_building_blocks"), () -> new ItemStack(Items.NRT_TICKET_1_EXIT.get())));

    public static void init() {
        ITEM_GROUPS.register();
    }
}
