package com.Nanbin.packet;

import net.minecraft.util.math.BlockPos;
import org.mtr.packet.PacketBufferReceiver;
import org.mtr.packet.PacketBufferSender;
import org.mtr.packet.PacketHandler;

public class PacketOpenCRTPlatformScreen extends PacketHandler {

	private final BlockPos blockPos;

	public PacketOpenCRTPlatformScreen(PacketBufferReceiver packetBufferReceiver) {
		this.blockPos = BlockPos.fromLong(packetBufferReceiver.readLong());
	}

	public PacketOpenCRTPlatformScreen(BlockPos blockPos) {
		this.blockPos = blockPos;
	}

	@Override
	public void write(PacketBufferSender sender) {
		sender.writeLong(blockPos.asLong());
	}

	@Override
	public void runClient() {
		ClientPacketHelper.openRailwaySignScreen(blockPos);
	}
}
