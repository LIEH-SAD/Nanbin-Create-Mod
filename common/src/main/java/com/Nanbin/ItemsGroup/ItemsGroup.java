package com.Nanbin.ItemsGroup;

import com.Nanbin.Blocks.Blocks;
import com.Nanbin.mapping.ItemGroupHolder;
import com.Nanbin.mapping.ObjectHolder;
import com.Nanbin.mapping.Registry;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import org.mtr.registry.Items;

public class ItemsGroup {

	public static final ItemGroupHolder CITY_BUILDING_BLOCKS;
	public static final ItemGroupHolder USING_STATION_BUILDING_BLOCKS;
	public static final ItemGroupHolder USING_RAILWAY_BUILD;
	public static final ItemGroupHolder CRT;
	public static final ItemGroupHolder NRT;

	public static ObjectHolder<ItemGroup> CITY_BUILDING_BLOCKS_TAB;
	public static ObjectHolder<ItemGroup> CRT_TAB;

	static {
		CITY_BUILDING_BLOCKS = new ItemGroupHolder("city_building_blocks", () -> new ItemStack(Blocks.LIGHT_GREEN_BLOCK.get()));
		USING_STATION_BUILDING_BLOCKS = new ItemGroupHolder("using_station_building_blocks", () -> new ItemStack(Blocks.LOGO.get()));
		USING_RAILWAY_BUILD = new ItemGroupHolder("using_railway_build", () -> new ItemStack((ItemConvertible) Items.RAIL_CONNECTOR_300_ONE_WAY.get()));
		CRT = new ItemGroupHolder("crt_building_blocks", () -> new ItemStack(Blocks.CRT_LOGO.get()));
		NRT = new ItemGroupHolder("nrt_building_blocks", () -> new ItemStack(Blocks.NRT_TICKET_1_ENTER.get()));
	}

	public static void init() {
	}

	public static void registerTabs() {
		CITY_BUILDING_BLOCKS_TAB = Registry.registerItemGroup(CITY_BUILDING_BLOCKS);
		Registry.registerItemGroup(USING_STATION_BUILDING_BLOCKS);
		Registry.registerItemGroup(USING_RAILWAY_BUILD);
		CRT_TAB = Registry.registerItemGroup(CRT);
		Registry.registerItemGroup(NRT);
	}
}
