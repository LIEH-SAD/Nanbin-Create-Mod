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
import java.util.function.Consumer;

import static mtr.block.IBlock.HALF;
import static mtr.block.IBlock.SIDE_EXTENDED;

/**
 * CRT 站台信息屏（双面）：结构与单面版相同（3 格宽 × 3 格高），但正面与反面各自保存
 * 一套 URL、指示牌数据与站台选择。用刷子点击对应面即可分别配置 / 切换条带方向。
 * 渲染由 {@link com.Nanbin.client.Render.RenderCRTStationInfo1Double} 完成。
 */
public class BlockCRTStationInfo1Double extends BlockDirectionalMapper implements EntityBlockMapper {

	public BlockCRTStationInfo1Double(Settings settings) {
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
		final Direction direction = ctx.getHorizontalPlayerFacing();
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
					if (!breakPos.equals(pos) && world.getBlockState(breakPos).getBlock() instanceof BlockCRTStationInfo1Double) {
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
			if (!(world.getBlockState(belowPos).getBlock() instanceof BlockCRTStationInfo1Double)) {
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
		final Direction hitSide = hit.getSide();
		if (hitSide != facing && hitSide != facing.getOpposite()) {
			return ActionResult.PASS;
		}
		final boolean isFront = hitSide.getOpposite() == facing;
		if (!(world.getBlockEntity(pos) instanceof BlockEntity entity)) {
			return ActionResult.PASS;
		}
		if (IBlock.getStatePropertySafe(state, HALF) == DoubleBlockHalf.UPPER) {
			entity.toggleFlip(isFront);
		} else {
			Registry.sendPacketToClient((ServerPlayerEntity) player, PacketHandler.PACKET_OPEN_STATION_INFO, buf -> {
				buf.writeBlockPos(pos);
				buf.writeString(entity.getUrl(isFront));
				buf.writeBoolean(isFront);
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

	/** 方块实体：正反面各保存一套 URL / 条带方向 / 指示牌数据 / 所选站台，并在结构内 9 个方块之间同步。 */
	public static class BlockEntity extends BlockEntityClientSerializableMapper {

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
				for (final long platformId : lineSelected) {
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
			markDirty();
			syncData();
		}

		public void setFlip(boolean front, boolean newFlip) {
			if (front) {
				this.flip = newFlip;
			} else {
				this.flipBack = newFlip;
			}
			syncFlipToOtherBlocks(front);
			markDirty();
			syncData();
		}

		private void syncFlipToOtherBlocks(boolean front) {
			final boolean flipValue = front ? this.flip : this.flipBack;
			forEachStructurePos(otherPos -> {
				if (getWorld().getBlockEntity(otherPos) instanceof BlockEntity entity) {
					if (front) {
						entity.flip = flipValue;
					} else {
						entity.flipBack = flipValue;
					}
					entity.markDirty();
					entity.syncData();
				}
			});
		}

		public void setUrl(boolean front, String newUrl) {
			if (front) {
				this.url = newUrl;
			} else {
				this.urlBack = newUrl;
			}
			syncDataToOtherBlocks(front);
			markDirty();
			syncData();
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
			updateRouteNumbers(front);
			markDirty();
			syncData();
		}

		public void setSignData(boolean front, String[][] newSignIds, List<LongAVLTreeSet> newSelectedIds) {
			copySignDataOnly(front, newSignIds, newSelectedIds);
			syncSignDataToOtherBlocks(front);
			updateRouteNumbers(front);
			markDirty();
			syncData();
		}

		private void syncDataToOtherBlocks(boolean front) {
			forEachStructurePos(otherPos -> {
				if (getWorld().getBlockEntity(otherPos) instanceof BlockEntity entity) {
					if (front) {
						entity.url = this.url;
					} else {
						entity.urlBack = this.urlBack;
					}
					entity.markDirty();
					entity.syncData();
				}
			});
		}

		private void syncSignDataToOtherBlocks(boolean front) {
			forEachStructurePos(otherPos -> {
				if (getWorld().getBlockEntity(otherPos) instanceof BlockEntity entity) {
					entity.copySignDataOnly(front, front ? this.signIds : this.signIdsBack, front ? this.selectedIds : this.selectedIdsBack);
					entity.updateRouteNumbers(front);
					// 必须把新数据推送给客户端，否则负责渲染的方块仍显示旧内容
					entity.markDirty();
					entity.syncData();
				}
			});
		}

		private void forEachStructurePos(Consumer<BlockPos> action) {
			final Direction facing = IBlock.getStatePropertySafe(getCachedState(), FACING);
			final Direction left = facing.rotateYCounterclockwise();
			final IBlock.EnumSide side = IBlock.getStatePropertySafe(getCachedState(), SIDE_EXTENDED);
			BlockPos basePos = getPos().offset(left, side == IBlock.EnumSide.MIDDLE ? 1 : side == IBlock.EnumSide.RIGHT ? 2 : 0);
			while (basePos.getY() > 0 && getWorld().getBlockState(basePos.down()).getBlock() instanceof BlockCRTStationInfo1Double) {
				basePos = basePos.down();
			}
			final Direction right = facing.rotateYClockwise();
			for (int y = 0; y < 3; y++) {
				for (int x = 0; x < 3; x++) {
					final BlockPos otherPos = basePos.up(y).offset(right, x);
					if (!otherPos.equals(getPos())) {
						action.accept(otherPos);
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
			markDirty();
		}

		/** 服务端解析所选站台的线路编号并写入 NBT 供渲染端读取（按正/反面分别缓存）。 */
		private void updateRouteNumbers(boolean front) {
			final List<LongAVLTreeSet> targetIds = front ? selectedIds : selectedIdsBack;
			final Long2ObjectOpenHashMap<String> targetNumbers = front ? routeNumbers : routeNumbersBack;
			targetNumbers.clear();
			if (getWorld() == null || getWorld().isClient) {
				return;
			}
			final LongAVLTreeSet allPlatformIds = new LongAVLTreeSet();
			for (final LongAVLTreeSet lineSelected : targetIds) {
				allPlatformIds.addAll(lineSelected);
			}
			if (!allPlatformIds.isEmpty()) {
				try {
					for (final long platformId : allPlatformIds) {
						final String number = BlockCRTStationInfo1.BlockEntity.resolveRouteNumber(platformId);
						if (!number.isEmpty()) {
							targetNumbers.put(platformId, number);
						}
					}
				} catch (Exception e) {
					Init.LOGGER.error("[CRTStationInfo1Double]: Failed to resolve route numbers at {}", getPos().toShortString(), e);
				}
			}
			markDirty();
		}

		@Override
		public void readCompoundTag(NbtCompound nbt) {
			super.readCompoundTag(nbt);
			url = nbt.getString(KEY_URL);
			urlBack = nbt.getString(KEY_URL_BACK);
			flip = nbt.getBoolean(KEY_FLIP);
			flipBack = nbt.getBoolean(KEY_FLIP_BACK);
			readSideData(nbt, KEY_SIGN_IDS, KEY_SELECTED_IDS, KEY_ROUTE_COLORS, KEY_ROUTE_NUMBERS, signIds, selectedIds, routeNumbers);
			readSideData(nbt, KEY_SIGN_IDS_BACK, KEY_SELECTED_IDS_BACK, KEY_ROUTE_COLORS_BACK, KEY_ROUTE_NUMBERS_BACK, signIdsBack, selectedIdsBack, routeNumbersBack);
		}

		private void readSideData(NbtCompound nbt, String signIdsKey, String selectedIdsKey, String routeColorsKey, String routeNumbersKey, String[][] targetSignIds, List<LongAVLTreeSet> targetSelectedIds, Long2ObjectOpenHashMap<String> targetRouteNumbers) {
			for (int i = 0; i < targetSignIds.length; i++) {
				final long[] ids = nbt.getLongArray(selectedIdsKey + "_" + i);
				final LongAVLTreeSet set = targetSelectedIds.get(i);
				set.clear();
				for (final long id : ids) {
					set.add(id);
				}
				for (int j = 0; j < targetSignIds[i].length; j++) {
					final String signId = nbt.getString(signIdsKey + "_" + i + "_" + j);
					targetSignIds[i][j] = signId.isEmpty() ? null : signId;
				}
			}
			targetRouteNumbers.clear();
			final long[] colors = nbt.getLongArray(routeColorsKey);
			final String joined = nbt.getString(routeNumbersKey);
			if (!joined.isEmpty()) {
				final String[] numbers = joined.split("\u0001", -1);
				for (int i = 0; i < colors.length && i < numbers.length; i++) {
					targetRouteNumbers.put(colors[i], numbers[i]);
				}
			}
		}

		@Override
		public void writeCompoundTag(NbtCompound nbt) {
			super.writeCompoundTag(nbt);
			nbt.putString(KEY_URL, url);
			nbt.putString(KEY_URL_BACK, urlBack);
			nbt.putBoolean(KEY_FLIP, flip);
			nbt.putBoolean(KEY_FLIP_BACK, flipBack);
			writeSideData(nbt, KEY_SIGN_IDS, KEY_SELECTED_IDS, KEY_ROUTE_COLORS, KEY_ROUTE_NUMBERS, signIds, selectedIds, routeNumbers);
			writeSideData(nbt, KEY_SIGN_IDS_BACK, KEY_SELECTED_IDS_BACK, KEY_ROUTE_COLORS_BACK, KEY_ROUTE_NUMBERS_BACK, signIdsBack, selectedIdsBack, routeNumbersBack);
		}

		private void writeSideData(NbtCompound nbt, String signIdsKey, String selectedIdsKey, String routeColorsKey, String routeNumbersKey, String[][] targetSignIds, List<LongAVLTreeSet> targetSelectedIds, Long2ObjectOpenHashMap<String> targetRouteNumbers) {
			for (int i = 0; i < targetSignIds.length; i++) {
				nbt.putLongArray(selectedIdsKey + "_" + i, new ArrayList<>(targetSelectedIds.get(i)));
				for (int j = 0; j < targetSignIds[i].length; j++) {
					nbt.putString(signIdsKey + "_" + i + "_" + j, targetSignIds[i][j] == null ? "" : targetSignIds[i][j]);
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
			nbt.putLongArray(routeColorsKey, colors);
			nbt.putString(routeNumbersKey, sb.toString());
		}
	}
}
