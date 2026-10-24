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
 * CRT 第二代售票机：外观与一代不同，行为一致。
 */
public class BlockCRTTicketMachine2 extends BlockDirectionalDoubleBlockBase {

	public BlockCRTTicketMachine2(AbstractBlock.Settings settings) {
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

	public void addBlockProperties(List<Property<?>> properties) {
		properties.add(Properties.HORIZONTAL_FACING);
		properties.add(IBlock.HALF);
		properties.add(TicketFault.FAULT);
	}
}
