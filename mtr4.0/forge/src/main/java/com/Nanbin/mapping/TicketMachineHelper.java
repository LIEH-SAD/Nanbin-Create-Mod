
package com.Nanbin.mapping;

import org.mtr.mapping.holder.ClientPlayerEntity;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.PlayerEntity;
import org.mtr.mapping.holder.ServerPlayerEntity;
import org.mtr.mapping.holder.World;
import org.mtr.mod.Init;
import org.mtr.mod.InitClient;
import org.mtr.mod.data.IGui;
import org.mtr.mod.data.TicketSystem;
import org.mtr.mod.packet.PacketOpenTicketMachineScreen;


public class TicketMachineHelper {
    public static void openTicketMachineScreen(World world, PlayerEntity player) {
        if (world.isClient()) {
            return;
        }

        long playerBalance = TicketSystem.getBalance(world, player);

        Init.REGISTRY.sendPacketToClient(ServerPlayerEntity.cast(player), new PacketOpenTicketMachineScreen((int) playerBalance));
    }

    /**
     * Get the station name where the player is currently located. Returns empty string when not in any station. Client-side only.
     */
    public static String getCurrentStationName() {
        final ClientPlayerEntity player = MinecraftClient.getInstance().getPlayerMapped();
        if (player == null) {
            return "";
        }
        final org.mtr.core.data.Station station = InitClient.findStation(player.getBlockPos());
        return station == null ? "" : formatStationName(IGui.textOrUntitled(station.getName()));
    }

    /**
     * Name processing
     */
    public static String formatStationName(String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }
        final String[] parts = name.split("\\|");
        final StringBuilder result = new StringBuilder();
        for (final String part : parts) {
            if (part.isEmpty()) {
                break;
            }
            if (result.length() > 0) {
                result.append(" ");
            }
            result.append(part);
        }
        return result.toString();
    }
}