package com.Nanbin.Registry.RegBlock;

import com.Nanbin.mapping.IBlockExtension.EnumGlassMode;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.mtr.block.BlockPSDAPGGlassBase;
import org.mtr.block.IBlock;

import java.util.List;

import static com.Nanbin.mapping.IBlockExtension.GLASS_MODE;

/**
 * CRT 屏蔽门玻璃（一代）：用刷子右键可按「1/2/3 格一组」循环组合玻璃。
 * <p>
 * 4.1 适配：{@code IntegerProperty} → 原版 {@link IntProperty}；{@code onUse2} → 直接覆写
 * MTR {@link BlockPSDAPGGlassBase} 的 {@code onUse}；{@code getYMapped()} → {@code getY()}；
 * {@code FACING} 用原版 {@code Properties.HORIZONTAL_FACING}。
 */
public class BlockCRTAPGGlass1 extends BlockPSDAPGGlassBase {

	/** 一组玻璃的格数（1-3）。 */
	public static final IntProperty SIDE_COUNT = IntProperty.of("side_count", 1, 3);

	public BlockCRTAPGGlass1(net.minecraft.block.AbstractBlock.Settings settings) {
		super(settings);
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		final double y = hit.getPos().getY();
		if (y - Math.floor(y) > 0.21875) {
			final boolean isUpper = IBlock.getStatePropertySafe(state, IBlock.HALF) == IBlock.DoubleBlockHalf.UPPER;
			final BlockPos targetPos = isUpper ? pos : pos.up();
			final BlockState targetState = isUpper ? state : world.getBlockState(targetPos);

			return IBlock.checkHoldingBrush(world, player, () -> {
				final Direction facing = IBlock.getStatePropertySafe(targetState, Properties.HORIZONTAL_FACING);
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
					} else if (leftCount == 2) {
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
		return super.onUse(state, world, pos, player, hit);
	}

	private void setBothHalves(World world, BlockPos pos, BlockState state, int side, EnumGlassMode mode) {
		world.setBlockState(pos, state.with(SIDE_COUNT, side).with(GLASS_MODE, mode));
		final BlockPos downPos = pos.down();
		world.setBlockState(downPos, world.getBlockState(downPos).with(SIDE_COUNT, side).with(GLASS_MODE, mode));
	}

	/** 同一组玻璃的判定：由子类指定自己的方块类型。 */
	protected boolean isSameGlass(World world, BlockPos pos) {
		return world.getBlockState(pos).getBlock() instanceof BlockCRTAPGGlass1;
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		final Direction facing = IBlock.getStatePropertySafe(state, Properties.HORIZONTAL_FACING);
		final boolean upper = IBlock.getStatePropertySafe(state, IBlock.HALF) == IBlock.DoubleBlockHalf.UPPER;
		return upper
				? IBlock.getVoxelShapeByDirection(0, 0, 0, 16, 2, 4, facing)
				: IBlock.getVoxelShapeByDirection(0, 0, 0, 16, 16, 4, facing);
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		final Direction facing = IBlock.getStatePropertySafe(state, Properties.HORIZONTAL_FACING);
		final boolean upper = IBlock.getStatePropertySafe(state, IBlock.HALF) == IBlock.DoubleBlockHalf.UPPER;
		return upper
				? IBlock.getVoxelShapeByDirection(0, 0, 0, 16, 8, 4, facing)
				: IBlock.getVoxelShapeByDirection(0, 0, 0, 16, 16, 4, facing);
	}

	@Override
	protected void appendProperties(net.minecraft.state.StateManager.Builder<net.minecraft.block.Block, BlockState> builder) {
		super.appendProperties(builder);
		builder.add(SIDE_COUNT, GLASS_MODE);
	}

	/** 供外部读取属性列表（保持与 4.0 一致的调用点）。 */
	public void addBlockProperties(List<Property<?>> properties) {
		properties.add(Properties.HORIZONTAL_FACING);
		properties.add(IBlock.HALF);
		properties.add(IBlock.SIDE_EXTENDED);
		properties.add(SIDE_COUNT);
		properties.add(GLASS_MODE);
	}
}
