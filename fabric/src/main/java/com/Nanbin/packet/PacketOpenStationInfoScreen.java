package com.Nanbin.packet;

import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;

public class PacketOpenStationInfoScreen extends PacketHandler {
    private final BlockPos blockPos;
    private final String url;
    private final boolean isFront;

    public PacketOpenStationInfoScreen(PacketBufferReceiver packetBufferReceiver) {
        blockPos = BlockPos.fromLong(packetBufferReceiver.readLong());
        url = packetBufferReceiver.readString();
        isFront = packetBufferReceiver.readBoolean();
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