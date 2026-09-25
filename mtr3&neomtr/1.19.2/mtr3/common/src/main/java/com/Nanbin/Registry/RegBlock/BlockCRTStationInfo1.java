package com.Nanbin.Registry.RegBlock;

import com.Nanbin.Init;
import com.Nanbin.entity.BlockEntityTypes;
import com.Nanbin.mapping.Registry;
import com.Nanbin.mapping.TranslationProvider;
import com.Nanbin.packet.PacketHandler;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import mtr.Items;
import mtr.block.IBlock;
import mtr.client.ClientData;
import mtr.data.Platform;
import mtr.data.Route;
import mtr.mappings.BlockDirectionalMapper;
import mtr.mappings.BlockEntityClientSerializableMapper;
import mtr.mappings.BlockEntityMapper;
import mtr.mappings.EntityBlockMapper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static mtr.block.IBlock.HALF;
import static mtr.block.IBlock.SIDE_EXTENDED;

/**
 * CRT 站台信息屏（单面）：3 格宽 × 3 格高的多块结构（下部 2 格高本体 + 顶部条带）。
 * 放置时一次铺开整块结构，破坏任意一块会连带清除整组；刷子点击顶部切换条带镜像方向，
 * 点击下半部打开配置界面（URL + 选择站台）。渲染由
 * {@link com.Nanbin.client.Render.RenderCRTStationInfo1} 完成。
 */
public class BlockCRTStationInfo1 extends BlockDirectionalMapper implements EntityBlockMapper {

	public BlockCRTStationInfo1(Settings settings) {
		super(settings);
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING, SIDE_EXTENDED, HALF);
	}

	@Override
	public BlockEntityMapper createBlockEntity(BlockPos pos, BlockState state) {
		return new BlockEntity(pos, state);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		final Direction direction = ctx.getPlayerFacing();
		final BlockState baseState = getDefaultState().with(FACING, direction).with(HALF, DoubleBlockHalf.LOWER);
		final Direction right = direction.rotateYClockwise();
		final BlockPos startPos = ctx.getBlockPos();
		if (canPlace(ctx, startPos, right)) {
			placeStructure(ctx.getWorld(), startPos, direction, baseState);
			return baseState.with(SIDE_EXTENDED, IBlock.EnumSide.LEFT);
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
			final DoubleBlockHalf half = y < 2 ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER;
			for (int x = 0; x < 3; x++) {
				if (x == 0 && y == 0) {
					continue;
				}
				final BlockPos placePos = startPos.up(y).offset(right, x);
				final IBlock.EnumSide side = x == 0 ? IBlock.EnumSide.LEFT : (x == 1 ? IBlock.EnumSide.MIDDLE : IBlock.EnumSide.RIGHT);
				world.setBlockState(placePos, baseState.with(SIDE_EXTENDED, side).with(HALF, half), 3);
			}
		}
		world.updateNeighbors(startPos, Blocks.AIR);
	}

	@Override
	public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
		final Direction direction = IBlock.getStatePropertySafe(state, FACING);
		final Direction right = direction.rotateYClockwise();
		final IBlock.EnumSide side = IBlock.getStatePropertySafe(state, SIDE_EXTENDED);
		final DoubleBlockHalf half = IBlock.getStatePropertySafe(state, HALF);
		final BlockPos bottomLeftPos = findBottomLeftPosition(world, pos, direction, side, half);
		if (bottomLeftPos != null) {
			for (int y = 0; y < 3; y++) {
				for (int x = 0; x < 3; x++) {
					final BlockPos breakPos = bottomLeftPos.up(y).offset(right, x);
					if (!breakPos.equals(pos) && world.getBlockState(breakPos).getBlock() instanceof BlockCRTStationInfo1) {
						world.setBlockState(breakPos, Blocks.AIR.getDefaultState(), 3);
					}
				}
			}
		}
		super.onBreak(world, pos, state, player);
	}

	/** 沿竖直方向回溯到结构最下层的对应方块位置。 */
	private static BlockPos findBottomLeftPosition(World world, BlockPos pos, Direction facing, IBlock.EnumSide side, DoubleBlockHalf half) {
		final Direction left = facing.rotateYCounterclockwise();
		BlockPos basePos = pos;
		if (side == IBlock.EnumSide.MIDDLE) {
			basePos = pos.offset(left, 1);
		} else if (side == IBlock.EnumSide.RIGHT) {
			basePos = pos.offset(left, 2);
		}
		while (basePos.getY() > 0) {
			final BlockPos belowPos = basePos.down();
			if (!(world.getBlockState(belowPos).getBlock() instanceof BlockCRTStationInfo1)) {
				return basePos;
			}
			basePos = belowPos;
		}
		return basePos;
	}

	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		if (!player.isHolding(Items.BRUSH.get())) {
			return ActionResult.PASS;
		}
		if (world.isClient) {
			return ActionResult.SUCCESS;
		}
		final Direction facing = IBlock.getStatePropertySafe(state, FACING);
		if (hit.getSide() != facing.getOpposite()) {
			return ActionResult.PASS;
		}
		if (!(world.getBlockEntity(pos) instanceof BlockEntity entity)) {
			return ActionResult.PASS;
		}
		if (IBlock.getStatePropertySafe(state, HALF) == DoubleBlockHalf.UPPER) {
			entity.toggleFlip();
		} else {
			Registry.sendPacketToClient((ServerPlayerEntity) player, PacketHandler.PACKET_OPEN_STATION_INFO, buf -> {
				buf.writeBlockPos(pos);
				buf.writeString(entity.getUrl());
				buf.writeBoolean(true);
			});
		}
		return ActionResult.SUCCESS;
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		final Direction direction = IBlock.getStatePropertySafe(state, FACING);
		final IBlock.EnumSide side = IBlock.getStatePropertySafe(state, SIDE_EXTENDED);
		final boolean upper = IBlock.getStatePropertySafe(state, HALF) == DoubleBlockHalf.UPPER;
		final double top = upper ? 8 : 16;
		if (side == IBlock.EnumSide.LEFT) {
			return IBlock.getVoxelShapeByDirection(1, 0, 6, 16, top, 10, direction);
		} else if (side == IBlock.EnumSide.MIDDLE) {
			return IBlock.getVoxelShapeByDirection(0, 0, 6, 16, top, 10, direction);
		} else {
			return IBlock.getVoxelShapeByDirection(0, 0, 6, 15, top, 10, direction);
		}
	}

	@Override
	public void appendTooltip(ItemStack stack, @Nullable BlockView world, List<Text> tooltip, TooltipContext options) {
		tooltip.add(TranslationProvider.BRUSH_USE.getText().copy().formatted(Formatting.DARK_GRAY));
	}

	/**
	 * 方块实体：保存 URL、条带镜像方向、2 行 × 7 格指示牌数据与所选站台，
	 * 并在结构内 9 个方块之间互相同步。
	 */
	public static class BlockEntity extends BlockEntityClientSerializableMapper {

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
				for (final long platformId : lineSelected) {
					final String number = routeNumbers.get(platformId);
					if (number != null && !number.isEmpty()) {
						result.add(number);
					}
				}
			}
			return result.toArray(new String[0]);
		}

		/** 指定站台（平台 ID）的线路编号；无则返回空串。 */
		public String getRouteNumber(long platformId) {
			final String number = routeNumbers.get(platformId);
			return number == null ? "" : number;
		}

		public boolean isFlip() {
			return flip;
		}

		/** 切换顶部条带方向，并同步到其他 8 个方块。 */
		public void toggleFlip() {
			this.flip = !this.flip;
			syncFlipToOtherBlocks();
			markDirty();
			syncData();
		}

		public void setFlip(boolean newFlip) {
			this.flip = newFlip;
			syncFlipToOtherBlocks();
			markDirty();
			syncData();
		}

		private void syncFlipToOtherBlocks() {
			final BlockPos bottomLeftPos = findBottomLeftPosition();
			if (bottomLeftPos == null) {
				return;
			}
			forEachStructurePos(bottomLeftPos, otherPos -> {
				if (getWorld().getBlockEntity(otherPos) instanceof BlockEntity entity) {
					entity.flip = this.flip;
					entity.markDirty();
					entity.syncData();
				}
			});
		}

		public void setUrl(String newUrl) {
			this.url = newUrl;
			syncDataToOtherBlocks();
			markDirty();
			syncData();
		}

		/** 更新指定行的站台选择（如行 0 由 StationInfoScreen 的「选择站台」写入），并同步到其他方块。 */
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
			updateRouteNumbers();
			markDirty();
			syncData();
		}

		public void setSignData(String[][] newSignIds, List<LongAVLTreeSet> newSelectedIds) {
			copySignDataOnly(newSignIds, newSelectedIds);
			syncSignDataToOtherBlocks();
			updateRouteNumbers();
			markDirty();
			syncData();
		}

		private void syncDataToOtherBlocks() {
			final BlockPos bottomLeftPos = findBottomLeftPosition();
			if (bottomLeftPos == null) {
				return;
			}
			forEachStructurePos(bottomLeftPos, otherPos -> {
				if (getWorld().getBlockEntity(otherPos) instanceof BlockEntity entity) {
					entity.url = this.url;
					entity.markDirty();
					entity.syncData();
				}
			});
		}

		private void syncSignDataToOtherBlocks() {
			final BlockPos bottomLeftPos = findBottomLeftPosition();
			if (bottomLeftPos == null) {
				return;
			}
			forEachStructurePos(bottomLeftPos, otherPos -> {
				if (getWorld().getBlockEntity(otherPos) instanceof BlockEntity entity) {
					entity.copySignDataOnly(this.signIds, this.selectedIds);
					entity.updateRouteNumbers();
					// 必须把新数据推送给客户端，否则负责渲染的方块仍显示旧内容
					entity.markDirty();
					entity.syncData();
				}
			});
		}

		private void forEachStructurePos(BlockPos bottomLeftPos, java.util.function.Consumer<BlockPos> action) {
			final Direction facing = IBlock.getStatePropertySafe(getCachedState(), FACING);
			final Direction right = facing.rotateYClockwise();
			for (int y = 0; y < 3; y++) {
				for (int x = 0; x < 3; x++) {
					final BlockPos otherPos = bottomLeftPos.up(y).offset(right, x);
					if (!otherPos.equals(getPos())) {
						action.accept(otherPos);
					}
				}
			}
		}

		private void copySignDataOnly(String[][] newSignIds, List<LongAVLTreeSet> newSelectedIds) {
			if (newSignIds != null) {
				for (int i = 0; i < signIds.length && i < newSignIds.length; i++) {
					final String[] src = newSignIds[i];
					if (src == null) {
						continue;
					}
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
			markDirty();
		}

		/**
		 * 服务端解析所选站台的线路编号并写入 NBT 供渲染端读取。
		 * MTR 客户端数据没有 routeNumber 字段，因此由 lightRailRouteNumber 或线路名推导。
		 */
		private void updateRouteNumbers() {
			routeNumbers.clear();
			if (getWorld() == null || getWorld().isClient) {
				return;
			}
			final LongAVLTreeSet allPlatformIds = new LongAVLTreeSet();
			for (final LongAVLTreeSet lineSelected : selectedIds) {
				allPlatformIds.addAll(lineSelected);
			}
			if (!allPlatformIds.isEmpty()) {
				try {
					for (final long platformId : allPlatformIds) {
						final String number = resolveRouteNumber(platformId);
						if (!number.isEmpty()) {
							routeNumbers.put(platformId, number);
						}
					}
				} catch (Exception e) {
					Init.LOGGER.error("[CRTStationInfo1]: Failed to resolve route numbers at {}", getPos().toShortString(), e);
				}
			}
			markDirty();
		}

		/** 由站台 ID 找到途经线路并取其显示编号。 */
		public static String resolveRouteNumber(long platformId) {
			final Platform platform = ClientData.DATA_CACHE.platformIdMap.get(platformId);
			if (platform == null) {
				return "";
			}
			for (final Route route : ClientData.ROUTES) {
				if (route.getPlatformIdIndex(platformId) >= 0) {
					final String number = BlockCRTStationName1.BlockEntity.routeNumberOf(route);
					if (!number.isEmpty()) {
						return number;
					}
				}
			}
			return "";
		}

		private BlockPos findBottomLeftPosition() {
			final Direction facing = IBlock.getStatePropertySafe(getCachedState(), FACING);
			final Direction left = facing.rotateYCounterclockwise();
			final IBlock.EnumSide side = IBlock.getStatePropertySafe(getCachedState(), SIDE_EXTENDED);
			BlockPos basePos = getPos().offset(left, side == IBlock.EnumSide.MIDDLE ? 1 : side == IBlock.EnumSide.RIGHT ? 2 : 0);
			while (basePos.getY() > 0) {
				final BlockPos belowPos = basePos.down();
				if (!(getWorld().getBlockState(belowPos).getBlock() instanceof BlockCRTStationInfo1)) {
					return basePos;
				}
				basePos = belowPos;
			}
			return basePos;
		}

		@Override
		public void readCompoundTag(NbtCompound nbt) {
			super.readCompoundTag(nbt);
			url = nbt.getString(KEY_URL);
			flip = nbt.getBoolean(KEY_FLIP);
			for (int i = 0; i < signIds.length; i++) {
				final long[] ids = nbt.getLongArray(KEY_SELECTED_IDS + "_" + i);
				final LongAVLTreeSet set = selectedIds.get(i);
				set.clear();
				for (final long id : ids) {
					set.add(id);
				}
				for (int j = 0; j < signIds[i].length; j++) {
					final String signId = nbt.getString(KEY_SIGN_IDS + "_" + i + "_" + j);
					signIds[i][j] = signId.isEmpty() ? null : signId;
				}
			}
			routeNumbers.clear();
			final long[] colors = nbt.getLongArray(KEY_ROUTE_COLORS);
			final String joined = nbt.getString(KEY_ROUTE_NUMBERS);
			if (!joined.isEmpty()) {
				final String[] numbers = joined.split("\u0001", -1);
				for (int i = 0; i < colors.length && i < numbers.length; i++) {
					routeNumbers.put(colors[i], numbers[i]);
				}
			}
		}

		@Override
		public void writeCompoundTag(NbtCompound nbt) {
			super.writeCompoundTag(nbt);
			nbt.putString(KEY_URL, url);
			nbt.putBoolean(KEY_FLIP, flip);
			for (int i = 0; i < signIds.length; i++) {
				nbt.putLongArray(KEY_SELECTED_IDS + "_" + i, new ArrayList<>(selectedIds.get(i)));
				for (int j = 0; j < signIds[i].length; j++) {
					nbt.putString(KEY_SIGN_IDS + "_" + i + "_" + j, signIds[i][j] == null ? "" : signIds[i][j]);
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
			nbt.putLongArray(KEY_ROUTE_COLORS, colors);
			nbt.putString(KEY_ROUTE_NUMBERS, sb.toString());
		}
	}
}
