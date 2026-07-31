package com.Nanbin.packet;

import com.Nanbin.Init;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.Screen;
import org.mtr.mapping.mapper.ScreenExtension;
import org.mtr.mod.screen.RailwaySignScreen;
import org.mtr.mod.screen.TicketMachineScreen;

import java.util.function.Predicate;

public final class ClientPacketHelper {

    public static void openRailwaySignScreen(BlockPos blockPos) {
        Init.LOGGER.info("open requested for pos={}", blockPos);
        openScreen(new RailwaySignScreen(blockPos), screen -> screen instanceof RailwaySignScreen);
    }

    public static void openTicketMachineScreen(int balance) {
        openScreen(new TicketMachineScreen(balance), screen -> screen instanceof TicketMachineScreen);
    }

    public static void openScreen(ScreenExtension screenExtension, Predicate<ScreenExtension> isInstance) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        Screen screen = minecraftClient.getCurrentScreenMapped();
        if (screen == null || screen.data instanceof ScreenExtension && !isInstance.test((ScreenExtension) screen.data)) {
            minecraftClient.openScreen(new Screen(screenExtension));
        }
    }
}
