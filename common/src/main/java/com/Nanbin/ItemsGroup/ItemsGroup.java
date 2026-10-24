package com.Nanbin.ItemsGroup;

import com.Nanbin.Init;
import com.Nanbin.Items.Items;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;

public class ItemsGroup {
    public static final DeferredRegister<ItemGroup> ITEM_GROUPS = DeferredRegister.create(Init.MOD_ID, RegistryKeys.ITEM_GROUP);

    public static final RegistrySupplier<ItemGroup> CITY_BUILDING_BLOCKS = ITEM_GROUPS.register("city_building_blocks",
            () -> ItemGroup.create(ItemGroup.Row.TOP, 0)
                    .displayName(Text.translatable("itemGroup.nanbin.city_building_blocks"))
                    .icon(() -> new ItemStack(Items.LIGHT_GREEN_BLOCK.get()))
                    .entries((context, entries) -> {
                        entries.add(Items.BUS_TICKET_PROCESSOR.get());
                        entries.add(Items.BLUEFENCE.get());
                        entries.add(Items.GREENFENCE.get());
                        entries.add(Items.METALFENCE.get());
                        entries.add(Items.NANBIN_WHITE_BLOCK.get());
                        entries.add(Items.NANBIN_RED_BLOCK.get());
                        entries.add(Items.NANBIN_YELLOW_BLOCK.get());
                        entries.add(Items.NANBIN_GREEN_BLOCK.get());
                        entries.add(Items.NANBIN_BLUE_BLOCK.get());
                        entries.add(Items.NANBIN_PURPLE_BLOCK.get());
                        entries.add(Items.NANBIN_PINK_BLOCK.get());
                        entries.add(Items.LIGHT_RED_BLOCK.get());
                        entries.add(Items.LIGHT_YELLOW_BLOCK.get());
                        entries.add(Items.LIGHT_GREEN_BLOCK.get());
                        entries.add(Items.LIGHT_BLUE_BLOCK.get());
                        entries.add(Items.LIGHT_PURPLE_BLOCK.get());
                        entries.add(Items.LIGHT_PINK_BLOCK.get());
                        entries.add(Items.CRT_RED_WALL_BLOCK.get());
                        entries.add(Items.CRT_YELLOW_WALL_BLOCK.get());
                        entries.add(Items.CRT_GREEN_WALL_BLOCK.get());
                        entries.add(Items.CRT_BLUE_WALL_BLOCK.get());
                        entries.add(Items.CRT_PURPLE_WALL_BLOCK.get());
                        entries.add(Items.CRT_PINK_WALL_BLOCK.get());
                        entries.add(Items.CRT_GRADIENT_RED_WALL_BLOCK.get());
                        entries.add(Items.CRT_GRADIENT_YELLOW_WALL_BLOCK.get());
                        entries.add(Items.CRT_GRADIENT_GREEN_WALL_BLOCK.get());
                        entries.add(Items.CRT_GRADIENT_BLUE_WALL_BLOCK.get());
                        entries.add(Items.CRT_GRADIENT_PURPLE_WALL_BLOCK.get());
                        entries.add(Items.CRT_GRADIENT_PINK_WALL_BLOCK.get());
                        entries.add(Items.BLACK_MARBLE.get());
                        entries.add(Items.WHITE_MARBLE.get());
                        entries.add(Items.CEMENT.get());
                        entries.add(Items.TERRAZZO.get());
                        entries.add(Items.BLACK_TERRAZZO.get());
                        entries.add(Items.PAVEMENT_1.get());
                        entries.add(Items.PAVEMENT_1_HALF.get());
                        entries.add(Items.PAVEMENT_2.get());
                        entries.add(Items.PAVEMENT_2_HALF.get());
                        entries.add(Items.PAVEMENT_3.get());
                        entries.add(Items.PAVEMENT_3_HALF.get());
                        entries.add(Items.PAVEMENT_4.get());
                        entries.add(Items.PAVEMENT_4_HALF.get());
                        entries.add(Items.DRAIN_COVER.get());
                        entries.add(Items.GRAY_TACTILE_BAVING.get());
                        entries.add(Items.GRAY_TACTILE_BAVING_HALF.get());
                        entries.add(Items.GRAY_TACTILE_BAVING_CONNECT.get());
                        entries.add(Items.GRAY_TACTILE_BAVING_CONNECT_HALF.get());
                        entries.add(Items.YELLOW_TACTILE_BAVING.get());
                        entries.add(Items.YELLOW_TACTILE_BAVING_HALF.get());
                        entries.add(Items.YELLOW_TACTILE_BAVING_CONNECT.get());
                        entries.add(Items.YELLOW_TACTILE_BAVING_CONNECT_HALF.get());
                        entries.add(Items.ROAD_NAME.get());
                    })
                    .build());

    public static final RegistrySupplier<ItemGroup> USING_STATION_BUILDING_BLOCKS = ITEM_GROUPS.register("using_station_building_blocks",
            () -> ItemGroup.create(ItemGroup.Row.TOP, 0)
                    .displayName(Text.translatable("itemGroup.nanbin.using_station_building_blocks"))
                    .icon(() -> new ItemStack(Items.LOGO.get()))
                    .entries((context, entries) -> {
                        entries.add(Items.LOGO.get());
                        entries.add(Items.CEILING.get());
                        entries.add(Items.CEILING_2.get());
                        entries.add(Items.CEILING_OVERHEAD.get());
                        entries.add(Items.CEILING_OVERHEAD_2.get());
                        entries.add(Items.STATION_COLOR_CEILING.get());
                        entries.add(Items.STATION_COLOR_CEILING_2.get());
                        entries.add(Items.CEILING_LIGHT.get());
                        entries.add(Items.CEILING_LIGHT_2.get());
                        entries.add(Items.PSD_TOP.get());
                        entries.add(Items.ORDINARY_STATION_NAME.get());
                    })
                    .build());

    public static final RegistrySupplier<ItemGroup> USING_RAILWAY_BUILD = ITEM_GROUPS.register("using_railway_build",
            () -> ItemGroup.create(ItemGroup.Row.TOP, 0)
                    .displayName(Text.translatable("itemGroup.nanbin.using_railway_build"))
                    .icon(() -> new ItemStack(mtr.Items.RAIL_CONNECTOR_300_ONE_WAY.get()))
                    .entries((context, entries) -> {
                        entries.add(Items.BEHAVIORALBLOCK.get());
                        entries.add(Items.BEHAVIORALBLOCK_.get());
                        entries.add(Items.SOUNDPROOFNET_TOP.get());
                        entries.add(Items.SOUNDPROOFNET_TOP_2.get());
                        entries.add(Items.SOUNDPROOFNET_GLASS.get());
                        entries.add(Items.SOUNDPROOFNET_GLASS_2.get());
                        entries.add(Items.SOUNDPROOFNET_BASE.get());
                        entries.add(Items.SOUNDPROOFNET.get());
                        entries.add(Items.SOUNDPROOFNET_UP.get());
                    })
                    .build());

    public static final RegistrySupplier<ItemGroup> CRT = ITEM_GROUPS.register("crt_building_blocks",
            () -> ItemGroup.create(ItemGroup.Row.TOP, 0)
                    .displayName(Text.translatable("itemGroup.nanbin.crt_building_blocks"))
                    .icon(() -> new ItemStack(Items.CRT_LOGO.get()))
                    .entries((context, entries) -> {
                        entries.add(Items.CRT_LOGO.get());
                        entries.add(Items.CRT_PLATFORM.get());
                        entries.add(Items.CRT_TICKET_1_ENTER.get());
                        entries.add(Items.CRT_TICKET_1_EXIT.get());
                        entries.add(Items.CRT_TICKET_2_ENTER.get());
                        entries.add(Items.CRT_TICKET_2_EXIT.get());
                        entries.add(Items.CRT_TICKET_3_ENTER.get());
                        entries.add(Items.CRT_TICKET_3_EXIT.get());
                        entries.add(Items.CRT_TICKET_MACHINE_1.get());
                        entries.add(Items.CRT_TICKET_MACHINE_2.get());
                        entries.add(Items.CRT_ESCALATOR.get());
                        entries.add(Items.CRT_STATION_INFO_1.get());
                        entries.add(Items.CRT_STATION_INFO_1_DOUBLE.get());
                        entries.add(Items.CRT_OLD_WALL1.get());
                        entries.add(Items.CRT_OLD_WALL2.get());
                        entries.add(Items.CRT_FENCE1.get());
                        entries.add(Items.CRT_FENCE8.get());
                        entries.add(Items.CRT_FENCE9.get());
                        entries.add(Items.CRT_FENCE10.get());
                        entries.add(Items.CRT_FENCE_TICKET.get());
                        entries.add(Items.CRT_FENCE_TICKET_FLIPPED.get());
                        entries.add(Items.CRT_LIFT_TIPS.get());
                        entries.add(Items.CRT_LIFT_TIPS_2.get());
                        entries.add(Items.CRT_LIFT_TIPS_3.get());
                        entries.add(Items.CRT_PSD_CAB_DOOR.get());
                        entries.add(Items.CRT_APG_CAB_DOOR_OLD.get());
                        entries.add(Items.CRT_APG_CAB_FENCE_OLD.get());
                        entries.add(Items.CRT_APG_CAB_FENCE_OLD_CONNECT.get());
                        entries.add(Items.CRT_APG_DOOR_1.get());
                        entries.add(Items.CRT_APG_GLASS_1.get());
                        entries.add(Items.CRT_APG_GLASS_END_1.get());
                        entries.add(Items.CRT_APG_DOOR_2.get());
                        entries.add(Items.CRT_APG_GLASS_2.get());
                        entries.add(Items.CRT_APG_GLASS_END_2.get());
                        entries.add(Items.CRT_RIGID_CATENARY_1.get());
                        entries.add(Items.CRT_TEMP_FENCE_1.get());
                        entries.add(Items.CRT_FENCE_LIFT_TIPS_1.get());
                        entries.add(Items.CRT_STATION_NAME_1.get());
                        entries.add(Items.CRT_STATION_NAME_2.get());
                        entries.add(Items.CRT_RAILWAY_SIGN_POLE.get());
                        entries.add(Items.CRT_RAILWAY_SIGN_3_EVEN.get());
                        entries.add(Items.CRT_RAILWAY_SIGN_3_ODD.get());
                        entries.add(Items.CRT_RAILWAY_SIGN_4_EVEN.get());
                        entries.add(Items.CRT_RAILWAY_SIGN_4_ODD.get());
                        entries.add(Items.CRT_RAILWAY_SIGN_5_EVEN.get());
                        entries.add(Items.CRT_RAILWAY_SIGN_5_ODD.get());
                        entries.add(Items.CRT_RAILWAY_SIGN_6_EVEN.get());
                        entries.add(Items.CRT_RAILWAY_SIGN_6_ODD.get());
                        entries.add(Items.CRT_RAILWAY_SIGN_7_EVEN.get());
                        entries.add(Items.CRT_RAILWAY_SIGN_7_ODD.get());
                        entries.add(Items.CRT_RAILWAY_SIGN_8_EVEN.get());
                        entries.add(Items.CRT_RAILWAY_SIGN_8_ODD.get());
                        entries.add(Items.CRT_RAILWAY_SIGN_9_EVEN.get());
                        entries.add(Items.CRT_RAILWAY_SIGN_9_ODD.get());
                        entries.add(Items.CRT_RAILWAY_SIGN_10_EVEN.get());
                        entries.add(Items.CRT_RAILWAY_SIGN_10_ODD.get());
                        entries.add(Items.CRT_RAILWAY_SIGN_11_EVEN.get());
                        entries.add(Items.CRT_RAILWAY_SIGN_11_ODD.get());
                    })
                    .build());

    public static final RegistrySupplier<ItemGroup> NRT = ITEM_GROUPS.register("nrt_building_blocks",
            () -> ItemGroup.create(ItemGroup.Row.TOP, 0)
                    .displayName(Text.translatable("itemGroup.nanbin.nrt_building_blocks"))
                    .icon(() -> new ItemStack(Items.NRT_TICKET_1_EXIT.get()))
                    .entries((context, entries) -> {
                        entries.add(Items.NRT_TICKET_1_ENTER.get());
                        entries.add(Items.NRT_TICKET_1_EXIT.get());
                    })
                    .build());

    public static void init() {
        ITEM_GROUPS.register();
    }
}
