package com.Nanbin.packet;

import com.Nanbin.Registry.RegBlock.BlockCRTStationInfoBase;
import net.minecraft.util.math.BlockPos;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import org.mtr.packet.PacketBufferReceiver;
import org.mtr.packet.PacketBufferSender;
import org.mtr.packet.PacketHandler;

/**
 * 客户端 → 服务端：更新站台信息屏的 url 与第 0 行选中的站台集合（正 / 反面分别更新）。
 */
public final class PacketUpdateStationInfoData extends PacketHandler {

	private final BlockPos blockPos;
	private final String url;
	private final LongAVLTreeSet selectedIds;
	private final boolean isFront;

	public PacketUpdateStationInfoData(PacketBufferReceiver packetBufferReceiver) {
		this.blockPos = BlockPos.fromLong(packetBufferReceiver.readLong());
		this.url = packetBufferReceiver.readString();
		final int size = packetBufferReceiver.readInt();
		this.selectedIds = new LongAVLTreeSet();
		for (int i = 0; i < size; i++) {
			selectedIds.add(packetBufferReceiver.readLong());
		}
		this.isFront = packetBufferReceiver.readBoolean();
	}

	public PacketUpdateStationInfoData(BlockPos blockPos, String url, LongAVLTreeSet selectedIds, boolean isFront) {
		this.blockPos = blockPos;
		this.url = url != null ? url : "";
		this.selectedIds = selectedIds != null ? selectedIds : new LongAVLTreeSet();
		this.isFront = isFront;
	}

	@Override
	public void write(PacketBufferSender packetBufferSender) {
		packetBufferSender.writeLong(blockPos.asLong());
		packetBufferSender.writeString(url);
		packetBufferSender.writeInt(selectedIds.size());
		selectedIds.forEach(packetBufferSender::writeLong);
		packetBufferSender.writeBoolean(isFront);
	}

	@Override
	public void runServer(net.minecraft.server.MinecraftServer minecraftServer, net.minecraft.server.network.ServerPlayerEntity serverPlayerEntity) {
		final net.minecraft.block.entity.BlockEntity blockEntity = serverPlayerEntity.getEntityWorld().getBlockEntity(blockPos);
		if (blockEntity instanceof BlockCRTStationInfoBase.BlockEntityBase entity) {
			entity.setUrl(isFront, url);
			entity.setSelectedIdsLine(isFront, 0, selectedIds);
		}
	}
}
