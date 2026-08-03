package com.Nanbin.packet;

import com.Nanbin.mapping.Registry;
import net.minecraft.util.math.BlockPos;


public class PacketOpenScreen {
    public static void registerClientReceivers() {
        Registry.registerReceiverS2C(PacketHandler.PACKET_OPEN_ROAD_NAME, (buf, context) -> {
            final BlockPos pos = buf.readBlockPos();
            final int count = buf.readVarInt();
            final String[] texts = new String[count];
            for (int i = 0; i < count; i++) {
                texts[i] = buf.readString();
            }
            context.queue(() -> ClientPacketHelper.openRoadNameScreen(pos, texts));
        });

        Registry.registerReceiverS2C(PacketHandler.PACKET_OPEN_CRT_STATION_NAME, (buf, context) -> {
            final BlockPos pos = buf.readBlockPos();
            context.queue(() -> ClientPacketHelper.openRailwaySignScreen(pos));
        });
    }
}
