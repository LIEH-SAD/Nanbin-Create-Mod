package com.Nanbin.Registry.RegBlock;

import mtr.block.BlockStationNameWallBase;
import mtr.mappings.BlockEntityMapper;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

/**
 * 站名方块基类 —— 继承 MTR 的 {@link BlockStationNameWallBase} 以获得贴墙安装行为
 * （放置面判定、背墙消失自动掉落、薄板形状）。
 */
public abstract class BlockStationNameBase extends BlockStationNameWallBase {

	public BlockStationNameBase(AbstractBlock.Settings settings) {
		super(settings);
	}

	/** 显式重新声明为抽象：MTR 的 EntityBlockMapper 在同一签名上同时提供抽象声明与默认实现。 */
	@Override
	public abstract BlockEntityMapper createBlockEntity(BlockPos pos, BlockState state);
}
