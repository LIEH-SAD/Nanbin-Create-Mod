package com.Nanbin.client.Registry;

import com.Nanbin.Blocks.Blocks;
import com.Nanbin.mapping.Registry;
import org.mtr.MTRClient;

public class BlockColor {

	public static void init() {
		Registry.registerBlockColors(
				(blockState, blockRenderView, blockPos, tintIndex) -> blockPos == null ? -1 : MTRClient.getStationColor(blockPos),
				Blocks.CRT_OLD_WALL1,
				Blocks.CRT_OLD_WALL2,
				Blocks.STATION_COLOR_CEILING,
				Blocks.STATION_COLOR_CEILING_2,
				Blocks.CRT_TICKET_1_ENTER,
				Blocks.CRT_TICKET_1_EXIT,
				Blocks.CRT_TICKET_3_ENTER,
				Blocks.CRT_TICKET_3_EXIT);
	}
}
