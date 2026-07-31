package com.Nanbin.client.Registry;

import com.Nanbin.client.Render.RenderCRTStationName1;
import com.Nanbin.entity.BlockEntityTypes;

import static org.mtr.mod.InitClient.REGISTRY_CLIENT;

public class BlockEntityRender {

    public static void init(){
        REGISTRY_CLIENT.registerBlockEntityRenderer(BlockEntityTypes.CRT_STATION_NAME_1, (dispatcher) -> new RenderCRTStationName1(dispatcher, true));
    }
}
