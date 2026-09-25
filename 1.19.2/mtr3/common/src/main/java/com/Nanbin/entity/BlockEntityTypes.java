package com.Nanbin.entity;

import com.Nanbin.Blocks.Blocks;
import com.Nanbin.Init;
import com.Nanbin.Registry.RegBlock.*;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.registry.Registry;

public class BlockEntityTypes {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY = DeferredRegister.create(Init.MOD_ID, Registry.BLOCK_ENTITY_TYPE_KEY);

    public static final RegistrySupplier<BlockEntityType<?>> ROAD_NAME = BLOCK_ENTITY.register("road_name", () -> BlockEntityType.Builder.create(BlockRoadName.BlockEntity::new, Blocks.ROAD_NAME.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<?>> CRT_STATION_NAME_1 = BLOCK_ENTITY.register("crt_station_name_1", () -> BlockEntityType.Builder.create(BlockCRTStationName1.BlockEntity::new, Blocks.CRT_STATION_NAME_1.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<?>> CRT_STATION_NAME_2 = BLOCK_ENTITY.register("crt_station_name_2", () -> BlockEntityType.Builder.create(BlockCRTStationName2.BlockEntity::new, Blocks.CRT_STATION_NAME_2.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<?>> ORDINARY_STATION_NAME = BLOCK_ENTITY.register("ordinary_station_name", () -> BlockEntityType.Builder.create(BlockOrdinaryStationName.BlockEntity::new, Blocks.ORDINARY_STATION_NAME.get()).build(null));
    //public static final RegistrySupplier<BlockEntityType<?>> CRT_APG_DOOR_1 = BLOCK_ENTITY.register("crt_apg_door_1", () -> BlockEntityType.Builder.create(BlockAPGDoor.BlockEntity::new, Blocks.CRT_STATION_NAME_1.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<?>> CRT_APG_DOOR_1 = BLOCK_ENTITY.register("crt_apg_door_1", () -> BlockEntityType.Builder.create(BlockCRTAPGDoor1.BlockEntity::new, Blocks.CRT_APG_DOOR_1.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<?>> CRT_APG_DOOR_2 = BLOCK_ENTITY.register("crt_apg_door_2", () -> BlockEntityType.Builder.create(BlockCRTAPGDoor2.BlockEntity::new, Blocks.CRT_APG_DOOR_2.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<?>> BUS_TICKET_PROCESSOR = BLOCK_ENTITY.register("bus_ticket_processor", () -> BlockEntityType.Builder.create(BlockBusTicketProcessor.BlockEntity::new, Blocks.BUS_TICKET_PROCESSOR.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<?>> CRT_STATION_INFO_1 = BLOCK_ENTITY.register("crt_station_info_1", () -> BlockEntityType.Builder.create(BlockCRTStationInfo1.BlockEntity::new, Blocks.CRT_STATION_INFO_1.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<?>> CRT_STATION_INFO_1_DOUBLE = BLOCK_ENTITY.register("crt_station_info_1_double", () -> BlockEntityType.Builder.create(BlockCRTStationInfo1Double.BlockEntity::new, Blocks.CRT_STATION_INFO_1_DOUBLE.get()).build(null));

    // ---- CRT 铁路告示牌（长度 3-11，偶/奇）方块实体 ----
    public static final RegistrySupplier<BlockEntityType<?>> CRT_RAILWAY_SIGN_3_EVEN = BLOCK_ENTITY.register("crt_railway_sign_3_even", () -> BlockEntityType.Builder.create((pos, state) -> new BlockCRTRailwaySign.BlockEntityCRTRailwaySign(3, false, pos, state), Blocks.CRT_RAILWAY_SIGN_3_EVEN.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<?>> CRT_RAILWAY_SIGN_3_ODD = BLOCK_ENTITY.register("crt_railway_sign_3_odd", () -> BlockEntityType.Builder.create((pos, state) -> new BlockCRTRailwaySign.BlockEntityCRTRailwaySign(3, true, pos, state), Blocks.CRT_RAILWAY_SIGN_3_ODD.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<?>> CRT_RAILWAY_SIGN_4_EVEN = BLOCK_ENTITY.register("crt_railway_sign_4_even", () -> BlockEntityType.Builder.create((pos, state) -> new BlockCRTRailwaySign.BlockEntityCRTRailwaySign(4, false, pos, state), Blocks.CRT_RAILWAY_SIGN_4_EVEN.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<?>> CRT_RAILWAY_SIGN_4_ODD = BLOCK_ENTITY.register("crt_railway_sign_4_odd", () -> BlockEntityType.Builder.create((pos, state) -> new BlockCRTRailwaySign.BlockEntityCRTRailwaySign(4, true, pos, state), Blocks.CRT_RAILWAY_SIGN_4_ODD.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<?>> CRT_RAILWAY_SIGN_5_EVEN = BLOCK_ENTITY.register("crt_railway_sign_5_even", () -> BlockEntityType.Builder.create((pos, state) -> new BlockCRTRailwaySign.BlockEntityCRTRailwaySign(5, false, pos, state), Blocks.CRT_RAILWAY_SIGN_5_EVEN.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<?>> CRT_RAILWAY_SIGN_5_ODD = BLOCK_ENTITY.register("crt_railway_sign_5_odd", () -> BlockEntityType.Builder.create((pos, state) -> new BlockCRTRailwaySign.BlockEntityCRTRailwaySign(5, true, pos, state), Blocks.CRT_RAILWAY_SIGN_5_ODD.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<?>> CRT_RAILWAY_SIGN_6_EVEN = BLOCK_ENTITY.register("crt_railway_sign_6_even", () -> BlockEntityType.Builder.create((pos, state) -> new BlockCRTRailwaySign.BlockEntityCRTRailwaySign(6, false, pos, state), Blocks.CRT_RAILWAY_SIGN_6_EVEN.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<?>> CRT_RAILWAY_SIGN_6_ODD = BLOCK_ENTITY.register("crt_railway_sign_6_odd", () -> BlockEntityType.Builder.create((pos, state) -> new BlockCRTRailwaySign.BlockEntityCRTRailwaySign(6, true, pos, state), Blocks.CRT_RAILWAY_SIGN_6_ODD.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<?>> CRT_RAILWAY_SIGN_7_EVEN = BLOCK_ENTITY.register("crt_railway_sign_7_even", () -> BlockEntityType.Builder.create((pos, state) -> new BlockCRTRailwaySign.BlockEntityCRTRailwaySign(7, false, pos, state), Blocks.CRT_RAILWAY_SIGN_7_EVEN.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<?>> CRT_RAILWAY_SIGN_7_ODD = BLOCK_ENTITY.register("crt_railway_sign_7_odd", () -> BlockEntityType.Builder.create((pos, state) -> new BlockCRTRailwaySign.BlockEntityCRTRailwaySign(7, true, pos, state), Blocks.CRT_RAILWAY_SIGN_7_ODD.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<?>> CRT_RAILWAY_SIGN_8_EVEN = BLOCK_ENTITY.register("crt_railway_sign_8_even", () -> BlockEntityType.Builder.create((pos, state) -> new BlockCRTRailwaySign.BlockEntityCRTRailwaySign(8, false, pos, state), Blocks.CRT_RAILWAY_SIGN_8_EVEN.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<?>> CRT_RAILWAY_SIGN_8_ODD = BLOCK_ENTITY.register("crt_railway_sign_8_odd", () -> BlockEntityType.Builder.create((pos, state) -> new BlockCRTRailwaySign.BlockEntityCRTRailwaySign(8, true, pos, state), Blocks.CRT_RAILWAY_SIGN_8_ODD.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<?>> CRT_RAILWAY_SIGN_9_EVEN = BLOCK_ENTITY.register("crt_railway_sign_9_even", () -> BlockEntityType.Builder.create((pos, state) -> new BlockCRTRailwaySign.BlockEntityCRTRailwaySign(9, false, pos, state), Blocks.CRT_RAILWAY_SIGN_9_EVEN.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<?>> CRT_RAILWAY_SIGN_9_ODD = BLOCK_ENTITY.register("crt_railway_sign_9_odd", () -> BlockEntityType.Builder.create((pos, state) -> new BlockCRTRailwaySign.BlockEntityCRTRailwaySign(9, true, pos, state), Blocks.CRT_RAILWAY_SIGN_9_ODD.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<?>> CRT_RAILWAY_SIGN_10_EVEN = BLOCK_ENTITY.register("crt_railway_sign_10_even", () -> BlockEntityType.Builder.create((pos, state) -> new BlockCRTRailwaySign.BlockEntityCRTRailwaySign(10, false, pos, state), Blocks.CRT_RAILWAY_SIGN_10_EVEN.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<?>> CRT_RAILWAY_SIGN_10_ODD = BLOCK_ENTITY.register("crt_railway_sign_10_odd", () -> BlockEntityType.Builder.create((pos, state) -> new BlockCRTRailwaySign.BlockEntityCRTRailwaySign(10, true, pos, state), Blocks.CRT_RAILWAY_SIGN_10_ODD.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<?>> CRT_RAILWAY_SIGN_11_EVEN = BLOCK_ENTITY.register("crt_railway_sign_11_even", () -> BlockEntityType.Builder.create((pos, state) -> new BlockCRTRailwaySign.BlockEntityCRTRailwaySign(11, false, pos, state), Blocks.CRT_RAILWAY_SIGN_11_EVEN.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<?>> CRT_RAILWAY_SIGN_11_ODD = BLOCK_ENTITY.register("crt_railway_sign_11_odd", () -> BlockEntityType.Builder.create((pos, state) -> new BlockCRTRailwaySign.BlockEntityCRTRailwaySign(11, true, pos, state), Blocks.CRT_RAILWAY_SIGN_11_ODD.get()).build(null));

    /** 按长度与奇偶性返回 CRT 铁路告示牌方块实体类型。 */
    public static BlockEntityType<?> getRailwaySignType(int length, boolean isOdd) {
        return switch (length) {
            case 3 -> isOdd ? CRT_RAILWAY_SIGN_3_ODD.get() : CRT_RAILWAY_SIGN_3_EVEN.get();
            case 4 -> isOdd ? CRT_RAILWAY_SIGN_4_ODD.get() : CRT_RAILWAY_SIGN_4_EVEN.get();
            case 5 -> isOdd ? CRT_RAILWAY_SIGN_5_ODD.get() : CRT_RAILWAY_SIGN_5_EVEN.get();
            case 6 -> isOdd ? CRT_RAILWAY_SIGN_6_ODD.get() : CRT_RAILWAY_SIGN_6_EVEN.get();
            case 7 -> isOdd ? CRT_RAILWAY_SIGN_7_ODD.get() : CRT_RAILWAY_SIGN_7_EVEN.get();
            case 8 -> isOdd ? CRT_RAILWAY_SIGN_8_ODD.get() : CRT_RAILWAY_SIGN_8_EVEN.get();
            case 9 -> isOdd ? CRT_RAILWAY_SIGN_9_ODD.get() : CRT_RAILWAY_SIGN_9_EVEN.get();
            case 10 -> isOdd ? CRT_RAILWAY_SIGN_10_ODD.get() : CRT_RAILWAY_SIGN_10_EVEN.get();
            default -> isOdd ? CRT_RAILWAY_SIGN_11_ODD.get() : CRT_RAILWAY_SIGN_11_EVEN.get();
        };
    }

    public static void init() {
        BLOCK_ENTITY.register();
    }
}