package com.Nanbin.Registry.RegItem;

import com.Nanbin.Registry.RegBlock.flag.BlockFlagCRTAPG;
import com.Nanbin.mapping.ItemExtension;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.mtr.block.BlockPSDAPGDoorBase;
import org.mtr.block.BlockPSDAPGGlassBase;
import org.mtr.block.BlockPSDAPGGlassEndBase;
import org.mtr.block.IBlock;

import java.util.List;

import static org.mtr.item.ItemPSDAPGBase.blocksNotReplaceable;

/**
 * CRT 屏蔽门 / 站台门玻璃的放置物品。
 * <p>
 * 4.0 里 {@code ItemCRTAPG1} 与 {@code ItemCRTAPG2} 的类体逐字相同（只有 import 和 javadoc 不同），
 * 所以这里合并成一个类，用构造参数区分要放置的方块；Items 里按 6 个方块各注册一次。
 * <p>
 * 与 4.0 的差异：
 * <ul>
 *     <li>{@code context.getPlayerFacing()} → {@code getHorizontalPlayerFacing()}。</li>
 *     <li>去掉了 {@code !(block instanceof BlockFlagDoorSingle)} 判断：4.0 里没有任何 CRT 方块实现
 *     那个标记接口，所以该分支永远为真，去掉后行为完全一致（{@link #horizontalBlocks()} 已说明）。</li>
 * </ul>
 */
public class ItemCRTAPG extends ItemExtension implements IBlock {

	public final Block block;

	public ItemCRTAPG(Block block, Item.Settings settings) {
		super(settings);
		this.block = block;
	}

	/** 门（两块宽）占 2 格，玻璃/端玻璃占 1 格。 */
	private int horizontalBlocks() {
		return block instanceof BlockPSDAPGDoorBase ? 2 : 1;
	}

	/** 4.0 里 CRT 方块都没有实现 {@code BlockFlagCRTAPG}，所以这里恒为 false（即高度 3）。 */
	private boolean isAPG() {
		return block instanceof BlockFlagCRTAPG;
	}

	@Override
	public ActionResult useOnBlock2(ItemUsageContext context) {
		final int horizontalBlocks = horizontalBlocks();
		if (blocksNotReplaceable(context, horizontalBlocks, isAPG() ? 2 : 3, block)) {
			return ActionResult.FAIL;
		}

		final World world = context.getWorld();
		final Direction playerFacing = context.getHorizontalPlayerFacing();
		final BlockPos pos = context.getBlockPos().offset(context.getSide());

		for (int x = 0; x < horizontalBlocks; x++) {
			final BlockPos newPos = pos.offset(playerFacing.rotateYClockwise(), x);

			for (int y = 0; y < 2; y++) {
				final BlockState state = block.getDefaultState()
						.with(Properties.HORIZONTAL_FACING, playerFacing)
						.with(IBlock.HALF, y == 1 ? IBlock.DoubleBlockHalf.UPPER : IBlock.DoubleBlockHalf.LOWER);
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
	public void addTooltips(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
		if (block instanceof BlockPSDAPGDoorBase) {
			tooltip.add(org.mtr.generated.lang.TranslationProvider.TOOLTIP_MTR_PSD_APG_DOOR.getMutableText().formatted(Formatting.GRAY));
		} else if (block instanceof BlockPSDAPGGlassEndBase) {
			tooltip.add(org.mtr.generated.lang.TranslationProvider.TOOLTIP_MTR_PSD_APG_GLASS_END.getMutableText().formatted(Formatting.GRAY));
		} else if (block instanceof BlockPSDAPGGlassBase) {
			tooltip.add(org.mtr.generated.lang.TranslationProvider.TOOLTIP_MTR_PSD_APG_GLASS.getMutableText().formatted(Formatting.GRAY));
		}
	}
}
