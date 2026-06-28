
package com.Nanbin.Registry.RegMean;

import org.mtr.mapping.holder.*;
import org.mtr.mod.Init;
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
}