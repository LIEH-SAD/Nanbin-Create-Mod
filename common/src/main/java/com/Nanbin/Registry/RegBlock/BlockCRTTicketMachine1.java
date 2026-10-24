package com.Nanbin.Registry.RegBlock;

import com.Nanbin.mapping.TicketMachineHelper;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.mtr.block.BlockDirectionalDoubleBlockBase;
import org.mtr.block.IBlock;
import org.mtr.registry.Items;

import java.util.List;

/**
 * CRT 第一代售票机。刷子右键切换上下两格一体的故障状态；故障期间停止售票。
 */
public class BlockCRTTicketMachine1 extends BlockDirectionalDoubleBlockBase {

	public BlockCRTTicketMachine1(AbstractBlock.Settings settings) {
		super(settings.nonOpaque().luminance(state -> 5));
	}

	@Override
	protected void appendProperties(StateManager.Builder<net.minecraft.block.Block, BlockState> builder) {
		builder.add(Properties.HORIZONTAL_FACING, IBlock.HALF, TicketFault.FAULT);
	}

	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (player.isHolding(Items.BRUSH.get())) {
			return IBlock.checkHoldingBrush(world, player, () -> toggleFault(world, pos, state, player));
		}
		if (TicketFault.isFault(state)) {
			if (!world.isClient) {
				TicketFault.notifyInUse(world, player);
			}
			return ActionResult.SUCCESS;
		}
		if (!world.isClient) {
			TicketMachineHelper.openTicketMachineScreen(world, player);
		}
		return ActionResult.SUCCESS;
	}

	/** 服务端：上下两格一并切换故障状态，故障期间售票机停止服务。 */
	private static void toggleFault(World world, BlockPos pos, BlockState state, PlayerEntity player) {
		final boolean fault = !TicketFault.isFault(state);
		world.setBlockState(pos, state.with(TicketFault.FAULT, fault), 3);
		final BlockPos otherPos = pos.offset(IBlock.getStatePropertySafe(state, IBlock.HALF) == IBlock.DoubleBlockHalf.UPPER ? Direction.DOWN : Direction.UP);
		final BlockState otherState = world.getBlockState(otherPos);
		if (otherState.isOf(state.getBlock())) {
			world.setBlockState(otherPos, otherState.with(TicketFault.FAULT, fault), 3);
		}
		TicketFault.notifyToggle(player, fault);
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		final Direction facing = IBlock.getStatePropertySafe(state, Properties.HORIZONTAL_FACING);
		return IBlock.getVoxelShapeByDirection(2, 0, 0, 14, 16, 11, facing);
	}

	/** 供外部读取属性列表（保持与 4.0 一致的调用点）。 */
	public void addBlockProperties(List<Property<?>> properties) {
		properties.add(Properties.HORIZONTAL_FACING);
		properties.add(IBlock.HALF);
		properties.add(TicketFault.FAULT);
	}
}
