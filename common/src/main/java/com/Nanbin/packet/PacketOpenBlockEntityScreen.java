package com.Nanbin.packet;

import net.minecraft.util.math.BlockPos;
import org.mtr.packet.PacketBufferReceiver;
import org.mtr.packet.PacketBufferSender;
import org.mtr.packet.PacketHandler;

public final class PacketOpenBlockEntityScreen extends PacketHandler {

	private final BlockPos blockPos;

	public PacketOpenBlockEntityScreen(PacketBufferReceiver packetBufferReceiver) {
		this.blockPos = BlockPos.fromLong(packetBufferReceiver.readLong());
	}

	public PacketOpenBlockEntityScreen(BlockPos blockPos) {
		this.blockPos = blockPos;
	}

	@Override
	public void write(PacketBufferSender packetBufferSender) {
		packetBufferSender.writeLong(this.blockPos.asLong());
	}

	@Override
	public void runClient() {
		ClientPacketHelper.openBlockEntityScreen(this.blockPos);
	}
}
