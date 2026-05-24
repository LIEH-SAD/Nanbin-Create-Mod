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

    public static void onInitialize() {
        CRT_FENCE = FilterBuilder.registerFilter(ItemsGroup.CRT, Text.translatable("FiltersGroup.nanbin.crt_fence"), () -> new ItemStack(Blocks.CRT_FENCE1))
                .addItems(Blocks.CRT_FENCE1.asItem())
                .addItems(Blocks.CRT_FENCE8.asItem())
                .addItems(Blocks.CRT_FENCE9.asItem());

        CRT_BUILDING_BLOCKS = FilterBuilder.registerFilter(ItemsGroup.CRT, Text.translatable("FiltersGroup.nanbin.crt_building_blocks"), () -> new ItemStack(Blocks.CRT_LOGO))
                .addItems(Blocks.CRT_LOGO.asItem())
                .addItems(Blocks.CRT_LIFT_TIPS.asItem())
                .addItems(Blocks.CRT_OLD_WALL1.asItem())
                .addItems(Blocks.CRT_OLD_WALL2.asItem())
                .addItems(Blocks.CRT_PLATFORM.asItem());

        CRT_TICKETS = FilterBuilder.registerFilter(ItemsGroup.CRT, Text.translatable("FiltersGroup.nanbin.crt_tickets"), () -> new ItemStack(Blocks.CRT_TICKET_1_EXIT))
                .addItems(Blocks.CRT_TICKET_1_ENTER.asItem())
                .addItems(Blocks.CRT_TICKET_1_EXIT.asItem())
                .addItems(Blocks.CRT_TICKET_2_ENTER.asItem())
                .addItems(Blocks.CRT_TICKET_2_EXIT.asItem())
                .addItems(Blocks.CRT_TICKET_MACHINE_1.asItem());

        //CRT_OVERHEAD_LINES = FilterBuilder.registerFilter(ItemsGroup.CRT, Text.translatable("FiltersGroup.nanbin.crt_overhead_lines"), () -> new ItemStack(Blocks.CRT_LOGO))
        //        .addItems();

        CRT_DOOR = FilterBuilder.registerFilter(ItemsGroup.CRT, Text.translatable("FiltersGroup.nanbin.crt_door"), () -> new ItemStack(Blocks.CRT_APG_CAB_DOOR_NEW.asItem()))
                //.addItems(Blocks.CRT_PSD_CAB_DOOR.asItem())
                .addItems(Blocks.CRT_APG_CAB_DOOR_OLD.asItem())
                .addItems(Blocks.CRT_APG_CAB_DOOR_NEW.asItem());

        COMMON_BUILDING_BLOCKS = FilterBuilder.registerFilter(ItemsGroup.CITY_BUILDING_BLOCKS, Text.translatable("FiltersGroup.nanbin.common_building_blocks"), () -> new ItemStack(Blocks.NANBIN_BLUE_BLOCK))
                .addItems(Blocks.NANBIN_WHITE_BLOCK.asItem())
                .addItems(Blocks.NANBIN_RED_BLOCK.asItem())
                .addItems(Blocks.NANBIN_YELLOW_BLOCK.asItem())
                .addItems(Blocks.NANBIN_GREEN_BLOCK.asItem())
                .addItems(Blocks.NANBIN_BLUE_BLOCK.asItem())
                .addItems(Blocks.NANBIN_PURPLE_BLOCK.asItem())
                .addItems(Blocks.NANBIN_PINK_BLOCK.asItem())
                .addItems(Blocks.LIGHT_RED_BLOCK.asItem())
                .addItems(Blocks.LIGHT_YELLOW_BLOCK.asItem())
                .addItems(Blocks.LIGHT_GREEN_BLOCK.asItem())
                .addItems(Blocks.LIGHT_BLUE_BLOCK.asItem())
                .addItems(Blocks.LIGHT_PURPLE_BLOCK.asItem())
                .addItems(Blocks.LIGHT_PINK_BLOCK.asItem())
                .addItems(Blocks.CRT_RED_WALL_BLOCK.asItem())
                .addItems(Blocks.CRT_YELLOW_WALL_BLOCK.asItem())
                .addItems(Blocks.CRT_GREEN_WALL_BLOCK.asItem())
                .addItems(Blocks.CRT_BLUE_WALL_BLOCK.asItem())
                .addItems(Blocks.CRT_PURPLE_WALL_BLOCK.asItem())
                .addItems(Blocks.CRT_PINK_WALL_BLOCK.asItem())
                .addItems(Blocks.CRT_GRADIENT_RED_WALL_BLOCK.asItem())
                .addItems(Blocks.CRT_GRADIENT_YELLOW_WALL_BLOCK.asItem())
                .addItems(Blocks.CRT_GRADIENT_GREEN_WALL_BLOCK.asItem())
                .addItems(Blocks.CRT_GRADIENT_BLUE_WALL_BLOCK.asItem())
                .addItems(Blocks.CRT_GRADIENT_PURPLE_WALL_BLOCK.asItem())
                .addItems(Blocks.CRT_GRADIENT_PINK_WALL_BLOCK.asItem())
                .addItems(Blocks.BLACK_MARBLE.asItem())
                .addItems(Blocks.WHITE_MARBLE.asItem())
                .addItems(Blocks.CEMENT.asItem());

        ROAD_BLOCKS = FilterBuilder.registerFilter(ItemsGroup.CITY_BUILDING_BLOCKS, Text.translatable("FiltersGroup.nanbin.road_blocks"), () -> new ItemStack(Blocks.YELLOW_TACTILE_BAVING))
                .addItems(Blocks.PAVEMENT_1.asItem())
                .addItems(Blocks.PAVEMENT_1_HALF.asItem())
                .addItems(Blocks.PAVEMENT_2.asItem())
                .addItems(Blocks.PAVEMENT_2_HALF.asItem())
                .addItems(Blocks.PAVEMENT_3.asItem())
                .addItems(Blocks.PAVEMENT_3_HALF.asItem())
                .addItems(Blocks.PAVEMENT_4.asItem())
                .addItems(Blocks.PAVEMENT_4_HALF.asItem())
                .addItems(Blocks.DRAIN_COVER.asItem())
                .addItems(Blocks.GRAY_TACTILE_BAVING.asItem())
                .addItems(Blocks.GRAY_TACTILE_BAVING_HALF.asItem())
                .addItems(Blocks.GRAY_TACTILE_BAVING_CONNECT.asItem())
                .addItems(Blocks.GRAY_TACTILE_BAVING_CONNECT_HALF.asItem())
                .addItems(Blocks.YELLOW_TACTILE_BAVING.asItem())
                .addItems(Blocks.YELLOW_TACTILE_BAVING_HALF.asItem())
                .addItems(Blocks.YELLOW_TACTILE_BAVING_CONNECT.asItem())
                .addItems(Blocks.YELLOW_TACTILE_BAVING_CONNECT_HALF.asItem());

        FENCE_BLOCKS = FilterBuilder.registerFilter(ItemsGroup.CITY_BUILDING_BLOCKS, Text.translatable("FiltersGroup.nanbin.fence_blocks"), () -> new ItemStack(Blocks.BLUEFENCE))
                .addItems(Blocks.BLUEFENCE.asItem())
                .addItems(Blocks.GREENFENCE.asItem())
                .addItems(Blocks.METALFENCE.asItem());

    }

}