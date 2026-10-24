package com.Nanbin.Registry.RegItem;

import com.Nanbin.Blocks.Blocks;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.DirectionHelper;
import org.mtr.mapping.mapper.ItemExtension;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mapping.registry.BlockRegistryObject;
import org.mtr.mod.block.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

import static org.mtr.mod.item.ItemPSDAPGBase.blocksNotReplaceable;

public class ItemCRTPSD1 extends ItemExtension {

    private static final int VERTICAL_BLOCKS = 2;
    private static final int PSD_HEIGHT = VERTICAL_BLOCKS + 1;
    private static final int DOOR_WIDTH = 2;

    private final Block block;
    private final boolean isDoor;

    public ItemCRTPSD1(BlockRegistryObject block, ItemSettings settings, boolean isDoor) {
        super(settings);
        this.block = block.get();
        this.isDoor = isDoor;
    }

    @Nonnull
    @Override
    public ActionResult useOnBlock2(ItemUsageContext context) {
        final int horizontalBlocks = isDoor ? DOOR_WIDTH : 1;
        if (blocksNotReplaceable(context, horizontalBlocks, PSD_HEIGHT, block)) {
            return ActionResult.FAIL;
        }

        final World world = context.getWorld();
        final Direction playerFacing = context.getPlayerFacing();
        final BlockPos pos = context.getBlockPos().offset(context.getSide());

        for (int x = 0; x < horizontalBlocks; x++) {
            final BlockPos horizontalPos = pos.offset(playerFacing.rotateYClockwise(), x);
            for (int y = 0; y < VERTICAL_BLOCKS; y++) {
                BlockState state = block.getDefaultState()
                        .with(new Property<>(DirectionHelper.FACING.data), playerFacing.data)
                        .with(new Property<>(IBlock.HALF.data), y == 0 ? IBlock.DoubleBlockHalf.LOWER : IBlock.DoubleBlockHalf.UPPER);
                if (isDoor) {
                    state = state.with(new Property<>(IBlock.SIDE.data), x == 0 ? IBlock.EnumSide.LEFT : IBlock.EnumSide.RIGHT);
                } else {
                    state = state.with(new Property<>(IBlock.SIDE_EXTENDED.data), IBlock.EnumSide.SINGLE);
                }
                world.setBlockState(horizontalPos.up(y), state);
            }
            placeTop(world, horizontalPos.up(VERTICAL_BLOCKS), playerFacing);
        }

        context.getStack().decrement(1);
        return ActionResult.SUCCESS;
    }

    private static void placeTop(World world, BlockPos topPos, Direction playerFacing) {
        world.setBlockState(topPos, Blocks.CRT_PSD_TOP_1.get().getDefaultState()
                .with(new Property<>(DirectionHelper.FACING.data), playerFacing.data));
        world.setBlockState(topPos, BlockPSDTop.getActualState(WorldAccess.cast(world), topPos));
    }

    @Override
    public void addTooltips(ItemStack stack, @Nullable World world, List<MutableText> tooltip, TooltipContext options) {
        if (this.block.data instanceof BlockPSDDoor) {
            tooltip.add(TextHelper.translatable("tooltip.mtr.psd_apg_door").formatted(TextFormatting.GRAY));
        } else if (this.block.data instanceof BlockPSDGlassEnd) {
            tooltip.add(TextHelper.translatable("tooltip.mtr.psd_apg_glass_end").formatted(TextFormatting.GRAY));
        } else if (this.block.data instanceof BlockPSDGlass) {
            tooltip.add(TextHelper.translatable("tooltip.mtr.psd_apg_glass").formatted(TextFormatting.GRAY));
        }
    }
}
