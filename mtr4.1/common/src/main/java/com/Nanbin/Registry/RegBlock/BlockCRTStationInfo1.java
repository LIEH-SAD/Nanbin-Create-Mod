package com.Nanbin.Registry.RegBlock;

import com.Nanbin.entity.BlockEntityTypes;
import com.Nanbin.mapping.ObjectHolder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.Direction;

/**
 * CRT 站台信息屏（单面）。
 * <p>
 * 4.0 里它与 {@link BlockCRTStationInfo1Double} 是两份几乎相同的代码，这里改为共用
 * {@link BlockCRTStationInfoBase}；单面版的差异只有两点：方块实体类型、以及只接受背面点击。
 */
public class BlockCRTStationInfo1 extends BlockCRTStationInfoBase {

	public BlockCRTStationInfo1(net.minecraft.block.AbstractBlock.Settings settings) {
		super(settings);
	}

	@Override
	protected ObjectHolder<BlockEntityType<BlockEntityBase>> getBlockEntityType() {
		return BlockEntityTypes.CRT_STATION_INFO_1;
	}

	@Override
	protected boolean acceptsHitSide(Direction hitSide, Direction facing) {
		return hitSide == facing.getOpposite();
	}
}
