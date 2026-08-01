package com.Nanbin.packet;

import com.Nanbin.Registry.RegBlock.BlockRoadName;
import org.mtr.mapping.holder.BlockEntity;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.MinecraftServer;
import org.mtr.mapping.holder.ServerPlayerEntity;
import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;

public final class PacketUpdateRoadNameData extends PacketHandler {

	private final BlockPos blockPos;
	private final String[] texts;

	public PacketUpdateRoadNameData(PacketBufferReceiver packetBufferReceiver) {
		blockPos = BlockPos.fromLong(packetBufferReceiver.readLong());
		texts = new String[]{packetBufferReceiver.readString(), packetBufferReceiver.readString(), packetBufferReceiver.readString(), packetBufferReceiver.readString()};
	}

	public PacketUpdateRoadNameData(BlockPos blockPos, String[] texts) {
		this.blockPos = blockPos;
		this.texts = texts != null && texts.length >= 4 ? texts : new String[]{"", "", "", ""};
	}

	@Override
	public void write(PacketBufferSender packetBufferSender) {
		packetBufferSender.writeLong(blockPos.asLong());
		for (final String text : texts) {
			packetBufferSender.writeString(text);
		}
	}

	@Override
	public void runServer(MinecraftServer minecraftServer, ServerPlayerEntity serverPlayerEntity) {
		final BlockEntity blockEntity = serverPlayerEntity.getEntityWorld().getBlockEntity(blockPos);
		if (blockEntity != null && blockEntity.data instanceof BlockRoadName.BlockEntity entity) {
			entity.setTexts(texts);
		}
	}
}
