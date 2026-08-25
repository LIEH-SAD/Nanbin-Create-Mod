package com.Nanbin.Registry.RegBlock;

import com.Nanbin.Blocks.Blocks;
import com.Nanbin.Init;
import com.Nanbin.entity.BlockEntityTypes;
import com.Nanbin.mapping.TranslationProvider;
import org.mtr.core.data.*;
import org.mtr.core.data.Position;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mapping.registry.BlockEntityTypeRegistryObject;
import org.mtr.mod.block.BlockRailwaySign;
import org.mtr.mod.block.IBlock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

public class BlockCRTRailwaySign extends BlockRailwaySign {

    public BlockCRTRailwaySign(int length, boolean isOdd) {
        super(length, isOdd);
    }

    @Override
    public BlockEntityExtension createBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityCRTRailwaySign(length, isOdd, pos, state);
    }

    /**
     * 放置告示牌时，在中间段放置 CRT 版中间方块（替代原版 RAILWAY_SIGN_MIDDLE）：
     * 单数格（isOdd=true）最中间一格为 middle_odd；双格（isOdd=false）最中间两格
     * 分别为 middle_even_1、middle_even_2；其余均为 middle_common。
     */
    @Override
    public void onPlaced2(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        if (!world.isClient()) {
            final Direction facing = IBlock.getStatePropertySafe(state, FACING);
            final int middleLength = getMiddleLength();
            for (int i = 1; i <= middleLength; i++) {
                final Block middleBlock;
                if (isOdd) {
                    middleBlock = i == (middleLength + 1) / 2 ? Blocks.CRT_RAILWAY_SIGN_MIDDLE_ODD.get() : Blocks.CRT_RAILWAY_SIGN_MIDDLE_COMMON.get();
                } else {
                    if (i == middleLength / 2) {
                        middleBlock = Blocks.CRT_RAILWAY_SIGN_MIDDLE_EVEN_1.get();
                    } else if (i == middleLength / 2 + 1) {
                        middleBlock = Blocks.CRT_RAILWAY_SIGN_MIDDLE_EVEN_2.get();
                    } else {
                        middleBlock = Blocks.CRT_RAILWAY_SIGN_MIDDLE_COMMON.get();
                    }
                }
                world.setBlockState(pos.offset(facing.rotateYClockwise(), i), middleBlock.getDefaultState().with(new Property<>(FACING.data), facing.data), 3);
            }
            world.setBlockState(pos.offset(facing.rotateYClockwise(), middleLength + 1), getDefaultState2().with(new Property<>(FACING.data), facing.getOpposite().data), 3);
            world.updateNeighbors(pos, org.mtr.mapping.holder.Blocks.getAirMapped());
            state.updateNeighbors(new WorldAccess(world.data), pos, 3);
        }
    }

    /** 重新实现原版 private 的 getMiddleLength（length/isOdd 均为 public 字段）。 */
    private int getMiddleLength() {
        return (length - (4 - getXStart() / 4)) / 2;
    }

    /**
     * CRT 版端点查找：跳过 CRT 中间方块（以及原版 middle），找到真正的告示牌端部。
     * 供本类与 {@link BlockCRTRailwaySignMiddle} 共用，保证破坏/点击联动正确。
     */
    static BlockPos findEndWithDirectionCRT(World world, BlockPos startPos, Direction direction, boolean allowOpposite) {
        int i = 0;
        while (true) {
            final BlockPos checkPos = startPos.offset(direction.rotateYCounterclockwise(), i);
            final BlockState checkState = world.getBlockState(checkPos);
            if (checkState.getBlock().data instanceof BlockRailwaySign) {
                final Direction facing = IBlock.getStatePropertySafe(checkState, FACING);
                if (!(checkState.getBlock().data instanceof BlockCRTRailwaySignMiddle) && !checkState.isOf(org.mtr.mod.Blocks.RAILWAY_SIGN_MIDDLE.get()) && (facing == direction || allowOpposite && facing == direction.getOpposite())) {
                    return checkPos;
                }
            } else {
                return null;
            }
            i++;
        }
    }

    @Nonnull
    public VoxelShape getOutlineShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        Direction facing = IBlock.getStatePropertySafe(state, FACING);
        if (state.isOf(Blocks.CRT_RAILWAY_SIGN_MIDDLE_COMMON.get())) {
            return IBlock.getVoxelShapeByDirection((double)0.0F, (double)0.0F, (double)7.0F, (double)16.0F, (double)16.0F, (double)9.0F, facing);
        } else {
            int xStart = this.getXStart();
            return IBlock.getVoxelShapeByDirection((double)xStart - (double)0.75F, (double)0.0F, (double)7.0F, (double)16.0F, (double)16.0F, (double)9.0F, facing);
        }
    }

    /**
     * MTR 原版 BlockRailwaySign.BlockEntity 的私有静态 getType 只支持长度 2-7，
     * 长度 8-11 会回退为长度 2 的 MTR 类型，导致方块实体类型与 CRT 方块不匹配：
     * 渲染时不会被分发到 CRT 渲染器，存档重载后还会按错误的类型 id 重建、丢失全部告示牌数据。
     * 此子类在构造函数内通过反射把 Minecraft 的 type 字段修正为 CRT 自己注册的方块实体类型，
     * 使长度 3-11 的告示牌都能使用 CRT 渲染器，并沿用 MTR 的 RailwaySignScreen / 数据包。
     */
    public static class BlockEntityCRTRailwaySign extends BlockRailwaySign.BlockEntity {

        private static final String KEY_ROUTE_COLORS = "crt_route_colors";
        private static final String KEY_ROUTE_NUMBERS = "crt_route_numbers";
        /** 每格独立线路颜色，格式："<格子下标>:<颜色>,<颜色>;<格子下标>:<颜色>"（空=无单独设置）。 */
        private static final String KEY_CELL_COLORS = "crt_cell_colors";
        /** 颜色 -> 线路编号。MTR 客户端数据只有 SimplifiedRoute（无 routeNumber），只能服务端解析后写入 NBT。 */
        private final Long2ObjectOpenHashMap<String> routeNumbers = new Long2ObjectOpenHashMap<>();
        /** 每格独立线路颜色：下标=格子，值为该格所在线路牌区域实际使用的线路颜色；null=沿用全局选择。 */
        private long[][] cellColors;
        private final BlockEntityTypeRegistryObject<BlockEntityCRTRailwaySign> blockEntityType;

        public BlockEntityCRTRailwaySign(int length, boolean isOdd, BlockPos pos, BlockState state) {
            super(length, isOdd, pos, state);
            blockEntityType = BlockEntityTypes.getRailwaySignType(length, isOdd);
            fixBlockEntityType();
        }

        /**
         * MTR 父类 BlockRailwaySign.BlockEntity 的构造函数通过私有静态 getType(length, isOdd)
         * 解析方块实体类型，该函数只支持长度 2-7，长度 8-11 会回退成长度 2 的类型，且无法被子类覆写。
         * 这里在构造函数内（对象发布前）通过反射把 Minecraft BlockEntity.type 字段修正为 CRT 自己注册的类型，
         * 否则 CRT 渲染器永远不会被调用，且存档重载后方块实体会按错误的类型重建。
         */
        private void fixBlockEntityType() {
            try {
                final Object rawType = blockEntityType.get().data;
                Class<?> clazz = getClass();
                while (clazz != null) {
                    for (final Field field : clazz.getDeclaredFields()) {
                        if (Modifier.isStatic(field.getModifiers()) || !field.getType().isAssignableFrom(rawType.getClass())) {
                            continue;
                        }
                        field.setAccessible(true);
                        field.set(this, rawType);
                        return;
                    }
                    clazz = clazz.getSuperclass();
                }
                Init.LOGGER.warn("BlockCRTRailwaySign: Unable to find block entity type field at {}", getPos2().toShortString());
            } catch (Exception e) {
                Init.LOGGER.error("BlockCRTRailwaySign: Failed to fix block entity type at {}", getPos2().toShortString(), e);
            }
        }

        /** 按 selectedIds（颜色）的顺序返回已解析的线路编号；无编号的颜色会被跳过。 */
        public String[] getRouteNumbers() {
            return getRouteNumbersForCell(getSelectedIds().longStream().toArray());
        }

        /** 获取指定格子单独设置的线路颜色；未单独设置时返回 null。 */
        public long[] getCellColors(int cell) {
            if (cellColors == null || cell < 0 || cell >= cellColors.length) {
                return null;
            }
            return cellColors[cell];
        }

        /** 编辑器预填用：返回指定格子所在线路牌区域当前使用的线路颜色（每格独立存储优先，无则回退全局选择）。 */
        public long[] getSelectedIdsForCell(int cell) {
            if (cellColors != null && cell >= 0 && cell < cellColors.length) {
                for (int i = cell; i >= 0; i--) {
                    final long[] colors = cellColors[i];
                    if (colors != null && colors.length > 0) {
                        return colors;
                    }
                }
                for (int i = cell + 1; i < cellColors.length; i++) {
                    final long[] colors = cellColors[i];
                    if (colors != null && colors.length > 0) {
                        return colors;
                    }
                }
            }
            return getSelectedIds().longStream().toArray();
        }

        /** 服务端专用：设置指定格子的线路颜色（空数组=清除该格设置），并重新解析线路编号。 */
        public void setCellColors(int cell, long[] colors) {
            ensureCellColors();
            if (cell < 0 || cell >= cellColors.length) {
                return;
            }
            cellColors[cell] = (colors == null || colors.length == 0) ? null : colors.clone();
            updateRouteNumbersFromServer();
        }

        /** 按线路颜色返回真实线路编号（服务端同步，可能为中文/字母或含 "|" 的双语编号）；无则返回空串。 */
        public String getRouteNumber(long color) {
            final String number = routeNumbers.get(color);
            return number == null ? "" : number;
        }

        /** 按颜色顺序返回线路编号；无编号的颜色会被跳过。 */
        public String[] getRouteNumbersForCell(long[] colors) {
            if (colors == null || colors.length == 0) {
                return new String[0];
            }
            final String[] result = new String[colors.length];
            int count = 0;
            for (final long color : colors) {
                final String number = routeNumbers.get(color);
                if (number != null) {
                    result[count++] = number;
                }
            }
            return Arrays.copyOf(result, count);
        }

        private void ensureCellColors() {
            final int length = getSignIds() == null ? 0 : getSignIds().length;
            if (cellColors == null || cellColors.length != length) {
                cellColors = new long[length][];
            }
        }

        @Override
        public void setData(LongAVLTreeSet selectedIds, String[] signIds) {
            super.setData(selectedIds, signIds);
            // 服务端才能拿到完整 Route（含 routeNumber），在玩家编辑告示牌时解析一次
            updateRouteNumbersFromServer();
        }

        @Override
        public void readCompoundTag(CompoundTag compoundTag) {
            super.readCompoundTag(compoundTag);
            routeNumbers.clear();
            final long[] colors = compoundTag.getLongArray(KEY_ROUTE_COLORS);
            final String joined = compoundTag.getString(KEY_ROUTE_NUMBERS);
            if (!joined.isEmpty()) {
                final String[] numbers = joined.split("\u0001", -1);
                for (int i = 0; i < colors.length && i < numbers.length; i++) {
                    routeNumbers.put(colors[i], numbers[i]);
                }
            }
            // 每格独立线路颜色："<格子>:<颜色>,<颜色>;<格子>:<颜色>"
            cellColors = null;
            ensureCellColors();
            final String cellColorsRaw = compoundTag.getString(KEY_CELL_COLORS);
            for (final String entry : cellColorsRaw.split(";")) {
                if (entry.isEmpty()) {
                    continue;
                }
                final int colon = entry.indexOf(':');
                if (colon <= 0) {
                    continue;
                }
                final int cell;
                final long[] cellArr;
                try {
                    cell = Integer.parseInt(entry.substring(0, colon).trim());
                    final String[] parts = entry.substring(colon + 1).split(",");
                    cellArr = new long[parts.length];
                    for (int i = 0; i < parts.length; i++) {
                        cellArr[i] = Long.parseLong(parts[i].trim());
                    }
                } catch (NumberFormatException ignored) {
                    continue;
                }
                if (cell >= 0 && cell < cellColors.length) {
                    cellColors[cell] = cellArr;
                }
            }
        }

        @Override
        public void writeCompoundTag(CompoundTag compoundTag) {
            super.writeCompoundTag(compoundTag);
            final long[] colors = routeNumbers.keySet().toLongArray();
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < colors.length; i++) {
                if (i > 0) {
                    sb.append('\u0001');
                }
                sb.append(routeNumbers.get(colors[i]));
            }
            compoundTag.putLongArray(KEY_ROUTE_COLORS, colors);
            compoundTag.putString(KEY_ROUTE_NUMBERS, sb.toString());
            // 每格独立线路颜色
            final StringBuilder cellSb = new StringBuilder();
            if (cellColors != null) {
                for (int i = 0; i < cellColors.length; i++) {
                    final long[] cellArr = cellColors[i];
                    if (cellArr == null || cellArr.length == 0) {
                        continue;
                    }
                    if (cellSb.length() > 0) {
                        cellSb.append(';');
                    }
                    cellSb.append(i).append(':');
                    for (int j = 0; j < cellArr.length; j++) {
                        if (j > 0) {
                            cellSb.append(',');
                        }
                        cellSb.append(cellArr[j]);
                    }
                }
            }
            compoundTag.putString(KEY_CELL_COLORS, cellSb.toString());
        }

        /**
         * 服务端专用：反射访问 MTR 的 Simulator，找到告示牌所在站点，收集所选颜色对应的
         * 完整 Route 的线路编号（routeNumber）。客户端数据没有该字段，只能从服务端获取。
         */
        private void updateRouteNumbersFromServer() {
            routeNumbers.clear();
            if (getWorld2() == null || getWorld2().isClient()) {
                return;
            }
            // 收集全局选择与所有格子独立选择的颜色，统一解析线路编号
            final LongAVLTreeSet allColors = new LongAVLTreeSet(getSelectedIds());
            if (cellColors != null) {
                for (final long[] cellArr : cellColors) {
                    if (cellArr != null) {
                        for (final long color : cellArr) {
                            allColors.add(color);
                        }
                    }
                }
            }
            if (allColors.isEmpty()) {
                markDirty2();
                return;
            }
            try {
                final Field mainField = Class.forName("org.mtr.mod.Init").getDeclaredField("main");
                mainField.setAccessible(true);
                final Object main = mainField.get(null);
                if (main == null) {
                    return;
                }
                final Field simulatorsField = main.getClass().getDeclaredField("simulators");
                simulatorsField.setAccessible(true);
                final Iterable<?> simulators = (Iterable<?>) simulatorsField.get(main);
                final Position position = org.mtr.mod.Init.blockPosToPosition(getPos2());
                for (final Object simulator : simulators) {
                    final Data data = (Data) simulator;
                    for (final Station station : data.stations) {
                        if (!station.inArea(position)) {
                            continue;
                        }
                        for (final Platform platform : station.savedRails) {
                            for (final Route route : platform.routes) {
                                // selectedIds 存的是颜色（MTR 用 i2l 有符号扩展），Route.getColor() 返回 int
                                final long color = route.getColor();
                                if (allColors.contains(color) && !routeNumbers.containsKey(color)) {
                                    final String number = route.getRouteNumber();
                                    if (number != null && !number.isEmpty()) {
                                        routeNumbers.put(color, number);
                                    }
                                }
                            }
                        }
                        break;
                    }
                }
            } catch (Exception e) {
                Init.LOGGER.error("BlockCRTRailwaySign: Failed to resolve route numbers at {}", getPos2().toShortString(), e);
            }
            markDirty2();
        }
    }

    public void addTooltips(ItemStack stack, @Nullable BlockView world, List<MutableText> tooltip, TooltipContext options) {
        tooltip.add(TranslationProvider.BRUSH_USE.getMutableText(new Object[0]).formatted(TextFormatting.DARK_GRAY));
    }
}