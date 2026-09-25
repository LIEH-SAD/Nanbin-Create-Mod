package com.Nanbin.Registry.RegBlock;

import mtr.block.BlockPSDAPGGlassBase;
import mtr.block.IBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
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
 * CRT 第一代矮屏蔽门玻璃（APG 玻璃）。用刷子点击玻璃上部可按 1 / 2 / 3 联动
 * 重新划分一组玻璃（单块 / 左右两块 / 左中右三块），并同步上下两半。
 */
public class BlockCRTAPGGlass1 extends BlockPSDAPGGlassBase {

	/** 联动玻璃组内的块数：1-3。 */
	public static final IntProperty SIDE_COUNT = IntProperty.of("side_count", 1, 3);

	public BlockCRTAPGGlass1() {
		super();
		setDefaultState(getDefaultState().with(SIDE_COUNT, 1).with(GLASS_MODE, EnumGlassMode.SINGLE));
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING, HALF, SIDE_EXTENDED, SIDE_COUNT, GLASS_MODE);
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
		world.setBlockState(pos, state.with(SIDE_COUNT, side).with(GLASS_MODE, mode));
		final BlockPos downPos = pos.down();
		world.setBlockState(downPos, world.getBlockState(downPos).with(SIDE_COUNT, side).with(GLASS_MODE, mode));
	}

	private boolean isSameGlass(World world, BlockPos pos) {
		return world.getBlockState(pos).getBlock() instanceof BlockCRTAPGGlass1;
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		final Direction facing = IBlock.getStatePropertySafe(state, FACING);
		final boolean half = IBlock.getStatePropertySafe(state, HALF) == DoubleBlockHalf.UPPER;
		return half ? IBlock.getVoxelShapeByDirection(0, 0, 0, 16, 2, 4, facing) : IBlock.getVoxelShapeByDirection(0, 0, 0, 16, 16, 4, facing);
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		final Direction facing = IBlock.getStatePropertySafe(state, FACING);
		final boolean half = IBlock.getStatePropertySafe(state, HALF) == DoubleBlockHalf.UPPER;
		return half ? IBlock.getVoxelShapeByDirection(0, 0, 0, 16, 8, 4, facing) : IBlock.getVoxelShapeByDirection(0, 0, 0, 16, 16, 4, facing);
	}
}
