package com.Nanbin.Registry.RegItem;

import mtr.block.BlockPSDAPGBase;
import mtr.block.BlockPSDAPGDoorBase;
import mtr.block.BlockPSDAPGGlassBase;
import mtr.block.BlockPSDAPGGlassEndBase;
import mtr.block.IBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

import static mtr.item.ItemPSDAPGBase.blocksNotReplaceable;

/**
 * CRT 第二代矮屏蔽门系列物品（门 / 玻璃 / 端部玻璃）。
 * 门会一次性放置为并排 2 格、上下 2 格；玻璃与端部玻璃为 1 格宽、上下 2 格。
 *
 * @see com.Nanbin.Registry.RegBlock.BlockCRTAPGDoor2
 * @see com.Nanbin.Registry.RegBlock.BlockCRTAPGGlass2
 * @see com.Nanbin.Registry.RegBlock.BlockCRTAPGGlassEnd2
 */
public class ItemCRTAPG2 extends Item {

	private final Block block;

	public ItemCRTAPG2(Block block, Settings settings) {
		super(settings);
		this.block = block;
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		final int horizontalBlocks = block instanceof BlockPSDAPGDoorBase ? 2 : 1;
		if (blocksNotReplaceable(context, horizontalBlocks, 2, block)) {
			return ActionResult.FAIL;
		}

		final World world = context.getWorld();
		final Direction playerFacing = context.getHorizontalPlayerFacing();
		final BlockPos pos = context.getBlockPos().offset(context.getSide());

		for (int x = 0; x < horizontalBlocks; x++) {
			final BlockPos newPos = pos.offset(playerFacing.rotateYClockwise(), x);

			for (int y = 0; y < 2; y++) {
				final BlockState state = block.getDefaultState()
						.with(BlockPSDAPGBase.FACING, playerFacing)
						.with(IBlock.HALF, y == 1 ? DoubleBlockHalf.UPPER : DoubleBlockHalf.LOWER);
				if (block instanceof BlockPSDAPGDoorBase) {
					world.setBlockState(newPos.up(y), state.with(IBlock.SIDE, x == 0 ? IBlock.EnumSide.LEFT : IBlock.EnumSide.RIGHT));
				} else {
					world.setBlockState(newPos.up(y), state.with(IBlock.SIDE_EXTENDED, IBlock.EnumSide.SINGLE));
				}
			}
		}

		context.getStack().decrement(1);
		return ActionResult.SUCCESS;
	}

	@Override
	public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext options) {
		final String tooltipKey;
		if (block instanceof BlockPSDAPGDoorBase) {
			tooltipKey = "tooltip.mtr.psd_apg_door";
		} else if (block instanceof BlockPSDAPGGlassEndBase) {
			tooltipKey = "tooltip.mtr.psd_apg_glass_end";
		} else if (block instanceof BlockPSDAPGGlassBase) {
			tooltipKey = "tooltip.mtr.psd_apg_glass";
		} else {
			tooltipKey = null;
		}
		if (tooltipKey != null) {
			tooltip.add(Text.translatable(tooltipKey).formatted(Formatting.GRAY));
		}
	}
}
