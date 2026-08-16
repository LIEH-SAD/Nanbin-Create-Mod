package com.Nanbin.client.Registry;

import com.Nanbin.client.Render.*;
import com.Nanbin.entity.BlockEntityTypes;

import static org.mtr.mod.InitClient.REGISTRY_CLIENT;

public class BlockEntityRender {

    public static void init(){
        REGISTRY_CLIENT.registerBlockEntityRenderer(BlockEntityTypes.CRT_STATION_NAME_1, (dispatcher) -> new RenderCRTStationName1(dispatcher, true));
        REGISTRY_CLIENT.registerBlockEntityRenderer(BlockEntityTypes.CRT_STATION_NAME_2, (dispatcher) -> new RenderCRTStationName2(dispatcher, true));
        REGISTRY_CLIENT.registerBlockEntityRenderer(BlockEntityTypes.CRT_ORDINARY_RAILWAY_SIGN, RenderCRTOrdinaryRailwaySign::new);
        REGISTRY_CLIENT.registerBlockEntityRenderer(BlockEntityTypes.CRT_RAILWAY_SIGN_3_EVEN, (dispatcher) -> new RenderCRTRailwaySign(dispatcher));
        REGISTRY_CLIENT.registerBlockEntityRenderer(BlockEntityTypes.CRT_RAILWAY_SIGN_3_ODD, (dispatcher) -> new RenderCRTRailwaySign(dispatcher));
        REGISTRY_CLIENT.registerBlockEntityRenderer(BlockEntityTypes.CRT_RAILWAY_SIGN_4_EVEN, (dispatcher) -> new RenderCRTRailwaySign(dispatcher));
        REGISTRY_CLIENT.registerBlockEntityRenderer(BlockEntityTypes.CRT_RAILWAY_SIGN_4_ODD, (dispatcher) -> new RenderCRTRailwaySign(dispatcher));
        REGISTRY_CLIENT.registerBlockEntityRenderer(BlockEntityTypes.CRT_RAILWAY_SIGN_5_EVEN, (dispatcher) -> new RenderCRTRailwaySign(dispatcher));
        REGISTRY_CLIENT.registerBlockEntityRenderer(BlockEntityTypes.CRT_RAILWAY_SIGN_5_ODD, (dispatcher) -> new RenderCRTRailwaySign(dispatcher));
        REGISTRY_CLIENT.registerBlockEntityRenderer(BlockEntityTypes.CRT_RAILWAY_SIGN_6_EVEN, (dispatcher) -> new RenderCRTRailwaySign(dispatcher));
        REGISTRY_CLIENT.registerBlockEntityRenderer(BlockEntityTypes.CRT_RAILWAY_SIGN_6_ODD, (dispatcher) -> new RenderCRTRailwaySign(dispatcher));
        REGISTRY_CLIENT.registerBlockEntityRenderer(BlockEntityTypes.CRT_RAILWAY_SIGN_7_EVEN, (dispatcher) -> new RenderCRTRailwaySign(dispatcher));
        REGISTRY_CLIENT.registerBlockEntityRenderer(BlockEntityTypes.CRT_RAILWAY_SIGN_7_ODD, (dispatcher) -> new RenderCRTRailwaySign(dispatcher));
        REGISTRY_CLIENT.registerBlockEntityRenderer(BlockEntityTypes.CRT_RAILWAY_SIGN_8_EVEN, (dispatcher) -> new RenderCRTRailwaySign(dispatcher));
        REGISTRY_CLIENT.registerBlockEntityRenderer(BlockEntityTypes.CRT_RAILWAY_SIGN_8_ODD, (dispatcher) -> new RenderCRTRailwaySign(dispatcher));
        REGISTRY_CLIENT.registerBlockEntityRenderer(BlockEntityTypes.CRT_RAILWAY_SIGN_9_EVEN, (dispatcher) -> new RenderCRTRailwaySign(dispatcher));
        REGISTRY_CLIENT.registerBlockEntityRenderer(BlockEntityTypes.CRT_RAILWAY_SIGN_9_ODD, (dispatcher) -> new RenderCRTRailwaySign(dispatcher));
        REGISTRY_CLIENT.registerBlockEntityRenderer(BlockEntityTypes.CRT_RAILWAY_SIGN_10_EVEN, (dispatcher) -> new RenderCRTRailwaySign(dispatcher));
        REGISTRY_CLIENT.registerBlockEntityRenderer(BlockEntityTypes.CRT_RAILWAY_SIGN_10_ODD, (dispatcher) -> new RenderCRTRailwaySign(dispatcher));
        REGISTRY_CLIENT.registerBlockEntityRenderer(BlockEntityTypes.CRT_RAILWAY_SIGN_11_EVEN, (dispatcher) -> new RenderCRTRailwaySign(dispatcher));
        REGISTRY_CLIENT.registerBlockEntityRenderer(BlockEntityTypes.CRT_RAILWAY_SIGN_11_ODD, (dispatcher) -> new RenderCRTRailwaySign(dispatcher));
        REGISTRY_CLIENT.registerBlockEntityRenderer(BlockEntityTypes.BLOCK_ROAD_NAME, RenderRoadNameBlock::new);
        REGISTRY_CLIENT.registerBlockEntityRenderer(BlockEntityTypes.CRT_APG_DOOR_1, (dispatcher) -> new RenderCRTAPGDoor1<>(dispatcher, 2));
        REGISTRY_CLIENT.registerBlockEntityRenderer(BlockEntityTypes.CRT_APG_DOOR_2, (dispatcher) -> new RenderCRTAPGDoor2<>(dispatcher, 2));
        REGISTRY_CLIENT.registerBlockEntityRenderer(BlockEntityTypes.CRT_STATION_INFO_1, RenderCRTStationInfo1::new);
    }
}
