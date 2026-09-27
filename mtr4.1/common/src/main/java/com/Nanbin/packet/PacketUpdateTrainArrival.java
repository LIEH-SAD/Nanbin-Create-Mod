package com.Nanbin.packet;

import com.Nanbin.Registry.RegBlock.BlockCRTAPGDoor2;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.mtr.packet.PacketBufferReceiver;
import org.mtr.packet.PacketBufferSender;
import org.mtr.packet.PacketHandler;

/** 客户端 → 服务端：上报屏蔽门的下一班到站时间。 */
public final class PacketUpdateTrainArrival extends PacketHandler {

	private final BlockPos blockPos;
	private final long nextArrivalTime;

	public PacketUpdateTrainArrival(PacketBufferReceiver packetBufferReceiver) {
		this(BlockPos.fromLong(packetBufferReceiver.readLong()), packetBufferReceiver.readLong());
	}

	public PacketUpdateTrainArrival(BlockPos pos, long nextArrivalTime) {
		this.blockPos = pos;
		this.nextArrivalTime = nextArrivalTime;
	}

	@Override
	public void write(PacketBufferSender packetBufferSender) {
		packetBufferSender.writeLong(blockPos.asLong());
		packetBufferSender.writeLong(nextArrivalTime);
	}

	@Override
	public void runServer(MinecraftServer minecraftServer, ServerPlayerEntity serverPlayerEntity) {
		final BlockEntity entity = serverPlayerEntity.getEntityWorld().getBlockEntity(blockPos);
		if (entity instanceof BlockCRTAPGDoor2.BlockEntity doorEntity) {
			doorEntity.setNextTrainArrivalTime(nextArrivalTime);
		}
	}
}
