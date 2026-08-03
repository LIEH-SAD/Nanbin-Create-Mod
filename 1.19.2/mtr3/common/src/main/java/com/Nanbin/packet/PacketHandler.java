package com.Nanbin.packet;

import com.Nanbin.Registry.RegBlock.BlockCRTStationName1;
import com.Nanbin.Registry.RegBlock.BlockRoadName;
import com.Nanbin.mapping.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class PacketHandler {
	/** 服务器 -> 客户端：打开路名配置界面 */
	public static final Identifier PACKET_OPEN_ROAD_NAME = new Identifier("nanbin", "open_road_name");
	/** 客户端 -> 服务器：保存路名数据 */
	public static final Identifier PACKET_UPDATE_ROAD_NAME = new Identifier("nanbin", "update_road_name");

	/** 服务器 -> 客户端：打开 CRT 站名牌配置界面 */
	public static final Identifier PACKET_OPEN_CRT_STATION_NAME = new Identifier("nanbin", "open_crt_station_name");
	/** 客户端 -> 服务器：保存 CRT 站名牌数据（站台 ID + 线路编号） */
	public static final Identifier PACKET_UPDATE_CRT_STATION_NAME = new Identifier("nanbin", "update_crt_station_name");

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
	}
}
