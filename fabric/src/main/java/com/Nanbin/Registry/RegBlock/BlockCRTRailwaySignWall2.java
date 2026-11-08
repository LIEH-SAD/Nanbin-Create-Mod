package com.Nanbin.Registry.RegBlock;

import com.Nanbin.entity.BlockEntityTypes;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockSettings;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.mapper.BlockEntityExtension;

import javax.annotation.Nonnull;

// CRT 墙挂铁路指示牌2：行为、连接、数据完全继承1，使用自己的方块实体类型和渲染器（模型与发光独立）。
public class BlockCRTRailwaySignWall2 extends BlockCRTRailwaySignWall1 {

    public BlockCRTRailwaySignWall2(BlockSettings blockSettings) {
        super(blockSettings);
    }

    @Nonnull
    @Override
    public BlockEntityExtension createBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntity(pos, state);
    }

    public static class BlockEntity extends BlockCRTRailwaySignWall1.BlockEntity {

        public BlockEntity(BlockPos pos, BlockState state) {
            super(BlockEntityTypes.CRT_RAILWAY_SIGN_WALL_2.get(), pos, state);
        }
    }
}
