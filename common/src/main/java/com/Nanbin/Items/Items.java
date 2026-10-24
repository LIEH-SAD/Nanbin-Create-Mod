package com.Nanbin.Items;

import com.Nanbin.Blocks.Blocks;
import com.Nanbin.ItemsGroup.ItemsGroup;
import com.Nanbin.Registry.RegItem.ItemCRTAPG;
import com.Nanbin.Registry.RegItem.ItemCRTEscalator;
import com.Nanbin.Registry.RegItem.ItemPhone;
import com.Nanbin.mapping.ObjectHolder;
import com.Nanbin.mapping.Registry;
import net.minecraft.block.Block;
import net.minecraft.item.Item;

public class Items {

	public static final ObjectHolder<Item> CRT_ESCALATOR;
	public static final ObjectHolder<Item> PHONE;
	public static final ObjectHolder<Item> CRT_APG_DOOR_1;
	public static final ObjectHolder<Item> CRT_APG_GLASS_1;
	public static final ObjectHolder<Item> CRT_APG_GLASS_END_1;
	public static final ObjectHolder<Item> CRT_APG_DOOR_2;
	public static final ObjectHolder<Item> CRT_APG_GLASS_2;
	public static final ObjectHolder<Item> CRT_APG_GLASS_END_2;

	static {
		CRT_ESCALATOR = Registry.registerItem("crt_escalator", settings -> new ItemCRTEscalator(settings), ItemsGroup.CRT);
		PHONE = Registry.registerItem("phone", ItemPhone::new, ItemsGroup.USING_STATION_BUILDING_BLOCKS);
		CRT_APG_DOOR_1 = apg("crt_apg_door_1", Blocks.CRT_APG_DOOR_1);
		CRT_APG_GLASS_1 = apg("crt_apg_glass_1", Blocks.CRT_APG_GLASS_1);
		CRT_APG_GLASS_END_1 = apg("crt_apg_glass_end_1", Blocks.CRT_APG_GLASS_END_1);
		CRT_APG_DOOR_2 = apg("crt_apg_door_2", Blocks.CRT_APG_DOOR_2);
		CRT_APG_GLASS_2 = apg("crt_apg_glass_2", Blocks.CRT_APG_GLASS_2);
		CRT_APG_GLASS_END_2 = apg("crt_apg_glass_end_2", Blocks.CRT_APG_GLASS_END_2);
	}

	private static ObjectHolder<Item> apg(String path, ObjectHolder<Block> block) {
		return Registry.registerItem(path, settings -> new ItemCRTAPG(block.get(), settings), ItemsGroup.CRT);
	}

	public static void init() {
	}
}
