package com.Nanbin.packet;

import com.Nanbin.Init;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.ScreenExtension;
import org.mtr.mod.screen.RailwaySignScreen;
import org.mtr.mod.screen.TicketMachineScreen;

import java.util.function.Predicate;

public final class ClientPacketHelper {

    public static void openRailwaySignScreen(BlockPos blockPos) {
        Init.LOGGER.info("open requested for pos={}", blockPos);
        // RailwaySignScreen natively handles BlockRouteSignBase$BlockEntityBase:
        //   - constructor  → loads saved platformId into selectedIds
        //   - onClose2()   → sends PacketUpdateRailwaySignConfig
        //   - server side  → calls setPlatformId() on our entity
        openScreen(new RailwaySignScreen(blockPos), screen -> screen instanceof RailwaySignScreen);
    }

    public static void openTicketMachineScreen(int balance) {
        openScreen(new TicketMachineScreen(balance), screen -> screen instanceof TicketMachineScreen);
    }

    private static void openScreen(ScreenExtension screenExtension, Predicate<ScreenExtension> isInstance) {
        final MinecraftClient minecraftClient = MinecraftClient.getInstance();
        final Screen screen = minecraftClient.getCurrentScreenMapped();
        if (screen == null || screen.data instanceof ScreenExtension && !isInstance.test((ScreenExtension) screen.data)) {
            minecraftClient.openScreen(new Screen(screenExtension));
        }
    }
}
