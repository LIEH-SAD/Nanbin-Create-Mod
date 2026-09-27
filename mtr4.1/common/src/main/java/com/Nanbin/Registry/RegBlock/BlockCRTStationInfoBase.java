package com.Nanbin.Registry.RegBlock;

import com.Nanbin.Init;
import com.Nanbin.entity.BlockEntityTypes;
import com.Nanbin.mapping.BlockEntityHelper;
import com.Nanbin.mapping.BlockExtension;
import com.Nanbin.mapping.IBlockExtension;
import com.Nanbin.mapping.MtrServerData;
import com.Nanbin.mapping.ObjectHolder;
import com.Nanbin.mapping.Registry;
import com.Nanbin.mapping.TranslationProvider;
import com.Nanbin.packet.PacketOpenStationInfoScreen;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.mtr.block.IBlock;
import org.mtr.core.data.Platform;
import org.mtr.core.data.Route;
import org.mtr.core.data.RoutePlatformData;
import org.mtr.core.data.Station;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongAVLTreeSet;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * CRT 站台信息屏的公共基类。
 * <p>
 * 源项目里 {@code BlockCRTStationInfo1} 与 {@code BlockCRTStationInfo1Double} 是两份几乎逐行相同的代码，
 * 区别只是「双面版」多了一整套 {@code *Back}（背面）字段与 {@code front} 参数。
 * 这里合并成一个基类：按「正/反面」统一建模，单面版永远传 {@code front = true}。
 * 两块的尺寸、放置与拆除逻辑完全相同（3 宽 × 3 高），所以只留一个 {@code placeStructure}。
 * <p>
 * 4.1 适配要点：
 * <ul>
 *     <li>{@code BlockWithEntity} → 原版 {@link BlockEntityProvider}，{@code createBlockEntity} 返回原版 {@link BlockEntity}。</li>
 *     <li>{@code world.getBlockEntity(pos)} 直接返回原版方块实体，不再有 4.0 的 {@code .data} 取值层。</li>
 *     <li>{@code readCompoundTag}/{@code writeCompoundTag} → MTR 的 {@code readNbt}/{@code writeNbt}（单参 NbtCompound）。</li>
 *     <li>{@code getPos2/getWorld2/getCachedState2} → 原版的 {@code getPos/getWorld/getCachedState}；
 *     {@code markDirty2} → {@link BlockEntityHelper#markDirty}（4.1 的 {@code markDirty()} 不再同步客户端）。</li>
 * </ul>
 */
public abstract class BlockCRTStationInfoBase extends BlockExtension implements BlockEntityProvider {

	/** 一块屏共 3×3 = 9 格，连通扩散时以此为上限，避免扫到相邻的屏。 */
	private static final int SCREEN_CELLS = 9;

	public BlockCRTStationInfoBase(net.minecraft.block.AbstractBlock.Settings settings) {
		super(settings);
	}

	/** 子类给出自己的方块实体类型。 */
	protected abstract ObjectHolder<BlockEntityType<BlockEntityBase>> getBlockEntityType();

	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new BlockEntityBase(getBlockEntityType().get(), pos, state);
	}

	@Override
	public void addBlockProperties(List<Property<?>> properties) {
		super.addBlockProperties(properties);
		properties.add(Properties.HORIZONTAL_FACING);
		properties.add(IBlock.SIDE_EXTENDED);
		properties.add(IBlock.HALF);
	}

	@Override
	public BlockState getPlacementState2(ItemPlacementContext ctx) {
		final Direction direction = ctx.getHorizontalPlayerFacing();
		final BlockState baseState = getDefaultState2()
				.with(Properties.HORIZONTAL_FACING, direction)
				.with(IBlock.HALF, IBlock.DoubleBlockHalf.LOWER);
		final Direction right = direction.rotateYClockwise();
		final BlockPos startPos = ctx.getBlockPos();
		if (canPlace(ctx, startPos, right)) {
			placeStructure(ctx.getWorld(), startPos, direction, baseState);
			return baseState.with(IBlock.SIDE_EXTENDED, IBlock.EnumSide.LEFT);
		}
		return null;
	}

	private boolean canPlace(ItemPlacementContext ctx, BlockPos startPos, Direction right) {
		final World world = ctx.getWorld();
		for (int y = 0; y < 3; y++) {
			for (int x = 0; x < 3; x++) {
				if (x == 0 && y == 0) {
					continue;
				}
				final BlockState checkState = world.getBlockState(startPos.up(y).offset(right, x));
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
				if (x == 0 && y == 0) {
					continue;
				}
				final IBlock.EnumSide side = x == 0 ? IBlock.EnumSide.LEFT : (x == 1 ? IBlock.EnumSide.MIDDLE : IBlock.EnumSide.RIGHT);
				world.setBlockState(startPos.up(y).offset(right, x), baseState
						.with(IBlock.SIDE_EXTENDED, side)
						.with(IBlock.HALF, half), 3);
			}
		}
		world.updateNeighbors(startPos, net.minecraft.block.Blocks.AIR);
	}

	@Override
	public void onBreak2(World world, BlockPos pos, BlockState state, PlayerEntity player) {
		final Direction right = IBlock.getStatePropertySafe(state, Properties.HORIZONTAL_FACING).rotateYClockwise();
		final BlockPos bottomLeftPos = findBottomLeftPosition(world, pos, right);
		for (int y = 0; y < 3; y++) {
			for (int x = 0; x < 3; x++) {
				final BlockPos breakPos = bottomLeftPos.up(y).offset(right, x);
				if (!breakPos.equals(pos)) {
					IBlockExtension.breakBlock(world, breakPos);
				}
			}
		}
		super.onBreak2(world, pos, state, player);
	}

	/**
	 * 找整块屏的「左下角」：从给定格出发，在<b>同一平面</b>内按同类方块连通扩散，
	 * 收集这块屏的所有格（一块屏 9 格），再取「最下面、且最靠 facing 逆时针方向」那一格。
	 * <p>
	 * 4.0 的做法是「按 {@code side}/{@code half} 回推一列，再竖直向下走到底」，
	 * 依赖方块状态完全正确、而且不能有上下堆叠的屏。实测发现这样有时会少走一层，
	 * 于是 {@code onBreak2} 清掉的是偏上一层的 3×3（大部分是空气），真正的屏留在原地。
	 * 改成连通扩散后不依赖 any 状态推断，也不会多清：上限 9 格，刚好一块屏。
	 */
	private static BlockPos findBottomLeftPosition(World world, BlockPos pos, Direction right) {
		final List<BlockPos> cells = new ArrayList<>();
		final Set<BlockPos> visited = new HashSet<>();
		final Deque<BlockPos> queue = new ArrayDeque<>();
		visited.add(pos);
		queue.add(pos);
		cells.add(pos);
		while (!queue.isEmpty() && cells.size() < SCREEN_CELLS) {
			final BlockPos current = queue.poll();
			for (final Direction direction : new Direction[]{right, right.getOpposite(), Direction.UP, Direction.DOWN}) {
				final BlockPos next = current.offset(direction);
				if (!visited.add(next) || !(world.getBlockState(next).getBlock() instanceof BlockCRTStationInfoBase)) {
					continue;
				}
				cells.add(next);
				queue.add(next);
				if (cells.size() >= SCREEN_CELLS) {
					break;
				}
			}
		}

		BlockPos bottomLeft = cells.get(0);
		int bottomLeftStep = Integer.MAX_VALUE;
		for (final BlockPos cell : cells) {
			final int step = cell.getX() * right.getOffsetX() + cell.getZ() * right.getOffsetZ();
			if (cell.getY() < bottomLeft.getY() || (cell.getY() == bottomLeft.getY() && step < bottomLeftStep)) {
				bottomLeft = cell;
				bottomLeftStep = step;
			}
		}
		return bottomLeft;
	}

	@Override
	public ActionResult onUse2(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		return IBlockExtension.checkHoldingBrush(world, player, () -> {
			final Direction facing = IBlock.getStatePropertySafe(state, Properties.HORIZONTAL_FACING);
			final Direction hitSide = hit.getSide();
			if (!acceptsHitSide(hitSide, facing)) {
				return;
			}
			final boolean isFront = hitSide.getOpposite() == facing;
			final BlockEntity blockEntity = world.getBlockEntity(pos);
			if (!(blockEntity instanceof BlockEntityBase entity)) {
				return;
			}
			if (IBlock.getStatePropertySafe(state, IBlock.HALF) == IBlock.DoubleBlockHalf.UPPER) {
				entity.toggleFlip(isFront);
			} else {
				Registry.sendPacketToClient((ServerPlayerEntity) player, new PacketOpenStationInfoScreen(pos, entity.getUrl(isFront), isFront));
			}
		});
	}

	/**
	 * 允许被刷子点击的朝向。
	 * <p>
	 * 单面版只认背面（4.0 的 {@code hitSide != facing.getOpposite()} 就直接返回）；
	 * 双面版正反两面都能点。
	 */
	protected abstract boolean acceptsHitSide(Direction hitSide, Direction facing);

	@Override
	public VoxelShape getOutlineShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		final Direction direction = IBlock.getStatePropertySafe(state, Properties.HORIZONTAL_FACING);
		final IBlock.EnumSide side = IBlock.getStatePropertySafe(state, IBlock.SIDE_EXTENDED);
		final boolean upper = IBlock.getStatePropertySafe(state, IBlock.HALF) == IBlock.DoubleBlockHalf.UPPER;
		final double y2 = upper ? 8 : 16;
		if (side == IBlock.EnumSide.LEFT) {
			return IBlock.getVoxelShapeByDirection(1, 0, 6, 16, y2, 10, direction);
		} else if (side == IBlock.EnumSide.MIDDLE) {
			return IBlock.getVoxelShapeByDirection(0, 0, 6, 16, y2, 10, direction);
		} else {
			return IBlock.getVoxelShapeByDirection(0, 0, 6, 15, y2, 10, direction);
		}
	}

	@Override
	public void addTooltips(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(TranslationProvider.BRUSH_USE.getMutableText().formatted(Formatting.DARK_GRAY));
	}

	/**
	 * 站台信息屏方块实体。
	 * <p>
	 * 每块屏保存 2 行 × 7 格的告示牌数据 + 顶部条带方向 + 一个可选的 url（自定义内容），
	 * 并且正反面各存一份（单面版只用正面）。
	 */
	public static class BlockEntityBase extends org.mtr.block.BlockEntityExtension {

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
		/** 顶部条带方向：false = 灰色矩形在左、线路色组合图在右；true = 镜像 */
		private boolean flip = false;
		private boolean flipBack = false;
		private final String[][] signIds = new String[SIGN_LINES][SIGN_LENGTH];
		private final String[][] signIdsBack = new String[SIGN_LINES][SIGN_LENGTH];
		private final List<LongAVLTreeSet> selectedIds = new ArrayList<>();
		private final List<LongAVLTreeSet> selectedIdsBack = new ArrayList<>();
		/** 站台 ID → 线路编号。MTR 客户端数据只有 SimplifiedRoute（无 routeNumber），只能服务端解析后写入 NBT。 */
		private final Long2ObjectOpenHashMap<String> routeNumbers = new Long2ObjectOpenHashMap<>();
		private final Long2ObjectOpenHashMap<String> routeNumbersBack = new Long2ObjectOpenHashMap<>();

		public BlockEntityBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {
			super(type, pos, state);
			selectedIds.add(new LongAVLTreeSet());
			selectedIds.add(new LongAVLTreeSet());
			selectedIdsBack.add(new LongAVLTreeSet());
			selectedIdsBack.add(new LongAVLTreeSet());
		}

		public String getUrl(boolean front) {
			return front ? url : urlBack;
		}

		public void setUrl(boolean front, String newUrl) {
			final String value = newUrl == null ? "" : newUrl;
			if (front) {
				url = value;
			} else {
				urlBack = value;
			}
			syncDataToOtherBlocks(front);
			BlockEntityHelper.markDirty(this);
		}

		public String[][] getSignIds(boolean front) {
			return front ? signIds : signIdsBack;
		}

		public List<LongAVLTreeSet> getSelectedIds(boolean front) {
			return front ? selectedIds : selectedIdsBack;
		}

		/** 按所有选中站台的顺序返回线路编号；无线路编号的站台会被跳过。 */
		public String[] getRouteNumbers(boolean front) {
			final List<LongAVLTreeSet> targetIds = front ? selectedIds : selectedIdsBack;
			final Long2ObjectOpenHashMap<String> targetNumbers = front ? routeNumbers : routeNumbersBack;
			final List<String> result = new ArrayList<>();
			for (final LongAVLTreeSet lineSelected : targetIds) {
				for (final long platformId : lineSelected) {
					final String number = targetNumbers.get(platformId);
					if (number != null && !number.isEmpty()) {
						result.add(number);
					}
				}
			}
			return result.toArray(new String[0]);
		}

		public String getRouteNumber(long platformId, boolean front) {
			final String number = (front ? routeNumbers : routeNumbersBack).get(platformId);
			return number == null ? "" : number;
		}

		public boolean isFlip(boolean front) {
			return front ? flip : flipBack;
		}

		public void toggleFlip(boolean front) {
			setFlipValue(front, !isFlip(front));
			syncFlipToOtherBlocks(front);
			BlockEntityHelper.markDirty(this);
		}

		public void setFlip(boolean front, boolean newFlip) {
			setFlipValue(front, newFlip);
			syncFlipToOtherBlocks(front);
			BlockEntityHelper.markDirty(this);
		}

		private void setFlipValue(boolean front, boolean value) {
			if (front) {
				flip = value;
			} else {
				flipBack = value;
			}
		}

		private void syncFlipToOtherBlocks(boolean front) {
			forEachOtherBlock(front, target -> {
				if (front) {
					target.flip = flip;
				} else {
					target.flipBack = flipBack;
				}
			});
		}

		/** 更新指定行的站台选择（行 0 由 StationInfoScreen 的「选择站台」写入），并同步到其他方块。 */
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
			forEachOtherBlock(front, target -> {
				if (front) {
					target.url = url;
				} else {
					target.urlBack = urlBack;
				}
			});
		}

		private void syncSignDataToOtherBlocks(boolean front) {
			forEachOtherBlock(front, target -> {
				target.copySignDataOnly(front, front ? signIds : signIdsBack, front ? selectedIds : selectedIdsBack);
				target.updateRouteNumbersFromServer(front);
			});
		}

		/** 遍历同一块屏的其余 8 个方块实体，执行同一份修改。 */
		private void forEachOtherBlock(boolean front, java.util.function.Consumer<BlockEntityBase> action) {
			final World world = getWorld();
			if (world == null) {
				return;
			}
			final Direction facing = IBlock.getStatePropertySafe(getCachedState(), Properties.HORIZONTAL_FACING);
			final Direction right = facing.rotateYClockwise();
			final BlockPos bottomLeftPos = findBottomLeftPosition(world, getPos(), right);
			if (bottomLeftPos == null) {
				return;
			}
			for (int y = 0; y < 3; y++) {
				for (int x = 0; x < 3; x++) {
					final BlockPos otherPos = bottomLeftPos.up(y).offset(right, x);
					if (!otherPos.equals(getPos()) && world.getBlockEntity(otherPos) instanceof BlockEntityBase entity) {
						action.accept(entity);
						BlockEntityHelper.markDirty(entity);
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
					if (src == null) {
						continue;
					}
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
			BlockEntityHelper.markDirty(this);
		}

		/**
		 * 服务端解析线路编号：站台 id → {@code Route#getRouteNumber()}。
		 * <p>
		 * 必须放在服务端做：MTR 客户端只有 {@code SimplifiedRoute}，没有 {@code routeNumber} 字段
		 * （见 {@link MtrServerData} 的说明），轻铁这类「名字与编号分开填」的线路在客户端取不到编号。
		 * 4.0 就是这么做的，4.1 移植时被写成了空实现导致编号丢失。
		 */
		private void updateRouteNumbersFromServer(boolean front) {
			final Long2ObjectOpenHashMap<String> targetNumbers = front ? routeNumbers : routeNumbersBack;
			targetNumbers.clear();
			if (getWorld() != null && !getWorld().isClient()) {
				final LongAVLTreeSet allPlatformIds = new LongAVLTreeSet();
				for (final LongAVLTreeSet lineSelected : (front ? selectedIds : selectedIdsBack)) {
					allPlatformIds.addAll(lineSelected);
				}
				final Station station = allPlatformIds.isEmpty() ? null : MtrServerData.findStation(getPos());
				if (station != null) {
					for (final Platform platform : station.savedRails) {
						for (final Route route : platform.routes) {
							for (final RoutePlatformData routePlatform : route.getRoutePlatforms()) {
								// 检查该线路是否经过所选站台
								final Platform routePlatformEntry = routePlatform.getPlatform();
								final long routePlatformId = routePlatformEntry == null ? -1 : routePlatformEntry.getId();
								if (routePlatformId >= 0 && allPlatformIds.contains(routePlatformId) && !targetNumbers.containsKey(routePlatformId)) {
									final String number = route.getRouteNumber();
									if (number != null && !number.isEmpty()) {
										targetNumbers.put(routePlatformId, number);
									}
								}
							}
						}
					}
				}
			}
			BlockEntityHelper.markDirty(this);
		}

		@Override
		protected void readNbt(NbtCompound nbtCompound) {
			super.readNbt(nbtCompound);
			url = nbtCompound.getString(KEY_URL);
			urlBack = nbtCompound.getString(KEY_URL_BACK);
			flip = nbtCompound.getBoolean(KEY_FLIP);
			flipBack = nbtCompound.getBoolean(KEY_FLIP_BACK);
			readSideData(nbtCompound, KEY_SIGN_IDS, KEY_SELECTED_IDS, KEY_ROUTE_COLORS, KEY_ROUTE_NUMBERS, signIds, selectedIds, routeNumbers);
			readSideData(nbtCompound, KEY_SIGN_IDS_BACK, KEY_SELECTED_IDS_BACK, KEY_ROUTE_COLORS_BACK, KEY_ROUTE_NUMBERS_BACK, signIdsBack, selectedIdsBack, routeNumbersBack);
		}

		@Override
		protected void writeNbt(NbtCompound nbtCompound) {
			super.writeNbt(nbtCompound);
			nbtCompound.putString(KEY_URL, url);
			nbtCompound.putString(KEY_URL_BACK, urlBack);
			nbtCompound.putBoolean(KEY_FLIP, flip);
			nbtCompound.putBoolean(KEY_FLIP_BACK, flipBack);
			writeSideData(nbtCompound, KEY_SIGN_IDS, KEY_SELECTED_IDS, KEY_ROUTE_COLORS, KEY_ROUTE_NUMBERS, signIds, selectedIds, routeNumbers);
			writeSideData(nbtCompound, KEY_SIGN_IDS_BACK, KEY_SELECTED_IDS_BACK, KEY_ROUTE_COLORS_BACK, KEY_ROUTE_NUMBERS_BACK, signIdsBack, selectedIdsBack, routeNumbersBack);
		}

		private static void readSideData(NbtCompound nbtCompound, String signIdsKey, String selectedIdsKey, String routeColorsKey, String routeNumbersKey, String[][] targetSignIds, List<LongAVLTreeSet> targetSelectedIds, Long2ObjectOpenHashMap<String> targetRouteNumbers) {
			for (int i = 0; i < targetSignIds.length; i++) {
				final LongAVLTreeSet set = targetSelectedIds.get(i);
				set.clear();
				for (final long id : nbtCompound.getLongArray(selectedIdsKey + "_" + i)) {
					set.add(id);
				}
				for (int j = 0; j < targetSignIds[i].length; j++) {
					final String signId = nbtCompound.getString(signIdsKey + "_" + i + "_" + j);
					targetSignIds[i][j] = signId.isEmpty() ? null : signId;
				}
			}
			targetRouteNumbers.clear();
			final long[] colors = nbtCompound.getLongArray(routeColorsKey);
			final String joined = nbtCompound.getString(routeNumbersKey);
			if (!joined.isEmpty()) {
				final String[] numbers = joined.split("\u0001", -1);
				for (int i = 0; i < colors.length && i < numbers.length; i++) {
					targetRouteNumbers.put(colors[i], numbers[i]);
				}
			}
		}

		private static void writeSideData(NbtCompound nbtCompound, String signIdsKey, String selectedIdsKey, String routeColorsKey, String routeNumbersKey, String[][] targetSignIds, List<LongAVLTreeSet> targetSelectedIds, Long2ObjectOpenHashMap<String> targetRouteNumbers) {
			for (int i = 0; i < targetSignIds.length; i++) {
				nbtCompound.putLongArray(selectedIdsKey + "_" + i, new ArrayList<>(targetSelectedIds.get(i)));
				for (int j = 0; j < targetSignIds[i].length; j++) {
					nbtCompound.putString(signIdsKey + "_" + i + "_" + j, targetSignIds[i][j] == null ? "" : targetSignIds[i][j]);
				}
			}
			final long[] colors = targetRouteNumbers.keySet().toLongArray();
			final StringBuilder stringBuilder = new StringBuilder();
			for (int i = 0; i < colors.length; i++) {
				if (i > 0) {
					stringBuilder.append('\u0001');
				}
				stringBuilder.append(targetRouteNumbers.get(colors[i]));
			}
			nbtCompound.putLongArray(routeColorsKey, colors);
			nbtCompound.putString(routeNumbersKey, stringBuilder.toString());
		}
	}

	/** 单面屏的方块实体。 */
	public static class BlockEntitySingle extends BlockEntityBase {

		public BlockEntitySingle(BlockPos pos, BlockState state) {
			super(BlockEntityTypes.CRT_STATION_INFO_1.get(), pos, state);
		}
	}

	/** 双面屏的方块实体。 */
	public static class BlockEntityDouble extends BlockEntityBase {

		public BlockEntityDouble(BlockPos pos, BlockState state) {
			super(BlockEntityTypes.CRT_STATION_INFO_1_DOUBLE.get(), pos, state);
		}
	}
}
