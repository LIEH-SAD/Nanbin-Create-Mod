package com.Nanbin.packet;

import com.Nanbin.mapping.Registry;
import org.mtr.mapping.holder.Inventory;
import org.mtr.mapping.holder.Items;
import org.mtr.mapping.holder.MinecraftServer;
import org.mtr.mapping.holder.PlayerEntity;
import org.mtr.mapping.holder.PlayerInventory;
import org.mtr.mapping.holder.ServerPlayerEntity;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.InventoryHelper;
import org.mtr.mapping.mapper.PlayerHelper;
import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;
import org.mtr.mod.data.TicketSystem;

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
		final PlayerEntity player = PlayerEntity.cast(serverPlayerEntity);
		final PlayerInventory playerInventory = PlayerHelper.getPlayerInventory(player);
		if (playerInventory != null && playerInventory.count(Items.getEmeraldMapped()) >= EMERALD_COSTS[index]) {
			TicketSystem.addBalance(world, player, ADD_AMOUNTS[index]);
			InventoryHelper.remove(new Inventory(playerInventory.data), itemStack -> itemStack.getItem().equals(Items.getEmeraldMapped()), EMERALD_COSTS[index], false);
		}
		Registry.sendPacketToClient(serverPlayerEntity, new PacketSyncCustomBalance(TicketSystem.getBalance(world, player)));
	}
}