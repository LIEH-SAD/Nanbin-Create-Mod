package com.Nanbin.packet;

import com.Nanbin.Registry.RegBlock.BlockCRTRailwaySignWall1;
import com.Nanbin.Registry.RegBlock.BlockCRTStationInfo1;
import com.Nanbin.Registry.RegBlock.BlockCRTStationInfo1Double;
import com.Nanbin.client.Screen.*;
import com.Nanbin.mapping.Registry;
import org.mtr.core.data.Position;
import org.mtr.core.data.Station;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
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
import com.Nanbin.client.CRTRailwaySignEditor.editor.CRTSignEditorHost;
import com.Nanbin.client.CRTRailwaySignEditor.editor.CRTSignEditorHostImpl;
import com.Nanbin.client.Screen.CRTSignEditorScreen;
import com.Nanbin.client.CRTRailwaySignEditor.render.CRTSignDataResolver;
import com.Nanbin.client.CRTRailwaySignEditor.legacy.CRTSignLegacyImporter;
import com.Nanbin.client.Screen.CRTSignDoubleScreen;

public final class ClientPacketHelper {

    public static void openBlockEntityScreen(BlockPos blockPos) {
        final org.mtr.mapping.holder.ClientWorld world = MinecraftClient.getInstance().getWorldMapped();
        if (world != null) {
            final org.mtr.mapping.holder.BlockState state = world.getBlockState(blockPos);
            if (state.getBlock().data instanceof BlockCRTRailwaySignWall1) {
                openCRTSignWallEditor(world, blockPos, state);
                return;
            }
            if (state.getBlock().data instanceof com.Nanbin.Registry.RegBlock.BlockCRTRailwaySign) {
                openCRTSignEditor(world, blockPos);
                return;
            }
        }
        openRailwaySignScreen(blockPos);
    }

    /**
     * 打开 CRT 铁路指示牌的自由布局编辑器（{@link CRTSignEditorScreen}，网页编辑器风格）。
     *
     * <p>数据流：方块实体 NBT 里的 {@code crt_sign_layout} JSON → 布局模型 → 编辑器；
     * 保存走 {@link PacketUpdateCRTSignLayout}。旧存档没有布局时先用 signIds 自动转一份
     * （关闭时若不修改则不回写，世界渲染继续走旧格子渲染，外观不变）。</p>
     */
    public static void openCRTSignEditor(org.mtr.mapping.holder.ClientWorld world, BlockPos blockPos) {
        final org.mtr.mapping.holder.BlockEntity blockEntity = world.getBlockEntity(blockPos);
        if (blockEntity != null && blockEntity.data instanceof org.mtr.mod.block.BlockRailwaySign.BlockEntity) {
            final org.mtr.mod.block.BlockRailwaySign.BlockEntity entity = (org.mtr.mod.block.BlockRailwaySign.BlockEntity) blockEntity.data;
            final java.util.function.LongFunction<String> routeNumberGetter;
            final String savedLayoutJson;
            if (entity instanceof com.Nanbin.Registry.RegBlock.BlockCRTRailwaySign.BlockEntityCRTRailwaySign) {
                final com.Nanbin.Registry.RegBlock.BlockCRTRailwaySign.BlockEntityCRTRailwaySign crtEntity = (com.Nanbin.Registry.RegBlock.BlockCRTRailwaySign.BlockEntityCRTRailwaySign) entity;
                routeNumberGetter = crtEntity::getRouteNumber;
                savedLayoutJson = crtEntity.getSignLayout();
            } else {
                routeNumberGetter = color -> "";
                savedLayoutJson = "";
            }
            final boolean hasSavedLayout = savedLayoutJson != null && !savedLayoutJson.isEmpty();
            com.Nanbin.signage.crt.CRTSignLayout layout = hasSavedLayout ? com.Nanbin.signage.crt.CRTSignLayout.fromJson(savedLayoutJson) : null;
            if (layout == null) {
                layout = CRTSignLegacyImporter.convert(entity.getSignIds(), entity.getSelectedIds(), routeNumberGetter);
            }
            final CRTSignDataResolver data = new CRTSignDataResolver(routeNumberGetter, findStationForSelection(blockPos));
            final String baselineJson = hasSavedLayout ? savedLayoutJson : layout.toJson();
            final CRTSignEditorHost host = new CRTSignEditorHostImpl(blockPos, baselineJson);
            final CRTSignEditorScreen screen = new CRTSignEditorScreen(null, blockPos, layout, data, host);
            openScreen(screen, screenExtension -> screenExtension instanceof CRTSignEditorScreen);
            return;
        }
        // 兜底：实体类型不符时仍用原版界面，避免刷子点击没有任何反应
        openRailwaySignScreen(blockPos);
    }

    /** 墙挂指示牌：任意一格用刷子打开整条牌的编辑器；布局存在整条牌的起始方块实体上。 */
    public static void openCRTSignWallEditor(org.mtr.mapping.holder.ClientWorld world, BlockPos blockPos, org.mtr.mapping.holder.BlockState state) {
        final org.mtr.mapping.holder.BlockView view = org.mtr.mapping.holder.BlockView.cast(world);
        final BlockPos start = BlockCRTRailwaySignWall1.findStart(view, blockPos, state);
        final List<BlockPos> run = BlockCRTRailwaySignWall1.collectRun(view, start);
        if (run.isEmpty()) {
            return;
        }
        // 每个方块两格内容：旧数据转换时按格展开，长度 = 方块数 × 2
        final int cellsPerBlock = BlockCRTRailwaySignWall1.CELLS_PER_BLOCK;
        final String[] cells = new String[run.size() * cellsPerBlock];
        final LongAVLTreeSet selectedIds = new LongAVLTreeSet();
        java.util.function.LongFunction<String> routeNumberGetter = color -> "";
        String savedLayoutJson = "";
        for (int i = 0; i < run.size(); i++) {
            final BlockCRTRailwaySignWall1.BlockEntity entity = BlockCRTRailwaySignWall1.getBlockEntity(view, run.get(i));
            if (entity != null) {
                for (int j = 0; j < cellsPerBlock; j++) {
                    cells[i * cellsPerBlock + j] = entity.getSignId(j);
                }
                selectedIds.addAll(entity.getSelectedIds());
                if (i == 0) {
                    routeNumberGetter = entity::getRouteNumber;
                    savedLayoutJson = entity.getSignLayout();
                }
            }
        }
        final boolean hasSavedLayout = savedLayoutJson != null && !savedLayoutJson.isEmpty();
        com.Nanbin.signage.crt.CRTSignLayout layout = hasSavedLayout ? com.Nanbin.signage.crt.CRTSignLayout.fromJson(savedLayoutJson) : null;
        if (layout == null) {
            layout = CRTSignLegacyImporter.convert(cells, selectedIds, routeNumberGetter);
        }
        final CRTSignDataResolver data = new CRTSignDataResolver(routeNumberGetter, findStationForSelection(start));
        final String baselineJson = hasSavedLayout ? savedLayoutJson : layout.toJson();
        final CRTSignEditorHost host = new CRTSignEditorHostImpl(start, baselineJson);
        final CRTSignEditorScreen screen = new CRTSignEditorScreen(null, start, layout, data, host);
        openScreen(screen, screenExtension -> screenExtension instanceof CRTSignEditorScreen);
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

    public static void openStationInfoScreen(BlockPos blockPos, String url, boolean isFront) {
        openScreen(new StationInfoScreen(blockPos, url, isFront), screen -> screen instanceof StationInfoScreen);
    }

    public static void saveStationInfoScreen(BlockPos blockPos, String url, LongAVLTreeSet selectedIds, boolean isFront) {
        Registry.sendPacketToServer(new PacketUpdateStationInfoData(blockPos, url, selectedIds, isFront));
    }

    /** 打开只用于选择站台/路线的选择器，选完后自动返回 parent 屏幕。 */
    public static void openPlatformSelectionScreen(BlockPos blockPos, LongAVLTreeSet selectedIds, ScreenExtension parent) {
        final Station station = findStationForSelection(blockPos);
        final ObjectImmutableList<DashboardListItem> platformsForList = station == null
                ? ObjectImmutableList.of()
                : PIDSConfigScreen.getPlatformsForList(new ObjectArrayList<>(station.savedRails));
        MinecraftClient.getInstance().openScreen(new Screen(new DashboardListSelectorScreen(platformsForList, selectedIds, true, false, parent)));
    }

    /**
     * 优先精确匹配方块所在站区域；其次找最近的站台所属站；最后取全图最近的站。
     * <p>
     * 指示牌常被挂在车站外的出入口处，{@link InitClient#findStation} 只做区域精确匹配，
     * 直接用它会得到 null，选择屏就会一个数据按钮都不显示，所以统一走这里的兜底链。
     */
    public static Station findStationForSelection(BlockPos blockPos) {
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

    // ─── 车站查询缓存（渲染端用） ───────────────────────────────────────────

    /** 车站查询缓存有效期（毫秒）：出口信息条每次布局都会问同一坐标，避免反复遍历 MTR 数据。 */
    private static final long STATION_CACHE_TTL_MS = 2000L;
    /** 车站查询缓存条目上限（一个世界里同时可见的指示牌数量级）。 */
    private static final int STATION_CACHE_LIMIT = 64;
    private static final java.util.Map<Long, StationCacheEntry> STATION_CACHE =
            new java.util.LinkedHashMap<Long, StationCacheEntry>(16, 0.75F, true) {
                @Override
                protected boolean removeEldestEntry(java.util.Map.Entry<Long, StationCacheEntry> eldest) {
                    return size() > STATION_CACHE_LIMIT;
                }
            };

    /** 一次车站查询的结果（{@code station} 允许为 null：查询过但当前没有车站）。 */
    private static final class StationCacheEntry {
        private final Station station;
        private final long time;

        private StationCacheEntry(Station station, long time) {
            this.station = station;
            this.time = time;
        }
    }

    /**
     * {@link #findStationForSelection(BlockPos)} 的缓存版：世界渲染每个牌子每帧都会调用，
     * 而精确匹配/最近站台/全图最近站三层兜底并不便宜，因此按坐标缓存 2 秒。
     *
     * <p>MTR 数据同步完成前可能返回 null，缓存也会把 null 记下来（2 秒后重试）。</p>
     */
    public static Station findStationForSelectionCached(BlockPos blockPos) {
        if (blockPos == null) {
            return null;
        }
        // 用 asLong() 当键：渲染每个牌子每帧都会调用，避免反复生成坐标字符串
        final long key = blockPos.asLong();
        final long now = System.currentTimeMillis();
        synchronized (STATION_CACHE) {
            final StationCacheEntry cached = STATION_CACHE.get(key);
            if (cached != null && now - cached.time < STATION_CACHE_TTL_MS) {
                return cached.station;
            }
        }
        final Station station = findStationForSelection(blockPos);
        synchronized (STATION_CACHE) {
            STATION_CACHE.put(key, new StationCacheEntry(station, now));
        }
        return station;
    }

    /** 打开 StationInfo 专用的双行指示牌编辑器（2 行 × 8 格）。 */
    public static void openCRTSignDoubleScreen(BlockPos blockPos, boolean isFront) {
        final org.mtr.mapping.holder.ClientWorld world = MinecraftClient.getInstance().getWorldMapped();
        if (world == null) {
            return;
        }
        final org.mtr.mapping.holder.BlockEntity blockEntity = world.getBlockEntity(blockPos);
        final int lines;
        final int length;
        final String[][] signIds;
        final List<LongAVLTreeSet> selectedIds;
        if (blockEntity != null && blockEntity.data instanceof BlockCRTStationInfo1.BlockEntity) {
            final BlockCRTStationInfo1.BlockEntity entity = (BlockCRTStationInfo1.BlockEntity) blockEntity.data;
            lines = BlockCRTStationInfo1.BlockEntity.SIGN_LINES;
            length = BlockCRTStationInfo1.BlockEntity.SIGN_LENGTH;
            signIds = entity.getSignIds();
            selectedIds = entity.getSelectedIds();
        } else if (blockEntity != null && blockEntity.data instanceof BlockCRTStationInfo1Double.BlockEntity) {
            final BlockCRTStationInfo1Double.BlockEntity entity = (BlockCRTStationInfo1Double.BlockEntity) blockEntity.data;
            lines = BlockCRTStationInfo1Double.BlockEntity.SIGN_LINES;
            length = BlockCRTStationInfo1Double.BlockEntity.SIGN_LENGTH;
            signIds = entity.getSignIds(isFront);
            selectedIds = entity.getSelectedIds(isFront);
        } else {
            lines = BlockCRTStationInfo1.BlockEntity.SIGN_LINES;
            length = BlockCRTStationInfo1.BlockEntity.SIGN_LENGTH;
            signIds = new String[lines][length];
            selectedIds = new ArrayList<>();
            for (int i = 0; i < lines; i++) {
                selectedIds.add(new LongAVLTreeSet());
            }
        }
        final boolean hasData = blockEntity != null;
        final String[][] finalSignIds = signIds;
        final List<LongAVLTreeSet> finalSelectedIds = selectedIds;
        openScreen(new CRTSignDoubleScreen(blockPos, lines, length, finalSignIds, finalSelectedIds, hasData, () -> Registry.sendPacketToServer(new PacketUpdateStationInfoSignData(blockPos, finalSignIds, finalSelectedIds, isFront))), screen -> screen instanceof CRTSignDoubleScreen);
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