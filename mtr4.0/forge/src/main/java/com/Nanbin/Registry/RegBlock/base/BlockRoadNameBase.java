package com.Nanbin.Registry.RegBlock.base;

import org.mtr.mapping.holder.BlockSettings;
import org.mtr.mapping.mapper.BlockExtension;
import org.mtr.mapping.mapper.BlockWithEntity;
import org.mtr.mapping.mapper.DirectionHelper;

/**
 * 站名方块基类 — 继承 BlockStationNameWallBase 以获得墙面安装行为。
 */
public abstract class BlockRoadNameBase extends BlockExtension implements DirectionHelper, BlockWithEntity {

    public BlockRoadNameBase(BlockSettings blockSettings) {
        super(blockSettings);
    }


}
