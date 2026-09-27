package com.Nanbin.packet;

import com.Nanbin.mapping.Registry;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.mtr.data.TicketSystem;
import org.mtr.packet.PacketBufferReceiver;
import org.mtr.packet.PacketBufferSender;
import org.mtr.packet.PacketHandler;

/**
 * 售票机「用绿宝石充值」请求。
 * <p>
 * 4.1 适配：4.0 的 {@code InventoryHelper}/{@code PlayerHelper} 包装类没有了，
 * 直接用原版 {@link PlayerInventory} 校验并扣除绿宝石。
 */
public final class PacketAddCustomBalance extends PacketHandler {

	private static final int[] ADD_AMOUNTS = {10, 20, 30, 50, 100, 150, 200, 300};
	private static final int[] EMERALD_COSTS = {1, 2, 3, 5, 10, 15, 20, 30};

	private final int index;

	public PacketAddCustomBalance(PacketBufferReceiver packetBufferReceiver) {
		this.index = packetBufferReceiver.readInt();
	}

	public PacketAddCustomBalance(int index) {
		this.index = index;
	}

	@Override
	public void write(PacketBufferSender packetBufferSender) {
		packetBufferSender.writeInt(index);
	}

	@Override
	public void runServer(MinecraftServer minecraftServer, ServerPlayerEntity serverPlayerEntity) {
		if (index < 0 || index >= ADD_AMOUNTS.length) {
			return;
		}
		final World world = serverPlayerEntity.getEntityWorld();
		final PlayerInventory inventory = serverPlayerEntity.getInventory();
		if (inventory.count(Items.EMERALD) >= EMERALD_COSTS[index]) {
			removeItems(inventory, EMERALD_COSTS[index]);
			TicketSystem.addBalance(world, serverPlayerEntity, ADD_AMOUNTS[index]);
		}
		Registry.sendPacketToClient(serverPlayerEntity, new PacketSyncCustomBalance((int) TicketSystem.getBalance(world, serverPlayerEntity)));
	}

	/** 从背包里扣除指定数量的绿宝石（主背包 + 副手）。 */
	private static void removeItems(PlayerInventory inventory, int count) {
		int remaining = count;
		remaining = removeFromList(inventory.main, remaining);
		if (remaining > 0) {
			removeFromList(inventory.offHand, remaining);
		}
		inventory.markDirty();
	}

	private static int removeFromList(java.util.List<ItemStack> stacks, int remaining) {
		for (int i = 0; i < stacks.size() && remaining > 0; i++) {
			final ItemStack stack = stacks.get(i);
			if (stack.isOf(Items.EMERALD)) {
				final int taken = Math.min(remaining, stack.getCount());
				stack.decrement(taken);
				remaining -= taken;
				if (stack.isEmpty()) {
					stacks.set(i, ItemStack.EMPTY);
				}
			}
		}
		return remaining;
	}
}
