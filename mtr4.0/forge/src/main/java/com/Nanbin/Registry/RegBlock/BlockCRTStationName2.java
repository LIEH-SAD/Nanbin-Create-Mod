package com.Nanbin.Registry.RegBlock;

import com.Nanbin.Init;
import com.Nanbin.entity.BlockEntityTypes;
import com.Nanbin.mapping.IBlockExtension;
import com.Nanbin.mapping.TranslationProvider;
import com.Nanbin.packet.PacketOpenCRTPlatformScreen;
import org.mtr.core.data.Data;
import org.mtr.core.data.Platform;
import org.mtr.core.data.Route;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mapping.mapper.BlockWithEntity;
import org.mtr.mapping.mapper.DirectionHelper;
import org.mtr.mapping.tool.HolderBase;
import org.mtr.mod.block.BlockRouteSignBase;
import org.mtr.mod.block.IBlock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;


public class BlockCRTStationName2 extends BlockStationNameBase implements DirectionHelper,BlockWithEntity {

    public BlockCRTStationName2(BlockSettings blockSettings) {
        super(blockSettings);
    }

    @Nonnull
    public BlockEntityExtension createBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new BlockEntity(blockPos, blockState);
    }

    @Override
    public @Nonnull ActionResult onUse2(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        return IBlockExtension.checkHoldingBrush(world, player, () -> Init.REGISTRY.sendPacketToClient(ServerPlayerEntity.cast(player), new PacketOpenCRTPlatformScreen(pos)));
    }


    public void addBlockProperties(List<HolderBase<?>> properties) {
        properties.add(FACING);
    }

    public void addTooltips(ItemStack stack, @Nullable BlockView world, List<MutableText> tooltip, TooltipContext options) {
        tooltip.add(TranslationProvider.TOOLTIP_STATION_COLOR.getMutableText(new Object[0]).formatted(TextFormatting.DARK_GRAY));
        tooltip.add(TranslationProvider.BRUSH_USE.getMutableText(new Object[0]).formatted(TextFormatting.DARK_GRAY));
    }

    public static class BlockEntity extends BlockRouteSignBase.BlockEntityBase {

        private static final String KEY_ROUTE_NUMBER = "routeNumber";
        private String routeNumber = "";

        public BlockEntity(BlockPos pos, BlockState state) {
            super(BlockEntityTypes.CRT_STATION_NAME_2.get(), pos, state);
        }

        public int getColor(BlockState state) {
            return -1; // 白色
        }

        public String getRouteNumber() {
            return routeNumber;
        }

        @Override
        public void setPlatformId(long platformId) {
            super.setPlatformId(platformId);
            // MTR 客户端游戏数据不含完整路线（routeNumber），因此在服务端配置平台时
            // 同步一次真实的线路编号并写入 NBT，渲染端直接读取。
            updateRouteNumberFromServer();
        }

        @Override
        public void readCompoundTag(CompoundTag compoundTag) {
            super.readCompoundTag(compoundTag);
            routeNumber = compoundTag.getString(KEY_ROUTE_NUMBER);
        }

        @Override
        public void writeCompoundTag(CompoundTag compoundTag) {
            super.writeCompoundTag(compoundTag);
            compoundTag.putString(KEY_ROUTE_NUMBER, routeNumber);
        }

        public void setData(long platformId) {
            this.setPlatformId(platformId);
            final BlockPos pos;
            pos = this.getPos2().offset(IBlock.getStatePropertySafe(this.getCachedState2(), FACING).rotateYClockwise());

            org.mtr.mapping.holder.BlockEntity blockEntity = this.getWorld2().getBlockEntity(pos);
            if (blockEntity.data instanceof BlockEntity entity) {
                entity.setPlatformId(platformId);
                entity.markDirty2();
            } else {
                Init.LOGGER.error("BlockStationName2.BlockEntity: Unable to set data for block entity at {}", pos.toShortString());
            }
            markDirty2();
        }

        /**
         * 服务端专用：反射访问 MTR 的 Simulator，按 platformId 找到服务该站台的完整 Route，
         * 取出真实的线路编号（routeNumber）并缓存。MTR 客户端数据没有该字段，只能从服务端获取。
         */
        private void updateRouteNumberFromServer() {
            if (getWorld2() == null || getWorld2().isClient()) {
                return;
            }
            final long platformId = getPlatformId();
            if (platformId == 0) {
                return;
            }
            try {
                final java.lang.reflect.Field mainField = Class.forName("org.mtr.mod.Init").getDeclaredField("main");
                mainField.setAccessible(true);
                final Object main = mainField.get(null);
                if (main == null) {
                    return;
                }
                final java.lang.reflect.Field simulatorsField = main.getClass().getDeclaredField("simulators");
                simulatorsField.setAccessible(true);
                final Iterable<?> simulators = (Iterable<?>) simulatorsField.get(main);
                for (final Object simulator : simulators) {
                    final Data data = (Data) simulator;
                    final Platform platform = data.platformIdMap.get(platformId);
                    if (platform != null) {
                        for (final Route route : platform.routes) {
                            final String number = route.getRouteNumber();
                            if (number != null && !number.isEmpty()) {
                                routeNumber = number;
                                markDirty2();
                                return;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Init.LOGGER.error("BlockCRTStationName2: Failed to fetch route number for platform {}", platformId, e);
            }
        }
    }
}