package com.Nanbin.entity;

import com.Nanbin.Blocks.Blocks;
import com.Nanbin.Init;
import com.Nanbin.Registry.RegBlock.BlockCRTStationName1;
import com.Nanbin.Registry.RegBlock.BlockPSDTOP;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.registry.BlockEntityTypeRegistryObject;

public class BlockEntityTypes {
    public static final BlockEntityTypeRegistryObject<BlockCRTStationName1.BlockEntity> CRT_STATION_NAME_1;
    public static final BlockEntityTypeRegistryObject<BlockPSDTOP.BlockEntity> PSD_TOP;

    static {
        CRT_STATION_NAME_1 = Init.REGISTRY.registerBlockEntityType(new Identifier("nanbin", "crt_station_name_1"), BlockCRTStationName1.BlockEntity::new, Blocks.CRT_STATION_NAME_1::get);
        PSD_TOP = Init.REGISTRY.registerBlockEntityType(new Identifier("nanbin", "psd_top"), BlockPSDTOP.BlockEntity::new, Blocks.PSD_TOP::get);
    }

    public static void init() {}
}
