package com.Nanbin.Registry.RegItem;

import com.Nanbin.Blocks.Blocks;
import mtr.block.BlockPSDAPGBase;
import mtr.block.BlockPSDDoor;
import mtr.block.BlockPSDGlass;
import mtr.block.BlockPSDGlassEnd;
import mtr.block.BlockPSDTop;
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
 * CRT 第一代高屏蔽门系列物品（门 / 玻璃 / 端部玻璃）。
 *
 * <p>与矮屏蔽门物品 {@link ItemCRTAPG1} 的区别：高屏蔽门整体高 3 格（门体 2 格 + 顶部吊板 1 格），
 * 放置后会在最上方再放一块本模组的吊板 {@code crt_psd_top_1}，并用 MTR 的
 * {@link BlockPSDTop#getActualState} 刷成正确的悬空/朝向状态。</p>
 */
public class ItemCRTPSD1 extends Item {

	private static final int VERTICAL_BLOCKS = 2;
	private static final int PSD_HEIGHT = VERTICAL_BLOCKS + 1;
	private static final int DOOR_WIDTH = 2;

	private final Block block;
	private final boolean isDoor;

	public ItemCRTPSD1(Block block, Settings settings, boolean isDoor) {
		super(settings);
		this.block = block;
		this.isDoor = isDoor;
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		final int horizontalBlocks = isDoor ? DOOR_WIDTH : 1;
		if (blocksNotReplaceable(context, horizontalBlocks, PSD_HEIGHT, block)) {
			return ActionResult.FAIL;
		}

		final World world = context.getWorld();
		final Direction playerFacing = context.getHorizontalPlayerFacing();
		final BlockPos pos = context.getBlockPos().offset(context.getSide());

		for (int x = 0; x < horizontalBlocks; x++) {
			final BlockPos horizontalPos = pos.offset(playerFacing.rotateYClockwise(), x);
			for (int y = 0; y < VERTICAL_BLOCKS; y++) {
				BlockState state = block.getDefaultState()
						.with(BlockPSDAPGBase.FACING, playerFacing)
						.with(IBlock.HALF, y == 0 ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER);
				if (isDoor) {
					state = state.with(IBlock.SIDE, x == 0 ? IBlock.EnumSide.LEFT : IBlock.EnumSide.RIGHT);
				} else {
					state = state.with(IBlock.SIDE_EXTENDED, IBlock.EnumSide.SINGLE);
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
				.with(BlockPSDAPGBase.FACING, playerFacing));
		world.setBlockState(topPos, BlockPSDTop.getActualState(world, topPos));
	}

	@Override
	public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext options) {
		if (block instanceof BlockPSDDoor) {
			tooltip.add(Text.translatable("tooltip.mtr.psd_apg_door").formatted(Formatting.GRAY));
		} else if (block instanceof BlockPSDGlassEnd) {
			tooltip.add(Text.translatable("tooltip.mtr.psd_apg_glass_end").formatted(Formatting.GRAY));
		} else if (block instanceof BlockPSDGlass) {
			tooltip.add(Text.translatable("tooltip.mtr.psd_apg_glass").formatted(Formatting.GRAY));
		}
	}
}
