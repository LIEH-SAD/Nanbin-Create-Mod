package com.Nanbin.packet;

import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;

public class PacketOpenRoadNameScreen extends PacketHandler {
	private final BlockPos blockPos;
	private final String[] texts;

	public PacketOpenRoadNameScreen(PacketBufferReceiver packetBufferReceiver) {
		blockPos = BlockPos.fromLong(packetBufferReceiver.readLong());
		texts = new String[]{packetBufferReceiver.readString(), packetBufferReceiver.readString(), packetBufferReceiver.readString(), packetBufferReceiver.readString()};
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
