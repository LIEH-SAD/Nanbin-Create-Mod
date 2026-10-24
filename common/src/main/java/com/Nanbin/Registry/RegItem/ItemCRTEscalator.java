package com.Nanbin.Registry.RegItem;

import com.Nanbin.Blocks.Blocks;
import com.Nanbin.mapping.TranslationProvider;
import mtr.block.BlockEscalatorBase;
import mtr.block.IBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
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
 * CRT 自动扶梯物品：一次放置 2 格宽的梯级与两侧侧板（各 2 格高）。
 * 在已有扶梯的末端继续放置会自动反向衔接，从而延长整段扶梯；
 * 平台（landing）、斜坡（slope）与上下过渡段由方块自身按相邻方块判定。
 *
 * @see com.Nanbin.Registry.RegBlock.Escalator.BlockCRTEscalatorStep
 * @see com.Nanbin.Registry.RegBlock.Escalator.BlockCRTEscalatorSide
 */
public class ItemCRTEscalator extends Item {

	public ItemCRTEscalator(Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		// 2 格宽 × 2 格高的空间必须都可替换
		if (blocksNotReplaceable(context, 2, 2, null)) {
			return ActionResult.PASS;
		}

		final World world = context.getWorld();
		Direction facing = context.getPlayerFacing();
		BlockPos pos = context.getBlockPos().offset(context.getSide());
		BlockPos pos2 = pos.offset(facing.getOpposite());

		// 贴着已有扶梯放置时反向衔接，使新的一段与已有段朝向一致
		final BlockState adjacentState = world.getBlockState(pos.offset(facing));
		if (adjacentState.getBlock() instanceof BlockEscalatorBase && IBlock.getStatePropertySafe(adjacentState, HorizontalFacingBlock.FACING) == facing.getOpposite()) {
			facing = facing.getOpposite();
			final BlockPos temp = pos;
			pos = pos2;
			pos2 = temp;
		}

		final BlockState stepState = Blocks.CRT_ESCALATOR_STEP.get().getDefaultState().with(HorizontalFacingBlock.FACING, facing);
		world.setBlockState(pos, stepState.with(IBlock.SIDE, IBlock.EnumSide.LEFT));
		world.setBlockState(pos2, stepState.with(IBlock.SIDE, IBlock.EnumSide.RIGHT));

		final BlockState sideState = Blocks.CRT_ESCALATOR_SIDE.get().getDefaultState().with(HorizontalFacingBlock.FACING, facing);
		world.setBlockState(pos.up(), sideState.with(IBlock.SIDE, IBlock.EnumSide.LEFT));
		world.setBlockState(pos2.up(), sideState.with(IBlock.SIDE, IBlock.EnumSide.RIGHT));

		context.getStack().decrement(1);
		return ActionResult.SUCCESS;
	}

	@Override
	public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext options) {
		tooltip.add(TranslationProvider.BRUSH_USE.getText().copy().formatted(Formatting.DARK_GRAY));
	}
}
