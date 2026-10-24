package com.Nanbin.packet;

import net.minecraft.util.math.BlockPos;
import org.mtr.packet.PacketBufferReceiver;
import org.mtr.packet.PacketBufferSender;
import org.mtr.packet.PacketHandler;

public class PacketOpenStationInfoScreen extends PacketHandler {

	private final BlockPos blockPos;
	private final String url;
	private final boolean isFront;

	public PacketOpenStationInfoScreen(PacketBufferReceiver packetBufferReceiver) {
		this.blockPos = BlockPos.fromLong(packetBufferReceiver.readLong());
		this.url = packetBufferReceiver.readString();
		this.isFront = packetBufferReceiver.readBoolean();
	}

	public PacketOpenStationInfoScreen(BlockPos blockPos, String url, boolean isFront) {
		this.blockPos = blockPos;
		this.url = url != null ? url : "";
		this.isFront = isFront;
	}

	@Override
	public void write(PacketBufferSender sender) {
		sender.writeLong(blockPos.asLong());
		sender.writeString(url);
		sender.writeBoolean(isFront);
	}

	@Override
	public void runClient() {
		ClientPacketHelper.openStationInfoScreen(blockPos, url, isFront);
	}
}
