package com.Nanbin.packet;

import net.minecraft.util.math.BlockPos;
import org.mtr.packet.PacketBufferReceiver;
import org.mtr.packet.PacketBufferSender;
import org.mtr.packet.PacketHandler;

public class PacketOpenRoadNameScreen extends PacketHandler {

	private final BlockPos blockPos;
	private final String[] texts;

	public PacketOpenRoadNameScreen(PacketBufferReceiver packetBufferReceiver) {
		this.blockPos = BlockPos.fromLong(packetBufferReceiver.readLong());
		this.texts = new String[]{
				packetBufferReceiver.readString(), packetBufferReceiver.readString(),
				packetBufferReceiver.readString(), packetBufferReceiver.readString()};
	}

	public PacketOpenRoadNameScreen(BlockPos blockPos, String[] texts) {
		this.blockPos = blockPos;
		this.texts = texts != null && texts.length >= 4 ? texts : new String[]{"", "", "", ""};
	}

	@Override
	public void write(PacketBufferSender sender) {
		sender.writeLong(blockPos.asLong());
		for (final String text : texts) {
			sender.writeString(text);
		}
	}

	@Override
	public void runClient() {
		ClientPacketHelper.openRoadNameScreen(blockPos, texts);
	}
}
