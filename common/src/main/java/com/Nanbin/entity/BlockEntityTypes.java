package com.Nanbin.entity;

import com.Nanbin.Blocks.Blocks;
import com.Nanbin.Init;
import com.Nanbin.Registry.RegBlock.BlockBusTicketProcessor;
import com.Nanbin.Registry.RegBlock.BlockCRTAPGDoor1;
import com.Nanbin.Registry.RegBlock.BlockCRTAPGDoor2;
import com.Nanbin.Registry.RegBlock.BlockCRTStationName1;
import com.Nanbin.Registry.RegBlock.BlockCRTStationName2;
import com.Nanbin.Registry.RegBlock.BlockCRTRailwaySign;
import com.Nanbin.Registry.RegBlock.BlockCRTStationInfoBase;
import com.Nanbin.Registry.RegBlock.BlockOrdinaryStationName;
import com.Nanbin.Registry.RegBlock.BlockPSDTOP;
import com.Nanbin.Registry.RegBlock.BlockRoadName;
import com.Nanbin.mapping.ObjectHolder;
import com.Nanbin.mapping.Registry;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;

public class BlockEntityTypes {

	public static final ObjectHolder<BlockEntityType<BlockPSDTOP.BlockEntity>> PSD_TOP;
	public static final ObjectHolder<BlockEntityType<BlockRoadName.BlockEntity>> BLOCK_ROAD_NAME;
	public static final ObjectHolder<BlockEntityType<BlockBusTicketProcessor.BlockEntity>> BUS_TICKET_PROCESSOR;
	public static final ObjectHolder<BlockEntityType<BlockCRTStationName1.BlockEntity>> CRT_STATION_NAME_1;
	public static final ObjectHolder<BlockEntityType<BlockCRTStationName2.BlockEntity>> CRT_STATION_NAME_2;
	public static final ObjectHolder<BlockEntityType<BlockOrdinaryStationName.BlockEntity>> ORDINARY_STATION_NAME;
	public static final ObjectHolder<BlockEntityType<BlockCRTAPGDoor1.BlockEntity>> CRT_APG_DOOR_1;
	public static final ObjectHolder<BlockEntityType<BlockCRTAPGDoor2.BlockEntity>> CRT_APG_DOOR_2;
	public static final ObjectHolder<BlockEntityType<BlockCRTRailwaySign.BlockEntityCRTRailwaySign>> CRT_RAILWAY_SIGN_3_EVEN;
	public static final ObjectHolder<BlockEntityType<BlockCRTRailwaySign.BlockEntityCRTRailwaySign>> CRT_RAILWAY_SIGN_3_ODD;
	public static final ObjectHolder<BlockEntityType<BlockCRTRailwaySign.BlockEntityCRTRailwaySign>> CRT_RAILWAY_SIGN_4_EVEN;
	public static final ObjectHolder<BlockEntityType<BlockCRTRailwaySign.BlockEntityCRTRailwaySign>> CRT_RAILWAY_SIGN_4_ODD;
	public static final ObjectHolder<BlockEntityType<BlockCRTRailwaySign.BlockEntityCRTRailwaySign>> CRT_RAILWAY_SIGN_5_EVEN;
	public static final ObjectHolder<BlockEntityType<BlockCRTRailwaySign.BlockEntityCRTRailwaySign>> CRT_RAILWAY_SIGN_5_ODD;
	public static final ObjectHolder<BlockEntityType<BlockCRTRailwaySign.BlockEntityCRTRailwaySign>> CRT_RAILWAY_SIGN_6_EVEN;
	public static final ObjectHolder<BlockEntityType<BlockCRTRailwaySign.BlockEntityCRTRailwaySign>> CRT_RAILWAY_SIGN_6_ODD;
	public static final ObjectHolder<BlockEntityType<BlockCRTRailwaySign.BlockEntityCRTRailwaySign>> CRT_RAILWAY_SIGN_7_EVEN;
	public static final ObjectHolder<BlockEntityType<BlockCRTRailwaySign.BlockEntityCRTRailwaySign>> CRT_RAILWAY_SIGN_7_ODD;
	public static final ObjectHolder<BlockEntityType<BlockCRTRailwaySign.BlockEntityCRTRailwaySign>> CRT_RAILWAY_SIGN_8_EVEN;
	public static final ObjectHolder<BlockEntityType<BlockCRTRailwaySign.BlockEntityCRTRailwaySign>> CRT_RAILWAY_SIGN_8_ODD;
	public static final ObjectHolder<BlockEntityType<BlockCRTRailwaySign.BlockEntityCRTRailwaySign>> CRT_RAILWAY_SIGN_9_EVEN;
	public static final ObjectHolder<BlockEntityType<BlockCRTRailwaySign.BlockEntityCRTRailwaySign>> CRT_RAILWAY_SIGN_9_ODD;
	public static final ObjectHolder<BlockEntityType<BlockCRTRailwaySign.BlockEntityCRTRailwaySign>> CRT_RAILWAY_SIGN_10_EVEN;
	public static final ObjectHolder<BlockEntityType<BlockCRTRailwaySign.BlockEntityCRTRailwaySign>> CRT_RAILWAY_SIGN_10_ODD;
	public static final ObjectHolder<BlockEntityType<BlockCRTRailwaySign.BlockEntityCRTRailwaySign>> CRT_RAILWAY_SIGN_11_EVEN;
	public static final ObjectHolder<BlockEntityType<BlockCRTRailwaySign.BlockEntityCRTRailwaySign>> CRT_RAILWAY_SIGN_11_ODD;
	public static final ObjectHolder<BlockEntityType<BlockCRTStationInfoBase.BlockEntityBase>> CRT_STATION_INFO_1;
	public static final ObjectHolder<BlockEntityType<BlockCRTStationInfoBase.BlockEntityBase>> CRT_STATION_INFO_1_DOUBLE;

	static {
		PSD_TOP = Registry.registerBlockEntityType("psd_top", BlockPSDTOP.BlockEntity::new, Blocks.PSD_TOP::get);
		BLOCK_ROAD_NAME = Registry.registerBlockEntityType("road_name_block", BlockRoadName.BlockEntity::new, Blocks.ROAD_NAME::get);
		BUS_TICKET_PROCESSOR = Registry.registerBlockEntityType("bus_ticket_processor", BlockBusTicketProcessor.BlockEntity::new, Blocks.BUS_TICKET_PROCESSOR::get);
		CRT_STATION_NAME_1 = Registry.registerBlockEntityType("crt_station_name_1", BlockCRTStationName1.BlockEntity::new, Blocks.CRT_STATION_NAME_1::get);
		CRT_STATION_NAME_2 = Registry.registerBlockEntityType("crt_station_name_2", BlockCRTStationName2.BlockEntity::new, Blocks.CRT_STATION_NAME_2::get);
		ORDINARY_STATION_NAME = Registry.registerBlockEntityType("ordinary_station_name", BlockOrdinaryStationName.BlockEntity::new, Blocks.ORDINARY_STATION_NAME::get);
		CRT_APG_DOOR_1 = Registry.registerBlockEntityType("crt_apg_door_1", BlockCRTAPGDoor1.BlockEntity::new, Blocks.CRT_APG_DOOR_1::get);
		CRT_APG_DOOR_2 = Registry.registerBlockEntityType("crt_apg_door_2", BlockCRTAPGDoor2.BlockEntity::new, Blocks.CRT_APG_DOOR_2::get);
		CRT_RAILWAY_SIGN_3_EVEN = signType("crt_railway_sign_3_even", 3, false);
		CRT_RAILWAY_SIGN_3_ODD = signType("crt_railway_sign_3_odd", 3, true);
		CRT_RAILWAY_SIGN_4_EVEN = signType("crt_railway_sign_4_even", 4, false);
		CRT_RAILWAY_SIGN_4_ODD = signType("crt_railway_sign_4_odd", 4, true);
		CRT_RAILWAY_SIGN_5_EVEN = signType("crt_railway_sign_5_even", 5, false);
		CRT_RAILWAY_SIGN_5_ODD = signType("crt_railway_sign_5_odd", 5, true);
		CRT_RAILWAY_SIGN_6_EVEN = signType("crt_railway_sign_6_even", 6, false);
		CRT_RAILWAY_SIGN_6_ODD = signType("crt_railway_sign_6_odd", 6, true);
		CRT_RAILWAY_SIGN_7_EVEN = signType("crt_railway_sign_7_even", 7, false);
		CRT_RAILWAY_SIGN_7_ODD = signType("crt_railway_sign_7_odd", 7, true);
		CRT_RAILWAY_SIGN_8_EVEN = signType("crt_railway_sign_8_even", 8, false);
		CRT_RAILWAY_SIGN_8_ODD = signType("crt_railway_sign_8_odd", 8, true);
		CRT_RAILWAY_SIGN_9_EVEN = signType("crt_railway_sign_9_even", 9, false);
		CRT_RAILWAY_SIGN_9_ODD = signType("crt_railway_sign_9_odd", 9, true);
		CRT_RAILWAY_SIGN_10_EVEN = signType("crt_railway_sign_10_even", 10, false);
		CRT_RAILWAY_SIGN_10_ODD = signType("crt_railway_sign_10_odd", 10, true);
		CRT_RAILWAY_SIGN_11_EVEN = signType("crt_railway_sign_11_even", 11, false);
		CRT_RAILWAY_SIGN_11_ODD = signType("crt_railway_sign_11_odd", 11, true);
		CRT_STATION_INFO_1 = Registry.registerBlockEntityType("crt_station_info_1", BlockCRTStationInfoBase.BlockEntitySingle::new, Blocks.CRT_STATION_INFO_1::get);
		CRT_STATION_INFO_1_DOUBLE = Registry.registerBlockEntityType("crt_station_info_1_double", BlockCRTStationInfoBase.BlockEntityDouble::new, Blocks.CRT_STATION_INFO_1_DOUBLE::get);
	}

	private static ObjectHolder<BlockEntityType<BlockCRTRailwaySign.BlockEntityCRTRailwaySign>> signType(String path, int length, boolean isOdd) {
		final ObjectHolder<Block> block = length == 3
				? (isOdd ? Blocks.CRT_RAILWAY_SIGN_3_ODD : Blocks.CRT_RAILWAY_SIGN_3_EVEN)
				: length == 4
				? (isOdd ? Blocks.CRT_RAILWAY_SIGN_4_ODD : Blocks.CRT_RAILWAY_SIGN_4_EVEN)
				: length == 5
				? (isOdd ? Blocks.CRT_RAILWAY_SIGN_5_ODD : Blocks.CRT_RAILWAY_SIGN_5_EVEN)
				: length == 6
				? (isOdd ? Blocks.CRT_RAILWAY_SIGN_6_ODD : Blocks.CRT_RAILWAY_SIGN_6_EVEN)
				: length == 7
				? (isOdd ? Blocks.CRT_RAILWAY_SIGN_7_ODD : Blocks.CRT_RAILWAY_SIGN_7_EVEN)
				: length == 8
				? (isOdd ? Blocks.CRT_RAILWAY_SIGN_8_ODD : Blocks.CRT_RAILWAY_SIGN_8_EVEN)
				: length == 9
				? (isOdd ? Blocks.CRT_RAILWAY_SIGN_9_ODD : Blocks.CRT_RAILWAY_SIGN_9_EVEN)
				: length == 10
				? (isOdd ? Blocks.CRT_RAILWAY_SIGN_10_ODD : Blocks.CRT_RAILWAY_SIGN_10_EVEN)
				: (isOdd ? Blocks.CRT_RAILWAY_SIGN_11_ODD : Blocks.CRT_RAILWAY_SIGN_11_EVEN);
		return Registry.registerBlockEntityType(path, (pos, state) -> new BlockCRTRailwaySign.BlockEntityCRTRailwaySign(length, isOdd, pos, state), block::get);
	}

	// 指示牌长度
	public static ObjectHolder<BlockEntityType<BlockCRTRailwaySign.BlockEntityCRTRailwaySign>> getRailwaySignType(int length, boolean isOdd) {
		return switch (length) {
			case 3 -> isOdd ? CRT_RAILWAY_SIGN_3_ODD : CRT_RAILWAY_SIGN_3_EVEN;
			case 4 -> isOdd ? CRT_RAILWAY_SIGN_4_ODD : CRT_RAILWAY_SIGN_4_EVEN;
			case 5 -> isOdd ? CRT_RAILWAY_SIGN_5_ODD : CRT_RAILWAY_SIGN_5_EVEN;
			case 6 -> isOdd ? CRT_RAILWAY_SIGN_6_ODD : CRT_RAILWAY_SIGN_6_EVEN;
			case 7 -> isOdd ? CRT_RAILWAY_SIGN_7_ODD : CRT_RAILWAY_SIGN_7_EVEN;
			case 8 -> isOdd ? CRT_RAILWAY_SIGN_8_ODD : CRT_RAILWAY_SIGN_8_EVEN;
			case 9 -> isOdd ? CRT_RAILWAY_SIGN_9_ODD : CRT_RAILWAY_SIGN_9_EVEN;
			case 10 -> isOdd ? CRT_RAILWAY_SIGN_10_ODD : CRT_RAILWAY_SIGN_10_EVEN;
			case 11 -> isOdd ? CRT_RAILWAY_SIGN_11_ODD : CRT_RAILWAY_SIGN_11_EVEN;
			default -> null;
		};
	}

	public static ObjectHolder<Block> getRailwaySignBlock(int length, boolean isOdd) {
		return switch (length) {
			case 3 -> isOdd ? Blocks.CRT_RAILWAY_SIGN_3_ODD : Blocks.CRT_RAILWAY_SIGN_3_EVEN;
			case 4 -> isOdd ? Blocks.CRT_RAILWAY_SIGN_4_ODD : Blocks.CRT_RAILWAY_SIGN_4_EVEN;
			case 5 -> isOdd ? Blocks.CRT_RAILWAY_SIGN_5_ODD : Blocks.CRT_RAILWAY_SIGN_5_EVEN;
			case 6 -> isOdd ? Blocks.CRT_RAILWAY_SIGN_6_ODD : Blocks.CRT_RAILWAY_SIGN_6_EVEN;
			case 7 -> isOdd ? Blocks.CRT_RAILWAY_SIGN_7_ODD : Blocks.CRT_RAILWAY_SIGN_7_EVEN;
			case 8 -> isOdd ? Blocks.CRT_RAILWAY_SIGN_8_ODD : Blocks.CRT_RAILWAY_SIGN_8_EVEN;
			case 9 -> isOdd ? Blocks.CRT_RAILWAY_SIGN_9_ODD : Blocks.CRT_RAILWAY_SIGN_9_EVEN;
			case 10 -> isOdd ? Blocks.CRT_RAILWAY_SIGN_10_ODD : Blocks.CRT_RAILWAY_SIGN_10_EVEN;
			case 11 -> isOdd ? Blocks.CRT_RAILWAY_SIGN_11_ODD : Blocks.CRT_RAILWAY_SIGN_11_EVEN;
			default -> null;
		};
	}

	private static org.mtr.registry.ObjectHolder<? extends BlockEntityType<?>> mtrRailwaySignType(int length, boolean isOdd) {
		return switch (length) {
			case 3 -> isOdd ? org.mtr.registry.BlockEntityTypes.RAILWAY_SIGN_3_ODD : org.mtr.registry.BlockEntityTypes.RAILWAY_SIGN_3_EVEN;
			case 4 -> isOdd ? org.mtr.registry.BlockEntityTypes.RAILWAY_SIGN_4_ODD : org.mtr.registry.BlockEntityTypes.RAILWAY_SIGN_4_EVEN;
			case 5 -> isOdd ? org.mtr.registry.BlockEntityTypes.RAILWAY_SIGN_5_ODD : org.mtr.registry.BlockEntityTypes.RAILWAY_SIGN_5_EVEN;
			case 6 -> isOdd ? org.mtr.registry.BlockEntityTypes.RAILWAY_SIGN_6_ODD : org.mtr.registry.BlockEntityTypes.RAILWAY_SIGN_6_EVEN;
			case 7 -> isOdd ? org.mtr.registry.BlockEntityTypes.RAILWAY_SIGN_7_ODD : org.mtr.registry.BlockEntityTypes.RAILWAY_SIGN_7_EVEN;
			default -> org.mtr.registry.BlockEntityTypes.RAILWAY_SIGN_2_EVEN;
		};
	}

	private static boolean mtrSignTypesPatched = false;

	public static void ensureMtrSignTypesAcceptCrtBlocks() {
		if (mtrSignTypesPatched) {
			return;
		}
		mtrSignTypesPatched = true;
		int patched = 0;
		int failed = 0;
		for (int length = 3; length <= 11; length++) {
			for (final boolean isOdd : new boolean[]{false, true}) {
				final ObjectHolder<Block> block = getRailwaySignBlock(length, isOdd);
				final org.mtr.registry.ObjectHolder<? extends BlockEntityType<?>> mtrType = mtrRailwaySignType(length, isOdd);
				if (block == null || mtrType == null) {
					continue;
				}
				final Block blockObject = block.get();
				final BlockEntityType<?> typeObject = mtrType.get();
				addBlockToType(typeObject, blockObject);
				// 直接问一次原版：这个类型现在是否接受这个方块？
				// 这正是 NeoForge 在 BlockEntity 构造函数里做的校验，校验不过就会崩客户端。
				if (typeObject.supports(blockObject.getDefaultState())) {
					patched++;
				} else {
					failed++;
					Init.LOGGER.error("MTR 的 {} 仍然不接受 CRT 方块 {}，放置该告示牌会在 NeoForge 上崩溃", typeObject, blockObject);
				}
			}
		}
		if (failed == 0) {
			Init.LOGGER.info("MTR 告示牌类型兼容性检查通过（{}/{}）", patched, patched);
		}
	}

	private static void addBlockToType(BlockEntityType<?> type, Block block) {
		for (final java.lang.reflect.Field field : BlockEntityType.class.getDeclaredFields()) {
			if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) || !java.util.Set.class.isAssignableFrom(field.getType())) {
				continue;
			}
			try {
				field.setAccessible(true);
				final Object value = field.get(type);
				if (!(value instanceof java.util.Set<?> existing)) {
					continue;
				}
				final java.util.Set<Block> updated = new java.util.HashSet<>();
				for (final Object element : existing) {
					if (element instanceof Block existingBlock) {
						updated.add(existingBlock);
					}
				}
				if (updated.add(block)) {
					// 原集合可能是不可变集合，所以整体替换而不是 add
					field.set(type, updated);
				}
			} catch (Exception e) {
				Init.LOGGER.error("无法把 {} 加入 MTR 的方块实体类型 {}", block, type, e);
			}
			return;
		}
		Init.LOGGER.error("找不到 BlockEntityType 的方块集合字段，CRT 告示牌在 NeoForge 上会校验失败");
	}

	public static void init() {
	}
}
