
package com.Nanbin.Registry.RegMean;

import org.mtr.mapping.holder.PlayerEntity;
import org.mtr.mapping.holder.ServerPlayerEntity;
import org.mtr.mapping.holder.World;
import org.mtr.mod.Init;
import org.mtr.mod.data.TicketSystem;
import org.mtr.mod.packet.PacketOpenTicketMachineScreen;


public class TicketMachineHelper {

    public static void openTicketMachineScreen(World world, PlayerEntity player) {
        // 仅在服务端执行，避免客户端报错
        if (world.isClient()) {
            return;
        }

        // 获取玩家票务余额
        long playerBalance = TicketSystem.getBalance(world, player);

        // 发送数据包打开售票机界面
        Init.REGISTRY.sendPacketToClient(ServerPlayerEntity.cast(player), new PacketOpenTicketMachineScreen((int) playerBalance));
    }
}