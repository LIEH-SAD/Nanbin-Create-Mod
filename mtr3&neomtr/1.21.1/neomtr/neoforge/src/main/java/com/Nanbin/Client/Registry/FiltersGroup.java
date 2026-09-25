package com.Nanbin.Client.Registry;

import com.Nanbin.Blocks.Blocks;
import com.Nanbin.ItemsGroup.ItemsGroup;
import mtr.mappings.Text;
import net.minecraft.world.item.ItemStack;
import ziyue.filters.Filter;
import ziyue.filters.FilterBuilder;

public class FiltersGroup {
    public static Filter CRT_FENCE;
    public static Filter CRT_BUILDING_BLOCKS;
    public static Filter CRT_TICKETS;
    public static Filter CRT_OVERHEAD_LINES;
    public static Filter CRT_DOOR;
    public static Filter COMMON_BUILDING_BLOCKS;
    public static Filter ROAD_BLOCKS;
    public static Filter FENCE_BLOCKS;

    private FiltersGroup() {}

    public static void init(){
        CRT_FENCE = FilterBuilder.registerFilter(ItemsGroup.CRT.get(), Text.translatable("FiltersGroup.nanbin.crt_fence"), () -> new ItemStack(Blocks.CRT_TICKET_MACHINE_1.get().asItem()))
                .addItems(Blocks.CRT_FENCE1.get().asItem())
                .addItems(Blocks.CRT_FENCE8.get().asItem())
                .addItems(Blocks.CRT_FENCE9.get().asItem());

        CRT_BUILDING_BLOCKS = FilterBuilder.registerFilter(ItemsGroup.CRT.get(), Text.translatable("FiltersGroup.nanbin.crt_building_blocks"), () -> new ItemStack(Blocks.CRT_PLATFORM.get().asItem()))
                //.addItems(Blocks.CRT_LOGO.get().asItem())
                //.addItems(Blocks.CRT_LIFT_TIPS.get().asItem())
                //.addItems(Blocks.CRT_LIFT_TIPS_3.get().asItem())
                .addItems(Blocks.CRT_OLD_WALL1.get().asItem())
                .addItems(Blocks.CRT_OLD_WALL2.get().asItem())
                .addItems(Blocks.CRT_PLATFORM.get().asItem());

        CRT_TICKETS = FilterBuilder.registerFilter(ItemsGroup.CRT.get(), Text.translatable("FiltersGroup.nanbin.crt_tickets"), () -> new ItemStack(Blocks.CRT_TICKET_1_EXIT.get()))
                .addItems(Blocks.CRT_TICKET_MACHINE_1.get().asItem())
                .addItems(Blocks.CRT_TICKET_1_ENTER.get().asItem())
                .addItems(Blocks.CRT_TICKET_1_EXIT.get().asItem())
                .addItems(Blocks.CRT_TICKET_2_ENTER.get().asItem())
                .addItems(Blocks.CRT_TICKET_2_EXIT.get().asItem());

        CRT_OVERHEAD_LINES = FilterBuilder.registerFilter(ItemsGroup.CRT.get(), Text.translatable("FiltersGroup.nanbin.crt_overhead_lines"), () -> new ItemStack(Blocks.CRT_TICKET_MACHINE_1.get()))
               .addItems();

        CRT_DOOR = FilterBuilder.registerFilter(ItemsGroup.CRT.get(), Text.translatable("FiltersGroup.nanbin.crt_door"), () -> new ItemStack(Blocks.CRT_TICKET_MACHINE_1.get()))
                .addItems();
                //.addItems(Blocks.CRT_PSD_CAB_DOOR.get().asItem())
                //.addItems(Blocks.CRT_APG_CAB_DOOR_OLD.get().asItem())
                //.addItems(Blocks.CRT_APG_CAB_DOOR_NEW.get().asItem());

        COMMON_BUILDING_BLOCKS = FilterBuilder.registerFilter(ItemsGroup.CITY_BUILDING_BLOCKS.get(), Text.translatable("FiltersGroup.nanbin.common_building_blocks"), () -> new ItemStack(Blocks.NANBIN_BLUE_BLOCK.get()))
                .addItems(Blocks.NANBIN_WHITE_BLOCK.get().asItem())
                .addItems(Blocks.NANBIN_RED_BLOCK.get().asItem())
                .addItems(Blocks.NANBIN_YELLOW_BLOCK.get().asItem())
                .addItems(Blocks.NANBIN_GREEN_BLOCK.get().asItem())
                .addItems(Blocks.NANBIN_BLUE_BLOCK.get().asItem())
                .addItems(Blocks.NANBIN_PURPLE_BLOCK.get().asItem())
                .addItems(Blocks.NANBIN_PINK_BLOCK.get().asItem())
                .addItems(Blocks.LIGHT_RED_BLOCK.get().asItem())
                .addItems(Blocks.LIGHT_YELLOW_BLOCK.get().asItem())
                .addItems(Blocks.LIGHT_GREEN_BLOCK.get().asItem())
                .addItems(Blocks.LIGHT_BLUE_BLOCK.get().asItem())
                .addItems(Blocks.LIGHT_PURPLE_BLOCK.get().asItem())
                .addItems(Blocks.LIGHT_PINK_BLOCK.get().asItem())
                .addItems(Blocks.CRT_RED_WALL_BLOCK.get().asItem())
                .addItems(Blocks.CRT_YELLOW_WALL_BLOCK.get().asItem())
                .addItems(Blocks.CRT_GREEN_WALL_BLOCK.get().asItem())
                .addItems(Blocks.CRT_BLUE_WALL_BLOCK.get().asItem())
                .addItems(Blocks.CRT_PURPLE_WALL_BLOCK.get().asItem())
                .addItems(Blocks.CRT_PINK_WALL_BLOCK.get().asItem())
                .addItems(Blocks.CRT_GRADIENT_RED_WALL_BLOCK.get().asItem())
                .addItems(Blocks.CRT_GRADIENT_YELLOW_WALL_BLOCK.get().asItem())
                .addItems(Blocks.CRT_GRADIENT_GREEN_WALL_BLOCK.get().asItem())
                .addItems(Blocks.CRT_GRADIENT_BLUE_WALL_BLOCK.get().asItem())
                .addItems(Blocks.CRT_GRADIENT_PURPLE_WALL_BLOCK.get().asItem())
                .addItems(Blocks.CRT_GRADIENT_PINK_WALL_BLOCK.get().asItem())
                .addItems(Blocks.BLACK_MARBLE.get().asItem())
                .addItems(Blocks.WHITE_MARBLE.get().asItem())
                .addItems(Blocks.CEMENT.get().asItem());

        ROAD_BLOCKS = FilterBuilder.registerFilter(ItemsGroup.CITY_BUILDING_BLOCKS.get(), Text.translatable("FiltersGroup.nanbin.road_blocks"), () -> new ItemStack(Blocks.PAVEMENT_1.get()))
                .addItems(Blocks.PAVEMENT_1.get().asItem())
                .addItems(Blocks.PAVEMENT_1_HALF.get().asItem())
                .addItems(Blocks.PAVEMENT_2.get().asItem())
                .addItems(Blocks.PAVEMENT_2_HALF.get().asItem())
                .addItems(Blocks.PAVEMENT_3.get().asItem())
                .addItems(Blocks.PAVEMENT_3_HALF.get().asItem())
                .addItems(Blocks.PAVEMENT_4.get().asItem())
                .addItems(Blocks.PAVEMENT_4_HALF.get().asItem())
                .addItems(Blocks.DRAIN_COVER.get().asItem())
                //.addItems(Blocks.GRAY_TACTILE_BAVING.get().asItem())
                //.addItems(Blocks.GRAY_TACTILE_BAVING_HALF.get().asItem())
                .addItems(Blocks.GRAY_TACTILE_BAVING_CONNECT.get().asItem())
                .addItems(Blocks.GRAY_TACTILE_BAVING_CONNECT_HALF.get().asItem())
                //.addItems(Blocks.YELLOW_TACTILE_BAVING.get().asItem())
                //.addItems(Blocks.YELLOW_TACTILE_BAVING_HALF.get().asItem())
                .addItems(Blocks.YELLOW_TACTILE_BAVING_CONNECT.get().asItem())
                .addItems(Blocks.YELLOW_TACTILE_BAVING_CONNECT_HALF.get().asItem());

        //FENCE_BLOCKS = FilterBuilder.registerFilter(ItemsGroup.CITY_BUILDING_BLOCKS.get(), Text.translatable("FiltersGroup.nanbin.fence_blocks"), () -> new ItemStack(Blocks.BLUEFENCE.get()))
                //.addItems(Blocks.BLUEFENCE.get().asItem())
                //.addItems(Blocks.GREENFENCE.get().asItem())
                //.addItems(Blocks.METALFENCE.get().asItem());
    }
}
