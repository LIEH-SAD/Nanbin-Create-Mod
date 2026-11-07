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

        Registry.registerReceiverS2C(PacketHandler.PACKET_OPEN_TICKET_MENU, (buf, context) -> {
            final int balance = buf.readInt();
            context.queue(() -> ClientPacketHelper.openTicketMachineScreen(balance));
        });

        Registry.registerReceiverS2C(PacketHandler.PACKET_SYNC_TICKET_BALANCE, (buf, context) -> {
            final int balance = buf.readInt();
            context.queue(() -> ClientPacketHelper.syncTicketMachineBalance(balance));
        });

        Registry.registerReceiverS2C(PacketHandler.PACKET_OPEN_BUS_TICKET_PROCESSOR, (buf, context) -> {
            final BlockPos pos = buf.readBlockPos();
            final int mode = buf.readInt();
            final int amount = buf.readInt();
            context.queue(() -> ClientPacketHelper.openBusTicketProcessorScreen(pos, mode, amount));
        });

        Registry.registerReceiverS2C(PacketHandler.PACKET_OPEN_STATION_INFO, (buf, context) -> {
            final BlockPos pos = buf.readBlockPos();
            final String url = buf.readString();
            final boolean isFront = buf.readBoolean();
            context.queue(() -> ClientPacketHelper.openStationInfoScreen(pos, url, isFront));
        });

        // 来源 PacketOpenBlockEntityScreen：请求打开该方块实体对应的屏幕（墙挂牌/自定义色方块/闸机状态屏等）
        Registry.registerReceiverS2C(PacketHandler.PACKET_OPEN_BLOCK_ENTITY_SCREEN, (buf, context) -> {
            final BlockPos pos = buf.readBlockPos();
            context.queue(() -> ClientPacketHelper.openBlockEntityScreen(pos));
        });

        // 来源 PacketOpenCRTPlatformScreen：请求打开 CRT 站台/导视牌屏幕
        Registry.registerReceiverS2C(PacketHandler.PACKET_OPEN_CRT_PLATFORM_SCREEN, (buf, context) -> {
            final BlockPos pos = buf.readBlockPos();
            context.queue(() -> ClientPacketHelper.openCRTPlatformScreen(pos));
        });
    }
}