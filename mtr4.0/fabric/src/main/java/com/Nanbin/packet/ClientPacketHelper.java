package com.Nanbin.packet;

import com.Nanbin.Registry.RegBlock.BlockCRTStationInfo1;
import com.Nanbin.client.Screen.BusTicketProcessorScreen;
import com.Nanbin.client.Screen.RailwaySignDoubleScreen;
import com.Nanbin.client.Screen.RoadNameScreen;
import com.Nanbin.client.Screen.StationInfoScreen;
import com.Nanbin.client.Screen.TicketMachineScreen;
import com.Nanbin.mapping.Registry;
import org.mtr.core.data.Position;
import org.mtr.core.data.Station;
import org.mtr.libraries.it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArraySet;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.Screen;
import org.mtr.mapping.mapper.ScreenExtension;
import org.mtr.mod.Init;
import org.mtr.mod.InitClient;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.screen.DashboardListItem;
import org.mtr.mod.screen.DashboardListSelectorScreen;
import org.mtr.mod.screen.PIDSConfigScreen;
import org.mtr.mod.screen.RailwaySignScreen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class ClientPacketHelper {

    public static void openBlockEntityScreen(BlockPos blockPos) {
        openRailwaySignScreen(blockPos);
    }

    public static void openRailwaySignScreen(BlockPos blockPos) {
        openScreen(new RailwaySignScreen(blockPos), screen -> screen instanceof RailwaySignScreen);
    }

    public static void openRoadNameScreen(BlockPos blockPos, String[] texts) {
        openScreen(new RoadNameScreen(blockPos, texts), screen -> screen instanceof RoadNameScreen);
    }

    public static void openTicketMachineScreen(int balance) {
        openScreen(new TicketMachineScreen(balance), (screenExtension) -> screenExtension instanceof TicketMachineScreen);
    }

    public static void openBusTicketProcessorScreen(BlockPos blockPos, int mode, int amount) {
        openScreen(new BusTicketProcessorScreen(blockPos, mode, amount), (screenExtension) -> screenExtension instanceof BusTicketProcessorScreen);
    }

    public static void syncTicketMachineBalance(int balance) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        Screen screen = minecraftClient.getCurrentScreenMapped();
        if (screen != null && screen.data instanceof TicketMachineScreen) {
            ((TicketMachineScreen) screen.data).updateBalance(balance);
        }
    }

    public static void saveRoadNameScreen(BlockPos blockPos, String[] texts) {
        Registry.sendPacketToServer(new PacketUpdateRoadNameData(blockPos, texts));
    }

    public static void openStationInfoScreen(BlockPos blockPos, String url) {
        openScreen(new StationInfoScreen(blockPos, url), screen -> screen instanceof StationInfoScreen);
    }

    public static void saveStationInfoScreen(BlockPos blockPos, String url, LongAVLTreeSet selectedIds) {
        Registry.sendPacketToServer(new PacketUpdateStationInfoData(blockPos, url, selectedIds));
    }

    /** 打开只用于选择站台/路线的选择器，选完后自动返回 parent 屏幕。 */
    public static void openPlatformSelectionScreen(BlockPos blockPos, LongAVLTreeSet selectedIds, ScreenExtension parent) {
        final Station station = findStationForSelection(blockPos);
        final ObjectImmutableList<DashboardListItem> platformsForList = station == null
                ? ObjectImmutableList.of()
                : PIDSConfigScreen.getPlatformsForList(new ObjectArrayList<>(station.savedRails));
        MinecraftClient.getInstance().openScreen(new Screen(new DashboardListSelectorScreen(platformsForList, selectedIds, true, false, parent)));
    }

    /** 优先精确匹配方块所在站区域；其次找最近的站台所属站；最后取全图最近的站。 */
    private static Station findStationForSelection(BlockPos blockPos) {
        final Station exact = InitClient.findStation(blockPos);
        if (exact != null) {
            return exact;
        }
        final Station[] closeStation = {null};
        InitClient.findClosePlatform(blockPos, 256, platform -> closeStation[0] = platform.area);
        if (closeStation[0] != null) {
            return closeStation[0];
        }
        final Position position = Init.blockPosToPosition(blockPos);
        Station nearest = null;
        long nearestDistance = Long.MAX_VALUE;
        for (final Station station : MinecraftClientData.getInstance().stations) {
            final long distance = station.getCenter().manhattanDistance(position);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = station;
            }
        }
        return nearest;
    }

    /** 打开 StationInfo1 专用的双行指示牌编辑器（2 行 × 8 格）。 */
    public static void openRailwaySignDoubleScreen(BlockPos blockPos) {
        final org.mtr.mapping.holder.ClientWorld world = MinecraftClient.getInstance().getWorldMapped();
        if (world == null) {
            return;
        }
        final org.mtr.mapping.holder.BlockEntity blockEntity = world.getBlockEntity(blockPos);
        final int lines = BlockCRTStationInfo1.BlockEntity.SIGN_LINES;
        final int length = BlockCRTStationInfo1.BlockEntity.SIGN_LENGTH;
        if (blockEntity != null && blockEntity.data instanceof BlockCRTStationInfo1.BlockEntity entity) {
            final String[][] signIds = entity.getSignIds();
            final List<LongAVLTreeSet> selectedIds = entity.getSelectedIds();
            openScreen(new RailwaySignDoubleScreen(blockPos, lines, length, signIds, selectedIds, true, () -> Registry.sendPacketToServer(new PacketUpdateStationInfoSignData(blockPos, signIds, selectedIds))), screen -> screen instanceof RailwaySignDoubleScreen);
        } else {
            final String[][] signIds = new String[lines][length];
            final List<LongAVLTreeSet> selectedIds = new ArrayList<>();
            for (int i = 0; i < lines; i++) {
                selectedIds.add(new LongAVLTreeSet());
            }
            openScreen(new RailwaySignDoubleScreen(blockPos, lines, length, signIds, selectedIds, false, () -> Registry.sendPacketToServer(new PacketUpdateStationInfoSignData(blockPos, signIds, selectedIds))), screen -> screen instanceof RailwaySignDoubleScreen);
        }
    }

    public static void openScreen(ScreenExtension screenExtension, Predicate<ScreenExtension> isInstance) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        Screen screen = minecraftClient.getCurrentScreenMapped();
        boolean shouldOpen = screen == null || screen.data instanceof ScreenExtension && !isInstance.test((ScreenExtension) screen.data);
        if (shouldOpen) {
            minecraftClient.openScreen(new Screen(screenExtension));
        }
    }
}