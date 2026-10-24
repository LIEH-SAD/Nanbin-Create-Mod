package com.Nanbin.Registry.RegBlock;

import com.Nanbin.Init;
import com.Nanbin.entity.BlockEntityTypes;
import com.Nanbin.mapping.IBlockExtension;
import com.Nanbin.mapping.Registry;
import com.Nanbin.mapping.TranslationProvider;
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

import static org.mtr.mod.block.IBlock.HALF;
import static org.mtr.mod.block.IBlock.SIDE_EXTENDED;

public class BlockCRTStationInfo1Double extends BlockExtension implements BlockWithEntity, DirectionHelper {

    public BlockCRTStationInfo1Double(BlockSettings blockSettings) {
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
            final Direction facing = IBlock.getStatePropertySafe(state, FACING);
            final Direction hitSide = hit.getSide();
            if (hitSide != facing && hitSide != facing.getOpposite()) {
                return;
            }
            final boolean isFront = hitSide.getOpposite() == facing;
            final org.mtr.mapping.holder.BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity == null || !(blockEntity.data instanceof BlockEntity entity)) {
                return;
            }
            final IBlock.DoubleBlockHalf half = IBlock.getStatePropertySafe(state, HALF);
            if (half == IBlock.DoubleBlockHalf.UPPER) {
                entity.toggleFlip(isFront);
            } else {
                Registry.sendPacketToClient(ServerPlayerEntity.cast(player), new PacketOpenStationInfoScreen(pos, entity.getUrl(isFront), isFront));
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
        } else {
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
        private static final String KEY_URL_BACK = "url_back";
        private static final String KEY_SIGN_IDS = "crt_sign_ids";
        private static final String KEY_SIGN_IDS_BACK = "crt_sign_ids_back";
        private static final String KEY_SELECTED_IDS = "crt_selected_ids";
        private static final String KEY_SELECTED_IDS_BACK = "crt_selected_ids_back";
        private static final String KEY_FLIP = "crt_flip";
        private static final String KEY_FLIP_BACK = "crt_flip_back";
        private static final String KEY_ROUTE_COLORS = "crt_route_colors";
        private static final String KEY_ROUTE_COLORS_BACK = "crt_route_colors_back";
        private static final String KEY_ROUTE_NUMBERS = "crt_route_numbers";
        private static final String KEY_ROUTE_NUMBERS_BACK = "crt_route_numbers_back";

        private String url = "";
        private String urlBack = "";
        private boolean flip = false;
        private boolean flipBack = false;
        private final String[][] signIds = new String[SIGN_LINES][SIGN_LENGTH];
        private final String[][] signIdsBack = new String[SIGN_LINES][SIGN_LENGTH];
        private final List<LongAVLTreeSet> selectedIds = new ArrayList<>();
        private final List<LongAVLTreeSet> selectedIdsBack = new ArrayList<>();
        private final Long2ObjectOpenHashMap<String> routeNumbers = new Long2ObjectOpenHashMap<>();
        private final Long2ObjectOpenHashMap<String> routeNumbersBack = new Long2ObjectOpenHashMap<>();

        public BlockEntity(BlockPos pos, BlockState state) {
            super(BlockEntityTypes.CRT_STATION_INFO_1_DOUBLE.get(), pos, state);
            selectedIds.add(new LongAVLTreeSet());
            selectedIds.add(new LongAVLTreeSet());
            selectedIdsBack.add(new LongAVLTreeSet());
            selectedIdsBack.add(new LongAVLTreeSet());
        }

        public String getUrl(boolean front) {
            return front ? url : urlBack;
        }

        public String[][] getSignIds(boolean front) {
            return front ? signIds : signIdsBack;
        }

        public List<LongAVLTreeSet> getSelectedIds(boolean front) {
            return front ? selectedIds : selectedIdsBack;
        }

        public String[] getRouteNumbers(boolean front) {
            final List<LongAVLTreeSet> targetIds = front ? selectedIds : selectedIdsBack;
            final Long2ObjectOpenHashMap<String> targetNumbers = front ? routeNumbers : routeNumbersBack;
            final List<String> result = new ArrayList<>();
            for (final LongAVLTreeSet lineSelected : targetIds) {
                for (final long platformId : lineSelected.longStream().toArray()) {
                    final String number = targetNumbers.get(platformId);
                    if (number != null && !number.isEmpty()) {
                        result.add(number);
                    }
                }
            }
            return result.toArray(new String[0]);
        }

        /** 指定站台（平台 ID）的线路编号（正面）；无则返回空串。 */
        public String getRouteNumber(long platformId) {
            return getRouteNumber(platformId, true);
        }

        /** 指定站台（平台 ID）的线路编号（front 指定正/反面）；无则返回空串。 */
        public String getRouteNumber(long platformId, boolean front) {
            final String number = (front ? routeNumbers : routeNumbersBack).get(platformId);
            return number == null ? "" : number;
        }

        public boolean isFlip(boolean front) {
            return front ? flip : flipBack;
        }

        public void toggleFlip(boolean front) {
            if (front) {
                this.flip = !this.flip;
            } else {
                this.flipBack = !this.flipBack;
            }
            syncFlipToOtherBlocks(front);
            markDirty2();
        }

        public void setFlip(boolean front, boolean newFlip) {
            if (front) {
                this.flip = newFlip;
            } else {
                this.flipBack = newFlip;
            }
            syncFlipToOtherBlocks(front);
            markDirty2();
        }

        private void syncFlipToOtherBlocks(boolean front) {
            final Direction facing = IBlock.getStatePropertySafe(this.getCachedState2(), FACING);
            final Direction right = facing.rotateYClockwise();
            final BlockPos bottomLeftPos = findBottomLeftPosition(this.getWorld2(), this.getPos2(), facing);
            if (bottomLeftPos == null) return;
            final boolean flipValue = front ? this.flip : this.flipBack;
            for (int y = 0; y < 3; y++) {
                for (int x = 0; x < 3; x++) {
                    final BlockPos otherPos = bottomLeftPos.up(y).offset(right, x);
                    if (!otherPos.equals(this.getPos2())) {
                        final org.mtr.mapping.holder.BlockEntity blockEntity = this.getWorld2().getBlockEntity(otherPos);
                        if (blockEntity != null && blockEntity.data instanceof BlockEntity entity) {
                            if (front) {
                                entity.flip = flipValue;
                            } else {
                                entity.flipBack = flipValue;
                            }
                            entity.markDirty2();
                        }
                    }
                }
            }
        }

        public void setUrl(boolean front, String newUrl) {
            if (front) {
                this.url = newUrl;
            } else {
                this.urlBack = newUrl;
            }
            syncDataToOtherBlocks(front);
            markDirty2();
        }

        public void setSelectedIdsLine(boolean front, int line, LongAVLTreeSet newSelectedIds) {
            final List<LongAVLTreeSet> targetIds = front ? selectedIds : selectedIdsBack;
            if (line < 0 || line >= targetIds.size()) {
                return;
            }
            final LongAVLTreeSet destSet = targetIds.get(line);
            destSet.clear();
            if (newSelectedIds != null) {
                destSet.addAll(newSelectedIds);
            }
            syncSignDataToOtherBlocks(front);
            updateRouteNumbersFromServer(front);
        }

        public void setSignData(boolean front, String[][] newSignIds, List<LongAVLTreeSet> newSelectedIds) {
            copySignDataOnly(front, newSignIds, newSelectedIds);
            syncSignDataToOtherBlocks(front);
            updateRouteNumbersFromServer(front);
        }

        private void syncDataToOtherBlocks(boolean front) {
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
                            if (front) {
                                entity.url = this.url;
                            } else {
                                entity.urlBack = this.urlBack;
                            }
                            entity.markDirty2();
                        }
                    }
                }
            }
        }

        private void syncSignDataToOtherBlocks(boolean front) {
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
                            entity.copySignDataOnly(front, front ? this.signIds : this.signIdsBack, front ? this.selectedIds : this.selectedIdsBack);
                            entity.updateRouteNumbersFromServer(front);
                        }
                    }
                }
            }
        }

        private void copySignDataOnly(boolean front, String[][] newSignIds, List<LongAVLTreeSet> newSelectedIds) {
            final String[][] targetSignIds = front ? signIds : signIdsBack;
            final List<LongAVLTreeSet> targetSelectedIds = front ? selectedIds : selectedIdsBack;
            if (newSignIds != null) {
                for (int i = 0; i < targetSignIds.length && i < newSignIds.length; i++) {
                    final String[] src = newSignIds[i];
                    if (src == null) continue;
                    final String[] dst = targetSignIds[i];
                    final int copyLength = Math.min(src.length, dst.length);
                    System.arraycopy(src, 0, dst, 0, copyLength);
                    for (int j = copyLength; j < dst.length; j++) {
                        dst[j] = null;
                    }
                }
            }
            if (newSelectedIds != null) {
                for (int i = 0; i < targetSelectedIds.size() && i < newSelectedIds.size(); i++) {
                    final LongAVLTreeSet sourceSet = newSelectedIds.get(i);
                    final LongAVLTreeSet destSet = targetSelectedIds.get(i);
                    destSet.clear();
                    if (sourceSet != null) {
                        destSet.addAll(sourceSet);
                    }
                }
            }
            markDirty2();
        }

        private void updateRouteNumbersFromServer(boolean front) {
            final List<LongAVLTreeSet> targetIds = front ? selectedIds : selectedIdsBack;
            final Long2ObjectOpenHashMap<String> targetNumbers = front ? routeNumbers : routeNumbersBack;
            targetNumbers.clear();
            if (getWorld2() == null || getWorld2().isClient()) {
                return;
            }
            final LongAVLTreeSet allPlatformIds = new LongAVLTreeSet();
            for (final LongAVLTreeSet lineSelected : targetIds) {
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
                                for (final RoutePlatformData routePlatform : route.getRoutePlatforms()) {
                                    final long routePlatformId = routePlatform.getPlatform() == null ? -1 : routePlatform.getPlatform().getId();
                                    if (routePlatformId >= 0 && allPlatformIds.contains(routePlatformId) && !targetNumbers.containsKey(routePlatformId)) {
                                        final String number = route.getRouteNumber();
                                        if (number != null && !number.isEmpty()) {
                                            targetNumbers.put(routePlatformId, number);
                                        }
                                    }
                                }
                            }
                        }
                        break;
                    }
                }
            } catch (Exception e) {
                Init.LOGGER.error("BlockCRTStationInfo2: Failed to resolve route numbers at {}", getPos2().toShortString(), e);
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
                if (!(belowState.getBlock().data instanceof BlockCRTStationInfo1Double)) {
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
            urlBack = compoundTag.getString(KEY_URL_BACK);
            flip = compoundTag.getBoolean(KEY_FLIP);
            flipBack = compoundTag.getBoolean(KEY_FLIP_BACK);
            readSideData(compoundTag, KEY_SIGN_IDS, KEY_SELECTED_IDS, KEY_ROUTE_COLORS, KEY_ROUTE_NUMBERS, signIds, selectedIds, routeNumbers);
            readSideData(compoundTag, KEY_SIGN_IDS_BACK, KEY_SELECTED_IDS_BACK, KEY_ROUTE_COLORS_BACK, KEY_ROUTE_NUMBERS_BACK, signIdsBack, selectedIdsBack, routeNumbersBack);
        }

        private void readSideData(CompoundTag compoundTag, String signIdsKey, String selectedIdsKey, String routeColorsKey, String routeNumbersKey, String[][] targetSignIds, List<LongAVLTreeSet> targetSelectedIds, Long2ObjectOpenHashMap<String> targetRouteNumbers) {
            for (int i = 0; i < targetSignIds.length; i++) {
                final long[] ids = compoundTag.getLongArray(selectedIdsKey + "_" + i);
                final LongAVLTreeSet set = targetSelectedIds.get(i);
                set.clear();
                for (final long id : ids) {
                    set.add(id);
                }
                for (int j = 0; j < targetSignIds[i].length; j++) {
                    final String signId = compoundTag.getString(signIdsKey + "_" + i + "_" + j);
                    targetSignIds[i][j] = signId.isEmpty() ? null : signId;
                }
            }
            targetRouteNumbers.clear();
            final long[] colors = compoundTag.getLongArray(routeColorsKey);
            final String joined = compoundTag.getString(routeNumbersKey);
            if (!joined.isEmpty()) {
                final String[] numbers = joined.split("\u0001", -1);
                for (int i = 0; i < colors.length && i < numbers.length; i++) {
                    targetRouteNumbers.put(colors[i], numbers[i]);
                }
            }
        }

        @Override
        public void writeCompoundTag(CompoundTag compoundTag) {
            super.writeCompoundTag(compoundTag);
            compoundTag.putString(KEY_URL, url);
            compoundTag.putString(KEY_URL_BACK, urlBack);
            compoundTag.putBoolean(KEY_FLIP, flip);
            compoundTag.putBoolean(KEY_FLIP_BACK, flipBack);
            writeSideData(compoundTag, KEY_SIGN_IDS, KEY_SELECTED_IDS, KEY_ROUTE_COLORS, KEY_ROUTE_NUMBERS, signIds, selectedIds, routeNumbers);
            writeSideData(compoundTag, KEY_SIGN_IDS_BACK, KEY_SELECTED_IDS_BACK, KEY_ROUTE_COLORS_BACK, KEY_ROUTE_NUMBERS_BACK, signIdsBack, selectedIdsBack, routeNumbersBack);
        }

        private void writeSideData(CompoundTag compoundTag, String signIdsKey, String selectedIdsKey, String routeColorsKey, String routeNumbersKey, String[][] targetSignIds, List<LongAVLTreeSet> targetSelectedIds, Long2ObjectOpenHashMap<String> targetRouteNumbers) {
            for (int i = 0; i < targetSignIds.length; i++) {
                compoundTag.putLongArray(selectedIdsKey + "_" + i, new ArrayList<>(targetSelectedIds.get(i)));
                for (int j = 0; j < targetSignIds[i].length; j++) {
                    compoundTag.putString(signIdsKey + "_" + i + "_" + j, targetSignIds[i][j] == null ? "" : targetSignIds[i][j]);
                }
            }
            final long[] colors = targetRouteNumbers.keySet().toLongArray();
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < colors.length; i++) {
                if (i > 0) {
                    sb.append('\u0001');
                }
                sb.append(targetRouteNumbers.get(colors[i]));
            }
            compoundTag.putLongArray(routeColorsKey, colors);
            compoundTag.putString(routeNumbersKey, sb.toString());
        }
    }

    public void addTooltips(ItemStack stack, @Nullable BlockView world, List<MutableText> tooltip, TooltipContext options) {
        tooltip.add(TranslationProvider.BRUSH_USE.getMutableText(new Object[0]).formatted(TextFormatting.DARK_GRAY));
    }
}