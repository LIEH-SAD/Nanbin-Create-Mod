package com.Nanbin.mapping;

import com.Nanbin.packet.PacketHandler;
import mtr.client.ClientData;
import mtr.data.IGui;
import mtr.data.Station;
import mtr.data.TicketSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.ScoreboardPlayerScore;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * MTR 4.0 {@code com.Nanbin.mapping.TicketMachineHelper} 的 Architectury/1.20.1 版本。
 *
 * <p>来源里打开售票机界面走 MTR 4 的 {@code Registry.REGISTRY.sendPacketToClient(...)} +
 * {@code PacketOpenTicketMachineScreen}；目标侧没有这个包类，改用本工程既有的
 * {@link PacketHandler#PACKET_OPEN_TICKET_MENU}（与 {@code BlockCRTTicketMachine1/2} 完全同一条通道，
 * 负载同样是 4 字节余额）。余额读取也换成 MTR 3.6.3 的 {@link TicketSystem#getPlayerScore}。</p>
 *
 * <p>车站名解析：来源用 {@code InitClient.findStation(BlockPos)}（MTR 4 core data）；
 * 目标侧没有该方法，改为在 MTR 3.6.3 的客户端数据缓存 {@link ClientData#DATA_CACHE} 里按
 * {@link Station#inArea(int, int)}（X/Z 平面区域）匹配玩家所在车站。</p>
 */
public final class TicketMachineHelper {

	private TicketMachineHelper() {
	}

	public static void openTicketMachineScreen(World world, PlayerEntity player) {
		if (world.isClient() || !(player instanceof ServerPlayerEntity serverPlayer)) {
			return;
		}
		TicketSystem.addObjectivesIfMissing(world);
		final ScoreboardPlayerScore score = TicketSystem.getPlayerScore(world, player, TicketSystem.BALANCE_OBJECTIVE);
		final int balance = score == null ? 0 : score.getScore();
		Registry.sendPacketToClient(serverPlayer, PacketHandler.PACKET_OPEN_TICKET_MENU, buf -> buf.writeInt(balance));
	}

	/**
	 * 获取玩家当前所在的车站名称。未处于任何车站时返回空字符串。仅在客户端调用。
	 */
	public static String getCurrentStationName() {
		final ClientPlayerEntity player = MinecraftClient.getInstance().player;
		if (player == null) {
			return "";
		}
		final BlockPos pos = player.getBlockPos();
		for (final Station station : ClientData.DATA_CACHE.stationIdMap.values()) {
			if (station.inArea(pos.getX(), pos.getZ())) {
				return formatStationName(IGui.textOrUntitled(station.name));
			}
		}
		return "";
	}

	/**
	 * 名称处理：MTR 多语言站名用 {@code |} 分隔；出现空段说明是 {@code "||"} 或更多连续 {@code "|"}，其后的内容全部隐藏。
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
				result.append(' ');
			}
			result.append(part);
		}
		return result.toString();
	}
}
