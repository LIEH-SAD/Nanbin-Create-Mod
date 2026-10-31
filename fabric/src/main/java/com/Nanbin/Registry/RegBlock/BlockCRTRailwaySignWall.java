package com.Nanbin.Registry.RegBlock;

import com.Nanbin.Init;
import com.Nanbin.entity.BlockEntityTypes;
import com.Nanbin.mapping.TranslationProvider;
import com.Nanbin.packet.PacketOpenBlockEntityScreen;
import org.mtr.core.data.Data;
import org.mtr.core.data.Platform;
import org.mtr.core.data.Position;
import org.mtr.core.data.Route;
import org.mtr.core.data.Station;
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

// CRT 墙挂铁路指示牌：同一面墙上的相邻方块自动连接成一整块牌，每块占 1 格内容。
public class BlockCRTRailwaySignWall extends BlockExtension implements IBlock, DirectionHelper, BlockWithEntity {

    // 连接状态：LEFT / RIGHT 表示观察者左右两侧是否有同向的相邻方块。
    public static final BooleanProperty LEFT = BooleanProperty.of("left");
    public static final BooleanProperty RIGHT = BooleanProperty.of("right");
    // 单次扫描的最大长度，避免异常数据导致死循环。
    public static final int MAX_RUN = 64;

    public BlockCRTRailwaySignWall(BlockSettings blockSettings) {
        super(blockSettings);
    }

    @Nonnull
    @Override
    public BlockEntityExtension createBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntity(pos, state);
    }

    @Override
    public void addBlockProperties(List<HolderBase<?>> properties) {
        super.addBlockProperties(properties);
        properties.add(FACING);
        properties.add(LEFT);
        properties.add(RIGHT);
    }

    public static Direction leftDirection(Direction facing) {
        return facing.rotateYCounterclockwise();
    }

    public static Direction rightDirection(Direction facing) {
        return facing.rotateYClockwise();
    }

    public static boolean isWallSign(BlockState state) {
        return state.getBlock().data instanceof BlockCRTRailwaySignWall;
    }

    public static boolean isConnected(BlockState state, Direction facing) {
        return isWallSign(state) && IBlock.getStatePropertySafe(state, FACING) == facing;
    }

    @Nullable
    public static BlockEntity getBlockEntity(BlockView world, BlockPos pos) {
        final org.mtr.mapping.holder.BlockEntity blockEntity = world.getBlockEntity(pos);
        return blockEntity != null && blockEntity.data instanceof BlockEntity wallEntity ? wallEntity : null;
    }

    // 从任意一格向观察者左侧回溯，找到整块牌的左端（渲染与编辑都从左端开始）。
    public static BlockPos findStart(BlockView world, BlockPos pos, BlockState state) {
        if (!isWallSign(state)) {
            return pos;
        }
        final Direction facing = IBlock.getStatePropertySafe(state, FACING);
        BlockPos start = pos;
        for (int i = 0; i < MAX_RUN; i++) {
            final BlockPos next = start.offset(leftDirection(facing));
            if (!isConnected(world.getBlockState(next), facing)) {
                return start;
            }
            start = next;
        }
        return start;
    }

    // 从左端向右收集整块牌的所有方块位置，顺序即内容格顺序。
    public static List<BlockPos> collectRun(BlockView world, BlockPos start) {
        final List<BlockPos> positions = new ArrayList<>();
        final BlockState startState = world.getBlockState(start);
        if (!isWallSign(startState)) {
            return positions;
        }
        final Direction facing = IBlock.getStatePropertySafe(startState, FACING);
        for (int i = 0; i < MAX_RUN; i++) {
            final BlockPos pos = start.offset(rightDirection(facing), i);
            if (!isConnected(world.getBlockState(pos), facing)) {
                break;
            }
            positions.add(pos);
        }
        return positions;
    }

    private static BlockState withConnections(BlockView world, BlockPos pos, BlockState state) {
        final Direction facing = IBlock.getStatePropertySafe(state, FACING);
        final boolean left = isConnected(world.getBlockState(pos.offset(leftDirection(facing))), facing);
        final boolean right = isConnected(world.getBlockState(pos.offset(rightDirection(facing))), facing);
        return state.with(new Property<>(LEFT.data), left).with(new Property<>(RIGHT.data), right);
    }

    @Override
    public BlockState getPlacementState2(ItemPlacementContext ctx) {
        // FACING 指向墙内，可见面为 FACING 的反方向；优先贴点击的墙面，点击上下表面时退化为玩家朝向。
        final Direction hitSide = ctx.getSide();
        final boolean horizontal = hitSide != Direction.UP && hitSide != Direction.DOWN;
        final Direction facing = horizontal ? hitSide.getOpposite() : ctx.getPlayerFacing();
        final BlockState state = getDefaultState2().with(new Property<>(FACING.data), facing.data);
        return withConnections(BlockView.cast(ctx.getWorld()), ctx.getBlockPos(), state);
    }

    @Nonnull
    @Override
    public BlockState getStateForNeighborUpdate2(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        return withConnections(BlockView.cast(world), pos, state);
    }

    @Override
    public void onPlaced2(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        if (world.isClient()) {
            return;
        }
        // 新方块继承相邻牌面的数据选择，避免中途加长后整块牌丢线路/车站数据。
        final Direction facing = IBlock.getStatePropertySafe(state, FACING);
        BlockEntity source = getBlockEntity(BlockView.cast(world), pos.offset(leftDirection(facing)));
        if (source == null) {
            source = getBlockEntity(BlockView.cast(world), pos.offset(rightDirection(facing)));
        }
        final BlockEntity entity = getBlockEntity(BlockView.cast(world), pos);
        if (entity != null && source != null) {
            entity.copySelectedIds(source.getSelectedIds());
        }
    }

    @Nonnull
    @Override
    public ActionResult onUse2(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        return IBlock.checkHoldingBrush(world, player, () -> {
            final BlockPos start = findStart(BlockView.cast(world), pos, state);
            Init.REGISTRY.sendPacketToClient(ServerPlayerEntity.cast(player), new PacketOpenBlockEntityScreen(start));
        });
    }

    @Nonnull
    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        final Direction facing = IBlock.getStatePropertySafe(state, FACING);
        return IBlock.getVoxelShapeByDirection(0, 3, 0, 16, 13, 2, facing);
    }

    public void addTooltips(ItemStack stack, @Nullable BlockView world, List<MutableText> tooltip, TooltipContext options) {
        tooltip.add(TranslationProvider.BRUSH_USE.getMutableText(new Object[0]).formatted(TextFormatting.DARK_GRAY));
    }

    public static class BlockEntity extends BlockEntityExtension {

        private static final String KEY_SIGN_ID = "wall_sign_id";
        private static final String KEY_SELECTED_IDS = "selected_ids";
        private static final String KEY_ROUTE_COLORS = "wall_route_colors";
        private static final String KEY_ROUTE_NUMBERS = "wall_route_numbers";

        private String signId;
        private final LongAVLTreeSet selectedIds = new LongAVLTreeSet();
        // 颜色 -> 线路编号；MTR 客户端数据只有 SimplifiedRoute，只能服务端解析后随 NBT 同步。
        private final Long2ObjectOpenHashMap<String> routeNumbers = new Long2ObjectOpenHashMap<>();

        public BlockEntity(BlockPos pos, BlockState state) {
            super(BlockEntityTypes.CRT_RAILWAY_SIGN_WALL_1.get(), pos, state);
        }

        public String getSignId() {
            return signId;
        }

        public LongAVLTreeSet getSelectedIds() {
            return selectedIds;
        }

        public void copySelectedIds(LongAVLTreeSet ids) {
            selectedIds.clear();
            selectedIds.addAll(ids);
            markDirty2();
        }

        // 服务端写入本格内容；selectedIds 为整块牌共用的数据选择。
        public void setData(String newSignId, LongAVLTreeSet newSelectedIds) {
            signId = newSignId == null || newSignId.isEmpty() ? null : newSignId;
            if (newSelectedIds != null) {
                selectedIds.clear();
                selectedIds.addAll(newSelectedIds);
            }
            updateRouteNumbersFromServer();
            markDirty2();
        }

        @Override
        public void readCompoundTag(CompoundTag compoundTag) {
            super.readCompoundTag(compoundTag);
            signId = compoundTag.getString(KEY_SIGN_ID);
            if (signId.isEmpty()) {
                signId = null;
            }
            selectedIds.clear();
            for (final long id : compoundTag.getLongArray(KEY_SELECTED_IDS)) {
                selectedIds.add(id);
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
            compoundTag.putString(KEY_SIGN_ID, signId == null ? "" : signId);
            compoundTag.putLongArray(KEY_SELECTED_IDS, selectedIds.toLongArray());
            final long[] colors = routeNumbers.keySet().toLongArray();
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < colors.length; i++) {
                if (i > 0) {
                    sb.append('\u0001');
                }
                final String number = routeNumbers.get(colors[i]);
                sb.append(number == null ? "" : number);
            }
            compoundTag.putLongArray(KEY_ROUTE_COLORS, colors);
            compoundTag.putString(KEY_ROUTE_NUMBERS, sb.toString());
        }

        // 按 selectedIds 顺序返回已解析的线路编号，无编号的颜色跳过。
        public String[] getRouteNumbers() {
            final long[] colors = selectedIds.toLongArray();
            final List<String> numbers = new ArrayList<>();
            for (final long color : colors) {
                final String number = routeNumbers.get(color);
                if (number != null && !number.isEmpty()) {
                    numbers.add(number);
                }
            }
            return numbers.toArray(new String[0]);
        }

        public String getRouteNumber(long color) {
            final String number = routeNumbers.get(color);
            return number == null ? "" : number;
        }

        // 服务端专用：反射访问 MTR 的 Simulator，在告示牌所在站点解析所选颜色对应的 line number。
        private void updateRouteNumbersFromServer() {
            routeNumbers.clear();
            if (getWorld2() == null || getWorld2().isClient() || selectedIds.isEmpty()) {
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
                                final long color = route.getColor();
                                if (selectedIds.contains(color) && !routeNumbers.containsKey(color)) {
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
                Init.LOGGER.error("BlockCRTRailwaySignWall: Failed to resolve route numbers at {}", getPos2().toShortString(), e);
            }
            markDirty2();
        }
    }
}
