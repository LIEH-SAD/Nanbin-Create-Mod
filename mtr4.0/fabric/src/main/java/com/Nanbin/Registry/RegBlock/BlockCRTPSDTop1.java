package com.Nanbin.Registry.RegBlock;

import com.Nanbin.Blocks.Blocks;
import com.Nanbin.entity.BlockEntityTypes;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mapping.mapper.BlockWithEntity;
import org.mtr.mod.block.BlockPSDTop;
import org.mtr.mod.block.IBlock;

import javax.annotation.Nonnull;

/**
 * CRT 第一代高屏蔽门吊板（屏蔽门上方的横贯板），继承 MTR 的 {@link BlockPSDTop}：
 * 属性、左右悬空判断、persistent 切换、碰撞箱与贴图均与 MTR 的 psd_top 一致。
 *
 * <p>区别在于吊板本身承载 MTR 标准导视，由 {@link com.Nanbin.client.Render.RenderCRTPSDTop1}
 * 绘制本站台的线路图 / 方向箭头。
 */
public class BlockCRTPSDTop1 extends BlockPSDTop implements BlockWithEntity {

    public BlockCRTPSDTop1() {
    }

    @Nonnull
    @Override
    public BlockEntityExtension createBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new BlockEntity(blockPos, blockState);
    }

    /** 父类会返回 MTR 的 psd_top 物品，这里改回本模组的吊板物品。 */
    @Override
    public Item asItem2() {
        return Blocks.CRT_PSD_TOP_1.get().asItem();
    }

    @Override
    public BlockState getPlacementState2(ItemPlacementContext ctx) {
        return getDefaultState2().with(new Property<>(FACING.data), ctx.getPlayerFacing().data);
    }

    public static class BlockEntity extends BlockPSDTop.BlockEntityBase {

        public BlockEntity(BlockPos pos, BlockState state) {
            super(BlockEntityTypes.CRT_PSD_TOP_1.get(), pos, state);
        }
    }
}
