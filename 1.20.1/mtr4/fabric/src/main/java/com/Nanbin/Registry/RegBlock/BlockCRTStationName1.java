package com.Nanbin.Registry.RegBlock;

import com.Nanbin.packet.PacketOpenCRTPlatformScreen;
import com.Nanbin.entity.BlockEntityTypes;
import com.Nanbin.Init;
import com.Nanbin.mapping.IBlockExtension;
import org.mtr.core.data.Platform;
import org.mtr.core.data.Route;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mapping.mapper.BlockWithEntity;
import org.mtr.mapping.tool.HolderBase;
import org.mtr.mod.block.BlockRouteSignBase;
import org.mtr.mod.block.IBlock;
import org.mtr.mod.client.MinecraftClientData;

import javax.annotation.Nonnull;
import java.util.List;


public class BlockCRTStationName1 extends BlockStationNameBase implements BlockWithEntity {

    public BlockCRTStationName1(BlockSettings blockSettings) {
        super(blockSettings);
    }

    @Nonnull
    public BlockEntityExtension createBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new BlockCRTStationName1.BlockEntity(blockPos, blockState);
    }

    @Override
    public @Nonnull ActionResult onUse2(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        return IBlockExtension.checkHoldingBrush(world, player, () -> Init.REGISTRY.sendPacketToClient(ServerPlayerEntity.cast(player), new PacketOpenCRTPlatformScreen(pos)));
    }


    public void addBlockProperties(List<HolderBase<?>> properties) {
        properties.add(FACING);
    }

    public static class BlockEntity extends BlockRouteSignBase.BlockEntityBase {

        private int routeColor;
        private String routeNumber = "";
        private String platformName = "";

        private static final String KEY_ROUTE_COLOR = "route_color";
        private static final String KEY_ROUTE_NUMBER = "route_number";
        private static final String KEY_PLATFORM_NAME = "platform_name";

        public BlockEntity(BlockPos pos, BlockState state) {
            super(BlockEntityTypes.CRT_STATION_NAME_1.get(), pos, state);
        }

        @Override
        public void readCompoundTag(CompoundTag tag) {
            super.readCompoundTag(tag);
            routeColor = tag.getInt(KEY_ROUTE_COLOR);
            routeNumber = tag.getString(KEY_ROUTE_NUMBER);
            platformName = tag.getString(KEY_PLATFORM_NAME);
            resolvePlatformData();
        }

        @Override
        public void writeCompoundTag(CompoundTag tag) {
            super.writeCompoundTag(tag);
            tag.putInt(KEY_ROUTE_COLOR, routeColor);
            tag.putString(KEY_ROUTE_NUMBER, routeNumber);
            tag.putString(KEY_PLATFORM_NAME, platformName);
        }

        /**
         * 从保存的 platformId 查找站台，缓存路线信息到字段。
         * 在 readCompoundTag（区块加载）、setPlatformId（PacketUpdateRailwaySignConfig 服务端回调）
         * 和 setData（GUI 关闭后）时自动调用。
         * 客户端从 MinecraftClientData 解析（routes 为空时触发 PacketRequestPlatformRouteData 向服务端请求）。
         */
        private void resolvePlatformData() {
            try {
                if (getWorld2() == null) return;
                final long id = getPlatformId();
                if (id == 0) return;

                if (getWorld2().isClient()) {
                    // 客户端：从 MinecraftClientData 解析（platform.routes 可能为空直到收到 PacketUpdateData）
                    final Platform platform = MinecraftClientData.getInstance().platformIdMap.get(id);
                    if (platform != null && !platform.routes.isEmpty()) {
                        final Route route = platform.routes.iterator().next();
                        routeColor = route.getColor();
                        routeNumber = route.getRouteNumber();
                        platformName = platform.getName();
                        markDirty2();
                    }
                }
            } catch (Exception ignored) {
            }
        }

        @Override
        public void setPlatformId(long platformId) {
            super.setPlatformId(platformId);
            // 在客户端和服务端都尝试解析：服务端通过反射获取完整路线数据并持久化，
            // 客户端从 MinecraftClientData 解析（成功后通过 PacketSyncStationNameData 同步到服务端）。
            resolvePlatformData();
        }

        public int getRouteColor() {
            return routeColor;
        }

        public void setRouteColor(int routeColor) {
            this.routeColor = routeColor;
        }

        public String getRouteNumber() {
            return routeNumber;
        }

        public void setRouteNumber(String routeNumber) {
            this.routeNumber = routeNumber == null ? "" : routeNumber;
        }

        public String getPlatformName() {
            return platformName;
        }

        public void setPlatformName(String platformName) {
            this.platformName = platformName == null ? "" : platformName;
        }

        public int getColor(BlockState state) {
            return -1; // 白色，让纹理完全控制颜色
        }

        public void setData(long platformId) {
            this.setPlatformId(platformId);
            resolvePlatformData();
            final Direction facing = IBlock.getStatePropertySafe(this.getCachedState2(), FACING);
            final BlockPos neighborPos = this.getPos2().offset(facing.rotateYClockwise());

            final org.mtr.mapping.holder.BlockEntity blockEntity = this.getWorld2().getBlockEntity(neighborPos);
            if (blockEntity != null && blockEntity.data instanceof BlockEntity entity) {
                entity.setPlatformId(platformId);
                entity.markDirty2();
            }
            markDirty2();
        }
    }
}