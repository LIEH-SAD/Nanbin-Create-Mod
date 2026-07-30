package com.Nanbin.packet;

import com.Nanbin.Registry.RegBlock.BlockCRTStationName1;
import com.Nanbin.Init;
import org.mtr.core.data.Platform;
import org.mtr.core.data.Route;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;

import java.lang.reflect.Field;

/**
 * 客户端 → 服务端：请求指定站名牌的路线数据。
 * 服务端通过反射访问 MTR Init.main 获取 Simulator，从中查找到 platform 的路线信息，
 * 然后直接保存到方块实体的 NBT 中。后续区块同步会将数据传回客户端。
 */
public class PacketRequestPlatformRouteData extends PacketHandler {

    private final BlockPos blockPos;

    public PacketRequestPlatformRouteData(PacketBufferReceiver receiver) {
        blockPos = BlockPos.fromLong(receiver.readLong());
    }

    public PacketRequestPlatformRouteData(BlockPos blockPos) {
        this.blockPos = blockPos;
    }

    @Override
    public void write(PacketBufferSender sender) {
        sender.writeLong(blockPos.asLong());
    }

    @Override
    public void runServer(MinecraftServer server, ServerPlayerEntity player) {
        final World world = player.getEntityWorld();
        final BlockEntity blockEntity = world.getBlockEntity(blockPos);
        if (blockEntity == null || !(blockEntity.data instanceof BlockCRTStationName1.BlockEntity entity)) {
            return;
        }

        final long platformId = entity.getPlatformId();
        if (platformId == 0) return;

        resolveAndSaveRouteData(entity, platformId, player);
    }

    /**
     * 在服务端通过反射访问 MTR Init.main，找到当前世界的 Simulator，
     * 从中获取 platformId 对应的站台和路线数据，保存到方块实体 NBT，
     * 并发送响应包回客户端立即更新。
     */
    private void resolveAndSaveRouteData(BlockCRTStationName1.BlockEntity entity, long platformId, ServerPlayerEntity player) {
        try {
            final Field mainField = org.mtr.mod.Init.class.getDeclaredField("main");
            mainField.setAccessible(true);
            final Object main = mainField.get(null);
            if (main == null) {
                Init.LOGGER.warn("MTR main not initialized, cannot resolve route data");
                return;
            }

            final Field simulatorsField = main.getClass().getDeclaredField("simulators");
            simulatorsField.setAccessible(true);
            final Object simulatorsObj = simulatorsField.get(main);
            if (simulatorsObj == null) return;

            final String worldId = org.mtr.mod.Init.getWorldId(entity.getWorld2());

            for (final Object obj : (Iterable<?>) simulatorsObj) {
                final org.mtr.core.simulation.Simulator simulator = (org.mtr.core.simulation.Simulator) obj;
                if (simulator.dimension.equals(worldId)) {
                    final Platform platform = simulator.platformIdMap.get(platformId);
                    if (platform != null && !platform.routes.isEmpty()) {
                        final Route route = platform.routes.iterator().next();
                        final int color = route.getColor();
                        final String number = route.getRouteNumber();
                        final String name = platform.getName();

                        // 保存到服务端 NBT
                        entity.setRouteColor(color);
                        entity.setRouteNumber(number);
                        entity.setPlatformName(name);
                        entity.markDirty2();

                        // 立即发回客户端更新内存中的实体
                        Init.REGISTRY.sendPacketToClient(player,
                                new PacketSyncStationNameData(entity.getPos2(), color, number, name));

                        Init.LOGGER.info("Saved route data for platform {}: color={}, number={}, name={}",
                                platformId, color, number, name);
                    } else {
                        Init.LOGGER.warn("Platform {} found in Simulator but routes is {}",
                                platformId, platform == null ? "null" : "empty");
                    }
                    break;
                }
            }
        } catch (Exception e) {
            Init.LOGGER.error("Failed to resolve route data for platform {}", platformId, e);
        }
    }
}
