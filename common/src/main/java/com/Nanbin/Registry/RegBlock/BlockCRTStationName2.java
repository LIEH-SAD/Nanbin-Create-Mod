package com.Nanbin.Registry.RegBlock;

import com.Nanbin.entity.BlockEntityTypes;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;

/** CRT 站名牌（二代）。 */
public class BlockCRTStationName2 extends BlockCRTStationNameBase {

	public BlockCRTStationName2(AbstractBlock.Settings settings) {
		super(settings);
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new BlockEntity(pos, state);
	}

	public static class BlockEntity extends StationNameBlockEntity {

		public BlockEntity(BlockPos pos, BlockState state) {
			super(BlockEntityTypes.CRT_STATION_NAME_2.get(), pos, state);
		}
	}
}
