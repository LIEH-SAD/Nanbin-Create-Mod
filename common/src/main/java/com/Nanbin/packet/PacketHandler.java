package com.Nanbin.packet;

import com.Nanbin.Registry.RegBlock.BlockBusTicketProcessor;
import com.Nanbin.Registry.RegBlock.BlockCRTStationName1;
import com.Nanbin.Registry.RegBlock.BlockCRTStationInfo1;
import com.Nanbin.Registry.RegBlock.BlockCRTStationInfo1Double;
import com.Nanbin.Registry.RegBlock.BlockRoadName;
import com.Nanbin.mapping.Registry;
import it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import mtr.data.TicketSystem;
import net.minecraft.item.Items;
import net.minecraft.scoreboard.ScoreboardPlayerScore;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class PacketHandler {
	public static final Identifier PACKET_OPEN_ROAD_NAME = new Identifier("nanbin", "open_road_name");
	public static final Identifier PACKET_UPDATE_ROAD_NAME = new Identifier("nanbin", "update_road_name");
	public static final Identifier PACKET_OPEN_CRT_STATION_NAME = new Identifier("nanbin", "open_crt_station_name");
	public static final Identifier PACKET_UPDATE_CRT_STATION_NAME = new Identifier("nanbin", "update_crt_station_name");
	public static final Identifier PACKET_OPEN_TICKET_MENU = new Identifier("nanbin", "open_ticket_menu");
	public static final Identifier PACKET_UPDATE_TICKET_MENU = new Identifier("nanbin", "update_ticket_menu");
	public static final Identifier PACKET_SYNC_TICKET_BALANCE = new Identifier("nanbin", "sync_ticket_balance");
	public static final Identifier PACKET_OPEN_BUS_TICKET_PROCESSOR = new Identifier("nanbin", "open_bus_ticket_processor");
	public static final Identifier PACKET_UPDATE_BUS_TICKET_PROCESSOR = new Identifier("nanbin", "update_bus_ticket_processor");
	public static final Identifier PACKET_OPEN_STATION_INFO = new Identifier("nanbin", "open_station_info");
	public static final Identifier PACKET_UPDATE_STATION_INFO = new Identifier("nanbin", "update_station_info");
	public static final Identifier PACKET_UPDATE_STATION_INFO_SIGN = new Identifier("nanbin", "update_station_info_sign");

	/** 售票机充值选项：充值金额。 */
	public static final int[] ADD_AMOUNTS = {10, 20, 30, 50, 100, 150, 200, 300};
	/** 售票机充值选项：所需绿宝石数量。 */
	public static final int[] EMERALD_COSTS = {1, 2, 3, 5, 10, 15, 20, 30};

	/** 服务器端注册 C2S 接收器（保存路名数据到方块实体）。 */
	public static void registerServerReceivers() {
		Registry.registerReceiverC2S(PACKET_UPDATE_ROAD_NAME, (server, player, buf) -> {
			final BlockPos pos = buf.readBlockPos();
			final int count = buf.readVarInt();
			final String[] texts = new String[count];
			for (int i = 0; i < count; i++) {
				texts[i] = buf.readString();
			}
			server.execute(() -> {
				if (player.getWorld().getBlockEntity(pos) instanceof BlockRoadName.BlockEntity entity) {
					entity.setTexts(texts);
				}
			});
		});

		Registry.registerReceiverC2S(PACKET_UPDATE_CRT_STATION_NAME, (server, player, buf) -> {
			final BlockPos pos = buf.readBlockPos();
			final long platformId = buf.readLong();
			final String routeNumber = buf.readString();
			server.execute(() -> {
				if (player.getWorld().getBlockEntity(pos) instanceof BlockCRTStationName1.BlockEntity entity) {
					entity.setData(platformId, routeNumber);
				}
			});
		});

		Registry.registerReceiverC2S(PACKET_UPDATE_TICKET_MENU, (server, player, buf) -> {
			final int index = buf.readInt();
			server.execute(() -> {
				if (index < 0 || index >= ADD_AMOUNTS.length) {
					return;
				}
				final World world = player.getWorld();
				TicketSystem.addObjectivesIfMissing(world);
				final ScoreboardPlayerScore score = TicketSystem.getPlayerScore(world, player, TicketSystem.BALANCE_OBJECTIVE);
				if (score != null && player.getInventory().count(Items.EMERALD) >= EMERALD_COSTS[index]) {
					score.setScore(score.getScore() + ADD_AMOUNTS[index]);
					player.getInventory().remove(itemStack -> itemStack.getItem() == Items.EMERALD, EMERALD_COSTS[index], player.getInventory());
				}
				final int balance = score == null ? 0 : score.getScore();
				Registry.sendPacketToClient(player, PACKET_SYNC_TICKET_BALANCE, buf2 -> buf2.writeInt(balance));
			});
		});

		Registry.registerReceiverC2S(PACKET_UPDATE_BUS_TICKET_PROCESSOR, (server, player, buf) -> {
			final BlockPos pos = buf.readBlockPos();
			final int mode = buf.readInt();
			final int amount = buf.readInt();
			server.execute(() -> {
				if (player.getWorld().getBlockEntity(pos) instanceof BlockBusTicketProcessor.BlockEntity entity) {
					entity.setData(mode, amount);
				}
			});
		});

		// 站台信息屏：保存 URL + 所选站台
		Registry.registerReceiverC2S(PACKET_UPDATE_STATION_INFO, (server, player, buf) -> {
			final BlockPos pos = buf.readBlockPos();
			final String url = buf.readString();
			final int count = buf.readVarInt();
			final LongAVLTreeSet selectedIds = new LongAVLTreeSet();
			for (int i = 0; i < count; i++) {
				selectedIds.add(buf.readLong());
			}
			final boolean isFront = buf.readBoolean();
			server.execute(() -> {
				if (player.getWorld().getBlockEntity(pos) instanceof BlockCRTStationInfo1.BlockEntity entity) {
					entity.setUrl(url);
					entity.setSelectedIdsLine(0, selectedIds);
				} else if (player.getWorld().getBlockEntity(pos) instanceof BlockCRTStationInfo1Double.BlockEntity entity) {
					entity.setUrl(isFront, url);
					entity.setSelectedIdsLine(isFront, 0, selectedIds);
				}
			});
		});

		// 站台信息屏：保存 2 行 × 7 格指示牌数据
		Registry.registerReceiverC2S(PACKET_UPDATE_STATION_INFO_SIGN, (server, player, buf) -> {
			final BlockPos pos = buf.readBlockPos();
			final int lineCount = buf.readVarInt();
			final String[][] signIds = new String[lineCount][];
			for (int line = 0; line < lineCount; line++) {
				final int cellCount = buf.readVarInt();
				signIds[line] = new String[cellCount];
				for (int cell = 0; cell < cellCount; cell++) {
					final String signId = buf.readString();
					signIds[line][cell] = signId.isEmpty() ? null : signId;
				}
			}
			final List<LongAVLTreeSet> selectedIds = new ArrayList<>();
			for (int line = 0; line < lineCount; line++) {
				final int idCount = buf.readVarInt();
				final LongAVLTreeSet lineIds = new LongAVLTreeSet();
				for (int i = 0; i < idCount; i++) {
					lineIds.add(buf.readLong());
				}
				selectedIds.add(lineIds);
			}
			final boolean isFront = buf.readBoolean();
			server.execute(() -> {
				if (player.getWorld().getBlockEntity(pos) instanceof BlockCRTStationInfo1.BlockEntity entity) {
					entity.setSignData(signIds, selectedIds);
				} else if (player.getWorld().getBlockEntity(pos) instanceof BlockCRTStationInfo1Double.BlockEntity entity) {
					entity.setSignData(isFront, signIds, selectedIds);
				}
			});
		});
	}
}
