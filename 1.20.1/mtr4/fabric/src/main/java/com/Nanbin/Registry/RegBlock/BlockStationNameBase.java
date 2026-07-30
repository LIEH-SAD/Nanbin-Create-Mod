package com.Nanbin.Registry.RegBlock;

import org.mtr.mapping.holder.BlockSettings;

/**
 * 站名方块基类 — 继承 BlockStationNameWallBase 以获得墙面安装行为。
 */
public abstract class BlockStationNameBase extends org.mtr.mod.block.BlockStationNameWallBase {

    public BlockStationNameBase(BlockSettings blockSettings) {
        super(blockSettings);
    }
}
