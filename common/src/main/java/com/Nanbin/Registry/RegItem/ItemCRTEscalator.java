package com.Nanbin.Registry.RegItem;

import com.Nanbin.Blocks.Blocks;
import com.Nanbin.mapping.ItemExtension;
import com.Nanbin.mapping.TranslationProvider;
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
import org.mtr.block.BlockEscalatorBase;
import org.mtr.block.IBlock;

import java.util.List;

import static org.mtr.item.ItemPSDAPGBase.blocksNotReplaceable;

public class ItemCRTEscalator extends ItemExtension implements IBlock {

	public ItemCRTEscalator(Item.Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult useOnBlock2(ItemUsageContext context) {
		if (blocksNotReplaceable(context, 2, 2, null)) {
			return ActionResult.PASS;
		}

		final var world = context.getWorld();
		Direction facing = context.getHorizontalPlayerFacing();
		BlockPos pos = context.getBlockPos().offset(context.getSide());
		BlockPos pos2 = pos.offset(facing.getOpposite());

		// 贴着已有扶梯放置时反向衔接，使新的一段与已有段朝向一致
		final BlockState adjacentState = world.getBlockState(pos.offset(facing));
		if (adjacentState.getBlock() instanceof BlockEscalatorBase && IBlock.getStatePropertySafe(adjacentState, Properties.HORIZONTAL_FACING) == facing.getOpposite()) {
			facing = facing.getOpposite();
			final BlockPos temp = pos;
			pos = pos2;
			pos2 = temp;
		}

		final BlockState stepState = Blocks.CRT_ESCALATOR_STEP.get().getDefaultState().with(Properties.HORIZONTAL_FACING, facing);
		world.setBlockState(pos, stepState.with(IBlock.SIDE, IBlock.EnumSide.LEFT));
		world.setBlockState(pos2, stepState.with(IBlock.SIDE, IBlock.EnumSide.RIGHT));

		final BlockState sideState = Blocks.CRT_ESCALATOR_SIDE.get().getDefaultState().with(Properties.HORIZONTAL_FACING, facing);
		world.setBlockState(pos.up(), sideState.with(IBlock.SIDE, IBlock.EnumSide.LEFT));
		world.setBlockState(pos2.up(), sideState.with(IBlock.SIDE, IBlock.EnumSide.RIGHT));

		context.getStack().decrement(1);
		return ActionResult.SUCCESS;
	}

	@Override
	public void addTooltips(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(TranslationProvider.BRUSH_USE.getMutableText().formatted(Formatting.DARK_GRAY));
	}
}
