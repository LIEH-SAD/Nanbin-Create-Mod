package com.Nanbin.client.FiltersGroup;

import com.Nanbin.Blocks.Blocks;
import com.Nanbin.Items.Items;
import com.Nanbin.ItemsGroup.ItemsGroup;
import com.Nanbin.mapping.FilterBuilder;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import ziyue.filters.Filter;

import java.util.function.Supplier;

public final class FiltersGroup {

	public static Filter CRT_FENCE;
	public static Filter CRT_BUILDING_BLOCKS;
	public static Filter CRT_TICKETS;
	public static Filter CRT_DOOR;
	public static Filter COMMON_BUILDING_BLOCKS;
	public static Filter ROAD_BLOCKS;
	public static Filter FENCE_BLOCKS;
	public static Filter FUNCTION_BLOCKS;

	private FiltersGroup() {
	}

	private static Supplier<ItemStack> icon(Item item) {
		return () -> new ItemStack(item);
	}

	public static boolean isReady() {
		return (ItemsGroup.CRT_TAB == null || ItemsGroup.CRT_TAB.tryGet() != null)
				&& (ItemsGroup.CITY_BUILDING_BLOCKS_TAB == null || ItemsGroup.CITY_BUILDING_BLOCKS_TAB.tryGet() != null);
	}

	public static void init() {
		final ItemGroup crtTab = ItemsGroup.CRT_TAB == null ? null : ItemsGroup.CRT_TAB.tryGet();
		final ItemGroup cityTab = ItemsGroup.CITY_BUILDING_BLOCKS_TAB == null ? null : ItemsGroup.CITY_BUILDING_BLOCKS_TAB.tryGet();
		if (crtTab == null || cityTab == null) {
			return;
		}

		CRT_FENCE = FilterBuilder.registerFilter(crtTab, Text.translatable("FiltersGroup.nanbin.crt_fence"), icon(Blocks.CRT_FENCE1.get().asItem()))
				.addItems(Blocks.CRT_FENCE1.get().asItem())
				.addItems(Blocks.CRT_FENCE8.get().asItem())
				.addItems(Blocks.CRT_FENCE9.get().asItem())
				.addItems(Blocks.CRT_FENCE10.get().asItem())
				.addItems(Blocks.CRT_FENCE_TICKET.get().asItem())
				.addItems(Blocks.CRT_FENCE_LIFT_TIPS_1.get().asItem())
				.addItems(Blocks.CRT_TEMP_FENCE_1.get().asItem());

		CRT_BUILDING_BLOCKS = FilterBuilder.registerFilter(crtTab, Text.translatable("FiltersGroup.nanbin.crt_building_blocks"), icon(Blocks.CRT_LOGO.get().asItem()))
				.addItems(Blocks.CRT_LOGO.get().asItem())
				.addItems(Blocks.CRT_LIFT_TIPS.get().asItem())
				.addItems(Blocks.CRT_LIFT_TIPS_2.get().asItem())
				.addItems(Blocks.CRT_LIFT_TIPS_3.get().asItem())
				.addItems(Blocks.CRT_OLD_WALL1.get().asItem())
				.addItems(Blocks.CRT_OLD_WALL2.get().asItem())
				.addItems(Blocks.CRT_PLATFORM.get().asItem())
				.addItems(Blocks.CRT_STATION_NAME_1.get().asItem())
				.addItems(Blocks.CRT_STATION_NAME_2.get().asItem())
				.addItems(Blocks.CRT_RAILWAY_SIGN_3_EVEN.get().asItem())
				.addItems(Blocks.CRT_RAILWAY_SIGN_3_ODD.get().asItem())
				.addItems(Blocks.CRT_RAILWAY_SIGN_4_EVEN.get().asItem())
				.addItems(Blocks.CRT_RAILWAY_SIGN_4_ODD.get().asItem())
				.addItems(Blocks.CRT_RAILWAY_SIGN_5_EVEN.get().asItem())
				.addItems(Blocks.CRT_RAILWAY_SIGN_5_ODD.get().asItem())
				.addItems(Blocks.CRT_RAILWAY_SIGN_6_EVEN.get().asItem())
				.addItems(Blocks.CRT_RAILWAY_SIGN_6_ODD.get().asItem())
				.addItems(Blocks.CRT_RAILWAY_SIGN_7_EVEN.get().asItem())
				.addItems(Blocks.CRT_RAILWAY_SIGN_7_ODD.get().asItem())
				.addItems(Blocks.CRT_RAILWAY_SIGN_8_EVEN.get().asItem())
				.addItems(Blocks.CRT_RAILWAY_SIGN_8_ODD.get().asItem())
				.addItems(Blocks.CRT_RAILWAY_SIGN_9_EVEN.get().asItem())
				.addItems(Blocks.CRT_RAILWAY_SIGN_9_ODD.get().asItem())
				.addItems(Blocks.CRT_RAILWAY_SIGN_10_EVEN.get().asItem())
				.addItems(Blocks.CRT_RAILWAY_SIGN_10_ODD.get().asItem())
				.addItems(Blocks.CRT_RAILWAY_SIGN_11_EVEN.get().asItem())
				.addItems(Blocks.CRT_RAILWAY_SIGN_11_ODD.get().asItem())
				.addItems(Blocks.CRT_RAILWAY_SIGN_POLE.get().asItem())
				.addItems(Blocks.CRT_STATION_INFO_1.get().asItem())
				.addItems(Blocks.CRT_STATION_INFO_1_DOUBLE.get().asItem());

		CRT_TICKETS = FilterBuilder.registerFilter(crtTab, Text.translatable("FiltersGroup.nanbin.crt_tickets"), icon(Blocks.CRT_TICKET_1_EXIT.get().asItem()))
				.addItems(Blocks.CRT_TICKET_1_ENTER.get().asItem())
				.addItems(Blocks.CRT_TICKET_1_EXIT.get().asItem())
				.addItems(Blocks.CRT_TICKET_2_ENTER.get().asItem())
				.addItems(Blocks.CRT_TICKET_2_EXIT.get().asItem())
				.addItems(Blocks.CRT_TICKET_3_ENTER.get().asItem())
				.addItems(Blocks.CRT_TICKET_3_EXIT.get().asItem())
				.addItems(Blocks.CRT_TICKET_MACHINE_1.get().asItem());

		CRT_DOOR = FilterBuilder.registerFilter(crtTab, Text.translatable("FiltersGroup.nanbin.crt_door"), icon(Blocks.CRT_APG_CAB_DOOR_NEW.get().asItem()))
				.addItems(Blocks.CRT_APG_CAB_DOOR_OLD.get().asItem())
				.addItems(Blocks.CRT_APG_CAB_FENCE_OLD.get().asItem())
				.addItems(Blocks.CRT_APG_CAB_FENCE_OLD_CONNECT.get().asItem())
				.addItems(Blocks.CRT_APG_CAB_DOOR_NEW.get().asItem())
				.addItems(Items.CRT_APG_DOOR_1.get())
				.addItems(Items.CRT_APG_GLASS_1.get())
				.addItems(Items.CRT_APG_GLASS_END_1.get())
				.addItems(Items.CRT_APG_DOOR_2.get())
				.addItems(Items.CRT_APG_GLASS_2.get())
				.addItems(Items.CRT_APG_GLASS_END_2.get());

		COMMON_BUILDING_BLOCKS = FilterBuilder.registerFilter(cityTab, Text.translatable("FiltersGroup.nanbin.common_building_blocks"), icon(Blocks.NANBIN_BLUE_BLOCK.get().asItem()))
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
				.addItems(Blocks.TERRAZZO.get().asItem())
				.addItems(Blocks.BLACK_TERRAZZO.get().asItem())
				.addItems(Blocks.CEMENT.get().asItem());

		ROAD_BLOCKS = FilterBuilder.registerFilter(cityTab, Text.translatable("FiltersGroup.nanbin.road_blocks"), icon(Blocks.YELLOW_TACTILE_BAVING.get().asItem()))
				.addItems(Blocks.PAVEMENT_1.get().asItem())
				.addItems(Blocks.PAVEMENT_1_HALF.get().asItem())
				.addItems(Blocks.PAVEMENT_2.get().asItem())
				.addItems(Blocks.PAVEMENT_2_HALF.get().asItem())
				.addItems(Blocks.PAVEMENT_3.get().asItem())
				.addItems(Blocks.PAVEMENT_3_HALF.get().asItem())
				.addItems(Blocks.PAVEMENT_4.get().asItem())
				.addItems(Blocks.PAVEMENT_4_HALF.get().asItem())
				.addItems(Blocks.DRAIN_COVER.get().asItem())
				.addItems(Blocks.GRAY_TACTILE_BAVING.get().asItem())
				.addItems(Blocks.GRAY_TACTILE_BAVING_HALF.get().asItem())
				.addItems(Blocks.GRAY_TACTILE_BAVING_CONNECT.get().asItem())
				.addItems(Blocks.GRAY_TACTILE_BAVING_CONNECT_HALF.get().asItem())
				.addItems(Blocks.YELLOW_TACTILE_BAVING.get().asItem())
				.addItems(Blocks.YELLOW_TACTILE_BAVING_HALF.get().asItem())
				.addItems(Blocks.YELLOW_TACTILE_BAVING_CONNECT.get().asItem())
				.addItems(Blocks.YELLOW_TACTILE_BAVING_CONNECT_HALF.get().asItem());

		FENCE_BLOCKS = FilterBuilder.registerFilter(cityTab, Text.translatable("FiltersGroup.nanbin.fence_blocks"), icon(Blocks.BLUEFENCE.get().asItem()))
				.addItems(Blocks.BLUEFENCE.get().asItem())
				.addItems(Blocks.GREENFENCE.get().asItem())
				.addItems(Blocks.METALFENCE.get().asItem());

		FUNCTION_BLOCKS = FilterBuilder.registerFilter(cityTab, Text.translatable("FiltersGroup.nanbin.function_blocks"), icon(Blocks.ROAD_NAME.get().asItem()))
				.addItems(Blocks.ROAD_NAME.get().asItem())
				.addItems(Blocks.BUS_TICKET_PROCESSOR.get().asItem());
	}
}
