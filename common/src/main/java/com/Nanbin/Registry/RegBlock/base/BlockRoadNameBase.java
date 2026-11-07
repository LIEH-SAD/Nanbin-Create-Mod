package com.Nanbin.Registry.RegBlock.base;

import mtr.mappings.BlockDirectionalMapper;
import mtr.mappings.BlockEntityMapper;
import mtr.mappings.EntityBlockMapper;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

/**
 * 路名方块基类 — 站名/路名系列共用的「墙面安装 + 方块实体」基类。
 *
 * <p>MTR 4 的 {@code BlockExtension implements BlockWithEntity} 在 MTR 3.6.3 由
 * {@link BlockDirectionalMapper}（带 FACING 的水平朝向方块）+ {@link EntityBlockMapper}（方块实体提供者）
 * 承担；本类只固定这两层基建，具体形状/放置行为仍由子类决定。</p>
 */
public abstract class BlockRoadNameBase extends BlockDirectionalMapper implements EntityBlockMapper {

	public BlockRoadNameBase(Settings settings) {
		super(settings);
	}

	/** 子类必须提供方块实体；显示声明以消除接口默认方法与抽象方法的重叠。 */
	@Override
	public abstract BlockEntityMapper createBlockEntity(BlockPos pos, BlockState state);
}
