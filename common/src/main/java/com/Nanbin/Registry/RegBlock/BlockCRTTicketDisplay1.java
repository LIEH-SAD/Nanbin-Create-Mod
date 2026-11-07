package com.Nanbin.Registry.RegBlock;

import mtr.block.IBlock;
import mtr.data.TicketSystem.EnumTicketBarrierOpen;
import mtr.mappings.BlockDirectionalMapper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Property;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * CRT 售票机显示屏：安装在售票设备上方，自动向下扫描最近的一组闸机，
 * 按闸机状态（关 / 开 / 故障）切换显示内容与朝向。数据全部来自方块状态，无方块实体。
 */
public class BlockCRTTicketDisplay1 extends BlockDirectionalMapper {

	private static final int MAX_SCAN_DISTANCE = 6;
	private static final int REFRESH_INTERVAL = 4;

	public static final EnumProperty<EnumDisplayState> DISPLAY = EnumProperty.of("display", EnumDisplayState.class);

	public BlockCRTTicketDisplay1(Settings settings) {
		super(settings);
		setDefaultState(getDefaultState().with(DISPLAY, EnumDisplayState.CLOSED));
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		final BarrierInfo barrier = findBarrier(ctx.getWorld(), ctx.getBlockPos());
		final Direction facing = barrier == null ? ctx.getHorizontalPlayerFacing() : barrier.facing;
		final EnumDisplayState display = barrier == null ? EnumDisplayState.CLOSED : barrier.display;
		return getDefaultState()
				.with(FACING, facing)
				.with(DISPLAY, display);
	}

	@Override
	public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean moved) {
		if (!world.isClient()) {
			scheduleRefresh(world, pos);
		}
	}

	/** MTR 的 BlockMapper 把原版 scheduledTick 转发到这里。 */
	@Override
	public void tick(BlockState state, ServerWorld world, BlockPos pos) {
		refresh(world, pos, state);
		scheduleRefresh(world, pos);
	}

	@Override
	public void neighborUpdate(BlockState state, World world, BlockPos pos, Block block, BlockPos sourcePos, boolean notify) {
		if (!world.isClient()) {
			refresh(world, pos, state);
			scheduleRefresh(world, pos);
		}
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return getDisplayShape(state);
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return getDisplayShape(state);
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING, DISPLAY);
	}

	private static VoxelShape getDisplayShape(BlockState state) {
		return IBlock.getVoxelShapeByDirection(0, 4, 6, 16, 16, 10, IBlock.getStatePropertySafe(state, FACING));
	}

	private void refresh(World world, BlockPos pos, BlockState state) {
		final BarrierInfo barrier = findBarrier(world, pos);
		final BlockState newState = barrier == null
				? state.with(DISPLAY, EnumDisplayState.CLOSED)
				: state.with(FACING, barrier.facing)
				.with(DISPLAY, barrier.display);
		if (!newState.equals(state)) {
			world.setBlockState(pos, newState, 3);
		}
	}

	private void scheduleRefresh(World world, BlockPos pos) {
		world.scheduleBlockTick(pos, this, REFRESH_INTERVAL);
	}

	private static BarrierInfo findBarrier(World world, BlockPos pos) {
		for (int distance = 1; distance <= MAX_SCAN_DISTANCE; distance++) {
			final BlockState barrierState = world.getBlockState(pos.down(distance));
			final EnumDisplayState display = getDisplayState(barrierState);
			if (display != null) {
				return new BarrierInfo(IBlock.getStatePropertySafe(barrierState, FACING), display);
			}
		}
		return null;
	}

	private static EnumDisplayState getDisplayState(BlockState state) {
		final Block block = state.getBlock();
		final EnumProperty<EnumTicketBarrierOpen> openProperty;

		if (block instanceof BlockCRTTicketBarrier1) {
			openProperty = BlockCRTTicketBarrier1.OPEN;
		} else if (block instanceof BlockCRTTicketBarrier2) {
			openProperty = BlockCRTTicketBarrier2.OPEN;
		} else if (block instanceof BlockCRTTicketBarrier3) {
			openProperty = BlockCRTTicketBarrier3.OPEN;
		} else if (block instanceof BlockNRTTicketBarrier1) {
			openProperty = BlockNRTTicketBarrier1.OPEN;
		} else {
			return null;
		}

		if (TicketFault.isFault(state)) {
			return EnumDisplayState.FAULT;
		}

		final EnumTicketBarrierOpen open = IBlock.getStatePropertySafe(state, openProperty);
		return open == EnumTicketBarrierOpen.OPEN || open == EnumTicketBarrierOpen.OPEN_CONCESSIONARY
				? EnumDisplayState.OPEN
				: EnumDisplayState.CLOSED;
	}

	private static final class BarrierInfo {
		private final Direction facing;
		private final EnumDisplayState display;

		private BarrierInfo(Direction facing, EnumDisplayState display) {
			this.facing = facing;
			this.display = display;
		}
	}

	public enum EnumDisplayState implements StringIdentifiable {
		CLOSED("closed"),
		OPEN("open"),
		FAULT("fault");

		private final String name;

		EnumDisplayState(String name) {
			this.name = name;
		}

		@Override
		public String asString() {
			return name;
		}
	}
}
