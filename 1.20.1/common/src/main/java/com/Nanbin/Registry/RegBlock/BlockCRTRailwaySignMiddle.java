package com.Nanbin.Registry.RegBlock;

import mtr.block.BlockRailwaySign;
import mtr.block.IBlock;
import mtr.mappings.BlockEntityMapper;
import mtr.packet.PacketTrainDataGuiServer;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * CRT 版铁路告示牌的中间方块（对应原版 MTR 的 RAILWAY_SIGN_MIDDLE）。
 * 无方块实体，仅作为告示牌中段的填充模型；放置逻辑见 {@link BlockCRTRailwaySign#onPlaced}。
 */
public class BlockCRTRailwaySignMiddle extends BlockRailwaySign {

	public BlockCRTRailwaySignMiddle() {
		super(0, false);
	}

	@Nullable
	@Override
	public BlockEntityMapper createBlockEntity(BlockPos pos, BlockState state) {
		return null;
	}

	@Nonnull
	@Override
	public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
		final Direction facing = IBlock.getStatePropertySafe(state, FACING);
		final boolean isNext = direction == facing.rotateYClockwise() || direction == facing.rotateYCounterclockwise();
		if (isNext && !(neighborState.getBlock() instanceof BlockRailwaySign)) {
			return net.minecraft.block.Blocks.AIR.getDefaultState();
		} else {
			return state;
		}
	}

	@Override
	public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
		final Direction facing = IBlock.getStatePropertySafe(state, FACING);
		final BlockPos checkPos = BlockCRTRailwaySign.findEndWithDirectionCRT(world, pos, facing, true);
		if (checkPos != null) {
			IBlock.onBreakCreative(world, player, checkPos);
		}
		super.onBreak(world, pos, state, player);
	}

	@Nonnull
	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		return IBlock.checkHoldingBrush(world, player, () -> {
			final Direction facing = IBlock.getStatePropertySafe(state, FACING);
			final Direction hitSide = hit.getSide();
			if (hitSide == facing || hitSide == facing.getOpposite()) {
				final BlockPos checkPos = BlockCRTRailwaySign.findEndWithDirectionCRT(world, pos, hitSide.getOpposite(), false);
				if (checkPos != null && player instanceof ServerPlayerEntity serverPlayer) {
					PacketTrainDataGuiServer.openRailwaySignScreenS2C(serverPlayer, checkPos);
				}
			}
		});
	}

	@Nonnull
	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		final Direction facing = IBlock.getStatePropertySafe(state, FACING);
		return IBlock.getVoxelShapeByDirection(0, 0, 7, 16, 12, 9, facing);
	}
}