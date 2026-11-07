package com.Nanbin.Registry.RegBlock.flag;

import mtr.block.BlockPSDAPGGlassEndBase;
import mtr.block.IBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

import static mtr.block.BlockPSDAPGDoorBase.END;
import static mtr.block.BlockPSDAPGDoorBase.UNLOCKED;
import static mtr.block.IBlock.HALF;

/**
 * 单开屏蔽门（一扇门只占 1 格宽）的共用行为：端部玻璃自动识别、刷子整列锁定/解锁、破坏时连带清除。
 *
 * <p>来源用 MTR 4 的 {@code org.mtr.mod.generated.lang.TranslationProvider}；MTR 3.6.3 没有生成的
 * lang 类，直接使用原版 {@code Text.translatable} 的 MTR 语言键（文案本身一致）。</p>
 */
public interface BlockFlagDoorSingle {

	boolean isLeft();

	static BlockState getStateForNeighborUpdate(BlockState state, WorldAccess world, BlockPos pos) {
		final boolean end = world.getBlockState(pos.offset(IBlock.getSideDirection(state).getOpposite())).getBlock() instanceof BlockPSDAPGGlassEndBase;
		return state.with(END, end);
	}

	static ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player) {
		return IBlock.checkHoldingBrush(world, player, () -> {
			final boolean unlocked = IBlock.getStatePropertySafe(state, UNLOCKED);
			for (int y = -1; y <= 1; y++) {
				final BlockState scanState = world.getBlockState(pos.up(y));
				if (state.isOf(scanState.getBlock())) {
					world.setBlockState(pos.up(y), scanState.with(UNLOCKED, !unlocked));
				}
			}
			player.sendMessage(mtr.mappings.Text.translatable(unlocked ? "gui.mtr.psd_apg_door_locked" : "gui.mtr.psd_apg_door_unlocked"), true);
		});
	}

	static void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
		final BlockPos offsetPos = IBlock.getStatePropertySafe(state, HALF) == DoubleBlockHalf.UPPER ? pos.down() : pos.up();
		IBlock.onBreakCreative(world, player, offsetPos);
		world.breakBlock(pos, false, player);
	}
}
