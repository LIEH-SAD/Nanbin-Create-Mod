package com.Nanbin.Registry.RegItem;

import com.Nanbin.Registry.RegBlock.BlockCRTAPGDoor2;
import com.Nanbin.Registry.RegBlock.BlockCRTAPGGlass2;
import com.Nanbin.Registry.RegBlock.BlockCRTAPGGlassEnd2;
import com.Nanbin.Registry.RegBlock.flag.BlockFlagCRTAPG;
import com.Nanbin.Registry.RegBlock.flag.BlockFlagDoorSingle;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.ItemExtension;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mapping.registry.BlockRegistryObject;
import org.mtr.mod.block.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

import static org.mtr.mod.item.ItemPSDAPGBase.blocksNotReplaceable;

/**
 * @author LIEH-SAD
 * @see BlockCRTAPGDoor1
 * @see BlockCRTAPGGlass1
 * @see BlockCRTAPGGlassEnd1
 */

public class ItemCRTAPG1 extends ItemExtension implements IBlock
{
    public final Block block;

    public ItemCRTAPG1(BlockRegistryObject block, ItemSettings settings) {
        super(settings);
        this.block = block.get();
    }

    @Nonnull
    @Override
    public ActionResult useOnBlock2(ItemUsageContext context) {
        final int horizontalBlocks = block.data instanceof BlockPSDAPGDoorBase && !(block.data instanceof BlockFlagDoorSingle) ? 2 : 1;
        if (blocksNotReplaceable(context, horizontalBlocks, isAPG() ? 2 : 3, this.block)) return ActionResult.FAIL;

        final World world = context.getWorld();
        final Direction playerFacing = context.getPlayerFacing();
        final BlockPos pos = context.getBlockPos().offset(context.getSide());

        for (int x = 0; x < horizontalBlocks; x++) {
            final BlockPos newPos = pos.offset(playerFacing.rotateYClockwise(), x);

            for (int y = 0; y < 2; y++) {
                final BlockState state = this.block.getDefaultState().with(new Property<>(BlockPSDAPGBase.FACING.data), playerFacing.data).with(new Property<>(HALF.data), y == 1 ? DoubleBlockHalf.UPPER : DoubleBlockHalf.LOWER);
                if (block.data instanceof BlockFlagDoorSingle doorSingle) {
                    BlockState neighborState = state.with(new Property<>(SIDE.data), doorSingle.isLeft() ? EnumSide.LEFT : EnumSide.RIGHT);
                    world.setBlockState(newPos.up(y), neighborState);
                } else if (block.data instanceof BlockPSDAPGDoorBase) {
                    BlockState neighborState = state.with(new Property<>(SIDE.data), x == 0 ? EnumSide.LEFT : EnumSide.RIGHT);
                    world.setBlockState(newPos.up(y), neighborState);
                } else {
                    world.setBlockState(newPos.up(y), state.with(new Property<>(SIDE_EXTENDED.data), EnumSide.SINGLE));
                }
            }
        }

        context.getStack().decrement(1);
        return ActionResult.SUCCESS;
    }

    public boolean isAPG() {
        return this.block.data instanceof BlockFlagCRTAPG;
    }

    @Override
    public void addTooltips(ItemStack stack, @Nullable World world, List<MutableText> tooltip, TooltipContext options) {
        if (this.block.data instanceof BlockPSDAPGDoorBase) {
            tooltip.add(TextHelper.translatable("tooltip.mtr.psd_apg_door").formatted(TextFormatting.GRAY));
        } else if (this.block.data instanceof BlockPSDAPGGlassEndBase) {
            tooltip.add(TextHelper.translatable("tooltip.mtr.psd_apg_glass_end").formatted(TextFormatting.GRAY));
        } else if (this.block.data instanceof BlockPSDAPGGlassBase) {
            tooltip.add(TextHelper.translatable("tooltip.mtr.psd_apg_glass").formatted(TextFormatting.GRAY));
        }
    }
}
