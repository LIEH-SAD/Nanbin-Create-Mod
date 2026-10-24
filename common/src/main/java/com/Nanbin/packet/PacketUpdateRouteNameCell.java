package com.Nanbin.packet;

import com.Nanbin.Registry.RegBlock.BlockCRTRailwaySign;
import net.minecraft.util.math.BlockPos;
import org.mtr.packet.PacketBufferReceiver;
import org.mtr.packet.PacketBufferSender;
import org.mtr.packet.PacketHandler;

/**
 * 客户端 → 服务端：更新告示牌上某一格（线路牌区域）独立选择的线路颜色集合。
 * <p>
 * 一块告示牌上的多个线路牌区域可以各自选择不同的线路，互不影响。
 * 4.0 是把「每格覆盖色」存在 CRT 自己的 {@code cellColors} 里；
 * 4.1 的 MTR 父类本身就是「一格一个 selectedIds 集合」，所以直接写进父类数据即可。
 */
public final class PacketUpdateRouteNameCell extends PacketHandler {

	private final BlockPos blockPos;
	private final int signIndex;
	private final long[] colors;

	public PacketUpdateRouteNameCell(PacketBufferReceiver packetBufferReceiver) {
		this(
				BlockPos.fromLong(packetBufferReceiver.readLong()),
				packetBufferReceiver.readInt(),
				readColors(packetBufferReceiver)
		);
	}

	public PacketUpdateRouteNameCell(BlockPos pos, int signIndex, long[] colors) {
		this.blockPos = pos;
		this.signIndex = signIndex;
		this.colors = colors == null ? new long[0] : colors;
	}

	private static long[] readColors(PacketBufferReceiver packetBufferReceiver) {
		final int count = packetBufferReceiver.readInt();
		final long[] colors = new long[count];
		for (int i = 0; i < count; i++) {
			colors[i] = packetBufferReceiver.readLong();
		}
		return colors;
	}

	@Override
	public void write(PacketBufferSender packetBufferSender) {
		packetBufferSender.writeLong(blockPos.asLong());
		packetBufferSender.writeInt(signIndex);
		packetBufferSender.writeInt(colors.length);
		for (final long color : colors) {
			packetBufferSender.writeLong(color);
		}
	}

	@Override
	public void runServer(net.minecraft.server.MinecraftServer minecraftServer, net.minecraft.server.network.ServerPlayerEntity serverPlayerEntity) {
		final net.minecraft.block.entity.BlockEntity entity = serverPlayerEntity.getEntityWorld().getBlockEntity(blockPos);
		if (entity instanceof BlockCRTRailwaySign.BlockEntityCRTRailwaySign signEntity) {
			signEntity.setSelectedIdsCell(signIndex, colors);
		}
	}
}
