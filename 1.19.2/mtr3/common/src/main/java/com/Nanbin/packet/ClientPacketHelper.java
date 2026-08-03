package com.Nanbin.packet;

import com.Nanbin.Init;
import com.Nanbin.client.Screen.RoadNameScreen;
import com.Nanbin.mapping.Registry;
import mtr.screen.RailwaySignScreen;
import mtr.screen.TicketMachineScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.math.BlockPos;

import java.util.function.Predicate;

public class ClientPacketHelper {

    public static void openRailwaySignScreen(BlockPos blockPos) {
        openScreen(new RailwaySignScreen(blockPos), screen -> screen instanceof RailwaySignScreen);
    }

    public static void openRoadNameScreen(BlockPos blockPos, String[] texts) {
        Init.LOGGER.info("open road name screen requested for pos={}", blockPos);
        openScreen(new RoadNameScreen(blockPos, texts), screen -> screen instanceof RoadNameScreen);
    }

    /** 客户端把四个文本框内容发回服务器 */
    public static void saveRoadNameScreen(BlockPos blockPos, String[] texts) {
        Registry.sendPacketToServer(PacketHandler.PACKET_UPDATE_ROAD_NAME, buf -> {
            buf.writeBlockPos(blockPos);
            buf.writeVarInt(texts.length);
            for (String text : texts) {
                buf.writeString(text);
            }
        });
    }

    /** 客户端把站台 ID 与线路编号发回服务器 */
    public static void saveCRTStationNameScreen(BlockPos blockPos, long platformId, String routeNumber) {
        Registry.sendPacketToServer(PacketHandler.PACKET_UPDATE_CRT_STATION_NAME, buf -> {
            buf.writeBlockPos(blockPos);
            buf.writeLong(platformId);
            buf.writeString(routeNumber);
        });
    }

    public static void openTicketMachineScreen(int balance) {
        openScreen(new TicketMachineScreen(balance), screen -> screen instanceof TicketMachineScreen);
    }

    private static void openScreen(Screen screen, Predicate<Screen> isInstance) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        Screen currentScreen = minecraftClient.currentScreen;
        if (currentScreen == null || !isInstance.test(currentScreen)) {
            minecraftClient.setScreen(screen);
        }
    }
}