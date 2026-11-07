package com.Nanbin.Registry.RegBlock;

import com.Nanbin.Init;
import com.Nanbin.entity.BlockEntityTypes;
import com.Nanbin.mapping.TranslationProvider;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import mtr.block.IBlock;
import mtr.client.ClientData;
import mtr.data.Platform;
import mtr.data.Route;
import mtr.data.Station;
import mtr.mappings.BlockDirectionalMapper;
import mtr.mappings.BlockEntityClientSerializableMapper;
import mtr.mappings.BlockEntityMapper;
import mtr.mappings.EntityBlockMapper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * CRT 墙挂铁路指示牌：同一面墙上的相邻方块自动连接成一整块牌，每块占 1 格内容。
 *
 * <p>MTR 4 → MTR 3.6.3 适配要点：{@code BlockExtension + BlockWithEntity} → {@link BlockDirectionalMapper}
 * + {@link EntityBlockMapper}；{@code BlockEntityExtension} → {@link BlockEntityClientSerializableMapper}；
 * {@code DirectionHelper.FACING} → 原版 {@code HorizontalFacingBlock.FACING}；
 * {@code markDirty2()} → {@code markDirty()}；线路编号在客户端数据 {@link ClientData} 上解析（MTR 4 走服务端反射）。</p>
 */
public class BlockCRTRailwaySignWall1 extends BlockDirectionalMapper implements EntityBlockMapper {

	// 连接状态：LEFT / RIGHT 表示观察者左右两侧是否有同向的相邻方块。
	public static final BooleanProperty LEFT = BooleanProperty.of("left");
	public static final BooleanProperty RIGHT = BooleanProperty.of("right");
	// 单次扫描的最大长度，避免异常数据导致死循环。
	public static final int MAX_RUN = 64;
	// 每个方块放两格内容（格宽 0.5），与 MTR 原版指示牌的单格尺寸一致。
	public static final int CELLS_PER_BLOCK = 2;

	public BlockCRTRailwaySignWall1(Settings settings) {
		super(settings);
		setDefaultState(getDefaultState().with(LEFT, false).with(RIGHT, false));
	}

	@Override
	public BlockEntityMapper createBlockEntity(BlockPos pos, BlockState state) {
		return new BlockEntity(pos, state);
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING, LEFT, RIGHT);
	}

	public static Direction leftDirection(Direction facing) {
		return facing.rotateYCounterclockwise();
	}

	public static Direction rightDirection(Direction facing) {
		return facing.rotateYClockwise();
	}

	public static boolean isWallSign(BlockState state) {
		return state.getBlock() instanceof BlockCRTRailwaySignWall1;
	}

	public static boolean isConnected(BlockState state, Direction facing) {
		return isWallSign(state) && IBlock.getStatePropertySafe(state, FACING) == facing;
	}

	@Nullable
	public static BlockEntity getBlockEntity(BlockView world, BlockPos pos) {
		final net.minecraft.block.entity.BlockEntity blockEntity = world.getBlockEntity(pos);
		return blockEntity instanceof BlockEntity ? (BlockEntity) blockEntity : null;
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
		return state.with(LEFT, left).with(RIGHT, right);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		// FACING 指向墙内，可见面为 FACING 的反方向；优先贴点击的墙面，点击上下表面时退化为玩家朝向。
		final Direction hitSide = ctx.getSide();
		final boolean horizontal = hitSide != Direction.UP && hitSide != Direction.DOWN;
		final Direction facing = horizontal ? hitSide.getOpposite() : ctx.getHorizontalPlayerFacing();
		final BlockState state = getDefaultState().with(FACING, facing);
		return withConnections(ctx.getWorld(), ctx.getBlockPos(), state);
	}

	@Override
	public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
		return withConnections(world, pos, state);
	}

	@Override
	public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
		if (world.isClient()) {
			return;
		}
		// 新方块继承相邻牌面的数据选择，避免中途加长后整块牌丢线路/车站数据。
		final Direction facing = IBlock.getStatePropertySafe(state, FACING);
		BlockEntity source = getBlockEntity(world, pos.offset(leftDirection(facing)));
		if (source == null) {
			source = getBlockEntity(world, pos.offset(rightDirection(facing)));
		}
		final BlockEntity entity = getBlockEntity(world, pos);
		if (entity != null && source != null) {
			entity.copySelectedIds(source.getSelectedIds());
		}
	}

	// 注：来源用 MTR 4 的 PacketOpenBlockEntityScreen 打开自由布局编辑器；目标侧该数据包由 Lead 的 packet 层统一接入，
	// 在通道就绪前这里不接管右键（保持原版 PASS），渲染/数据部分不受影响。
	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		final Direction facing = IBlock.getStatePropertySafe(state, FACING);
		return IBlock.getVoxelShapeByDirection(0, 3, 0, 16, 13, 2, facing);
	}

	@Override
	public void appendTooltip(ItemStack stack, @Nullable BlockView world, List<Text> tooltip, TooltipContext options) {
		tooltip.add(TranslationProvider.BRUSH_USE.getMutableText().copy().formatted(Formatting.DARK_GRAY));
	}

	public static class BlockEntity extends BlockEntityClientSerializableMapper {

		// 旧存档只有一格，读取时并入第 0 格。
		private static final String KEY_SIGN_ID = "wall_sign_id";
		private static final String KEY_SIGN_ID_PREFIX = "wall_sign_id_";
		private static final String KEY_SELECTED_IDS = "selected_ids";
		private static final String KEY_ROUTE_COLORS = "wall_route_colors";
		private static final String KEY_ROUTE_NUMBERS = "wall_route_numbers";
		/** 网页编辑器式的自由布局（JSON，存在整条牌的第一个方块实体上）；空串 = 继续用旧格子数据。 */
		private static final String KEY_SIGN_LAYOUT = "crt_sign_layout";

		private final String[] signIds = new String[CELLS_PER_BLOCK];
		private final LongAVLTreeSet selectedIds = new LongAVLTreeSet();
		// 颜色 -> 线路编号；MTR 客户端数据只有 Route，只能服务端解析后随 NBT 同步。
		private final Long2ObjectOpenHashMap<String> routeNumbers = new Long2ObjectOpenHashMap<>();
		/** 自由布局 JSON。 */
		private String signLayout = "";

		public BlockEntity(BlockPos pos, BlockState state) {
			this(BlockEntityTypes.CRT_RAILWAY_SIGN_WALL_1.get(), pos, state);
		}

		// 供墙挂牌2继承：用同一个实体实现，但注册到自己的方块实体类型。
		protected BlockEntity(BlockEntityType<?> blockEntityType, BlockPos pos, BlockState state) {
			super(blockEntityType, pos, state);
		}

		// 本方块第 cell 格（0 = 左半，1 = 右半）的内容 id。
		public String getSignId(int cell) {
			return cell >= 0 && cell < CELLS_PER_BLOCK ? signIds[cell] : null;
		}

		public LongAVLTreeSet getSelectedIds() {
			return selectedIds;
		}

		/** 自由布局 JSON；空 = 未使用新编辑器。 */
		public String getSignLayout() {
			return signLayout == null ? "" : signLayout;
		}

		/** 服务端写入自由布局并同步给客户端。 */
		public void setSignLayout(String layoutJson) {
			signLayout = layoutJson == null ? "" : layoutJson;
			updateRouteNumbersFromServer();
			markDirty();
			syncData();
		}

		public void copySelectedIds(LongAVLTreeSet ids) {
			selectedIds.clear();
			selectedIds.addAll(ids);
			markDirty();
			syncData();
		}

		// 服务端写入本方块的两格内容；selectedIds 为整块牌共用的数据选择。
		public void setData(String[] newSignIds, LongAVLTreeSet newSelectedIds) {
			for (int i = 0; i < CELLS_PER_BLOCK; i++) {
				final String signId = newSignIds != null && i < newSignIds.length ? newSignIds[i] : null;
				signIds[i] = signId == null || signId.isEmpty() ? null : signId;
			}
			if (newSelectedIds != null) {
				selectedIds.clear();
				selectedIds.addAll(newSelectedIds);
			}
			updateRouteNumbersFromServer();
			markDirty();
			syncData();
		}

		@Override
		public void readCompoundTag(NbtCompound compoundTag) {
			super.readCompoundTag(compoundTag);
			signLayout = compoundTag.getString(KEY_SIGN_LAYOUT);
			for (int i = 0; i < CELLS_PER_BLOCK; i++) {
				String signId = compoundTag.getString(KEY_SIGN_ID_PREFIX + i);
				if (signId.isEmpty() && i == 0) {
					signId = compoundTag.getString(KEY_SIGN_ID);
				}
				signIds[i] = signId.isEmpty() ? null : signId;
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
		public void writeCompoundTag(NbtCompound compoundTag) {
			super.writeCompoundTag(compoundTag);
			compoundTag.putString(KEY_SIGN_LAYOUT, signLayout == null ? "" : signLayout);
			for (int i = 0; i < CELLS_PER_BLOCK; i++) {
				compoundTag.putString(KEY_SIGN_ID_PREFIX + i, signIds[i] == null ? "" : signIds[i]);
			}
			compoundTag.putString(KEY_SIGN_ID, signIds[0] == null ? "" : signIds[0]);
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

		// 解析所选颜色对应的线路编号（MTR 3 客户端数据 ClientData 已含线路编号，与 BlockCRTRailwaySign 的做法一致）。
		private void updateRouteNumbersFromServer() {
			routeNumbers.clear();
			if (getWorld() == null || getWorld().isClient || selectedIds.isEmpty()) {
				markDirty();
				return;
			}
			try {
				final Station station = findStationAt(getPos());
				if (station != null) {
					for (final Platform platform : ClientData.PLATFORMS) {
						if (ClientData.DATA_CACHE.platformIdToStation.get(platform.id) != station) {
							continue;
						}
						for (final Route route : ClientData.ROUTES) {
							if (route.getPlatformIdIndex(platform.id) >= 0) {
								final long color = route.color;
								if (selectedIds.contains(color) && !routeNumbers.containsKey(color)) {
									final String number = route.lightRailRouteNumber;
									if (number != null && !number.isEmpty()) {
										routeNumbers.put(color, number);
									}
								}
							}
						}
					}
				}
			} catch (Exception e) {
				Init.LOGGER.error("BlockCRTRailwaySignWall: Failed to resolve route numbers at {}", getPos().toShortString(), e);
			}
			markDirty();
		}

		/** 按方块坐标就近查找所属车站（客户端数据缓存）。 */
		private static Station findStationAt(BlockPos pos) {
			final Station direct = ClientData.DATA_CACHE.blockPosToStation.get(pos);
			if (direct != null) {
				return direct;
			}
			for (final Station station : ClientData.STATIONS) {
				if (station.inArea(pos.getX(), pos.getZ())) {
					return station;
				}
			}
			return null;
		}
	}
}
