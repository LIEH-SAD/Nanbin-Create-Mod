package com.Nanbin.packet;

import com.Nanbin.Registry.RegBlock.BlockCRTRailwaySign;
import org.mtr.mapping.holder.BlockEntity;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.MinecraftServer;
import org.mtr.mapping.holder.ServerPlayerEntity;
import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;

/**
 * 客户端 → 服务端：更新某个格子独立选择的线路颜色集合。
 * 一块告示牌上的多个线路名牌区域可以各自选择不同的线路，互不影响。
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
        this.colors = colors;
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
    public void runServer(MinecraftServer minecraftServer, ServerPlayerEntity serverPlayerEntity) {
        final BlockEntity entity = serverPlayerEntity.getEntityWorld().getBlockEntity(blockPos);
        if (entity != null && entity.data instanceof BlockCRTRailwaySign.BlockEntityCRTRailwaySign) {
            ((BlockCRTRailwaySign.BlockEntityCRTRailwaySign) entity.data).setCellColors(signIndex, colors);
        }
    }
}
