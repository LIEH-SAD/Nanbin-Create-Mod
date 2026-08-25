package com.Nanbin.packet;

import com.Nanbin.Registry.RegBlock.BlockCRTAPGDoor2;
import org.mtr.mapping.holder.BlockEntity;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.MinecraftServer;
import org.mtr.mapping.holder.ServerPlayerEntity;
import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;

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
        if (entity != null && entity.data instanceof BlockCRTAPGDoor2.BlockEntity) {
            ((BlockCRTAPGDoor2.BlockEntity) entity.data).setNextTrainArrivalTime(nextArrivalTime);
        }
    }
}