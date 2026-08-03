package com.Nanbin.client.Registry;

import com.Nanbin.Registry.RegBlock.BlockCRTStationName1;
import com.Nanbin.Registry.RegBlock.BlockRoadName;
import com.Nanbin.client.Render.RenderCRTStationName1;
import com.Nanbin.client.Render.RenderRoadNameBlock;
import com.Nanbin.entity.BlockEntityTypes;
import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import net.minecraft.block.entity.BlockEntityType;

public class BlockEntityRender {

    public static void init(){
        BlockEntityRendererRegistry.register((BlockEntityType<BlockRoadName.BlockEntity>) BlockEntityTypes.ROAD_NAME.get(), RenderRoadNameBlock::new);
        BlockEntityRendererRegistry.register((BlockEntityType<BlockCRTStationName1.BlockEntity>) BlockEntityTypes.CRT_STATION_NAME_1.get(), RenderCRTStationName1::new);
    }
}
