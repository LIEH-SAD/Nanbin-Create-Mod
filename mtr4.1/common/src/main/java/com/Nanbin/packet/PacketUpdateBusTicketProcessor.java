package com.Nanbin.packet;

import com.Nanbin.Registry.RegBlock.BlockBusTicketProcessor;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.mtr.packet.PacketBufferReceiver;
import org.mtr.packet.PacketBufferSender;
import org.mtr.packet.PacketHandler;

public final class PacketUpdateBusTicketProcessor extends PacketHandler {

	private final BlockPos blockPos;
	private final int mode;
	private final int amount;

	public PacketUpdateBusTicketProcessor(PacketBufferReceiver packetBufferReceiver) {
		this.blockPos = BlockPos.fromLong(packetBufferReceiver.readLong());
		this.mode = packetBufferReceiver.readInt();
		this.amount = packetBufferReceiver.readInt();
	}

	public PacketUpdateBusTicketProcessor(BlockPos blockPos, int mode, int amount) {
		this.blockPos = blockPos;
		this.mode = mode;
		this.amount = amount;
	}

	@Override
	public void write(PacketBufferSender packetBufferSender) {
		packetBufferSender.writeLong(this.blockPos.asLong());
		packetBufferSender.writeInt(this.mode);
		packetBufferSender.writeInt(this.amount);
	}

	@Override
	public void runServer(MinecraftServer minecraftServer, ServerPlayerEntity serverPlayerEntity) {
		final BlockEntity blockEntity = serverPlayerEntity.getEntityWorld().getBlockEntity(blockPos);
		if (blockEntity instanceof BlockBusTicketProcessor.BlockEntity entity) {
			entity.setData(mode, amount);
		}
	}
}
