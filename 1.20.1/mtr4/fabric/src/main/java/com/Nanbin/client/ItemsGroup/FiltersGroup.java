package com.Nanbin.client.ItemsGroup;

import com.Nanbin.Blocks.Blocks;
import com.Nanbin.ItemsGroup.ItemsGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import ziyue.filters.Filter;
import ziyue.filters.FilterBuilder;

public final class FiltersGroup {

    public static Filter CRT_FENCE;
    public static Filter CRT_BUILDING_BLOCKS;
    public static Filter CRT_TICKETS;
    public static Filter CRT_OVERHEAD_LINES;
    public static Filter CRT_DOOR;
    public static Filter COMMON_BUILDING_BLOCKS;
    public static Filter ROAD_BLOCKS;
    public static Filter FENCE_BLOCKS;

    private FiltersGroup() {}

    public static void init() {
        CRT_FENCE = FilterBuilder.registerFilter(ItemsGroup.CRT.creativeModeTab, Text.translatable("FiltersGroup.nanbin.crt_fence"), () -> new ItemStack(Blocks.CRT_FENCE1.get().data))
                .addItems(Blocks.CRT_FENCE1.get().data.asItem())
                .addItems(Blocks.CRT_FENCE8.get().data.asItem())
                .addItems(Blocks.CRT_FENCE9.get().data.asItem())
                .addItems(Blocks.CRT_FENCE10.get().data.asItem())
                .addItems(Blocks.CRT_FENCE_TICKET.get().data.asItem())
                .addItems(Blocks.CRT_FENCE_LIFT_TIPS_1.get().data.asItem())
                .addItems(Blocks.CRT_TEMP_FENCE_1.get().data.asItem());

        CRT_BUILDING_BLOCKS = FilterBuilder.registerFilter(ItemsGroup.CRT.creativeModeTab, Text.translatable("FiltersGroup.nanbin.crt_building_blocks"), () -> new ItemStack(Blocks.CRT_LOGO.get().data))
                .addItems(Blocks.CRT_LOGO.get().data.asItem())
                .addItems(Blocks.CRT_LIFT_TIPS.get().data.asItem())
                .addItems(Blocks.CRT_LIFT_TIPS_3.get().data.asItem())
                .addItems(Blocks.CRT_OLD_WALL1.get().data.asItem())
                .addItems(Blocks.CRT_OLD_WALL2.get().data.asItem())
                .addItems(Blocks.CRT_PLATFORM.get().data.asItem());

        CRT_TICKETS = FilterBuilder.registerFilter(ItemsGroup.CRT.creativeModeTab, Text.translatable("FiltersGroup.nanbin.crt_tickets"), () -> new ItemStack(Blocks.CRT_TICKET_1_EXIT.get().data))
                .addItems(Blocks.CRT_TICKET_1_ENTER.get().data.asItem())
                .addItems(Blocks.CRT_TICKET_1_EXIT.get().data.asItem())
                .addItems(Blocks.CRT_TICKET_2_ENTER.get().data.asItem())
                .addItems(Blocks.CRT_TICKET_2_EXIT.get().data.asItem())
                .addItems(Blocks.CRT_TICKET_MACHINE_1.get().data.asItem());

        //CRT_OVERHEAD_LINES = FilterBuilder.registerFilter(ItemsGroup.CRT.creativeModeTab, Text.translatable("FiltersGroup.nanbin.crt_overhead_lines"), () -> new ItemStack(Blocks.CRT_LOGO.get().data))
                //.addItems(Blocks.CRT_RIGID_CATENARY_1.get().data.asItem());

        CRT_DOOR = FilterBuilder.registerFilter(ItemsGroup.CRT.creativeModeTab, Text.translatable("FiltersGroup.nanbin.crt_door"), () -> new ItemStack(Blocks.CRT_APG_CAB_DOOR_NEW.get().data))
                //.addItems(Blocks.CRT_PSD_CAB_DOOR.get().data.asItem())
                .addItems(Blocks.CRT_APG_CAB_DOOR_OLD.get().data.asItem())
                .addItems(Blocks.CRT_APG_CAB_FENCE_OLD.get().data.asItem())
                .addItems(Blocks.CRT_APG_CAB_FENCE_OLD_CONNECT.get().data.asItem())
                .addItems(Blocks.CRT_APG_CAB_DOOR_NEW.get().data.asItem());

        COMMON_BUILDING_BLOCKS = FilterBuilder.registerFilter(ItemsGroup.CITY_BUILDING_BLOCKS.creativeModeTab, Text.translatable("FiltersGroup.nanbin.common_building_blocks"), () -> new ItemStack(Blocks.NANBIN_BLUE_BLOCK.get().data))
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

        ROAD_BLOCKS = FilterBuilder.registerFilter(ItemsGroup.CITY_BUILDING_BLOCKS.creativeModeTab, Text.translatable("FiltersGroup.nanbin.road_blocks"), () -> new ItemStack(Blocks.YELLOW_TACTILE_BAVING.get().data))
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

        FENCE_BLOCKS = FilterBuilder.registerFilter(ItemsGroup.CITY_BUILDING_BLOCKS.creativeModeTab, Text.translatable("FiltersGroup.nanbin.fence_blocks"), () -> new ItemStack(Blocks.BLUEFENCE.get().data))
                .addItems(Blocks.BLUEFENCE.get().data.asItem())
                .addItems(Blocks.GREENFENCE.get().data.asItem())
                .addItems(Blocks.METALFENCE.get().data.asItem());

    }

}