package com.Nanbin.packet;

import com.Nanbin.Registry.RegBlock.BlockCRTStationInfo1;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import org.mtr.mapping.holder.BlockEntity;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.MinecraftServer;
import org.mtr.mapping.holder.ServerPlayerEntity;
import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;

import java.util.ArrayList;
import java.util.List;

public final class PacketUpdateStationInfoSignData extends PacketHandler {

    private final BlockPos blockPos;
    private final String[][] signIds;
    private final List<LongAVLTreeSet> selectedIds;

    public PacketUpdateStationInfoSignData(PacketBufferReceiver packetBufferReceiver) {
        blockPos = BlockPos.fromLong(packetBufferReceiver.readLong());
        final int lines = packetBufferReceiver.readInt();
        signIds = new String[lines][];
        selectedIds = new ArrayList<>();
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
    }

    public PacketUpdateStationInfoSignData(BlockPos blockPos, String[][] signIds, List<LongAVLTreeSet> selectedIds) {
        this.blockPos = blockPos;
        this.signIds = signIds;
        this.selectedIds = selectedIds;
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
    }

    @Override
    public void runServer(MinecraftServer minecraftServer, ServerPlayerEntity serverPlayerEntity) {
        final BlockEntity blockEntity = serverPlayerEntity.getEntityWorld().getBlockEntity(blockPos);
        if (blockEntity != null && blockEntity.data instanceof BlockCRTStationInfo1.BlockEntity entity) {
            entity.setSignData(signIds, selectedIds);
        }
    }
}
