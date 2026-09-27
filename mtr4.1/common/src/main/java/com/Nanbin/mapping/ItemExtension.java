package com.Nanbin.mapping;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.List;

/**
 * 物品基类适配层，取代 MTR 4.0 的 {@code org.mtr.mapping.mapper.ItemExtension}。
 * 保留 4.0 时代的 {@code useOnBlock2} / {@code addTooltips} 钩子名。
 * <p>
 * TODO(第二批)：{@code useWithoutResult} 需要覆写 {@code Item#use}，但 1.21.1 返回
 * {@code TypedActionResult<ItemStack>}、1.21.4 改成返回 {@code ActionResult}，无法共用一份实现，
 * 需要按版本分别实现（放到各平台/版本源码集里），届时再接上这个钩子。
 */
public abstract class ItemExtension extends Item {

	protected ItemExtension(Settings settings) {
		super(settings);
	}

	public ActionResult useOnBlock2(ItemUsageContext context) {
		return ActionResult.PASS;
	}

	public void useWithoutResult(World world, net.minecraft.entity.player.PlayerEntity user, Hand hand) {
	}

	public void addTooltips(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		return useOnBlock2(context);
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		addTooltips(stack, context, tooltip, type);
	}
}
