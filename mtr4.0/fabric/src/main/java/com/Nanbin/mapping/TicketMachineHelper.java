
package com.Nanbin.mapping;

import com.Nanbin.packet.PacketOpenTicketMachineScreen;
import org.mtr.mapping.holder.*;
import org.mtr.mod.InitClient;
import org.mtr.mod.data.IGui;
import org.mtr.mod.data.TicketSystem;


public class TicketMachineHelper {
    public static void openTicketMachineScreen(World world, PlayerEntity player) {
        if (world.isClient()) {
            return;
        }
        long playerBalance = TicketSystem.getBalance(world, player);
        Registry.REGISTRY.sendPacketToClient(ServerPlayerEntity.cast(player), new PacketOpenTicketMachineScreen((int) playerBalance));
    }

    /**
     * 获取玩家当前所处的车站名称。未处于任何车站时返回空字符串。仅在客户端调用。
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
     * 名称处理
     */
    public static String formatStationName(String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }
        final String[] parts = name.split("\\|");
        final StringBuilder result = new StringBuilder();
        for (final String part : parts) {
            // 出现空段说明是 "||" 或更多连续 "|"，其后的内容全部隐藏
            if (part.isEmpty()) {
                break;
            }
            if (result.length() > 0) {
                result.append(' ');
            }
            result.append(part);
        }
        return result.toString();
    }
}