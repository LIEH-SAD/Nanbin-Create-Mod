package com.Nanbin.mapping;

import com.Nanbin.Registry.SoundEvents;
import com.Nanbin.packet.PacketOpenTicketMachineScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.mtr.MTRClient;
import org.mtr.data.IGui;
import org.mtr.data.TicketSystem;

/**
 * 取代 MTR 4.0 的 {@code org.mtr.mapping.TicketMachineHelper}（自建工具类）。
 * <p>
 * 4.1 侧的变化：{@code PlayerEntity#getBlockPos()} / {@code MinecraftClient.getInstance().player}；
 * MTR 的找站方法从 {@code org.mtr.mod.InitClient.findStation} 挪到 {@link MTRClient}；
 * 站名来源 {@code station.getName()} 配合 {@link IGui#textOrUntitled}。
 */
public final class TicketMachineHelper {

	private TicketMachineHelper() {
	}

	public static void openTicketMachineScreen(World world, PlayerEntity player) {
		if (world.isClient) {
			return;
		}
		final long playerBalance = TicketSystem.getBalance(world, player);
		Registry.sendPacketToClient((ServerPlayerEntity) player, new PacketOpenTicketMachineScreen((int) playerBalance));
	}

	/** 获取玩家当前所处的车站名称。未处于任何车站时返回空字符串。仅在客户端调用。 */
	public static String getCurrentStationName() {
		final net.minecraft.client.network.ClientPlayerEntity player = net.minecraft.client.MinecraftClient.getInstance().player;
		if (player == null) {
			return "";
		}
		final org.mtr.core.data.Station station = MTRClient.findStation(player.getBlockPos());
		return station == null ? "" : formatStationName(IGui.textOrUntitled(station.getName()));
	}

	/** 名称处理：遇到空段（即 "||"）就隐藏其后的全部内容。 */
	public static String formatStationName(String name) {
		if (name == null || name.isEmpty()) {
			return "";
		}
		final StringBuilder result = new StringBuilder();
		for (final String part : name.split("\\|")) {
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
