package com.Nanbin.Registry.RegBlock;

import com.Nanbin.Blocks.Blocks;
import com.Nanbin.Init;
import com.Nanbin.entity.BlockEntityTypes;
import com.Nanbin.mapping.BlockEntityHelper;
import com.Nanbin.mapping.MtrServerData;
import com.Nanbin.mapping.ObjectHolder;
import com.Nanbin.mapping.TranslationProvider;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.mtr.block.BlockRailwaySign;
import org.mtr.block.IBlock;
import org.mtr.core.data.Platform;
import org.mtr.core.data.Route;
import org.mtr.core.data.Station;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import org.mtr.packet.PacketUpdateRailwaySignConfig;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

/**
 * CRT 版铁路告示牌（对应 MTR 原版 RAILWAY_SIGN_*，长度 3–11）。
 * <p>
 * 与 4.0 的差异（MTR 4.1 自身已经支持「每格独立线路颜色」）：
 * <ul>
 *     <li>4.0 的 {@code BlockEntityCRTRailwaySign} 用「一个全局 selectedIds + 每格 cellColors 覆盖」实现每格配色；
 *     4.1 的 {@link BlockRailwaySign.RailwaySignBlockEntity} 本身就是 {@code LongAVLTreeSet[]}（一格一个集合），
 *     所以那套 cellColors 逻辑直接删掉，改为直接使用父类的每格集合。</li>
 *     <li>4.0 需要覆写 {@code createBlockEntity} 返回 {@code BlockEntityExtension}；4.1 改成原版
 *     {@link net.minecraft.block.BlockEntityProvider#createBlockEntity}，返回 {@link BlockEntity}。</li>
 * </ul>
 * 仍然保留的 CRT 特性：中间的 CRT 专用贴图变体（odd / even_1 / even_2 / common）+ 每格线路编号。
 */
public class BlockCRTRailwaySign extends BlockRailwaySign {

	public BlockCRTRailwaySign(net.minecraft.block.AbstractBlock.Settings settings, int length, boolean isOdd) {
		super(settings, length, isOdd);
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new BlockEntityCRTRailwaySign(length, isOdd, pos, state);
	}

	/**
	 * 放置告示牌时，在中间段放置 CRT 版中间方块（替代原版 RAILWAY_SIGN_MIDDLE）：
	 * 单数格（isOdd=true）最中间一格为 middle_odd；双格（isOdd=false）最中间两格
	 * 分别为 middle_even_1、middle_even_2；其余均为 middle_common。
	 */
	@Override
	public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
		if (!world.isClient) {
			final Direction facing = IBlock.getStatePropertySafe(state, Properties.HORIZONTAL_FACING);
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
				world.setBlockState(pos.offset(facing.rotateYClockwise(), i), middleBlock.getDefaultState().with(Properties.HORIZONTAL_FACING, facing), 3);
			}
			world.setBlockState(pos.offset(facing.rotateYClockwise(), middleLength + 1), getDefaultState().with(Properties.HORIZONTAL_FACING, facing.getOpposite()), 3);
			world.updateNeighbors(pos, net.minecraft.block.Blocks.AIR);
			state.updateNeighbors(world, pos, 3);
		}
	}

	/**
	 * 破坏告示牌时把整签（两个端块 + 中间所有格）一起清掉。
	 * <p>
	 * 4.0 是靠覆写 {@code getStateForNeighborUpdate2} 实现链式消失的：那份实现<strong>两个方向都判</strong>，
	 * 所以破坏任意一格整签都会散掉。4.1 无法共用那份覆写（1.21.1 / 1.21.4 的
	 * {@code getStateForNeighborUpdate} 签名不同），只能靠 MTR 父类的 {@code updateShape}，
	 * 而它只在「顺时针」方向传播（「逆时针」那一支只对 MTR 自己的 {@code RAILWAY_SIGN_MIDDLE} 生效，
	 * CRT 的中间块判不中），于是从 CRT 中间格破坏时只有一侧消失、另一侧残留。
	 * 这里改为破坏时主动沿轴清空整签，生存 / 创造表现一致，也不再依赖版本相关的签名。
	 */
	@Override
	public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
		breakWholeSign(world, pos);
		return super.onBreak(world, pos, state, player);
	}

	/** 告示牌最长 11 格，沿轴最多探这么多格。 */
	static final int MAX_SIGN_LENGTH = 11;

	/**
	 * 沿告示牌轴向把同一块告示牌的所有格清掉（{@code pos} 本身留给原版删除）。
	 * 与 4.0 的链式消失等价：只认「还是告示牌方块」的格子，遇到别的方块就停。
	 */
	static void breakWholeSign(World world, BlockPos pos) {
		if (world.isClient) {
			return;
		}
		final BlockState state = world.getBlockState(pos);
		if (!(state.getBlock() instanceof BlockRailwaySign)) {
			return;
		}
		// 所有格子的水平朝向都落在同一条轴上，所以用被破坏那一格的朝向取轴即可
		final Direction right = IBlock.getStatePropertySafe(state, Properties.HORIZONTAL_FACING).rotateYClockwise();
		for (final Direction direction : new Direction[]{right, right.getOpposite()}) {
			for (int i = 1; i <= MAX_SIGN_LENGTH; i++) {
				final BlockPos checkPos = pos.offset(direction, i);
				if (!(world.getBlockState(checkPos).getBlock() instanceof BlockRailwaySign)) {
					break;
				}
				world.removeBlock(checkPos, false);
			}
		}
	}

	/** 重新实现原版 private 的 getMiddleLength（length/isOdd 均为 public 字段，getXStart 为 public）。 */
	int getMiddleLength() {
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
				final Direction facing = IBlock.getStatePropertySafe(checkState, Properties.HORIZONTAL_FACING);
				if (!(checkState.getBlock() instanceof BlockCRTRailwaySignMiddle) && !BlockCRTRailwaySignMiddle.isMtrMiddle(checkState) && (facing == direction || allowOpposite && facing == direction.getOpposite())) {
					return checkPos;
				}
			} else {
				return null;
			}
			i++;
		}
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		final Direction facing = IBlock.getStatePropertySafe(state, Properties.HORIZONTAL_FACING);
		if (state.getBlock() instanceof BlockCRTRailwaySignMiddle) {
			return IBlock.getVoxelShapeByDirection(0, 0, 7, 16, 16, 9, facing);
		} else {
			final int xStart = getXStart();
			return IBlock.getVoxelShapeByDirection(xStart - 0.75, 0, 7, 16, 16, 9, facing);
		}
	}

	@Override
	public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType options) {
		super.appendTooltip(stack, context, tooltip, options);
		tooltip.add(TranslationProvider.BRUSH_USE.getMutableText().formatted(Formatting.DARK_GRAY));
	}

	/**
	 * 告示牌方块实体。
	 * <p>
	 * 4.0 的私有静态 {@code getType(length, isOdd)} 只支持长度 2–7，长度 8–11 会回退为长度 2 的类型，
	 * 导致方块实体类型与 CRT 方块不匹配：渲染时不会被分发到 CRT 渲染器，存档重载后还会按错误的
	 * 类型 id 重建、丢失全部告示牌数据。4.1 里这段逻辑仍是 MTR 的 private static，无法覆写，
	 * 所以延续 4.0 的做法：构造完成后把原版 {@code BlockEntity.type} 字段修正为 CRT 自己注册的类型。
	 */
	public static class BlockEntityCRTRailwaySign extends BlockRailwaySign.RailwaySignBlockEntity {

		private static final String KEY_ROUTE_COLORS = "crt_route_colors";
		private static final String KEY_ROUTE_NUMBERS = "crt_route_numbers";

		/**
		 * 线路颜色（有符号扩展后的 long，与 {@code Route.getColor()} 一致）→ 线路编号。
		 * MTR 的客户端数据只有 {@code SimplifiedRoute}（没有 routeNumber 字段），
		 * 所以只能在服务端解析一次并写进 NBT。
		 */
		private final Long2ObjectOpenHashMap<String> routeNumbers = new Long2ObjectOpenHashMap<>();

		public BlockEntityCRTRailwaySign(int length, boolean isOdd, BlockPos pos, BlockState state) {
			// 注意参数里的 ensureMtrSignTypesAcceptCrtBlocks：父类 MTR 的构造函数会先用
			// length/isOdd 解析出一个 MTR 自己的方块实体类型，而 NeoForge 在 BlockEntity 的
			// 构造函数里立刻校验「该类型是否接受当前方块」，长度 8–11 会回退成
			// railway_sign_2_even 从而校验失败、客户端崩溃。所以必须在那之前把 CRT 方块补进
			// MTR 类型的可接受方块集合。Java 要求 super(...) 是第一条语句，只能借「实参求值」
			// 的时机执行；这也是唯一能覆盖全部构造路径（createBlockEntity 与方块实体类型工厂）
			// 的位置。
			super(withMtrTypeCompatibility(length), isOdd, pos, state);
			fixBlockEntityType(BlockEntityTypes.getRailwaySignType(length, isOdd));
		}

		/** 见构造函数的说明：修补 MTR 类型后原样返回 length。 */
		private static int withMtrTypeCompatibility(int length) {
			BlockEntityTypes.ensureMtrSignTypesAcceptCrtBlocks();
			return length;
		}

		/** 把原版 {@code BlockEntity.type} 字段改成 CRT 注册的类型（详见类注释）。 */
		private void fixBlockEntityType(ObjectHolder<BlockEntityType<BlockEntityCRTRailwaySign>> holder) {
			if (holder == null || !holder.exists()) {
				return;
			}
			final BlockEntityType<?> type = holder.get();
			if (getType() == type) {
				return;
			}
			for (final Field field : BlockEntity.class.getDeclaredFields()) {
				if (Modifier.isStatic(field.getModifiers()) || !field.getType().isAssignableFrom(type.getClass())) {
					continue;
				}
				try {
					field.setAccessible(true);
					field.set(this, type);
					return;
				} catch (Exception e) {
					Init.LOGGER.error("BlockCRTRailwaySign: Failed to fix block entity type at {}", getPos(), e);
					return;
				}
			}
			Init.LOGGER.warn("BlockCRTRailwaySign: Unable to find block entity type field at {}", getPos());
		}

		/** 全部格子的线路编号，按格子顺序返回（无编号的颜色会被跳过）。 */
		public String[] getRouteNumbers() {
			final String[] result = new String[getSignIds().length];
			for (int i = 0; i < result.length; i++) {
				result[i] = getRouteNumberForCell(i);
			}
			return result;
		}

		/** 指定格子的线路编号：该格选中的第一个有编号的颜色。 */
		public String getRouteNumberForCell(int cell) {
			final LongAVLTreeSet[] selectedIds = getSelectedIds();
			if (cell < 0 || cell >= selectedIds.length) {
				return "";
			}
			for (final long color : selectedIds[cell]) {
				final String number = routeNumbers.get(color);
				if (number != null && !number.isEmpty()) {
					return number;
				}
			}
			return "";
		}

		/** 按线路颜色返回真实线路编号（可能为中文/字母或含 "|" 的双语编号）；无则返回空串。 */
		public String getRouteNumber(long color) {
			final String number = routeNumbers.get(color);
			return number == null ? "" : number;
		}

		/** 按颜色顺序返回线路编号；无编号的颜色会被跳过。 */
		public String[] getRouteNumbersForColors(long[] colors) {
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

		@Override
		public void setData(LongAVLTreeSet[] selectedIds, String[] signIds) {
			super.setData(selectedIds, signIds);
			updateRouteNumbers();
		}

		/** 服务端专用：设置指定格子（线路牌区域）的线路颜色集合，并重新解析线路编号。 */
		public void setSelectedIdsCell(int cell, long[] colors) {
			final LongAVLTreeSet[] selectedIds = getSelectedIds();
			if (cell < 0 || cell >= selectedIds.length) {
				return;
			}
			selectedIds[cell].clear();
			if (colors != null) {
				for (final long color : colors) {
					selectedIds[cell].add(color);
				}
			}
			updateRouteNumbers();
		}

		/** 指定格子当前的线路颜色集合。 */
		public long[] getSelectedIdsCell(int cell) {
			final LongAVLTreeSet[] selectedIds = getSelectedIds();
			return cell < 0 || cell >= selectedIds.length ? new long[0] : selectedIds[cell].toLongArray();
		}

		@Override
		protected void readNbt(NbtCompound nbtCompound) {
			super.readNbt(nbtCompound);
			routeNumbers.clear();
			final long[] colors = nbtCompound.getLongArray(KEY_ROUTE_COLORS);
			final String joined = nbtCompound.getString(KEY_ROUTE_NUMBERS);
			if (!joined.isEmpty()) {
				final String[] numbers = joined.split("\u0001", -1);
				for (int i = 0; i < colors.length && i < numbers.length; i++) {
					routeNumbers.put(colors[i], numbers[i]);
				}
			}
		}

		@Override
		protected void writeNbt(NbtCompound nbtCompound) {
			super.writeNbt(nbtCompound);
			final long[] colors = routeNumbers.keySet().toLongArray();
			final StringBuilder stringBuilder = new StringBuilder();
			for (int i = 0; i < colors.length; i++) {
				if (i > 0) {
					stringBuilder.append('\u0001');
				}
				stringBuilder.append(routeNumbers.get(colors[i]));
			}
			nbtCompound.putLongArray(KEY_ROUTE_COLORS, colors);
			nbtCompound.putString(KEY_ROUTE_NUMBERS, stringBuilder.toString());
		}

		/**
		 * 服务端解析线路编号：颜色 → {@code Route#getRouteNumber()}。
		 * <p>
		 * 必须放在服务端做：MTR 客户端只有 {@code SimplifiedRoute}，没有 {@code routeNumber} 字段
		 * （见 {@link MtrServerData} 的说明），轻铁这类「名字与编号分开填」的线路在客户端取不到编号。
		 * 4.0 就是这么做的，4.1 移植时被写成了空实现导致编号丢失。
		 */
		private void updateRouteNumbers() {
			routeNumbers.clear();
			if (getWorld() != null && !getWorld().isClient()) {
				// 收集所有格子选中的颜色，统一解析
				final LongAVLTreeSet allColors = new LongAVLTreeSet();
				for (final LongAVLTreeSet cell : getSelectedIds()) {
					if (cell != null) {
						allColors.addAll(cell);
					}
				}
				final Station station = allColors.isEmpty() ? null : MtrServerData.findStation(getPos());
				if (station != null) {
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
				}
			}
			BlockEntityHelper.markDirty(this);
		}

		/** 供客户端渲染/编辑界面判断是否需要重新请求服务端数据。 */
		public boolean hasRouteNumber(long color) {
			return routeNumbers.containsKey(color);
		}
	}
}
