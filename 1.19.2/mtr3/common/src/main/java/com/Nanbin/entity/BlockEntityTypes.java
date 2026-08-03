package com.Nanbin.entity;

import com.Nanbin.Blocks.Blocks;
import com.Nanbin.Init;
import com.Nanbin.Registry.RegBlock.BlockCRTStationName1;
import com.Nanbin.Registry.RegBlock.BlockRoadName;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.registry.Registry;

public class BlockEntityTypes {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY = DeferredRegister.create(Init.MOD_ID, Registry.BLOCK_ENTITY_TYPE_KEY);

    public static final RegistrySupplier<BlockEntityType<?>> ROAD_NAME = BLOCK_ENTITY.register("road_name", () -> BlockEntityType.Builder.create(BlockRoadName.BlockEntity::new, Blocks.ROAD_NAME.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<?>> CRT_STATION_NAME_1 = BLOCK_ENTITY.register("crt_station_name_1", () -> BlockEntityType.Builder.create(BlockCRTStationName1.BlockEntity::new, Blocks.CRT_STATION_NAME_1.get()).build(null));

    public static void init() {
        BLOCK_ENTITY.register();
    }
}