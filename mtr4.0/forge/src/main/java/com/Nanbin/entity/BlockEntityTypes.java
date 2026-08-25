package com.Nanbin.entity;

import com.Nanbin.Blocks.Blocks;
import com.Nanbin.Init;
import com.Nanbin.Registry.RegBlock.*;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.registry.BlockEntityTypeRegistryObject;

public class BlockEntityTypes {
    public static final BlockEntityTypeRegistryObject<BlockCRTStationName1.BlockEntity> CRT_STATION_NAME_1;
    public static final BlockEntityTypeRegistryObject<BlockCRTStationName2.BlockEntity> CRT_STATION_NAME_2;
    public static final BlockEntityTypeRegistryObject<BlockOrdinaryStationName.BlockEntity> ORDINARY_STATION_NAME;
    public static final BlockEntityTypeRegistryObject<BlockCRTRailwaySign.BlockEntityCRTRailwaySign> CRT_RAILWAY_SIGN_3_EVEN;
    public static final BlockEntityTypeRegistryObject<BlockCRTRailwaySign.BlockEntityCRTRailwaySign> CRT_RAILWAY_SIGN_3_ODD;
    public static final BlockEntityTypeRegistryObject<BlockCRTRailwaySign.BlockEntityCRTRailwaySign> CRT_RAILWAY_SIGN_4_EVEN;
    public static final BlockEntityTypeRegistryObject<BlockCRTRailwaySign.BlockEntityCRTRailwaySign> CRT_RAILWAY_SIGN_4_ODD;
    public static final BlockEntityTypeRegistryObject<BlockCRTRailwaySign.BlockEntityCRTRailwaySign> CRT_RAILWAY_SIGN_5_EVEN;
    public static final BlockEntityTypeRegistryObject<BlockCRTRailwaySign.BlockEntityCRTRailwaySign> CRT_RAILWAY_SIGN_5_ODD;
    public static final BlockEntityTypeRegistryObject<BlockCRTRailwaySign.BlockEntityCRTRailwaySign> CRT_RAILWAY_SIGN_6_EVEN;
    public static final BlockEntityTypeRegistryObject<BlockCRTRailwaySign.BlockEntityCRTRailwaySign> CRT_RAILWAY_SIGN_6_ODD;
    public static final BlockEntityTypeRegistryObject<BlockCRTRailwaySign.BlockEntityCRTRailwaySign> CRT_RAILWAY_SIGN_7_EVEN;
    public static final BlockEntityTypeRegistryObject<BlockCRTRailwaySign.BlockEntityCRTRailwaySign> CRT_RAILWAY_SIGN_7_ODD;
    public static final BlockEntityTypeRegistryObject<BlockCRTRailwaySign.BlockEntityCRTRailwaySign> CRT_RAILWAY_SIGN_8_EVEN;
    public static final BlockEntityTypeRegistryObject<BlockCRTRailwaySign.BlockEntityCRTRailwaySign> CRT_RAILWAY_SIGN_8_ODD;
    public static final BlockEntityTypeRegistryObject<BlockCRTRailwaySign.BlockEntityCRTRailwaySign> CRT_RAILWAY_SIGN_9_EVEN;
    public static final BlockEntityTypeRegistryObject<BlockCRTRailwaySign.BlockEntityCRTRailwaySign> CRT_RAILWAY_SIGN_9_ODD;
    public static final BlockEntityTypeRegistryObject<BlockCRTRailwaySign.BlockEntityCRTRailwaySign> CRT_RAILWAY_SIGN_10_EVEN;
    public static final BlockEntityTypeRegistryObject<BlockCRTRailwaySign.BlockEntityCRTRailwaySign> CRT_RAILWAY_SIGN_10_ODD;
    public static final BlockEntityTypeRegistryObject<BlockCRTRailwaySign.BlockEntityCRTRailwaySign> CRT_RAILWAY_SIGN_11_EVEN;
    public static final BlockEntityTypeRegistryObject<BlockCRTRailwaySign.BlockEntityCRTRailwaySign> CRT_RAILWAY_SIGN_11_ODD;
    public static final BlockEntityTypeRegistryObject<BlockPSDTOP.BlockEntity> PSD_TOP;
    public static final BlockEntityTypeRegistryObject<BlockRoadName.BlockEntity> BLOCK_ROAD_NAME;
    public static final BlockEntityTypeRegistryObject<BlockCRTAPGDoor1.BlockEntity> CRT_APG_DOOR_1;
    public static final BlockEntityTypeRegistryObject<BlockCRTAPGDoor2.BlockEntity> CRT_APG_DOOR_2;
    public static final BlockEntityTypeRegistryObject<BlockBusTicketProcessor.BlockEntity> BUS_TICKET_PROCESSOR;
    public static final BlockEntityTypeRegistryObject<BlockCRTStationInfo1.BlockEntity> CRT_STATION_INFO_1;
    public static final BlockEntityTypeRegistryObject<BlockCRTStationInfo1Double.BlockEntity> CRT_STATION_INFO_1_DOUBLE;

    static {
        CRT_STATION_NAME_1 = Init.REGISTRY.registerBlockEntityType(new Identifier("nanbin", "crt_station_name_1"), BlockCRTStationName1.BlockEntity::new, Blocks.CRT_STATION_NAME_1::get);
        CRT_STATION_NAME_2 = Init.REGISTRY.registerBlockEntityType(new Identifier("nanbin", "crt_station_name_2"), BlockCRTStationName2.BlockEntity::new, Blocks.CRT_STATION_NAME_2::get);
        ORDINARY_STATION_NAME = Init.REGISTRY.registerBlockEntityType(new Identifier("nanbin", "ordinary_station_name"), BlockOrdinaryStationName.BlockEntity::new, Blocks.ORDINARY_STATION_NAME::get);
        CRT_RAILWAY_SIGN_3_EVEN = Init.REGISTRY.registerBlockEntityType(new Identifier("nanbin", "crt_railway_sign_3_even"), (pos, state) -> new BlockCRTRailwaySign.BlockEntityCRTRailwaySign(3, false, pos, state), Blocks.CRT_RAILWAY_SIGN_3_EVEN::get);
        CRT_RAILWAY_SIGN_3_ODD = Init.REGISTRY.registerBlockEntityType(new Identifier("nanbin", "crt_railway_sign_3_odd"), (pos, state) -> new BlockCRTRailwaySign.BlockEntityCRTRailwaySign(3, true, pos, state), Blocks.CRT_RAILWAY_SIGN_3_ODD::get);
        CRT_RAILWAY_SIGN_4_EVEN = Init.REGISTRY.registerBlockEntityType(new Identifier("nanbin", "crt_railway_sign_4_even"), (pos, state) -> new BlockCRTRailwaySign.BlockEntityCRTRailwaySign(4, false, pos, state), Blocks.CRT_RAILWAY_SIGN_4_EVEN::get);
        CRT_RAILWAY_SIGN_4_ODD = Init.REGISTRY.registerBlockEntityType(new Identifier("nanbin", "crt_railway_sign_4_odd"), (pos, state) -> new BlockCRTRailwaySign.BlockEntityCRTRailwaySign(4, true, pos, state), Blocks.CRT_RAILWAY_SIGN_4_ODD::get);
        CRT_RAILWAY_SIGN_5_EVEN = Init.REGISTRY.registerBlockEntityType(new Identifier("nanbin", "crt_railway_sign_5_even"), (pos, state) -> new BlockCRTRailwaySign.BlockEntityCRTRailwaySign(5, false, pos, state), Blocks.CRT_RAILWAY_SIGN_5_EVEN::get);
        CRT_RAILWAY_SIGN_5_ODD = Init.REGISTRY.registerBlockEntityType(new Identifier("nanbin", "crt_railway_sign_5_odd"), (pos, state) -> new BlockCRTRailwaySign.BlockEntityCRTRailwaySign(5, true, pos, state), Blocks.CRT_RAILWAY_SIGN_5_ODD::get);
        CRT_RAILWAY_SIGN_6_EVEN = Init.REGISTRY.registerBlockEntityType(new Identifier("nanbin", "crt_railway_sign_6_even"), (pos, state) -> new BlockCRTRailwaySign.BlockEntityCRTRailwaySign(6, false, pos, state), Blocks.CRT_RAILWAY_SIGN_6_EVEN::get);
        CRT_RAILWAY_SIGN_6_ODD = Init.REGISTRY.registerBlockEntityType(new Identifier("nanbin", "crt_railway_sign_6_odd"), (pos, state) -> new BlockCRTRailwaySign.BlockEntityCRTRailwaySign(6, true, pos, state), Blocks.CRT_RAILWAY_SIGN_6_ODD::get);
        CRT_RAILWAY_SIGN_7_EVEN = Init.REGISTRY.registerBlockEntityType(new Identifier("nanbin", "crt_railway_sign_7_even"), (pos, state) -> new BlockCRTRailwaySign.BlockEntityCRTRailwaySign(7, false, pos, state), Blocks.CRT_RAILWAY_SIGN_7_EVEN::get);
        CRT_RAILWAY_SIGN_7_ODD = Init.REGISTRY.registerBlockEntityType(new Identifier("nanbin", "crt_railway_sign_7_odd"), (pos, state) -> new BlockCRTRailwaySign.BlockEntityCRTRailwaySign(7, true, pos, state), Blocks.CRT_RAILWAY_SIGN_7_ODD::get);
        CRT_RAILWAY_SIGN_8_EVEN = Init.REGISTRY.registerBlockEntityType(new Identifier("nanbin", "crt_railway_sign_8_even"), (pos, state) -> new BlockCRTRailwaySign.BlockEntityCRTRailwaySign(8, false, pos, state), Blocks.CRT_RAILWAY_SIGN_8_EVEN::get);
        CRT_RAILWAY_SIGN_8_ODD = Init.REGISTRY.registerBlockEntityType(new Identifier("nanbin", "crt_railway_sign_8_odd"), (pos, state) -> new BlockCRTRailwaySign.BlockEntityCRTRailwaySign(8, true, pos, state), Blocks.CRT_RAILWAY_SIGN_8_ODD::get);
        CRT_RAILWAY_SIGN_9_EVEN = Init.REGISTRY.registerBlockEntityType(new Identifier("nanbin", "crt_railway_sign_9_even"), (pos, state) -> new BlockCRTRailwaySign.BlockEntityCRTRailwaySign(9, false, pos, state), Blocks.CRT_RAILWAY_SIGN_9_EVEN::get);
        CRT_RAILWAY_SIGN_9_ODD = Init.REGISTRY.registerBlockEntityType(new Identifier("nanbin", "crt_railway_sign_9_odd"), (pos, state) -> new BlockCRTRailwaySign.BlockEntityCRTRailwaySign(9, true, pos, state), Blocks.CRT_RAILWAY_SIGN_9_ODD::get);
        CRT_RAILWAY_SIGN_10_EVEN = Init.REGISTRY.registerBlockEntityType(new Identifier("nanbin", "crt_railway_sign_10_even"), (pos, state) -> new BlockCRTRailwaySign.BlockEntityCRTRailwaySign(10, false, pos, state), Blocks.CRT_RAILWAY_SIGN_10_EVEN::get);
        CRT_RAILWAY_SIGN_10_ODD = Init.REGISTRY.registerBlockEntityType(new Identifier("nanbin", "crt_railway_sign_10_odd"), (pos, state) -> new BlockCRTRailwaySign.BlockEntityCRTRailwaySign(10, true, pos, state), Blocks.CRT_RAILWAY_SIGN_10_ODD::get);
        CRT_RAILWAY_SIGN_11_EVEN = Init.REGISTRY.registerBlockEntityType(new Identifier("nanbin", "crt_railway_sign_11_even"), (pos, state) -> new BlockCRTRailwaySign.BlockEntityCRTRailwaySign(11, false, pos, state), Blocks.CRT_RAILWAY_SIGN_11_EVEN::get);
        CRT_RAILWAY_SIGN_11_ODD = Init.REGISTRY.registerBlockEntityType(new Identifier("nanbin", "crt_railway_sign_11_odd"), (pos, state) -> new BlockCRTRailwaySign.BlockEntityCRTRailwaySign(11, true, pos, state), Blocks.CRT_RAILWAY_SIGN_11_ODD::get);
        PSD_TOP = Init.REGISTRY.registerBlockEntityType(new Identifier("nanbin", "psd_top"), BlockPSDTOP.BlockEntity::new, Blocks.PSD_TOP::get);
        BLOCK_ROAD_NAME = Init.REGISTRY.registerBlockEntityType(new Identifier("nanbin", "road_name_block"), BlockRoadName.BlockEntity::new, Blocks.ROAD_NAME::get);
        CRT_APG_DOOR_1 = Init.REGISTRY.registerBlockEntityType(new Identifier("nanbin", "crt_apg_door_1"), BlockCRTAPGDoor1.BlockEntity::new, Blocks.CRT_APG_DOOR_1::get);
        CRT_APG_DOOR_2 = Init.REGISTRY.registerBlockEntityType(new Identifier("nanbin", "crt_apg_door_2"), BlockCRTAPGDoor2.BlockEntity::new, Blocks.CRT_APG_DOOR_2::get);
        BUS_TICKET_PROCESSOR = Init.REGISTRY.registerBlockEntityType(new Identifier("nanbin", "bus_ticket_processor"), BlockBusTicketProcessor.BlockEntity::new, Blocks.BUS_TICKET_PROCESSOR::get);
        CRT_STATION_INFO_1 = Init.REGISTRY.registerBlockEntityType(new Identifier("nanbin", "crt_station_info_1"), BlockCRTStationInfo1.BlockEntity::new, Blocks.CRT_STATION_INFO_1::get);
        CRT_STATION_INFO_1_DOUBLE = Init.REGISTRY.registerBlockEntityType(new Identifier("nanbin", "crt_station_info_1_double"), BlockCRTStationInfo1Double.BlockEntity::new, Blocks.CRT_STATION_INFO_1_DOUBLE::get);
    }

    public static BlockEntityTypeRegistryObject<BlockCRTRailwaySign.BlockEntityCRTRailwaySign> getRailwaySignType(int length, boolean isOdd) {
        switch (length) {
            case 3:
                return isOdd ? CRT_RAILWAY_SIGN_3_ODD : CRT_RAILWAY_SIGN_3_EVEN;
            case 4:
                return isOdd ? CRT_RAILWAY_SIGN_4_ODD : CRT_RAILWAY_SIGN_4_EVEN;
            case 5:
                return isOdd ? CRT_RAILWAY_SIGN_5_ODD : CRT_RAILWAY_SIGN_5_EVEN;
            case 6:
                return isOdd ? CRT_RAILWAY_SIGN_6_ODD : CRT_RAILWAY_SIGN_6_EVEN;
            case 7:
                return isOdd ? CRT_RAILWAY_SIGN_7_ODD : CRT_RAILWAY_SIGN_7_EVEN;
            case 8:
                return isOdd ? CRT_RAILWAY_SIGN_8_ODD : CRT_RAILWAY_SIGN_8_EVEN;
            case 9:
                return isOdd ? CRT_RAILWAY_SIGN_9_ODD : CRT_RAILWAY_SIGN_9_EVEN;
            case 10:
                return isOdd ? CRT_RAILWAY_SIGN_10_ODD : CRT_RAILWAY_SIGN_10_EVEN;
            default:    //不是这些就是11
                return isOdd ? CRT_RAILWAY_SIGN_11_ODD : CRT_RAILWAY_SIGN_11_EVEN;
        }
    }

    public static void init() {}
}