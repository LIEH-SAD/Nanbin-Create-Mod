package com.Nanbin.packet;

import com.Nanbin.Registry.RegBlock.BlockCRTStationName1;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;

public class PacketSyncStationNameData extends PacketHandler {
    private final BlockPos blockPos;
    private final int routeColor;
    private final String routeNumber;
    private final String platformName;

    private static final String EMPTY = "";

    public PacketSyncStationNameData(PacketBufferReceiver receiver) {
        blockPos = BlockPos.fromLong(receiver.readLong());
        routeColor = receiver.readInt();
        routeNumber = receiver.readString();
        platformName = receiver.readString();
    }

    public PacketSyncStationNameData(BlockPos blockPos, int routeColor, String routeNumber, String platformName) {
        this.blockPos = blockPos;
        this.routeColor = routeColor;
        this.routeNumber = routeNumber == null ? EMPTY : routeNumber;
        this.platformName = platformName == null ? EMPTY : platformName;
    }

    @Override
    public void write(PacketBufferSender sender) {
        sender.writeLong(blockPos.asLong());
        sender.writeInt(routeColor);
        sender.writeString(routeNumber);
        sender.writeString(platformName);
    }

    @Override
    public void runServer(MinecraftServer server, ServerPlayerEntity player) {
        final World world = player.getEntityWorld();
        applyToEntity(world);
    }

    @Override
    public void runClient() {
        final MinecraftClient client = MinecraftClient.getInstance();
        final ClientWorld clientWorld = client.getWorldMapped();
        if (clientWorld != null && World.isInstance(clientWorld)) {
            applyToEntity(World.cast(clientWorld));
        }
    }

    private void applyToEntity(World world) {
        if (world == null) return;
        final BlockEntity blockEntity = world.getBlockEntity(blockPos);
        if (blockEntity != null && blockEntity.data instanceof BlockCRTStationName1.BlockEntity entity) {
            entity.setRouteColor(routeColor);
            entity.setRouteNumber(routeNumber);
            entity.setPlatformName(platformName);
            entity.markDirty2();
        }
    }
}
