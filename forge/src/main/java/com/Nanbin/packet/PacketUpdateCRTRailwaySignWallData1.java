package com.Nanbin.packet;

import com.Nanbin.Registry.RegBlock.BlockCRTRailwaySignWall1;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.BlockView;
import org.mtr.mapping.holder.MinecraftServer;
import org.mtr.mapping.holder.ServerPlayerEntity;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;

import java.util.List;

// 墙挂指示牌编辑保存：客户端把整条牌的格子内容一次性发回，服务端按当前连接长度分配给每个方块实体。
public final class PacketUpdateCRTRailwaySignWallData1 extends PacketHandler {

    private final BlockPos startPos;
    private final String[] signIds;
    private final LongAVLTreeSet selectedIds;

    public PacketUpdateCRTRailwaySignWallData1(PacketBufferReceiver packetBufferReceiver) {
        startPos = BlockPos.fromLong(packetBufferReceiver.readLong());
        final int length = packetBufferReceiver.readInt();
        signIds = new String[length];
        for (int i = 0; i < length; i++) {
            final String signId = packetBufferReceiver.readString();
            signIds[i] = signId.isEmpty() ? null : signId;
        }
        selectedIds = new LongAVLTreeSet();
        final int size = packetBufferReceiver.readInt();
        for (int i = 0; i < size; i++) {
            selectedIds.add(packetBufferReceiver.readLong());
        }
    }

    public PacketUpdateCRTRailwaySignWallData1(BlockPos startPos, String[] signIds, LongAVLTreeSet selectedIds) {
        this.startPos = startPos;
        this.signIds = signIds;
        this.selectedIds = selectedIds;
    }

    @Override
    public void write(PacketBufferSender packetBufferSender) {
        packetBufferSender.writeLong(startPos.asLong());
        packetBufferSender.writeInt(signIds.length);
        for (final String signId : signIds) {
            packetBufferSender.writeString(signId == null ? "" : signId);
        }
        packetBufferSender.writeInt(selectedIds.size());
        selectedIds.forEach(packetBufferSender::writeLong);
    }

    @Override
    public void runServer(MinecraftServer minecraftServer, ServerPlayerEntity serverPlayerEntity) {
        final World world = serverPlayerEntity.getEntityWorld();
        if (!org.mtr.mod.Init.isChunkLoaded(world, startPos)) {
            return;
        }
        final BlockState state = world.getBlockState(startPos);
        if (!BlockCRTRailwaySignWall1.isWallSign(state)) {
            return;
        }
        final List<BlockPos> run = BlockCRTRailwaySignWall1.collectRun(BlockView.cast(world), startPos);
        final int cellsPerBlock = BlockCRTRailwaySignWall1.CELLS_PER_BLOCK;
        for (int i = 0; i < run.size(); i++) {
            final BlockCRTRailwaySignWall1.BlockEntity entity = BlockCRTRailwaySignWall1.getBlockEntity(BlockView.cast(world), run.get(i));
            if (entity != null) {
                final String[] cells = new String[cellsPerBlock];
                for (int j = 0; j < cellsPerBlock; j++) {
                    final int cell = i * cellsPerBlock + j;
                    cells[j] = cell < signIds.length ? signIds[cell] : null;
                }
                entity.setData(cells, selectedIds);
            }
        }
    }
}
