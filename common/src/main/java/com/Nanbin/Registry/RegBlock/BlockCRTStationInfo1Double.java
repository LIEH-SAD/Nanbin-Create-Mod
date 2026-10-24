package com.Nanbin.Registry.RegBlock;

import com.Nanbin.entity.BlockEntityTypes;
import com.Nanbin.mapping.ObjectHolder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.Direction;

/**
 * CRT 站台信息屏（双面）。
 * <p>
 * 4.0 里它与 {@link BlockCRTStationInfo1} 是两份几乎相同的代码，这里改为共用
 * {@link BlockCRTStationInfoBase}；双面版的差异只有两点：方块实体类型、以及正反两面都能点击编辑。
 */
public class BlockCRTStationInfo1Double extends BlockCRTStationInfoBase {

	public BlockCRTStationInfo1Double(net.minecraft.block.AbstractBlock.Settings settings) {
		super(settings);
	}

	@Override
	protected ObjectHolder<BlockEntityType<BlockEntityBase>> getBlockEntityType() {
		return BlockEntityTypes.CRT_STATION_INFO_1_DOUBLE;
	}

	@Override
	protected boolean acceptsHitSide(Direction hitSide, Direction facing) {
		return hitSide == facing || hitSide == facing.getOpposite();
	}
}
