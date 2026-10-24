package com.Nanbin.packet;

import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;

public final class PacketOpenBlockEntityScreen extends PacketHandler {
	private final BlockPos blockPos;

	public PacketOpenBlockEntityScreen(PacketBufferReceiver packetBufferReceiver) {
		this.blockPos = BlockPos.fromLong(packetBufferReceiver.readLong());
	}

	public PacketOpenBlockEntityScreen(BlockPos blockPos) {
		this.blockPos = blockPos;
	}

	public void write(PacketBufferSender packetBufferSender) {
		packetBufferSender.writeLong(this.blockPos.asLong());
	}

	public void runClient() {
		ClientPacketHelper.openBlockEntityScreen(this.blockPos);
	}
}
