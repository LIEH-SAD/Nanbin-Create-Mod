package com.Nanbin.packet;

import com.Nanbin.Registry.RegBlock.BlockBusTicketProcessor;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.MinecraftServer;
import org.mtr.mapping.holder.ServerPlayerEntity;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;

public final class PacketUpdateBusTicketProcessor extends PacketHandler {
    private final BlockPos blockPos;
    private final int mode;
    private final int amount;

    public PacketUpdateBusTicketProcessor(PacketBufferReceiver packetBufferReceiver) {
        this.blockPos = BlockPos.fromLong(packetBufferReceiver.readLong());
        this.mode = packetBufferReceiver.readInt();
        this.amount = packetBufferReceiver.readInt();
    }

    public PacketUpdateBusTicketProcessor(BlockPos blockPos, int mode, int amount) {
        this.blockPos = blockPos;
        this.mode = mode;
        this.amount = amount;
    }

    @Override
    public void write(PacketBufferSender packetBufferSender) {
        packetBufferSender.writeLong(this.blockPos.asLong());
        packetBufferSender.writeInt(this.mode);
        packetBufferSender.writeInt(this.amount);
    }

    @Override
    public void runServer(MinecraftServer minecraftServer, ServerPlayerEntity serverPlayerEntity) {
        World world = serverPlayerEntity.getEntityWorld();
        org.mtr.mapping.holder.BlockEntity blockEntity = world.getBlockEntity(blockPos);
        if (blockEntity != null && blockEntity.data instanceof BlockBusTicketProcessor.BlockEntity entity) {
            entity.setData(mode, amount);
        }
    }
}