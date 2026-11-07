package com.Nanbin.Registry.RegBlock;

import com.Nanbin.Items.Items;
import mtr.block.BlockPSDAPGGlassBase;
import mtr.block.BlockPSDGlass;
import mtr.block.IBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Property;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import static com.Nanbin.mapping.IBlockExtension.EnumGlassMode;
import static com.Nanbin.mapping.IBlockExtension.GLASS_MODE;

/**
 * CRT 第一代高屏蔽门玻璃。
 *
 * <p>沿用 MTR 原版屏蔽门玻璃的逻辑：继承 {@link BlockPSDGlass}（style 0 = MTR 的 psd_glass 一代），
 * 属性与 MTR 的 psd_glass 一致（facing / side / half），模型也直接沿用 MTR 的
 * {@code mtr:block/psd_glass_*_base}，与门、端玻璃、吊板成套。
 *
 * <p><b>为什么必须继承 {@link BlockPSDGlass} 而不是 {@link BlockPSDAPGGlassBase}：</b>
 * 原版 {@code BlockPSDTop.getActualState()} 只认 {@link BlockPSDGlass} / {@code BlockPSDDoor} /
 * {@code BlockPSDGlassEnd} 三种方块，靠它把下方门/玻璃的 facing 与左右分组写到吊板状态上。
 * 若直接继承基类，吊板上方分组的朝向/左右就不会被写入 —— 表现为「放置玻璃后吊板方向错误」。
 *
 * <p>刷子分组逻辑参考 {@link BlockCRTAPGGlass2}：使用刷子点击时把相邻玻璃按 1/2/3 列重新分组，
 * 同时额外记录 {@code side_count} / {@code mode}，并同步写入 MTR 的 {@code side} 属性，
 * 保证吊板与现有模型仍能正确识别左右分组。
 */
public class BlockCRTPSDGlass1 extends BlockPSDGlass {

	/** MTR psd_glass 一代的样式编号，与贴图 psd_glass_*_1 对应。 */
	private static final int STYLE = 0;

	public static final IntProperty SIDE_COUNT = IntProperty.of("side_count", 1, 3);

	public BlockCRTPSDGlass1() {
		super(STYLE);
		setDefaultState(getDefaultState().with(SIDE_COUNT, 1).with(GLASS_MODE, EnumGlassMode.SINGLE));
	}

	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		final double y = hit.getPos().y;
		if (y - Math.floor(y) > 0.21875) {
			final boolean isUpper = IBlock.getStatePropertySafe(state, HALF) == DoubleBlockHalf.UPPER;
			final BlockPos targetPos = isUpper ? pos : pos.up();
			final BlockState targetState = isUpper ? state : world.getBlockState(targetPos);

			return IBlock.checkHoldingBrush(world, player, () -> {
				final Direction facing = IBlock.getStatePropertySafe(targetState, FACING);
				final Direction left = facing.rotateYCounterclockwise();
				final Direction right = facing.rotateYClockwise();

				int leftCount = 0;
				for (int i = 1; i <= 2; i++) {
					if (isSameGlass(world, targetPos.offset(left, i))) {
						leftCount = i;
					} else {
						break;
					}
				}
				int rightCount = 0;
				for (int i = 1; i <= 2; i++) {
					if (isSameGlass(world, targetPos.offset(right, i))) {
						rightCount = i;
					} else {
						break;
					}
				}
				final int totalCount = leftCount + rightCount + 1;

				if (totalCount == 1) {
					setBothHalves(world, targetPos, targetState, 1, EnumGlassMode.SINGLE);
				} else if (totalCount == 2) {
					if (leftCount == 1) {
						setBothHalves(world, targetPos.offset(left, 1), world.getBlockState(targetPos.offset(left, 1)), 2, EnumGlassMode.LEFT);
						setBothHalves(world, targetPos, targetState, 2, EnumGlassMode.RIGHT);
					} else {
						setBothHalves(world, targetPos, targetState, 2, EnumGlassMode.LEFT);
						setBothHalves(world, targetPos.offset(right, 1), world.getBlockState(targetPos.offset(right, 1)), 2, EnumGlassMode.RIGHT);
					}
				} else if (totalCount == 3 && !isSameGlass(world, targetPos.offset(left, 3)) && !isSameGlass(world, targetPos.offset(right, 3))) {
					if (leftCount == 0 && rightCount == 2) {
						setBothHalves(world, targetPos, targetState, 3, EnumGlassMode.LEFT);
						setBothHalves(world, targetPos.offset(right, 1), world.getBlockState(targetPos.offset(right, 1)), 3, EnumGlassMode.MIDDLE);
						setBothHalves(world, targetPos.offset(right, 2), world.getBlockState(targetPos.offset(right, 2)), 3, EnumGlassMode.RIGHT);
					} else if (leftCount == 2 && rightCount == 0) {
						setBothHalves(world, targetPos.offset(left, 2), world.getBlockState(targetPos.offset(left, 2)), 3, EnumGlassMode.LEFT);
						setBothHalves(world, targetPos.offset(left, 1), world.getBlockState(targetPos.offset(left, 1)), 3, EnumGlassMode.MIDDLE);
						setBothHalves(world, targetPos, targetState, 3, EnumGlassMode.RIGHT);
					} else {
						setBothHalves(world, targetPos.offset(left, 1), world.getBlockState(targetPos.offset(left, 1)), 3, EnumGlassMode.LEFT);
						setBothHalves(world, targetPos, targetState, 3, EnumGlassMode.MIDDLE);
						setBothHalves(world, targetPos.offset(right, 1), world.getBlockState(targetPos.offset(right, 1)), 3, EnumGlassMode.RIGHT);
					}
				} else {
					if (leftCount >= rightCount) {
						setBothHalves(world, targetPos.offset(left, 1), world.getBlockState(targetPos.offset(left, 1)), 2, EnumGlassMode.LEFT);
						setBothHalves(world, targetPos, targetState, 2, EnumGlassMode.RIGHT);
					} else {
						setBothHalves(world, targetPos, targetState, 2, EnumGlassMode.LEFT);
						setBothHalves(world, targetPos.offset(right, 1), world.getBlockState(targetPos.offset(right, 1)), 2, EnumGlassMode.RIGHT);
					}
				}
			});
		}
		return super.onUse(state, world, pos, player, hand, hit);
	}

	private void setBothHalves(World world, BlockPos pos, BlockState state, int side, EnumGlassMode mode) {
		final BlockState newState = state
				.with(SIDE_COUNT, side)
				.with(GLASS_MODE, mode)
				.with(SIDE_EXTENDED, toSide(mode));
		world.setBlockState(pos, newState);
		final BlockPos downPos = pos.down();
		world.setBlockState(downPos, world.getBlockState(downPos)
				.with(SIDE_COUNT, side)
				.with(GLASS_MODE, mode)
				.with(SIDE_EXTENDED, toSide(mode)));
	}

	private static IBlock.EnumSide toSide(EnumGlassMode mode) {
		switch (mode) {
			case LEFT:
				return IBlock.EnumSide.LEFT;
			case MIDDLE:
				return IBlock.EnumSide.MIDDLE;
			case RIGHT:
				return IBlock.EnumSide.RIGHT;
			default:
				return IBlock.EnumSide.SINGLE;
		}
	}

	private boolean isSameGlass(World world, BlockPos pos) {
		return world.getBlockState(pos).getBlock() instanceof BlockCRTPSDGlass1;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING, HALF, SIDE_EXTENDED, SIDE_COUNT, GLASS_MODE);
	}

	@Override
	public Item asItem() {
		return Items.CRT_PSD_GLASS_1.get();
	}
}
