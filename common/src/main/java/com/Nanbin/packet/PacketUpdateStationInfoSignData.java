package com.Nanbin.packet;

import com.Nanbin.Registry.RegBlock.BlockCRTStationInfoBase;
import net.minecraft.util.math.BlockPos;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import org.mtr.packet.PacketBufferReceiver;
import org.mtr.packet.PacketBufferSender;
import org.mtr.packet.PacketHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * 客户端 → 服务端：整块站台信息屏的告示牌数据（2 行 × 7 格的标志 + 每行选中的站台）。
 */
public final class PacketUpdateStationInfoSignData extends PacketHandler {

	private final BlockPos blockPos;
	private final String[][] signIds;
	private final List<LongAVLTreeSet> selectedIds;
	private final boolean isFront;

	public PacketUpdateStationInfoSignData(PacketBufferReceiver packetBufferReceiver) {
		this.blockPos = BlockPos.fromLong(packetBufferReceiver.readLong());
		final int lines = packetBufferReceiver.readInt();
		this.signIds = new String[lines][];
		this.selectedIds = new ArrayList<>();
		for (int i = 0; i < lines; i++) {
			final int length = packetBufferReceiver.readInt();
			signIds[i] = new String[length];
			for (int j = 0; j < length; j++) {
				final String signId = packetBufferReceiver.readString();
				signIds[i][j] = signId.isEmpty() ? null : signId;
			}
			final int size = packetBufferReceiver.readInt();
			final LongAVLTreeSet set = new LongAVLTreeSet();
			for (int k = 0; k < size; k++) {
				set.add(packetBufferReceiver.readLong());
			}
			selectedIds.add(set);
		}
		this.isFront = packetBufferReceiver.readBoolean();
	}

	public PacketUpdateStationInfoSignData(BlockPos blockPos, String[][] signIds, List<LongAVLTreeSet> selectedIds, boolean isFront) {
		this.blockPos = blockPos;
		this.signIds = signIds;
		this.selectedIds = selectedIds;
		this.isFront = isFront;
	}

	@Override
	public void write(PacketBufferSender packetBufferSender) {
		packetBufferSender.writeLong(blockPos.asLong());
		packetBufferSender.writeInt(signIds.length);
		for (int i = 0; i < signIds.length; i++) {
			final String[] lineSignIds = signIds[i] == null ? new String[0] : signIds[i];
			packetBufferSender.writeInt(lineSignIds.length);
			for (final String signId : lineSignIds) {
				packetBufferSender.writeString(signId == null ? "" : signId);
			}
			final LongAVLTreeSet set = selectedIds.get(i);
			packetBufferSender.writeInt(set.size());
			set.forEach(packetBufferSender::writeLong);
		}
		packetBufferSender.writeBoolean(isFront);
	}

	@Override
	public void runServer(net.minecraft.server.MinecraftServer minecraftServer, net.minecraft.server.network.ServerPlayerEntity serverPlayerEntity) {
		final net.minecraft.block.entity.BlockEntity blockEntity = serverPlayerEntity.getEntityWorld().getBlockEntity(blockPos);
		if (blockEntity instanceof BlockCRTStationInfoBase.BlockEntityBase entity) {
			entity.setSignData(isFront, signIds, selectedIds);
		}
	}
}
