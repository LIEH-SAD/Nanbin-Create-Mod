package com.Nanbin.Registry.RegItem;

import com.Nanbin.mapping.ItemExtension;
import com.Nanbin.mapping.TicketMachineHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/**
 * 手机（随身售票机）。
 * <p>
 * 与 4.0 的差异：4.0 覆写的是 MTR 包装层的 {@code ItemExtension#useWithoutResult}；
 * 4.1 没有这层包装，必须直接覆写原版 {@code Item#use}，而它的返回值在
 * 1.21.1（{@code TypedActionResult<ItemStack>}）与 1.21.4（{@code ActionResult}）之间不兼容，
 * 所以这个类只能按版本各写一份 —— 见 {@code common/build.gradle} 里 versioned 源码目录的说明。
 * <p>
 * 另外 4.0 把手机放进 MTR 的核心标签页；4.1 的标签页无法由附属 mod 写入，这里改放进 CRT 标签页。
 */
public class ItemPhone extends ItemExtension {

	public ItemPhone(Settings settings) {
		super(settings.maxCount(1));
	}

	@Override
	public ActionResult use(World world, PlayerEntity user, Hand hand) {
		TicketMachineHelper.openTicketMachineScreen(world, user);
		return ActionResult.SUCCESS;
	}
}
