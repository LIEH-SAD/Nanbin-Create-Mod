package com.Nanbin.mapping;

import mtr.mappings.HorizontalBlockWithSoftLanding;
import net.minecraft.block.BlockSetType;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.enums.DoorHinge;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

/**
 * MTR 4.0 {@code org.mtr.mapping.mapper.DoorBlockExtension} 在本工程的等价基类。
 *
 * <p>MTR 4.0 的 mapper 基类本质是「带 {@code getOutlineShape2} 钩子的门方块」。MTR 3.6.3 没有这层
 * mapper，本类直接继承原版 {@link DoorBlock}（门的方向/开合/上下半/铰链属性与开关门音效全部沿用原版），
 * 只额外提供：
 * <ul>
 *   <li>{@link #getOutlineShape2} 钩子：子类返回非 {@code null} 时覆盖碰撞箱，返回 {@code null} 时退回原版；</li>
 *   <li>{@link #isOpen}/{@link #cycleOpen}/{@link #isRightHinge}/{@link #isUpperHalf} 四个便捷静态方法。</li>
 * </ul>
 * 无障碍落地逻辑由 MTR 的 {@link HorizontalBlockWithSoftLanding} 在放置方向层面处理，与本基类无冲突。</p>
 */
public abstract class DoorBlockExtension extends DoorBlock {

	protected DoorBlockExtension(BlockSetType blockSetType, Settings settings) {
		super(settings, blockSetType);
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		final VoxelShape result = getOutlineShape2(state, world, pos, context);
		return result == null ? super.getOutlineShape(state, world, pos, context) : result;
	}

	/** 返回 {@code null} 表示沿用原版门碰撞箱。 */
	public VoxelShape getOutlineShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return null;
	}

	/**
	 * 与来源 {@code org.mtr.mapping.mapper.DoorBlockExtension.isOpen} 等价。
	 * 名称加 {@code Door} 前缀：1.20.1 的 {@link DoorBlock} 已有同名实例方法，避免签名冲突告警。
	 */
	protected static boolean isDoorOpen(BlockState state) {
		return state.get(OPEN);
	}

	protected static BlockState cycleOpen(BlockState state) {
		return state.cycle(OPEN);
	}

	protected static boolean isRightHinge(BlockState state) {
		return state.get(HINGE) == DoorHinge.RIGHT;
	}

	protected static boolean isUpperHalf(BlockState state) {
		return state.get(HALF) == DoubleBlockHalf.UPPER;
	}
}
