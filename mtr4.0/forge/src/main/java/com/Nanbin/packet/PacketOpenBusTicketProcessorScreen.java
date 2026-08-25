package com.Nanbin.packet;

import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;

public final class PacketOpenBusTicketProcessorScreen extends PacketHandler {
    private final BlockPos blockPos;
    private final int mode;
    private final int amount;

    public PacketOpenBusTicketProcessorScreen(PacketBufferReceiver packetBufferReceiver) {
        this.blockPos = BlockPos.fromLong(packetBufferReceiver.readLong());
        this.mode = packetBufferReceiver.readInt();
        this.amount = packetBufferReceiver.readInt();
    }

    public PacketOpenBusTicketProcessorScreen(BlockPos blockPos, int mode, int amount) {
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
    public void runClient() {
        ClientPacketHelper.openBusTicketProcessorScreen(blockPos, mode, amount);
    }
}