package com.Nanbin.Registry.RegBlock;

import org.mtr.mapping.holder.BlockSettings;
import org.mtr.mod.block.BlockStationNameWallBase;

/**
 * 站名方块基类 — 继承 BlockStationNameWallBase 以获得墙面安装行为。
 */
public abstract class BlockStationNameBase extends BlockStationNameWallBase {

    public BlockStationNameBase(BlockSettings blockSettings) {
        super(blockSettings);
    }
}
