package com.Nanbin.Registry.RegBlock;

import com.Nanbin.Init;
import com.Nanbin.entity.BlockEntityTypes;
import com.Nanbin.mapping.IBlockExtension;
import com.Nanbin.mapping.Registry;
import com.Nanbin.packet.PacketOpenStationInfoScreen;
import org.mtr.core.data.*;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mapping.mapper.BlockExtension;
import org.mtr.mapping.mapper.BlockWithEntity;
import org.mtr.mapping.mapper.DirectionHelper;
import org.mtr.mapping.tool.HolderBase;
import org.mtr.mod.block.IBlock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.mtr.mod.block.IBlock.SIDE_EXTENDED;
import static org.mtr.mod.block.IBlock.HALF;

public class BlockCRTStationInfo1 extends BlockExtension implements BlockWithEntity, DirectionHelper {

    public BlockCRTStationInfo1(BlockSettings blockSettings) {
        super(blockSettings);
    }

    @Nonnull
    @Override
    public BlockEntityExtension createBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new BlockEntity(blockPos, blockState);
    }

    @Override
    public void addBlockProperties(List<HolderBase<?>> properties) {
        super.addBlockProperties(properties);
        properties.add(FACING);
        properties.add(SIDE_EXTENDED);
        properties.add(HALF);
    }

    @Override
    public @Nullable BlockState getPlacementState2(ItemPlacementContext ctx) {
        final Direction direction = ctx.getPlayerFacing();
        final BlockState baseState = getDefaultState2().with(new Property<>(FACING.data), direction.data).with(new Property<>(HALF.data), IBlock.DoubleBlockHalf.LOWER);
        final Direction right = direction.rotateYClockwise();
        final BlockPos startPos = ctx.getBlockPos();
        if (canPlace(ctx, startPos, right)) {
            placeStructure(ctx.getWorld(), startPos, direction, baseState);
            return baseState.with(new Property<>(SIDE_EXTENDED.data), IBlock.EnumSide.LEFT);
        }
        return null;
    }

    private boolean canPlace(ItemPlacementContext ctx, BlockPos startPos, Direction right) {
        final World world = ctx.getWorld();
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                if (x == 0 && y == 0) continue;
                final BlockPos checkPos = startPos.up(y).offset(right, x);
                final BlockState checkState = world.getBlockState(checkPos);
                if (!checkState.isAir() && !checkState.canReplace(ctx)) {
                    return false;
                }
            }
        }
        return true;
    }

    private void placeStructure(World world, BlockPos startPos, Direction facing, BlockState baseState) {
        final Direction right = facing.rotateYClockwise();
        for (int y = 0; y < 3; y++) {
            final IBlock.DoubleBlockHalf half = y < 2 ? IBlock.DoubleBlockHalf.LOWER : IBlock.DoubleBlockHalf.UPPER;
            for (int x = 0; x < 3; x++) {
                if (x == 0 && y == 0) continue;
                final BlockPos placePos = startPos.up(y).offset(right, x);
                final IBlock.EnumSide side = x == 0 ? IBlock.EnumSide.LEFT : (x == 1 ? IBlock.EnumSide.MIDDLE : IBlock.EnumSide.RIGHT);
                world.setBlockState(placePos, baseState.with(new Property<>(SIDE_EXTENDED.data), side).with(new Property<>(HALF.data), half), 3);
            }
        }
        world.updateNeighbors(startPos, org.mtr.mapping.holder.Blocks.getAirMapped());
    }

    @Override
    public void onBreak2(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        final Direction direction = IBlock.getStatePropertySafe(state, FACING);
        final Direction right = direction.rotateYClockwise();
        final Direction left = direction.rotateYCounterclockwise();
        final IBlock.EnumSide side = IBlock.getStatePropertySafe(state, SIDE_EXTENDED);
        final IBlock.DoubleBlockHalf half = IBlock.getStatePropertySafe(state, HALF);
        final BlockPos bottomLeftPos = findBottomLeftPosition(world, pos, direction, side, half);
        if (bottomLeftPos != null) {
            for (int y = 0; y < 3; y++) {
                for (int x = 0; x < 3; x++) {
                    final BlockPos breakPos = bottomLeftPos.up(y).offset(right, x);
                    if (!breakPos.equals(pos)) {
                        IBlockExtension.breakBlock(world, breakPos);
                    }
                }
            }
        }
        super.onBreak2(world, pos, state, player);
    }

    private BlockPos findBottomLeftPosition(World world, BlockPos pos, Direction facing, IBlock.EnumSide side, IBlock.DoubleBlockHalf half) {
        final Direction left = facing.rotateYCounterclockwise();
        BlockPos basePos = pos;
        if (side == IBlock.EnumSide.MIDDLE) {
            basePos = pos.offset(left, 1);
        } else if (side == IBlock.EnumSide.RIGHT) {
            basePos = pos.offset(left, 2);
        }
        while (basePos.getY() > 0) {
            final BlockPos belowPos = basePos.down();
            final BlockState belowState = world.getBlockState(belowPos);
            if (belowState.getBlock().data != this) {
                return basePos;
            }
            basePos = belowPos;
        }
        return basePos;
    }

    @Nonnull
    @Override
    public ActionResult onUse2(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        return IBlockExtension.checkHoldingBrush(world, player, () -> {
            final org.mtr.mapping.holder.BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity == null || !(blockEntity.data instanceof BlockEntity entity)) {
                return;
            }
            final IBlock.DoubleBlockHalf half = IBlock.getStatePropertySafe(state, HALF);
            if (half == IBlock.DoubleBlockHalf.UPPER) {
                entity.toggleFlip();
            } else {
                Registry.sendPacketToClient(ServerPlayerEntity.cast(player), new PacketOpenStationInfoScreen(pos, entity.getUrl()));
            }
        });
    }

    @Nonnull
    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        final Direction direction = IBlock.getStatePropertySafe(state, FACING);
        final IBlock.EnumSide side = IBlock.getStatePropertySafe(state, SIDE_EXTENDED);
        final IBlock.DoubleBlockHalf half = IBlock.getStatePropertySafe(state, HALF);
        if (half == IBlock.DoubleBlockHalf.UPPER) {
            if (side == IBlock.EnumSide.LEFT) {
                return IBlock.getVoxelShapeByDirection(1, 0, 6, 16, 8, 10, direction);
            } else if (side == IBlock.EnumSide.MIDDLE) {
                return IBlock.getVoxelShapeByDirection(0, 0, 6, 16, 8, 10, direction);
            } else {
                return IBlock.getVoxelShapeByDirection(0, 0, 6, 15, 8, 10, direction);
            }
        }else {
            if (side == IBlock.EnumSide.LEFT) {
                return IBlock.getVoxelShapeByDirection(1, 0, 6, 16, 16, 10, direction);
            } else if (side == IBlock.EnumSide.MIDDLE) {
                return IBlock.getVoxelShapeByDirection(0, 0, 6, 16, 16, 10, direction);
            } else {
                return IBlock.getVoxelShapeByDirection(0, 0, 6, 15, 16, 10, direction);
            }
        }
    }

    public static class BlockEntity extends BlockEntityExtension {

        public static final int SIGN_LINES = 2;
        public static final int SIGN_LENGTH = 7;

        private static final String KEY_URL = "url";
        private static final String KEY_SIGN_IDS = "crt_sign_ids";
        private static final String KEY_SELECTED_IDS = "crt_selected_ids";
        private static final String KEY_FLIP = "crt_flip";
        private static final String KEY_ROUTE_COLORS = "crt_route_colors";
        private static final String KEY_ROUTE_NUMBERS = "crt_route_numbers";

        private String url = "";
        /** 顶部条带方向：false = 灰色矩形在左、线路色组合图在右；true = 镜像 */
        private boolean flip = false;
        private final String[][] signIds = new String[SIGN_LINES][SIGN_LENGTH];
        private final List<LongAVLTreeSet> selectedIds = new ArrayList<>();
        /** 站台 ID -> 线路编号。MTR 客户端数据只有 SimplifiedRoute（无 routeNumber），只能服务端解析后写入 NBT。 */
        private final Long2ObjectOpenHashMap<String> routeNumbers = new Long2ObjectOpenHashMap<>();

        public BlockEntity(BlockPos pos, BlockState state) {
            super(BlockEntityTypes.CRT_STATION_INFO_1.get(), pos, state);
            selectedIds.add(new LongAVLTreeSet());
            selectedIds.add(new LongAVLTreeSet());
        }

        public String getUrl() {
            return url;
        }

        public String[][] getSignIds() {
            return signIds;
        }

        public List<LongAVLTreeSet> getSelectedIds() {
            return selectedIds;
        }

        /** 按所有选中站台的顺序返回线路编号；无线路编号的站台会被跳过。 */
        public String[] getRouteNumbers() {
            final List<String> result = new ArrayList<>();
            for (final LongAVLTreeSet lineSelected : selectedIds) {
                for (final long platformId : lineSelected.longStream().toArray()) {
                    final String number = routeNumbers.get(platformId);
                    if (number != null && !number.isEmpty()) {
                        result.add(number);
                    }
                }
            }
            return result.toArray(new String[0]);
        }

        public boolean isFlip() {
            return flip;
        }

        /** 切换顶部条带方向，并同步到其他 8 个方块。 */
        public void toggleFlip() {
            this.flip = !this.flip;
            syncFlipToOtherBlocks();
            markDirty2();
        }

        public void setFlip(boolean newFlip) {
            this.flip = newFlip;
            syncFlipToOtherBlocks();
            markDirty2();
        }

        private void syncFlipToOtherBlocks() {
            final Direction facing = IBlock.getStatePropertySafe(this.getCachedState2(), FACING);
            final Direction right = facing.rotateYClockwise();
            final BlockPos bottomLeftPos = findBottomLeftPosition(this.getWorld2(), this.getPos2(), facing);
            if (bottomLeftPos == null) return;
            for (int y = 0; y < 3; y++) {
                for (int x = 0; x < 3; x++) {
                    final BlockPos otherPos = bottomLeftPos.up(y).offset(right, x);
                    if (!otherPos.equals(this.getPos2())) {
                        final org.mtr.mapping.holder.BlockEntity blockEntity = this.getWorld2().getBlockEntity(otherPos);
                        if (blockEntity != null && blockEntity.data instanceof BlockEntity entity) {
                            entity.flip = this.flip;
                            entity.markDirty2();
                        }
                    }
                }
            }
        }

        public void setUrl(String newUrl) {
            this.url = newUrl;
            syncDataToOtherBlocks();
            markDirty2();
        }

        /** 更新指定行的站台选择（如行 0 由 StationInfoScreen 的“选择站台”写入），并同步到其他方块。 */
        public void setSelectedIdsLine(int line, LongAVLTreeSet newSelectedIds) {
            if (line < 0 || line >= selectedIds.size()) {
                return;
            }
            final LongAVLTreeSet destSet = selectedIds.get(line);
            destSet.clear();
            if (newSelectedIds != null) {
                destSet.addAll(newSelectedIds);
            }
            syncSignDataToOtherBlocks();
            updateRouteNumbersFromServer();
        }

        public void setSignData(String[][] newSignIds, List<LongAVLTreeSet> newSelectedIds) {
            copySignDataOnly(newSignIds, newSelectedIds);
            syncSignDataToOtherBlocks();
            updateRouteNumbersFromServer();
        }

        private void syncDataToOtherBlocks() {
            final Direction facing = IBlock.getStatePropertySafe(this.getCachedState2(), FACING);
            final Direction right = facing.rotateYClockwise();
            final BlockPos bottomLeftPos = findBottomLeftPosition(this.getWorld2(), this.getPos2(), facing);
            if (bottomLeftPos == null) return;
            for (int y = 0; y < 3; y++) {
                for (int x = 0; x < 3; x++) {
                    final BlockPos otherPos = bottomLeftPos.up(y).offset(right, x);
                    if (!otherPos.equals(this.getPos2())) {
                        final org.mtr.mapping.holder.BlockEntity blockEntity = this.getWorld2().getBlockEntity(otherPos);
                        if (blockEntity != null && blockEntity.data instanceof BlockEntity entity) {
                            entity.url = this.url;
                            entity.markDirty2();
                        }
                    }
                }
            }
        }

        private void syncSignDataToOtherBlocks() {
            final Direction facing = IBlock.getStatePropertySafe(this.getCachedState2(), FACING);
            final Direction right = facing.rotateYClockwise();
            final BlockPos bottomLeftPos = findBottomLeftPosition(this.getWorld2(), this.getPos2(), facing);
            if (bottomLeftPos == null) return;
            for (int y = 0; y < 3; y++) {
                for (int x = 0; x < 3; x++) {
                    final BlockPos otherPos = bottomLeftPos.up(y).offset(right, x);
                    if (!otherPos.equals(this.getPos2())) {
                        final org.mtr.mapping.holder.BlockEntity blockEntity = this.getWorld2().getBlockEntity(otherPos);
                        if (blockEntity != null && blockEntity.data instanceof BlockEntity entity) {
                            entity.copySignDataOnly(this.signIds, this.selectedIds);
                            entity.updateRouteNumbersFromServer();
                        }
                    }
                }
            }
        }

        private void copySignDataOnly(String[][] newSignIds, List<LongAVLTreeSet> newSelectedIds) {
            if (newSignIds != null) {
                for (int i = 0; i < signIds.length && i < newSignIds.length; i++) {
                    final String[] src = newSignIds[i];
                    if (src == null) continue;
                    final String[] dst = signIds[i];
                    final int copyLength = Math.min(src.length, dst.length);
                    System.arraycopy(src, 0, dst, 0, copyLength);
                    for (int j = copyLength; j < dst.length; j++) {
                        dst[j] = null;
                    }
                }
            }
            if (newSelectedIds != null) {
                for (int i = 0; i < selectedIds.size() && i < newSelectedIds.size(); i++) {
                    final LongAVLTreeSet sourceSet = newSelectedIds.get(i);
                    final LongAVLTreeSet destSet = selectedIds.get(i);
                    destSet.clear();
                    if (sourceSet != null) {
                        destSet.addAll(sourceSet);
                    }
                }
            }
            markDirty2();
        }

        /**
         * 服务端专用：反射访问 MTR 的 Simulator，找到方块所在站点，收集所选站台（platformId）
         * 所在线路的线路编号（routeNumber）。客户端数据没有该字段，只能从服务端获取。
         */
        private void updateRouteNumbersFromServer() {
            routeNumbers.clear();
            if (getWorld2() == null || getWorld2().isClient()) {
                return;
            }
            final LongAVLTreeSet allPlatformIds = new LongAVLTreeSet();
            for (final LongAVLTreeSet lineSelected : selectedIds) {
                allPlatformIds.addAll(lineSelected);
            }
            if (allPlatformIds.isEmpty()) {
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
                final org.mtr.core.data.Position position = org.mtr.mod.Init.blockPosToPosition(getPos2());
                for (final Object simulator : simulators) {
                    final Data data = (Data) simulator;
                    for (final Station station : data.stations) {
                        if (!station.inArea(position)) {
                            continue;
                        }
                        for (final Platform platform : station.savedRails) {
                            for (final Route route : platform.routes) {
                                // selectedIds 存的是站台 ID，检查该线路是否经过所选站台
                                for (final RoutePlatformData routePlatform : route.getRoutePlatforms()) {
                                    final long routePlatformId = routePlatform.getPlatform() == null ? -1 : routePlatform.getPlatform().getId();
                                    if (routePlatformId >= 0 && allPlatformIds.contains(routePlatformId) && !routeNumbers.containsKey(routePlatformId)) {
                                        final String number = route.getRouteNumber();
                                        if (number != null && !number.isEmpty()) {
                                            routeNumbers.put(routePlatformId, number);
                                        }
                                    }
                                }
                            }
                        }
                        break;
                    }
                }
            } catch (Exception e) {
                Init.LOGGER.error("BlockCRTStationInfo1: Failed to resolve route numbers at {}", getPos2().toShortString(), e);
            }
            markDirty2();
        }

        private BlockPos findBottomLeftPosition(World world, BlockPos pos, Direction facing) {
            final Direction left = facing.rotateYCounterclockwise();
            final IBlock.EnumSide side = IBlock.getStatePropertySafe(this.getCachedState2(), SIDE_EXTENDED);
            BlockPos basePos = pos.offset(left, side == IBlock.EnumSide.MIDDLE ? 1 : side == IBlock.EnumSide.RIGHT ? 2 : 0);
            while (basePos.getY() > 0) {
                final BlockPos belowPos = basePos.down();
                final BlockState belowState = world.getBlockState(belowPos);
                if (!(belowState.getBlock().data instanceof BlockCRTStationInfo1)) {
                    return basePos;
                }
                basePos = belowPos;
            }
            return basePos;
        }

        @Override
        public void readCompoundTag(CompoundTag compoundTag) {
            super.readCompoundTag(compoundTag);
            url = compoundTag.getString(KEY_URL);
            flip = compoundTag.getBoolean(KEY_FLIP);
            for (int i = 0; i < signIds.length; i++) {
                final long[] ids = compoundTag.getLongArray(KEY_SELECTED_IDS + "_" + i);
                final LongAVLTreeSet set = selectedIds.get(i);
                set.clear();
                for (final long id : ids) {
                    set.add(id);
                }
                for (int j = 0; j < signIds[i].length; j++) {
                    final String signId = compoundTag.getString(KEY_SIGN_IDS + "_" + i + "_" + j);
                    signIds[i][j] = signId.isEmpty() ? null : signId;
                }
            }
            routeNumbers.clear();
            final long[] colors = compoundTag.getLongArray(KEY_ROUTE_COLORS);
            final String joined = compoundTag.getString(KEY_ROUTE_NUMBERS);
            if (!joined.isEmpty()) {
                final String[] numbers = joined.split("\u0001", -1);
                for (int i = 0; i < colors.length && i < numbers.length; i++) {
                    routeNumbers.put(colors[i], numbers[i]);
                }
            }
        }

        @Override
        public void writeCompoundTag(CompoundTag compoundTag) {
            super.writeCompoundTag(compoundTag);
            compoundTag.putString(KEY_URL, url);
            compoundTag.putBoolean(KEY_FLIP, flip);
            for (int i = 0; i < signIds.length; i++) {
                compoundTag.putLongArray(KEY_SELECTED_IDS + "_" + i, new ArrayList<>(selectedIds.get(i)));
                for (int j = 0; j < signIds[i].length; j++) {
                    compoundTag.putString(KEY_SIGN_IDS + "_" + i + "_" + j, signIds[i][j] == null ? "" : signIds[i][j]);
                }
            }
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
        }
    }
}