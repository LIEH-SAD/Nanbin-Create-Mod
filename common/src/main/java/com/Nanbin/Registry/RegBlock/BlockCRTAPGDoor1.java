package com.Nanbin.Registry.RegBlock;

import com.Nanbin.entity.BlockEntityTypes;
import com.Nanbin.mapping.TranslationProvider;
import mtr.block.BlockPSDAPGDoorBase;
import mtr.block.IBlock;
import mtr.mappings.BlockEntityMapper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * CRT 第一代矮屏蔽门（APG）。门体由物品一次性放置为并排 2 格、上下 2 格；
 * 顶端（上半格）用刷子点击可切换整排门的编号方向，底端保留 MTR 原生的锁定/解锁行为。
 */
public class BlockCRTAPGDoor1 extends BlockPSDAPGDoorBase {

	/** 屏蔽门自动排序方向：true = 从右端开始编号，false = 从左端开始编号。 */
	public static final BooleanProperty SORT_FROM_RIGHT = BooleanProperty.of("sort_from_right");

	public BlockCRTAPGDoor1() {
		super();
		setDefaultState(getDefaultState().with(SORT_FROM_RIGHT, false));
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(END, FACING, HALF, SIDE, TEMP, UNLOCKED, SORT_FROM_RIGHT);
	}

	@Override
	public BlockEntityMapper createBlockEntity(BlockPos pos, BlockState state) {
		return new BlockEntity(pos, state);
	}

	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		// 顶端（上半格）：切换整排门的自动排序方向；底端（下半格）：保留原来的锁定/解锁
		if (IBlock.getStatePropertySafe(state, HALF) == DoubleBlockHalf.UPPER) {
			return IBlock.checkHoldingBrush(world, player, () -> toggleSortDirection(world, pos, state, player));
		}
		return super.onUse(state, world, pos, player, hand, hit);
	}

	/** 服务端：切换排序方向，并同步到同一排的所有门单元（上下两半 × 左右两块）。 */
	private static void toggleSortDirection(World world, BlockPos pos, BlockState state, PlayerEntity player) {
		final boolean newValue = !IBlock.getStatePropertySafe(state, SORT_FROM_RIGHT);
		final Direction axis = IBlock.getStatePropertySafe(state, FACING).rotateYClockwise();
		for (final BlockPos unitStart : APGDoorNumbering.findRowUnitStarts(world, pos)) {
			for (int x = 0; x < 2; x++) {
				final BlockPos base = unitStart.offset(axis, x);
				for (int y = 0; y < 2; y++) {
					final BlockPos p = base.up(y);
					final BlockState s = world.getBlockState(p);
					if (APGDoorNumbering.isCRTAPGDoor(s)) {
						world.setBlockState(p, s.with(SORT_FROM_RIGHT, newValue));
					}
				}
			}
		}
		player.sendMessage((newValue ? TranslationProvider.APG_DOOR_SORT_FROM_RIGHT : TranslationProvider.APG_DOOR_SORT_FROM_LEFT).getText(), true);
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		final Direction facing = IBlock.getStatePropertySafe(state, FACING);
		final boolean half = IBlock.getStatePropertySafe(state, HALF) == DoubleBlockHalf.UPPER;
		return half ? IBlock.getVoxelShapeByDirection(0, 0, 0, 16, 2, 4, facing) : IBlock.getVoxelShapeByDirection(0, 0, 0, 16, 16, 4, facing);
	}

	public static class BlockEntity extends BlockPSDAPGDoorBase.TileEntityPSDAPGDoorBase {

		public BlockEntity(BlockPos pos, BlockState state) {
			super(BlockEntityTypes.CRT_APG_DOOR_1.get(), pos, state);
		}
	}
}
