package com.Nanbin.Registry.RegBlock;

import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.World;
import org.mtr.mod.block.BlockPSDAPGDoorBase;
import org.mtr.mod.block.IBlock;

import java.util.*;

/**
 * CRT 屏蔽门自动排序编号工具。
 *
 * 一扇门单元由并排的 2 个方块组成（SIDE=LEFT 与 SIDE=RIGHT，各上下 2 格），门与门之间由玻璃隔开。
 * 沿墙（与 FACING 垂直的方向）扫描所有 CRT 屏蔽门单元并排序：
 * sortFromRight=false（从左）时最左端（墙面坐标最小）为 1 号，向右递增；
 * sortFromRight=true（从右）时最右端（墙面坐标最大）为 1 号，向左递增。
 * 该工具同时被方块 onUse2（切换排序方向）与客户端渲染器使用。
 */
public final class APGDoorNumbering {

	/** 单侧扫描的最大格数，防止极端情况下死循环。 */
	private static final int MAX_WALK = 512;

	private APGDoorNumbering() {
	}

	/** 该方块是否属于本 mod 的 CRT 屏蔽门（一代/二代）。 */
	public static boolean isCRTAPGDoor(BlockState state) {
		final Object blockData = state.getBlock().data;
		return blockData instanceof BlockCRTAPGDoor1 || blockData instanceof BlockCRTAPGDoor2;
	}

	/** 该方块是否属于屏蔽门墙体的 CRT 部件（门或玻璃），用于沿墙连续扫描。 */
	public static boolean isCRTAPGWallBlock(BlockState state) {
		final Object blockData = state.getBlock().data;
		return isCRTAPGDoor(state)
				|| blockData instanceof BlockCRTAPGGlass1
				|| blockData instanceof BlockCRTAPGGlass2
				|| blockData instanceof BlockCRTAPGGlassEnd1
				|| blockData instanceof BlockCRTAPGGlassEnd2;
	}

	/** 返回门的下半部分所在位置（传入上半或下半均可）。 */
	public static BlockPos getLowerPos(World world, BlockPos pos) {
		final BlockState state = world.getBlockState(pos);
		if (isCRTAPGDoor(state) && IBlock.getStatePropertySafe(state, BlockPSDAPGDoorBase.HALF) == IBlock.DoubleBlockHalf.UPPER) {
			return pos.down();
		}
		return pos;
	}

	/** 返回一扇门单元的门框起点（SIDE=LEFT 的下半方块位置），若该方块不是 CRT 门则返回 null。 */
	public static BlockPos getUnitStart(World world, BlockPos doorPos) {
		final BlockPos lowerPos = getLowerPos(world, doorPos);
		final BlockState lowerState = world.getBlockState(lowerPos);
		if (!isCRTAPGDoor(lowerState)) {
			return null;
		}
		final Direction axis = IBlock.getStatePropertySafe(lowerState, BlockPSDAPGDoorBase.FACING).rotateYClockwise();
		final boolean isRight = IBlock.getStatePropertySafe(lowerState, BlockPSDAPGDoorBase.SIDE) == IBlock.EnumSide.RIGHT;
		return isRight ? lowerPos.offset(axis.getOpposite()) : lowerPos;
	}

	/**
	 * 沿墙收集同一排所有门单元的门框起点，按墙面坐标升序排列。
	 * 只统计 FACING 与起始门一致的门；扫描从门出发向两侧延伸，遇到非 CRT 墙块即停止。
	 */
	public static List<BlockPos> findRowUnitStarts(World world, BlockPos doorPos) {
		final BlockPos unitStart = getUnitStart(world, doorPos);
		if (unitStart == null) {
			return new ArrayList<>();
		}
		final BlockState state = world.getBlockState(unitStart);
		final Direction facing = IBlock.getStatePropertySafe(state, BlockPSDAPGDoorBase.FACING);
		final Direction axis = facing.rotateYClockwise();

		final Set<BlockPos> starts = new LinkedHashSet<>();
		starts.add(unitStart);

		for (int i = 1; i <= MAX_WALK; i++) {
			final BlockPos p = unitStart.offset(axis, -i);
			if (!collectWallStart(starts, world, p, facing)) {
				break;
			}
		}
		for (int i = 1; i <= MAX_WALK; i++) {
			final BlockPos p = unitStart.offset(axis, i);
			if (!collectWallStart(starts, world, p, facing)) {
				break;
			}
		}

		final List<BlockPos> sorted = new ArrayList<>(starts);
		sorted.sort(Comparator.comparingInt(p -> axisCoord(p, axis)));
		return sorted;
	}

	/** 在位置 p 处继续扫描：若是同朝向门单元起点则记录并继续；若是其他 CRT 墙块则继续；否则停止。 */
	private static boolean collectWallStart(Set<BlockPos> starts, World world, BlockPos p, Direction facing) {
		final BlockState s = world.getBlockState(p);
		if (isCRTAPGDoor(s) && IBlock.getStatePropertySafe(s, BlockPSDAPGDoorBase.FACING) == facing) {
			if (IBlock.getStatePropertySafe(s, BlockPSDAPGDoorBase.SIDE) == IBlock.EnumSide.LEFT) {
				starts.add(p);
			}
			return true;
		}
		return isCRTAPGWallBlock(s);
	}

	/** 计算门编号；sortFromRight=true 表示从右端开始为 1。无法确定时返回 -1。 */
	public static int getDoorNumber(World world, BlockPos doorPos, boolean sortFromRight) {
		final BlockPos unitStart = getUnitStart(world, doorPos);
		if (unitStart == null) {
			return -1;
		}
		final List<BlockPos> row = findRowUnitStarts(world, unitStart);
		final int index = row.indexOf(unitStart);
		if (index < 0 || row.isEmpty()) {
			return -1;
		}
		return sortFromRight ? row.size() - index : index + 1;
	}

	private static int axisCoord(BlockPos pos, Direction axis) {
		return axis == Direction.EAST || axis == Direction.WEST ? pos.getX() : pos.getZ();
	}
}
