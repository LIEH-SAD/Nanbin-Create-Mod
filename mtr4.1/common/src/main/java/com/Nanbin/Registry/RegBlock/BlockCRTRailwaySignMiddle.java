package com.Nanbin.Registry.RegBlock;

import com.Nanbin.mapping.Registry;
import com.Nanbin.packet.PacketOpenBlockEntityScreen;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.mtr.block.BlockRailwaySign;
import org.mtr.block.IBlock;

/**
 * CRT 版铁路告示牌的中间方块（对应原版 MTR 的 RAILWAY_SIGN_MIDDLE，但 CRT 有 4 种贴图变体）。
 * 无方块实体，仅作为告示牌中段的填充模型；放置逻辑见 {@link BlockCRTRailwaySign#onPlaced}。
 * <p>
 * 4.1 适配：{@code BlockWithEntity.createBlockEntity} → 原版 {@link net.minecraft.block.BlockEntityProvider#createBlockEntity}；
 * {@code getStateForNeighborUpdate2} 因 1.21.1 / 1.21.4 签名不同而不再覆写，整签消失改由
 * {@link BlockCRTRailwaySign#breakWholeSign} 在被破坏时主动清理；MTR 父类自带的
 * 「顺时针邻居消失就变空气」仍然生效，只是它只覆盖一侧。
 */
public class BlockCRTRailwaySignMiddle extends BlockRailwaySign {

	public BlockCRTRailwaySignMiddle(net.minecraft.block.AbstractBlock.Settings settings) {
		super(settings, 0, false);
	}

	/** MTR 原版中间方块（{@code org.mtr.registry.Blocks.RAILWAY_SIGN_MIDDLE}）。 */
	static boolean isMtrMiddle(BlockState state) {
		return state.isOf(org.mtr.registry.Blocks.RAILWAY_SIGN_MIDDLE.get());
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return null;
	}

	/**
	 * 中间格被破坏时同样把整签清掉。
	 * <p>
	 * 4.0 靠两样东西：{@code getStateForNeighborUpdate2} 的双向判断（本移植无法共用，签名不同），
	 * 以及 {@code IBlock.onBreakCreative}。后者在生存模式直接 return（只有创造模式才真正清方块），
	 * 所以从中间格破坏时另一侧会残留。这里统一改成 {@link BlockCRTRailwaySign#breakWholeSign}。
	 */
	@Override
	public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
		BlockCRTRailwaySign.breakWholeSign(world, pos);
		return super.onBreak(world, pos, state, player);
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		return IBlock.checkHoldingBrush(world, player, () -> {
			final Direction facing = IBlock.getStatePropertySafe(state, Properties.HORIZONTAL_FACING);
			final Direction hitSide = hit.getSide();
			if (hitSide == facing || hitSide == facing.getOpposite()) {
				final BlockPos checkPos = BlockCRTRailwaySign.findEndWithDirectionCRT(world, pos, hitSide.getOpposite(), false);
				if (checkPos != null) {
					Registry.sendPacketToClient((ServerPlayerEntity) player, new PacketOpenBlockEntityScreen(checkPos));
				}
			}
		});
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		final Direction facing = IBlock.getStatePropertySafe(state, Properties.HORIZONTAL_FACING);
		return IBlock.getVoxelShapeByDirection(0, 0, 7, 16, 12, 9, facing);
	}
}
