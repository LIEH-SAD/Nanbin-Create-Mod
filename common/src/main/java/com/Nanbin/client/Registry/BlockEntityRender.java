package com.Nanbin.client.Registry;

import com.Nanbin.client.Render.RenderCRTAPGDoor1;
import com.Nanbin.client.Render.RenderCRTAPGDoor2;
import com.Nanbin.client.Render.RenderCRTRailwaySign;
import com.Nanbin.client.Render.RenderCRTStationInfo1;
import com.Nanbin.client.Render.RenderCRTStationInfo1Double;
import com.Nanbin.client.Render.RenderCRTStationName1;
import com.Nanbin.client.Render.RenderCRTStationName2;
import com.Nanbin.client.Render.RenderOrdinaryStationName;
import com.Nanbin.client.Render.RenderRoadNameBlock;
import com.Nanbin.entity.BlockEntityTypes;
import com.Nanbin.mapping.Registry;

public class BlockEntityRender {

	public static void init() {
		Registry.registerBlockEntityRenderer(BlockEntityTypes.CRT_STATION_NAME_1, dispatcher -> new RenderCRTStationName1(true));
		Registry.registerBlockEntityRenderer(BlockEntityTypes.CRT_STATION_NAME_2, dispatcher -> new RenderCRTStationName2(true));
		Registry.registerBlockEntityRenderer(BlockEntityTypes.ORDINARY_STATION_NAME, dispatcher -> new RenderOrdinaryStationName());
		Registry.registerBlockEntityRenderer(BlockEntityTypes.BLOCK_ROAD_NAME, dispatcher -> new RenderRoadNameBlock());
		Registry.registerBlockEntityRenderer(BlockEntityTypes.CRT_RAILWAY_SIGN_3_EVEN, dispatcher -> new RenderCRTRailwaySign());
		Registry.registerBlockEntityRenderer(BlockEntityTypes.CRT_RAILWAY_SIGN_3_ODD, dispatcher -> new RenderCRTRailwaySign());
		Registry.registerBlockEntityRenderer(BlockEntityTypes.CRT_RAILWAY_SIGN_4_EVEN, dispatcher -> new RenderCRTRailwaySign());
		Registry.registerBlockEntityRenderer(BlockEntityTypes.CRT_RAILWAY_SIGN_4_ODD, dispatcher -> new RenderCRTRailwaySign());
		Registry.registerBlockEntityRenderer(BlockEntityTypes.CRT_RAILWAY_SIGN_5_EVEN, dispatcher -> new RenderCRTRailwaySign());
		Registry.registerBlockEntityRenderer(BlockEntityTypes.CRT_RAILWAY_SIGN_5_ODD, dispatcher -> new RenderCRTRailwaySign());
		Registry.registerBlockEntityRenderer(BlockEntityTypes.CRT_RAILWAY_SIGN_6_EVEN, dispatcher -> new RenderCRTRailwaySign());
		Registry.registerBlockEntityRenderer(BlockEntityTypes.CRT_RAILWAY_SIGN_6_ODD, dispatcher -> new RenderCRTRailwaySign());
		Registry.registerBlockEntityRenderer(BlockEntityTypes.CRT_RAILWAY_SIGN_7_EVEN, dispatcher -> new RenderCRTRailwaySign());
		Registry.registerBlockEntityRenderer(BlockEntityTypes.CRT_RAILWAY_SIGN_7_ODD, dispatcher -> new RenderCRTRailwaySign());
		Registry.registerBlockEntityRenderer(BlockEntityTypes.CRT_RAILWAY_SIGN_8_EVEN, dispatcher -> new RenderCRTRailwaySign());
		Registry.registerBlockEntityRenderer(BlockEntityTypes.CRT_RAILWAY_SIGN_8_ODD, dispatcher -> new RenderCRTRailwaySign());
		Registry.registerBlockEntityRenderer(BlockEntityTypes.CRT_RAILWAY_SIGN_9_EVEN, dispatcher -> new RenderCRTRailwaySign());
		Registry.registerBlockEntityRenderer(BlockEntityTypes.CRT_RAILWAY_SIGN_9_ODD, dispatcher -> new RenderCRTRailwaySign());
		Registry.registerBlockEntityRenderer(BlockEntityTypes.CRT_RAILWAY_SIGN_10_EVEN, dispatcher -> new RenderCRTRailwaySign());
		Registry.registerBlockEntityRenderer(BlockEntityTypes.CRT_RAILWAY_SIGN_10_ODD, dispatcher -> new RenderCRTRailwaySign());
		Registry.registerBlockEntityRenderer(BlockEntityTypes.CRT_RAILWAY_SIGN_11_EVEN, dispatcher -> new RenderCRTRailwaySign());
		Registry.registerBlockEntityRenderer(BlockEntityTypes.CRT_RAILWAY_SIGN_11_ODD, dispatcher -> new RenderCRTRailwaySign());
		Registry.registerBlockEntityRenderer(BlockEntityTypes.CRT_STATION_INFO_1, dispatcher -> new RenderCRTStationInfo1());
		Registry.registerBlockEntityRenderer(BlockEntityTypes.CRT_STATION_INFO_1_DOUBLE, dispatcher -> new RenderCRTStationInfo1Double());
		Registry.registerBlockEntityRenderer(BlockEntityTypes.CRT_APG_DOOR_1, dispatcher -> new RenderCRTAPGDoor1());
		Registry.registerBlockEntityRenderer(BlockEntityTypes.CRT_APG_DOOR_2, dispatcher -> new RenderCRTAPGDoor2());
	}
}
