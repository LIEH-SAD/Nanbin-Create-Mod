package com.Nanbin.Registry.RegBlock;

import com.Nanbin.Blocks.Blocks;
import com.Nanbin.Init;
import com.Nanbin.entity.BlockEntityTypes;
import com.Nanbin.mapping.TranslationProvider;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import mtr.block.BlockRailwaySign;
import mtr.block.IBlock;
import mtr.client.ClientData;
import mtr.data.Platform;
import mtr.data.Route;
import mtr.data.Station;
import mtr.mappings.BlockEntityMapper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CRT 版铁路告示牌方块：继承 MTR 原版 {@link BlockRailwaySign}，渲染与编辑沿用 MTR 的
 * RailwaySignScreen / 数据包机制，并支持长度 3-11（含原版不支持的 8-11）。
 */
public class BlockCRTRailwaySign extends BlockRailwaySign {

	public static final DirectionProperty FACING = HorizontalFacingBlock.FACING;

	public BlockCRTRailwaySign(int length, boolean isOdd) {
		super(length, isOdd);
	}

	@Override
	public BlockEntityMapper createBlockEntity(BlockPos pos, BlockState state) {
		return new BlockEntityCRTRailwaySign(length, isOdd, pos, state);
	}

	/**
	 * 放置告示牌时，在中间段放置 CRT 版中间方块（替代原版 RAILWAY_SIGN_MIDDLE）：
	 * 单数格（isOdd=true）最中间一格为 middle_odd；双格（isOdd=false）最中间两格
	 * 分别为 middle_even_1、middle_even_2；其余均为 middle_common。
	 */
	@Override
	public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
		if (!world.isClient) {
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
				world.setBlockState(pos.offset(facing.rotateYClockwise(), i), middleBlock.getDefaultState().with(FACING, facing), 3);
			}
			world.setBlockState(pos.offset(facing.rotateYClockwise(), middleLength + 1), getDefaultState().with(FACING, facing.getOpposite()), 3);
			world.updateNeighbors(pos, net.minecraft.block.Blocks.AIR);
			state.updateNeighbors(world, pos, 3);
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
			if (checkState.getBlock() instanceof BlockRailwaySign) {
				final Direction facing = IBlock.getStatePropertySafe(checkState, FACING);
				if (!(checkState.getBlock() instanceof BlockCRTRailwaySignMiddle) && !checkState.isOf(mtr.Blocks.RAILWAY_SIGN_MIDDLE.get()) && (facing == direction || allowOpposite && facing == direction.getOpposite())) {
					return checkPos;
				}
			} else {
				return null;
			}
			i++;
		}
	}

	@Nonnull
	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		Direction facing = IBlock.getStatePropertySafe(state, FACING);
		if (state.isOf(Blocks.CRT_RAILWAY_SIGN_MIDDLE_COMMON.get())) {
			return IBlock.getVoxelShapeByDirection(0, 0, 7, 16, 16, 9, facing);
		} else {
			int xStart = getXStart();
			return IBlock.getVoxelShapeByDirection((double) xStart - 0.75, 0, 7, 16, 16, 9, facing);
		}
	}

	/**
	 * MTR 原版 BlockRailwaySign.BlockEntity 的私有静态 getType 只支持长度 2-7，
	 * 长度 8-11 会回退为长度 2 的 MTR 类型，导致方块实体类型与 CRT 方块不匹配。
	 * 此子类在构造函数内通过反射把 Minecraft 的 type 字段修正为 CRT 自己注册的方块实体类型。
	 */
	public static class BlockEntityCRTRailwaySign extends BlockRailwaySign.TileEntityRailwaySign {

		private static final String KEY_ROUTE_COLORS = "crt_route_colors";
		private static final String KEY_ROUTE_NUMBERS = "crt_route_numbers";
		/** 每格独立线路颜色，格式："<格子下标>:<颜色>,<颜色>;<格子下标>:<颜色>"（空=无单独设置）。 */
		private static final String KEY_CELL_COLORS = "crt_cell_colors";
		/** 颜色 -> 线路编号。 */
		private final Long2ObjectOpenHashMap<String> routeNumbers = new Long2ObjectOpenHashMap<>();
		/** 每格独立线路颜色：下标=格子，值为该格所在线路牌区域实际使用的线路颜色；null=沿用全局选择。 */
		private long[][] cellColors;

		public BlockEntityCRTRailwaySign(int length, boolean isOdd, BlockPos pos, BlockState state) {
			super(length, isOdd, pos, state);
			fixBlockEntityType(length, isOdd);
		}

		/** 通过反射把父类构造设置的 BlockEntityType 修正为 CRT 自己注册的类型。 */
		private void fixBlockEntityType(int length, boolean isOdd) {
			try {
				final BlockEntityType<?> rawType = BlockEntityTypes.getRailwaySignType(length, isOdd);
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
				Init.LOGGER.warn("[BlockCRTRailwaySign]: Unable to find block entity type field at {}", getPos().toShortString());
			} catch (Exception e) {
				Init.LOGGER.error("[BlockCRTRailwaySign]: Failed to fix block entity type at {}", getPos().toShortString(), e);
			}
		}

		/** 按 selectedIds（颜色）的顺序返回已解析的线路编号；无编号的颜色会被跳过。 */
		public String[] getRouteNumbers() {
			return getRouteNumbersForCell(getSelectedIds().stream().mapToLong(Long::longValue).toArray());
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
			return getSelectedIds().stream().mapToLong(Long::longValue).toArray();
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

		/** 按线路颜色返回真实线路编号；无则返回空串。 */
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
		public void setData(Set<Long> selectedIds, String[] signIds) {
			super.setData(selectedIds, signIds);
			updateRouteNumbersFromServer();
		}

		@Override
		public void readCompoundTag(NbtCompound compoundTag) {
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
		public void writeCompoundTag(NbtCompound compoundTag) {
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
		 * 解析所选颜色对应的线路编号。客户端数据（ClientData.ROUTES）包含 lightRailRouteNumber，
		 * 因此直接在客户端解析并写入 routeNumbers，供渲染器使用。
		 */
		private void updateRouteNumbersFromServer() {
			routeNumbers.clear();
			if (getWorld() == null || getWorld().isClient) {
				return;
			}
			// 收集全局选择与所有格子独立选择的颜色，统一解析线路编号
			final LongAVLTreeSet allColors = new LongAVLTreeSet();
			getSelectedIds().forEach(allColors::add);
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
				markDirty();
				return;
			}
			try {
				// 找到告示牌所在站点，收集途经线路的线路编号
				final BlockPos pos = getPos();
				final Station station = findStationAt(pos);
				if (station != null) {
					for (final Platform platform : ClientData.DATA_CACHE.platformIdToStation.entrySet().stream().filter(e -> e.getValue() == station).map(Map.Entry::getKey).map(ClientData.DATA_CACHE.platformIdMap::get).filter(java.util.Objects::nonNull).toList()) {
						// platformId -> routes
						for (final Route route : ClientData.ROUTES) {
							if (route.getPlatformIdIndex(platform.id) >= 0) {
								final long color = route.color;
								if (allColors.contains(color) && !routeNumbers.containsKey(color)) {
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
				Init.LOGGER.error("[BlockCRTRailwaySign]: Failed to resolve route numbers at {}", getPos().toShortString(), e);
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

	@Override
	public void appendTooltip(ItemStack stack, @Nullable BlockView world, List<Text> tooltip, TooltipContext options) {
		tooltip.add(TranslationProvider.BRUSH_USE.getMutableText().copy().formatted(Formatting.DARK_GRAY));
	}
}