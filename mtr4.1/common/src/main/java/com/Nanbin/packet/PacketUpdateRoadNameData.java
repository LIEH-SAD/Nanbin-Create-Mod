package com.Nanbin.packet;

import com.Nanbin.Registry.RegBlock.BlockRoadName;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.mtr.packet.PacketBufferReceiver;
import org.mtr.packet.PacketBufferSender;
import org.mtr.packet.PacketHandler;

public final class PacketUpdateRoadNameData extends PacketHandler {

	private final BlockPos blockPos;
	private final String[] texts;

	public PacketUpdateRoadNameData(PacketBufferReceiver packetBufferReceiver) {
		this.blockPos = BlockPos.fromLong(packetBufferReceiver.readLong());
		this.texts = new String[]{
				packetBufferReceiver.readString(), packetBufferReceiver.readString(),
				packetBufferReceiver.readString(), packetBufferReceiver.readString()};
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
		if (blockEntity instanceof BlockRoadName.BlockEntity entity) {
			entity.setTexts(texts);
		}
	}
}
