package com.Nanbin.client.Registry;

import com.Nanbin.Blocks.Blocks;
import org.mtr.core.data.Station;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.registry.BlockRegistryObject;

import javax.annotation.Nullable;

import static org.mtr.mod.InitClient.REGISTRY_CLIENT;
import static org.mtr.mod.InitClient.getStationColor;

public class BlockColor {
    public static void init(){
        REGISTRY_CLIENT.registerBlockColors((blockState, blockRenderView, blockPos, tintIndex) -> getStationColor(blockPos), new BlockRegistryObject[]{
                Blocks.CRT_OLD_WALL1,
                Blocks.CRT_OLD_WALL2,
                Blocks.STATION_COLOR_CEILING,
                Blocks.STATION_COLOR_CEILING_2,
                Blocks.CRT_TICKET_1_ENTER,
                Blocks.CRT_TICKET_1_EXIT,
                Blocks.CRT_TICKET_3_ENTER,
                Blocks.CRT_TICKET_3_EXIT
        });
    }
}
