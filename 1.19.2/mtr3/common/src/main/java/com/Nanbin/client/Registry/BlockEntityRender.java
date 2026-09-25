package com.Nanbin.client.Registry;

import com.Nanbin.Registry.RegBlock.*;
import com.Nanbin.client.Render.*;
import com.Nanbin.entity.BlockEntityTypes;
import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import net.minecraft.block.entity.BlockEntityType;

public class BlockEntityRender {

    public static void init(){
        BlockEntityRendererRegistry.register((BlockEntityType<BlockRoadName.BlockEntity>) BlockEntityTypes.ROAD_NAME.get(), RenderRoadNameBlock::new);
        BlockEntityRendererRegistry.register((BlockEntityType<BlockCRTStationName1.BlockEntity>) BlockEntityTypes.CRT_STATION_NAME_1.get(), RenderCRTStationName1::new);
        BlockEntityRendererRegistry.register((BlockEntityType<BlockCRTStationName2.BlockEntity>) BlockEntityTypes.CRT_STATION_NAME_2.get(), RenderCRTStationName2::new);
        BlockEntityRendererRegistry.register((BlockEntityType<BlockOrdinaryStationName.BlockEntity>) BlockEntityTypes.ORDINARY_STATION_NAME.get(), RenderOrdinaryStationName::new);
        BlockEntityRendererRegistry.register((BlockEntityType<BlockCRTStationInfo1.BlockEntity>) BlockEntityTypes.CRT_STATION_INFO_1.get(), RenderCRTStationInfo1::new);
        BlockEntityRendererRegistry.register((BlockEntityType<BlockCRTStationInfo1Double.BlockEntity>) BlockEntityTypes.CRT_STATION_INFO_1_DOUBLE.get(), RenderCRTStationInfo1Double::new);
        BlockEntityRendererRegistry.register((BlockEntityType<BlockCRTAPGDoor1.BlockEntity>) BlockEntityTypes.CRT_APG_DOOR_1.get(), RenderCRTAPGDoor1::new);
        BlockEntityRendererRegistry.register((BlockEntityType<BlockCRTAPGDoor2.BlockEntity>) BlockEntityTypes.CRT_APG_DOOR_2.get(), RenderCRTAPGDoor2::new);

        // CRT 铁路告示牌（长度 3-11，偶/奇）
        registerRailwaySignRenderers();
    }

    private static void registerRailwaySignRenderers() {
        final BlockEntityType<?>[] types = {
                BlockEntityTypes.CRT_RAILWAY_SIGN_3_EVEN.get(), BlockEntityTypes.CRT_RAILWAY_SIGN_3_ODD.get(),
                BlockEntityTypes.CRT_RAILWAY_SIGN_4_EVEN.get(), BlockEntityTypes.CRT_RAILWAY_SIGN_4_ODD.get(),
                BlockEntityTypes.CRT_RAILWAY_SIGN_5_EVEN.get(), BlockEntityTypes.CRT_RAILWAY_SIGN_5_ODD.get(),
                BlockEntityTypes.CRT_RAILWAY_SIGN_6_EVEN.get(), BlockEntityTypes.CRT_RAILWAY_SIGN_6_ODD.get(),
                BlockEntityTypes.CRT_RAILWAY_SIGN_7_EVEN.get(), BlockEntityTypes.CRT_RAILWAY_SIGN_7_ODD.get(),
                BlockEntityTypes.CRT_RAILWAY_SIGN_8_EVEN.get(), BlockEntityTypes.CRT_RAILWAY_SIGN_8_ODD.get(),
                BlockEntityTypes.CRT_RAILWAY_SIGN_9_EVEN.get(), BlockEntityTypes.CRT_RAILWAY_SIGN_9_ODD.get(),
                BlockEntityTypes.CRT_RAILWAY_SIGN_10_EVEN.get(), BlockEntityTypes.CRT_RAILWAY_SIGN_10_ODD.get(),
                BlockEntityTypes.CRT_RAILWAY_SIGN_11_EVEN.get(), BlockEntityTypes.CRT_RAILWAY_SIGN_11_ODD.get(),
        };
        for (final BlockEntityType<?> type : types) {
            BlockEntityRendererRegistry.register((BlockEntityType<BlockCRTRailwaySign.BlockEntityCRTRailwaySign>) type, RenderCRTRailwaySign::new);
        }
    }
}
