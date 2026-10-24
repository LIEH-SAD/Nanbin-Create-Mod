package com.Nanbin.client.Registry;

import com.Nanbin.Blocks.Blocks;
import com.Nanbin.mapping.Registry;
import net.minecraft.client.render.RenderLayer;

/**
 * 方块渲染层注册（4.0 用 {@code BlockRenderLayerHelper} 攒表，4.1 直接转发给 MTR 的
 * {@code RegistryClient.registerBlockRenderType}，两端差异由 MTR 内部处理）。
 */
public class RenderLayerReg {

	public static void init() {
		put(Blocks.CRT_LIFT_TIPS);
		put(Blocks.CRT_LIFT_TIPS_2);
		put(Blocks.CRT_LIFT_TIPS_3);
		put(Blocks.LOGO);
		put(Blocks.CRT_FENCE1);
		put(Blocks.CRT_FENCE8);
		put(Blocks.CRT_FENCE9);
		put(Blocks.CRT_FENCE10);
		put(Blocks.CRT_FENCE_TICKET);
		put(Blocks.CRT_FENCE_TICKET_FLIPPED);
		put(Blocks.CRT_TICKET_1_ENTER);
		put(Blocks.CRT_TICKET_1_EXIT);
		put(Blocks.CRT_TICKET_2_ENTER);
		put(Blocks.CRT_TICKET_2_EXIT);
		put(Blocks.CRT_TICKET_3_ENTER);
		put(Blocks.CRT_TICKET_3_EXIT);
		put(Blocks.CRT_TICKET_MACHINE_1);
		put(Blocks.BLUEFENCE);
		put(Blocks.BLUEFENCE_TOP);
		put(Blocks.GREENFENCE);
		put(Blocks.GREENFENCE_TOP);
		put(Blocks.METALFENCE);
		put(Blocks.METALFENCE_TOP);
		put(Blocks.ORDINARY_PSD_CAB_DOOR);
		put(Blocks.CRT_APG_CAB_DOOR_OLD);
		put(Blocks.CRT_APG_CAB_FENCE_OLD);
		put(Blocks.CRT_APG_CAB_FENCE_OLD_CONNECT);
		put(Blocks.CRT_APG_CAB_DOOR_NEW);
		put(Blocks.CRT_PSD_CAB_DOOR);
		put(Blocks.CRT_PSD_TOP);
		put(Blocks.CRT_PSD_TOP_CONNECT);
		put(Blocks.BUS_TICKET_PROCESSOR);
		put(Blocks.CRT_STATION_INFO_1);
		put(Blocks.CRT_STATION_INFO_1_DOUBLE);
		put(Blocks.ORDINARY_STATION_NAME);
		put(Blocks.CRT_APG_DOOR_1);
		put(Blocks.CRT_APG_GLASS_1);
		put(Blocks.CRT_APG_GLASS_END_1);
		put(Blocks.CRT_APG_DOOR_2);
		put(Blocks.CRT_APG_GLASS_2);
		put(Blocks.CRT_APG_GLASS_END_2);
		put(Blocks.SOUNDPROOFNET_GLASS);
		put(Blocks.SOUNDPROOFNET_GLASS_2);
		put(Blocks.CRT_FENCE_LIFT_TIPS_1);
		put(Blocks.NRT_TICKET_1_ENTER);
		put(Blocks.NRT_TICKET_1_EXIT);
	}

	private static void put(com.Nanbin.mapping.ObjectHolder<net.minecraft.block.Block> block) {
		Registry.registerBlockRenderLayer(RenderLayer.getCutout(), block);
	}
}
