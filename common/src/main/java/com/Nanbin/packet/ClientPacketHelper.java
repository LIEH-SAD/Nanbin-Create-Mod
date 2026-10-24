package com.Nanbin.packet;

import com.Nanbin.Registry.RegBlock.BlockCRTStationInfo1;
import com.Nanbin.Registry.RegBlock.BlockCRTStationInfo1Double;
import com.Nanbin.client.Screen.BusTicketProcessorScreen;
import com.Nanbin.client.Screen.RailwaySignDoubleScreen;
import com.Nanbin.client.Screen.RoadNameScreen;
import com.Nanbin.client.Screen.StationInfoScreen;
import com.Nanbin.client.Screen.TicketMachineScreen;
import com.Nanbin.mapping.Registry;
import it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import mtr.client.ClientData;
import mtr.data.NameColorDataBase;
import mtr.data.Station;
import mtr.screen.DashboardListSelectorScreen;
import mtr.screen.RailwaySignScreen;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public class ClientPacketHelper {

    public static void openRailwaySignScreen(BlockPos blockPos) {
        openScreen(new RailwaySignScreen(blockPos), screen -> screen instanceof RailwaySignScreen);
    }

    public static void openRoadNameScreen(BlockPos blockPos, String[] texts) {
        openScreen(new RoadNameScreen(blockPos, texts), screen -> screen instanceof RoadNameScreen);
    }

    public static void saveRoadNameScreen(BlockPos blockPos, String[] texts) {
        Registry.sendPacketToServer(PacketHandler.PACKET_UPDATE_ROAD_NAME, buf -> {
            buf.writeBlockPos(blockPos);
            buf.writeVarInt(texts.length);
            for (String text : texts) {
                buf.writeString(text);
            }
        });
    }

    public static void openTicketMachineScreen(int balance) {
        openScreen(new TicketMachineScreen(balance), screen -> screen instanceof TicketMachineScreen);
    }

    public static void openBusTicketProcessorScreen(BlockPos blockPos, int mode, int amount) {
        openScreen(new BusTicketProcessorScreen(blockPos, mode, amount), screen -> screen instanceof BusTicketProcessorScreen);
    }

    public static void openStationInfoScreen(BlockPos blockPos, String url, boolean isFront) {
        openScreen(new StationInfoScreen(blockPos, url, isFront), screen -> screen instanceof StationInfoScreen);
    }

    /** 保存站台信息屏的 URL 与所选站台。 */
    public static void saveStationInfoScreen(BlockPos blockPos, String url, LongAVLTreeSet selectedIds, boolean isFront) {
        Registry.sendPacketToServer(PacketHandler.PACKET_UPDATE_STATION_INFO, buf -> {
            buf.writeBlockPos(blockPos);
            buf.writeString(url == null ? "" : url);
            buf.writeVarInt(selectedIds.size());
            for (final long id : selectedIds) {
                buf.writeLong(id);
            }
            buf.writeBoolean(isFront);
        });
    }

	/** 打开站台选择器（选中站台的 ID 直接写回传入的集合）；选完立即回到配置界面并保存。 */
    public static void openPlatformSelectionScreen(BlockPos blockPos, LongAVLTreeSet selectedIds, StationInfoScreen parent) {
        final Station station = findStationForSelection(blockPos);
        final List<NameColorDataBase> platformsForList = new ArrayList<>();
        if (station != null) {
            platformsForList.addAll(ClientData.DATA_CACHE.requestStationIdToPlatforms(station.id).values());
            platformsForList.sort(Comparator.comparing(data -> data.name));
        }
        MinecraftClient.getInstance().setScreen(new DashboardListSelectorScreen(() -> {
            MinecraftClient.getInstance().setScreen(parent);
            saveStationInfoScreen(blockPos, parent.getUrl(), selectedIds, parent.isFront());
        }, platformsForList, selectedIds, false, false));
    }

    private static Station findStationForSelection(BlockPos blockPos) {
        final Station direct = ClientData.DATA_CACHE.blockPosToStation.get(blockPos);
        if (direct != null) {
            return direct;
        }
        for (final Station station : ClientData.STATIONS) {
            if (station.inArea(blockPos.getX(), blockPos.getZ())) {
                return station;
            }
        }
        return null;
    }

    /** 打开多行指示牌编辑器（2 行 × 7 格），数据变动 / 关闭时写回服务端。 */
    public static void openRailwaySignDoubleScreen(BlockPos blockPos, boolean isFront) {
        final World world = MinecraftClient.getInstance().world;
        if (world == null) {
            return;
        }
        final BlockEntity blockEntity = world.getBlockEntity(blockPos);
        MinecraftClient.getInstance().setScreen(new RailwaySignDoubleScreen(blockPos, isFront,
                () -> saveStationInfoSign(blockPos, getSignIds(blockEntity, isFront), getSelectedIds(blockEntity, isFront), isFront)));
    }

    private static String[][] getSignIds(BlockEntity blockEntity, boolean isFront) {
        if (blockEntity instanceof BlockCRTStationInfo1.BlockEntity entity) {
            return entity.getSignIds();
        } else if (blockEntity instanceof BlockCRTStationInfo1Double.BlockEntity entity) {
            return entity.getSignIds(isFront);
        }
        return new String[2][7];
    }

    private static List<LongAVLTreeSet> getSelectedIds(BlockEntity blockEntity, boolean isFront) {
        if (blockEntity instanceof BlockCRTStationInfo1.BlockEntity entity) {
            return entity.getSelectedIds();
        } else if (blockEntity instanceof BlockCRTStationInfo1Double.BlockEntity entity) {
            return entity.getSelectedIds(isFront);
        }
        final List<LongAVLTreeSet> fallback = new ArrayList<>();
        fallback.add(new LongAVLTreeSet());
        fallback.add(new LongAVLTreeSet());
        return fallback;
    }

    private static void saveStationInfoSign(BlockPos blockPos, String[][] signIds, List<LongAVLTreeSet> selectedIds, boolean isFront) {
        Registry.sendPacketToServer(PacketHandler.PACKET_UPDATE_STATION_INFO_SIGN, buf -> {
            buf.writeBlockPos(blockPos);
            buf.writeVarInt(signIds.length);
            for (final String[] lineIds : signIds) {
                buf.writeVarInt(lineIds.length);
                for (final String signId : lineIds) {
                    buf.writeString(signId == null ? "" : signId);
                }
            }
            for (int i = 0; i < signIds.length; i++) {
                final LongAVLTreeSet lineIds = i < selectedIds.size() ? selectedIds.get(i) : new LongAVLTreeSet();
                buf.writeVarInt(lineIds.size());
                for (final long id : lineIds) {
                    buf.writeLong(id);
                }
            }
            buf.writeBoolean(isFront);
        });
    }

    public static void syncTicketMachineBalance(int balance) {
        final Screen currentScreen = MinecraftClient.getInstance().currentScreen;
        if (currentScreen instanceof TicketMachineScreen screen) {
            screen.updateBalance(balance);
        }
    }

    private static void openScreen(Screen screen, Predicate<Screen> isInstance) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        Screen currentScreen = minecraftClient.currentScreen;
        if (currentScreen == null || !isInstance.test(currentScreen)) {
            minecraftClient.setScreen(screen);
        }
    }
}