package com.Nanbin.client.FiltersGroup;

import com.Nanbin.Items.Items;
import com.Nanbin.ItemsGroup.ItemsGroup;
import mtr.mappings.Text;
import net.minecraft.item.ItemStack;
import ziyue.filters.Filter;
import ziyue.filters.FilterBuilder;

public final class FiltersGroup {
    public static Filter CRT_FENCE;
    public static Filter CRT_BUILDING_Items;
    public static Filter CRT_TICKETS;
    //public static Filter CRT_OVERHEAD_LINES;
    public static Filter CRT_DOOR;
    public static Filter CRT_ESCALATOR;
    public static Filter COMMON_BUILDING_Items;
    public static Filter ROAD_Items;
    public static Filter FENCE_Items;
    public static Filter FUNCTION_Items;

    private FiltersGroup() {}

    public static void init() {
        CRT_FENCE = FilterBuilder.registerFilter(ItemsGroup.CRT.getKey(), Text.translatable("FiltersGroup.nanbin.crt_fence"), () -> new ItemStack(Items.CRT_FENCE1.get()))
                .addItems(Items.CRT_FENCE1.get())
                .addItems(Items.CRT_FENCE8.get())
                .addItems(Items.CRT_FENCE9.get())
                .addItems(Items.CRT_FENCE10.get())
                .addItems(Items.CRT_FENCE_TICKET.get())
                .addItems(Items.CRT_FENCE_TICKET_FLIPPED.get())
                .addItems(Items.CRT_FENCE_LIFT_TIPS_1.get())
                .addItems(Items.CRT_TEMP_FENCE_1.get());

        CRT_BUILDING_Items = FilterBuilder.registerFilter(ItemsGroup.CRT.getKey(), Text.translatable("FiltersGroup.nanbin.crt_building_blocks"), () -> new ItemStack(Items.CRT_LOGO.get()))
                .addItems(Items.CRT_LOGO.get())
                .addItems(Items.CRT_LIFT_TIPS.get())
                .addItems(Items.CRT_LIFT_TIPS_2.get())
                .addItems(Items.CRT_LIFT_TIPS_3.get())
                .addItems(Items.CRT_OLD_WALL1.get())
                .addItems(Items.CRT_OLD_WALL2.get())
                .addItems(Items.CRT_PLATFORM.get())
                .addItems(Items.CRT_STATION_NAME_1.get())
                .addItems(Items.CRT_STATION_NAME_2.get())
                .addItems(Items.ORDINARY_STATION_NAME.get())
                .addItems(Items.CRT_STATION_INFO_1.get())
                .addItems(Items.CRT_STATION_INFO_1_DOUBLE.get())
                .addItems(Items.CRT_RAILWAY_SIGN_3_ODD.get())
                .addItems(Items.CRT_RAILWAY_SIGN_3_EVEN.get())
                .addItems(Items.CRT_RAILWAY_SIGN_3_ODD.get())
                .addItems(Items.CRT_RAILWAY_SIGN_4_EVEN.get())
                .addItems(Items.CRT_RAILWAY_SIGN_4_ODD.get())
                .addItems(Items.CRT_RAILWAY_SIGN_5_EVEN.get())
                .addItems(Items.CRT_RAILWAY_SIGN_5_ODD.get())
                .addItems(Items.CRT_RAILWAY_SIGN_6_EVEN.get())
                .addItems(Items.CRT_RAILWAY_SIGN_6_ODD.get())
                .addItems(Items.CRT_RAILWAY_SIGN_7_EVEN.get())
                .addItems(Items.CRT_RAILWAY_SIGN_7_ODD.get())
                .addItems(Items.CRT_RAILWAY_SIGN_8_EVEN.get())
                .addItems(Items.CRT_RAILWAY_SIGN_8_ODD.get())
                .addItems(Items.CRT_RAILWAY_SIGN_9_EVEN.get())
                .addItems(Items.CRT_RAILWAY_SIGN_9_ODD.get())
                .addItems(Items.CRT_RAILWAY_SIGN_10_EVEN.get())
                .addItems(Items.CRT_RAILWAY_SIGN_10_ODD.get())
                .addItems(Items.CRT_RAILWAY_SIGN_11_EVEN.get())
                .addItems(Items.CRT_RAILWAY_SIGN_11_ODD.get())
                .addItems(Items.CRT_RAILWAY_SIGN_POLE.get());


        CRT_TICKETS = FilterBuilder.registerFilter(ItemsGroup.CRT.getKey(), Text.translatable("FiltersGroup.nanbin.crt_tickets"), () -> new ItemStack(Items.CRT_TICKET_1_EXIT.get()))
                .addItems(Items.CRT_TICKET_1_ENTER.get())
                .addItems(Items.CRT_TICKET_1_EXIT.get())
                .addItems(Items.CRT_TICKET_2_ENTER.get())
                .addItems(Items.CRT_TICKET_2_EXIT.get())
                .addItems(Items.CRT_TICKET_3_ENTER.get())
                .addItems(Items.CRT_TICKET_3_EXIT.get())
                .addItems(Items.CRT_TICKET_MACHINE_1.get());
                //.addItems(Items.CRT_TICKET_MACHINE_2.get());

        //CRT_OVERHEAD_LINES = FilterBuilder.registerFilter(ItemsGroup.CRT.getKey(), Text.translatable("FiltersGroup.nanbin.crt_overhead_lines"), () -> new ItemStack(new ItemConvertible(Items.CRT_LOGO.get())))
                //.addItems(Items.CRT_RIGID_CATENARY_1.get());

        //CRT_ESCALATOR = FilterBuilder.registerFilter(ItemsGroup.CRT.getKey(), Text.translatable("FiltersGroup.nanbin.crt_escalator"), () -> new ItemStack(Items.CRT_ESCALATOR.get()))
          //      .addItems(Items.CRT_ESCALATOR.get());

        CRT_DOOR = FilterBuilder.registerFilter(ItemsGroup.CRT.getKey(), Text.translatable("FiltersGroup.nanbin.crt_door"), () -> new ItemStack(Items.CRT_APG_CAB_DOOR_NEW.get()))
                //.addItems(Items.CRT_PSD_CAB_DOOR.get())
                .addItems(Items.CRT_APG_CAB_DOOR_OLD.get())
                .addItems(Items.CRT_APG_CAB_FENCE_OLD.get())
                .addItems(Items.CRT_APG_CAB_FENCE_OLD_CONNECT.get())
                .addItems(Items.CRT_APG_CAB_DOOR_NEW.get())
                .addItems(Items.CRT_APG_DOOR_1.get())
                .addItems(Items.CRT_APG_GLASS_1.get())
                .addItems(Items.CRT_APG_GLASS_END_1.get())
                .addItems(Items.CRT_APG_DOOR_2.get())
                .addItems(Items.CRT_APG_GLASS_2.get())
                .addItems(Items.CRT_APG_GLASS_END_2.get());

        COMMON_BUILDING_Items = FilterBuilder.registerFilter(ItemsGroup.CITY_BUILDING_BLOCKS.getKey(), Text.translatable("FiltersGroup.nanbin.common_building_blocks"), () -> new ItemStack(Items.NANBIN_BLUE_BLOCK.get()))
                .addItems(Items.BUS_TICKET_PROCESSOR.get())
                .addItems(Items.NANBIN_WHITE_BLOCK.get())
                .addItems(Items.NANBIN_RED_BLOCK.get())
                .addItems(Items.NANBIN_YELLOW_BLOCK.get())
                .addItems(Items.NANBIN_GREEN_BLOCK.get())
                .addItems(Items.NANBIN_BLUE_BLOCK.get())
                .addItems(Items.NANBIN_PURPLE_BLOCK.get())
                .addItems(Items.NANBIN_PINK_BLOCK.get())
                .addItems(Items.LIGHT_RED_BLOCK.get())
                .addItems(Items.LIGHT_YELLOW_BLOCK.get())
                .addItems(Items.LIGHT_GREEN_BLOCK.get())
                .addItems(Items.LIGHT_BLUE_BLOCK.get())
                .addItems(Items.LIGHT_PURPLE_BLOCK.get())
                .addItems(Items.LIGHT_PINK_BLOCK.get())
                .addItems(Items.CRT_RED_WALL_BLOCK.get())
                .addItems(Items.CRT_YELLOW_WALL_BLOCK.get())
                .addItems(Items.CRT_GREEN_WALL_BLOCK.get())
                .addItems(Items.CRT_BLUE_WALL_BLOCK.get())
                .addItems(Items.CRT_PURPLE_WALL_BLOCK.get())
                .addItems(Items.CRT_PINK_WALL_BLOCK.get())
                .addItems(Items.CRT_GRADIENT_RED_WALL_BLOCK.get())
                .addItems(Items.CRT_GRADIENT_YELLOW_WALL_BLOCK.get())
                .addItems(Items.CRT_GRADIENT_GREEN_WALL_BLOCK.get())
                .addItems(Items.CRT_GRADIENT_BLUE_WALL_BLOCK.get())
                .addItems(Items.CRT_GRADIENT_PURPLE_WALL_BLOCK.get())
                .addItems(Items.CRT_GRADIENT_PINK_WALL_BLOCK.get())
                .addItems(Items.BLACK_MARBLE.get())
                .addItems(Items.WHITE_MARBLE.get())
                .addItems(Items.TERRAZZO.get())
                .addItems(Items.BLACK_TERRAZZO.get())
                .addItems(Items.CEMENT.get());

        ROAD_Items = FilterBuilder.registerFilter(ItemsGroup.CITY_BUILDING_BLOCKS.getKey(), Text.translatable("FiltersGroup.nanbin.road_blocks"), () -> new ItemStack(Items.YELLOW_TACTILE_BAVING.get()))
                .addItems(Items.PAVEMENT_1.get())
                .addItems(Items.PAVEMENT_1_HALF.get())
                .addItems(Items.PAVEMENT_2.get())
                .addItems(Items.PAVEMENT_2_HALF.get())
                .addItems(Items.PAVEMENT_3.get())
                .addItems(Items.PAVEMENT_3_HALF.get())
                .addItems(Items.PAVEMENT_4.get())
                .addItems(Items.PAVEMENT_4_HALF.get())
                .addItems(Items.DRAIN_COVER.get())
                .addItems(Items.GRAY_TACTILE_BAVING.get())
                .addItems(Items.GRAY_TACTILE_BAVING_HALF.get())
                .addItems(Items.GRAY_TACTILE_BAVING_CONNECT.get())
                .addItems(Items.GRAY_TACTILE_BAVING_CONNECT_HALF.get())
                .addItems(Items.YELLOW_TACTILE_BAVING.get())
                .addItems(Items.YELLOW_TACTILE_BAVING_HALF.get())
                .addItems(Items.YELLOW_TACTILE_BAVING_CONNECT.get())
                .addItems(Items.YELLOW_TACTILE_BAVING_CONNECT_HALF.get());

        FENCE_Items = FilterBuilder.registerFilter(ItemsGroup.CITY_BUILDING_BLOCKS.getKey(), Text.translatable("FiltersGroup.nanbin.fence_blocks"), () -> new ItemStack(Items.BLUEFENCE.get()))
                .addItems(Items.BLUEFENCE.get())
                .addItems(Items.GREENFENCE.get())
                .addItems(Items.METALFENCE.get());

        FUNCTION_Items = FilterBuilder.registerFilter(ItemsGroup.CITY_BUILDING_BLOCKS.getKey(), Text.translatable("FiltersGroup.nanbin.function_blocks"), () -> new ItemStack(Items.ROAD_NAME.get()))
                .addItems(Items.ROAD_NAME.get());
    }
}