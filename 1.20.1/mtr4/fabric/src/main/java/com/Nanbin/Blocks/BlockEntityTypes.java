package com.Nanbin.Blocks;

import org.mtr.mapping.holder.*;
import org.mtr.mapping.registry.BlockEntityTypeRegistryObject;
import org.mtr.mod.block.BlockStationNameWallWhite;

import static com.Nanbin.Init.REGISTRY;


public class BlockEntityTypes {
    public static final BlockEntityTypeRegistryObject<BlockStationNameWallWhite.BlockEntity> CRT;

    static {
        CRT = REGISTRY.registerBlockEntityType(
                new Identifier("nanbin", "crt_logo"),
                BlockStationNameWallWhite.BlockEntity::new,
                Blocks.CRT_LOGO::get);
    }

    public static void init() {}
}
