package com.Nanbin.Registry.RegBlock;

import com.Nanbin.entity.BlockEntityTypes;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;

/** 普通站名牌。 */
public class BlockOrdinaryStationName extends BlockCRTStationNameBase {

	public BlockOrdinaryStationName(AbstractBlock.Settings settings) {
		super(settings);
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new BlockEntity(pos, state);
	}

	public static class BlockEntity extends StationNameBlockEntity {

		public BlockEntity(BlockPos pos, BlockState state) {
			super(BlockEntityTypes.ORDINARY_STATION_NAME.get(), pos, state);
		}
	}
}
