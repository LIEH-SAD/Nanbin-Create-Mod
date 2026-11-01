package com.Nanbin.packet;

import com.Nanbin.Registry.RegBlock.BlockCRTStationInfo1;
import com.Nanbin.Registry.RegBlock.BlockCRTStationInfo1Double;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import org.mtr.mapping.holder.BlockEntity;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.MinecraftServer;
import org.mtr.mapping.holder.ServerPlayerEntity;
import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;

public final class PacketUpdateStationInfoData extends PacketHandler {

    private final BlockPos blockPos;
    private final String url;
    private final LongAVLTreeSet selectedIds;
    private final boolean isFront;

    public PacketUpdateStationInfoData(PacketBufferReceiver packetBufferReceiver) {
        blockPos = BlockPos.fromLong(packetBufferReceiver.readLong());
        url = packetBufferReceiver.readString();
        final int size = packetBufferReceiver.readInt();
        selectedIds = new LongAVLTreeSet();
        for (int i = 0; i < size; i++) {
            selectedIds.add(packetBufferReceiver.readLong());
        }
        isFront = packetBufferReceiver.readBoolean();
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
    public void runServer(MinecraftServer minecraftServer, ServerPlayerEntity serverPlayerEntity) {
        final BlockEntity blockEntity = serverPlayerEntity.getEntityWorld().getBlockEntity(blockPos);
        if (blockEntity != null && blockEntity.data instanceof BlockCRTStationInfo1.BlockEntity) {
            final BlockCRTStationInfo1.BlockEntity entity = (BlockCRTStationInfo1.BlockEntity) blockEntity.data;
            entity.setUrl(url);
            entity.setSelectedIdsLine(0, selectedIds);
        } else if (blockEntity != null && blockEntity.data instanceof BlockCRTStationInfo1Double.BlockEntity) {
            final BlockCRTStationInfo1Double.BlockEntity entity = (BlockCRTStationInfo1Double.BlockEntity) blockEntity.data;
            entity.setUrl(isFront, url);
            entity.setSelectedIdsLine(isFront, 0, selectedIds);
        }
    }
}