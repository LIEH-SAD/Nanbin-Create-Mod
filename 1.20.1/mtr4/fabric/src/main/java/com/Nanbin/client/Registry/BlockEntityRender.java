package com.Nanbin.client.Registry;

import com.Nanbin.entity.BlockEntityTypes;
import com.Nanbin.client.Render.RenderCRTStationName1;
import com.Nanbin.client.Render.RenderPSDTOP;

import static org.mtr.mod.InitClient.REGISTRY_CLIENT;

public class BlockEntityRender {

    public static void init(){
        REGISTRY_CLIENT.registerBlockEntityRenderer(BlockEntityTypes.CRT_STATION_NAME_1, (dispatcher) -> new RenderCRTStationName1(dispatcher, true));
        REGISTRY_CLIENT.registerBlockEntityRenderer(BlockEntityTypes.PSD_TOP, RenderPSDTOP::new);
    }
}
