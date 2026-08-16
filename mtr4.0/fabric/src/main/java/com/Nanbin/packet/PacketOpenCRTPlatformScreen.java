package com.Nanbin.packet;

import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;

public class PacketOpenCRTPlatformScreen extends PacketHandler {
    private final BlockPos blockPos;

    public PacketOpenCRTPlatformScreen(PacketBufferReceiver packetBufferReceiver) {
        blockPos = BlockPos.fromLong(packetBufferReceiver.readLong());
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
