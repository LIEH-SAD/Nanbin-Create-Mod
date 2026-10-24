package com.Nanbin.Registry.RegBlock;

import com.Nanbin.Items.Items;
import com.Nanbin.entity.BlockEntityTypes;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.Item;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mapping.mapper.BlockWithEntity;
import org.mtr.mod.block.BlockPSDAPGDoorBase;
import org.mtr.mod.block.BlockPSDDoor;

import javax.annotation.Nonnull;

/**
 * CRT 第一代高屏蔽门门体。
 *
 * <p>完全沿用 MTR 原版屏蔽门的逻辑：继承 {@link BlockPSDDoor}（style 0 = MTR 的 psd_door 一代），
 * 属性、开关门动画、红石/解锁行为、放置方向都由父类决定；模型贴图由 MTR 的
 * {@code RenderPSDAPGDoor}（type 0）渲染，因此门体外观与 MTR 的 psd_door 一致。
 *
 * <p>只替换方块实体类型（挂本模组自己的 BlockEntity）与掉落物品（本模组的物品），
 * 便于与 CRT 屏蔽门玻璃/吊板成套使用。
 */
public class BlockCRTPSDDoor1 extends BlockPSDDoor implements BlockWithEntity {

    /** MTR psd_door 一代的样式编号，与 RenderPSDAPGDoor 的 type 参数一致。 */
    private static final int STYLE = 0;

    public BlockCRTPSDDoor1() {
        super(STYLE);
    }

    @Nonnull
    @Override
    public BlockEntityExtension createBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new BlockEntity(blockPos, blockState);
    }

    @Override
    public Item asItem2() {
        return Items.CRT_PSD_DOOR_1.get();
    }

    /**
     * 方块实体。
     *
     * <p>注意：不能继承 {@link BlockPSDDoor.BlockEntity} —— 它的构造器会把方块实体类型硬编码成
     * MTR 自己的 {@code mtr:psd_door_*}，与本方块注册的 BE 类型不一致，
     * 结果是原版判定「invalid for ticking」（不计 tick、门不会开关）且渲染器按 MTR 的类型查找，
     * 挂在本模组类型上的 RenderPSDAPGDoor 永远不会被调用（门不渲染）。
     * 因此直接继承 MTR 的 {@link BlockPSDAPGDoorBase.BlockEntityBase}，
     * 构造时传入本模组自己的 BE 类型。
     */
    public static class BlockEntity extends BlockPSDAPGDoorBase.BlockEntityBase {

        public BlockEntity(BlockPos pos, BlockState state) {
            super(BlockEntityTypes.CRT_PSD_DOOR_1.get(), pos, state);
        }
    }
}

