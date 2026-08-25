package com.Nanbin.client.FiltersGroup;

import com.Nanbin.Blocks.Blocks;
import com.Nanbin.Items.Items;
import com.Nanbin.ItemsGroup.ItemsGroup;
import com.Nanbin.mapping.FilterBuilder;
import org.mtr.mapping.holder.ItemConvertible;
import org.mtr.mapping.holder.ItemStack;
import org.mtr.mapping.mapper.TextHelper;
import ziyue.filters.Filter;

public final class FiltersGroup {

    public static Filter CRT_FENCE;
    public static Filter CRT_BUILDING_BLOCKS;
    public static Filter CRT_TICKETS;
    //public static Filter CRT_OVERHEAD_LINES;
    public static Filter CRT_DOOR;
    public static Filter COMMON_BUILDING_BLOCKS;
    public static Filter ROAD_BLOCKS;
    public static Filter FENCE_BLOCKS;
    public static Filter FUNCTION_BLOCKS;

    private FiltersGroup() {}

    public static void init() {
        CRT_FENCE = FilterBuilder.registerFilter(ItemsGroup.CRT, TextHelper.translatable("FiltersGroup.nanbin.crt_fence"), () -> new ItemStack(new ItemConvertible(Blocks.CRT_FENCE1.get().data)))
                .addItems(Blocks.CRT_FENCE1.get().data.asItem())
                .addItems(Blocks.CRT_FENCE8.get().data.asItem())
                .addItems(Blocks.CRT_FENCE9.get().data.asItem())
                .addItems(Blocks.CRT_FENCE10.get().data.asItem())
                .addItems(Blocks.CRT_FENCE_TICKET.get().data.asItem())
                .addItems(Blocks.CRT_FENCE_LIFT_TIPS_1.get().data.asItem())
                .addItems(Blocks.CRT_TEMP_FENCE_1.get().data.asItem());

        CRT_BUILDING_BLOCKS = FilterBuilder.registerFilter(ItemsGroup.CRT, TextHelper.translatable("FiltersGroup.nanbin.crt_building_blocks"), () -> new ItemStack(new ItemConvertible(Blocks.CRT_LOGO.get().data)))
                .addItems(Blocks.CRT_LOGO.get().data.asItem())
                .addItems(Blocks.CRT_LIFT_TIPS.get().data.asItem())
                .addItems(Blocks.CRT_LIFT_TIPS_2.get().data.asItem())
                .addItems(Blocks.CRT_LIFT_TIPS_3.get().data.asItem())
                .addItems(Blocks.CRT_OLD_WALL1.get().data.asItem())
                .addItems(Blocks.CRT_OLD_WALL2.get().data.asItem())
                .addItems(Blocks.CRT_PLATFORM.get().data.asItem())
                .addItems(Blocks.CRT_STATION_NAME_1.get().data.asItem())
                .addItems(Blocks.CRT_STATION_NAME_2.get().data.asItem())
                .addItems(Blocks.CRT_RAILWAY_SIGN_3_EVEN.get().data.asItem())
                .addItems(Blocks.CRT_RAILWAY_SIGN_3_ODD.get().data.asItem())
                .addItems(Blocks.CRT_RAILWAY_SIGN_4_EVEN.get().data.asItem())
                .addItems(Blocks.CRT_RAILWAY_SIGN_4_ODD.get().data.asItem())
                .addItems(Blocks.CRT_RAILWAY_SIGN_5_EVEN.get().data.asItem())
                .addItems(Blocks.CRT_RAILWAY_SIGN_5_ODD.get().data.asItem())
                .addItems(Blocks.CRT_RAILWAY_SIGN_6_EVEN.get().data.asItem())
                .addItems(Blocks.CRT_RAILWAY_SIGN_6_ODD.get().data.asItem())
                .addItems(Blocks.CRT_RAILWAY_SIGN_7_EVEN.get().data.asItem())
                .addItems(Blocks.CRT_RAILWAY_SIGN_7_ODD.get().data.asItem())
                .addItems(Blocks.CRT_RAILWAY_SIGN_8_EVEN.get().data.asItem())
                .addItems(Blocks.CRT_RAILWAY_SIGN_8_ODD.get().data.asItem())
                .addItems(Blocks.CRT_RAILWAY_SIGN_9_EVEN.get().data.asItem())
                .addItems(Blocks.CRT_RAILWAY_SIGN_9_ODD.get().data.asItem())
                .addItems(Blocks.CRT_RAILWAY_SIGN_10_EVEN.get().data.asItem())
                .addItems(Blocks.CRT_RAILWAY_SIGN_10_ODD.get().data.asItem())
                .addItems(Blocks.CRT_RAILWAY_SIGN_11_EVEN.get().data.asItem())
                .addItems(Blocks.CRT_RAILWAY_SIGN_11_ODD.get().data.asItem())
                .addItems(Blocks.CRT_RAILWAY_SIGN_POLE.get().data.asItem())
                .addItems(Blocks.CRT_STATION_INFO_1.get().data.asItem())
                .addItems(Blocks.CRT_STATION_INFO_1_DOUBLE.get().data.asItem());


        CRT_TICKETS = FilterBuilder.registerFilter(ItemsGroup.CRT, TextHelper.translatable("FiltersGroup.nanbin.crt_tickets"), () -> new ItemStack(new ItemConvertible(Blocks.CRT_TICKET_1_EXIT.get().data)))
                .addItems(Blocks.CRT_TICKET_1_ENTER.get().data.asItem())
                .addItems(Blocks.CRT_TICKET_1_EXIT.get().data.asItem())
                .addItems(Blocks.CRT_TICKET_2_ENTER.get().data.asItem())
                .addItems(Blocks.CRT_TICKET_2_EXIT.get().data.asItem())
                .addItems(Blocks.CRT_TICKET_3_ENTER.get().data.asItem())
                .addItems(Blocks.CRT_TICKET_3_EXIT.get().data.asItem())
                .addItems(Blocks.CRT_TICKET_MACHINE_1.get().data.asItem());

        //CRT_OVERHEAD_LINES = FilterBuilder.registerFilter(ItemsGroup.CRT, TextHelper.translatable("FiltersGroup.nanbin.crt_overhead_lines"), () -> new ItemStack(new ItemConvertible(Blocks.CRT_LOGO.get().data)))
                //.addItems(Blocks.CRT_RIGID_CATENARY_1.get().data.asItem());

        CRT_DOOR = FilterBuilder.registerFilter(ItemsGroup.CRT, TextHelper.translatable("FiltersGroup.nanbin.crt_door"), () -> new ItemStack(new ItemConvertible(Blocks.CRT_APG_CAB_DOOR_NEW.get().data)))
                //.addItems(Blocks.CRT_PSD_CAB_DOOR.get().data.asItem())
                .addItems(Blocks.CRT_APG_CAB_DOOR_OLD.get().data.asItem())
                .addItems(Blocks.CRT_APG_CAB_FENCE_OLD.get().data.asItem())
                .addItems(Blocks.CRT_APG_CAB_FENCE_OLD_CONNECT.get().data.asItem())
                .addItems(Blocks.CRT_APG_CAB_DOOR_NEW.get().data.asItem())
                .addItems(Items.CRT_APG_DOOR_1.get().data)
                .addItems(Items.CRT_APG_GLASS_1.get().data)
                //.addItems(Items.CRT_APG_GLASS_END_1.get().data)
                .addItems(Items.CRT_APG_DOOR_2.get().data)
                .addItems(Items.CRT_APG_GLASS_2.get().data);
                //.addItems(Items.CRT_APG_GLASS_END_2.get().data);

        COMMON_BUILDING_BLOCKS = FilterBuilder.registerFilter(ItemsGroup.CITY_BUILDING_BLOCKS, TextHelper.translatable("FiltersGroup.nanbin.common_building_blocks"), () -> new ItemStack(new ItemConvertible(Blocks.NANBIN_BLUE_BLOCK.get().data)))
                .addItems(Blocks.NANBIN_WHITE_BLOCK.get().data.asItem())
                .addItems(Blocks.NANBIN_RED_BLOCK.get().data.asItem())
                .addItems(Blocks.NANBIN_YELLOW_BLOCK.get().data.asItem())
                .addItems(Blocks.NANBIN_GREEN_BLOCK.get().data.asItem())
                .addItems(Blocks.NANBIN_BLUE_BLOCK.get().data.asItem())
                .addItems(Blocks.NANBIN_PURPLE_BLOCK.get().data.asItem())
                .addItems(Blocks.NANBIN_PINK_BLOCK.get().data.asItem())
                .addItems(Blocks.LIGHT_RED_BLOCK.get().data.asItem())
                .addItems(Blocks.LIGHT_YELLOW_BLOCK.get().data.asItem())
                .addItems(Blocks.LIGHT_GREEN_BLOCK.get().data.asItem())
                .addItems(Blocks.LIGHT_BLUE_BLOCK.get().data.asItem())
                .addItems(Blocks.LIGHT_PURPLE_BLOCK.get().data.asItem())
                .addItems(Blocks.LIGHT_PINK_BLOCK.get().data.asItem())
                .addItems(Blocks.CRT_RED_WALL_BLOCK.get().data.asItem())
                .addItems(Blocks.CRT_YELLOW_WALL_BLOCK.get().data.asItem())
                .addItems(Blocks.CRT_GREEN_WALL_BLOCK.get().data.asItem())
                .addItems(Blocks.CRT_BLUE_WALL_BLOCK.get().data.asItem())
                .addItems(Blocks.CRT_PURPLE_WALL_BLOCK.get().data.asItem())
                .addItems(Blocks.CRT_PINK_WALL_BLOCK.get().data.asItem())
                .addItems(Blocks.CRT_GRADIENT_RED_WALL_BLOCK.get().data.asItem())
                .addItems(Blocks.CRT_GRADIENT_YELLOW_WALL_BLOCK.get().data.asItem())
                .addItems(Blocks.CRT_GRADIENT_GREEN_WALL_BLOCK.get().data.asItem())
                .addItems(Blocks.CRT_GRADIENT_BLUE_WALL_BLOCK.get().data.asItem())
                .addItems(Blocks.CRT_GRADIENT_PURPLE_WALL_BLOCK.get().data.asItem())
                .addItems(Blocks.CRT_GRADIENT_PINK_WALL_BLOCK.get().data.asItem())
                .addItems(Blocks.BLACK_MARBLE.get().data.asItem())
                .addItems(Blocks.WHITE_MARBLE.get().data.asItem())
                .addItems(Blocks.TERRAZZO.get().data.asItem())
                .addItems(Blocks.BLACK_TERRAZZO.get().data.asItem())
                .addItems(Blocks.CEMENT.get().data.asItem());

        ROAD_BLOCKS = FilterBuilder.registerFilter(ItemsGroup.CITY_BUILDING_BLOCKS, TextHelper.translatable("FiltersGroup.nanbin.road_blocks"), () -> new ItemStack(new ItemConvertible(Blocks.YELLOW_TACTILE_BAVING.get().data)))
                .addItems(Blocks.PAVEMENT_1.get().data.asItem())
                .addItems(Blocks.PAVEMENT_1_HALF.get().data.asItem())
                .addItems(Blocks.PAVEMENT_2.get().data.asItem())
                .addItems(Blocks.PAVEMENT_2_HALF.get().data.asItem())
                .addItems(Blocks.PAVEMENT_3.get().data.asItem())
                .addItems(Blocks.PAVEMENT_3_HALF.get().data.asItem())
                .addItems(Blocks.PAVEMENT_4.get().data.asItem())
                .addItems(Blocks.PAVEMENT_4_HALF.get().data.asItem())
                .addItems(Blocks.DRAIN_COVER.get().data.asItem())
                .addItems(Blocks.GRAY_TACTILE_BAVING.get().data.asItem())
                .addItems(Blocks.GRAY_TACTILE_BAVING_HALF.get().data.asItem())
                .addItems(Blocks.GRAY_TACTILE_BAVING_CONNECT.get().data.asItem())
                .addItems(Blocks.GRAY_TACTILE_BAVING_CONNECT_HALF.get().data.asItem())
                .addItems(Blocks.YELLOW_TACTILE_BAVING.get().data.asItem())
                .addItems(Blocks.YELLOW_TACTILE_BAVING_HALF.get().data.asItem())
                .addItems(Blocks.YELLOW_TACTILE_BAVING_CONNECT.get().data.asItem())
                .addItems(Blocks.YELLOW_TACTILE_BAVING_CONNECT_HALF.get().data.asItem());

        FENCE_BLOCKS = FilterBuilder.registerFilter(ItemsGroup.CITY_BUILDING_BLOCKS, TextHelper.translatable("FiltersGroup.nanbin.fence_blocks"), () -> new ItemStack(new ItemConvertible(Blocks.BLUEFENCE.get().data)))
                .addItems(Blocks.BLUEFENCE.get().data.asItem())
                .addItems(Blocks.GREENFENCE.get().data.asItem())
                .addItems(Blocks.METALFENCE.get().data.asItem());

        FUNCTION_BLOCKS = FilterBuilder.registerFilter(ItemsGroup.CITY_BUILDING_BLOCKS, TextHelper.translatable("FiltersGroup.nanbin.function_blocks"), () -> new ItemStack(new ItemConvertible(Blocks.ROAD_NAME.get().data)))
                .addItems(Blocks.ROAD_NAME.get().data.asItem())
                .addItems(Blocks.BUS_TICKET_PROCESSOR.get().data.asItem());
    }

}