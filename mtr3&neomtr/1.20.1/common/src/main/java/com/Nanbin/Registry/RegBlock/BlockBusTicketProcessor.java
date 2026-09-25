package com.Nanbin.Registry.RegBlock;

import com.Nanbin.Registry.SoundEvents;
import com.Nanbin.entity.BlockEntityTypes;
import com.Nanbin.mapping.Registry;
import com.Nanbin.mapping.TranslationProvider;
import com.Nanbin.packet.PacketHandler;
import mtr.Items;
import mtr.block.BlockTicketProcessor;
import mtr.block.IBlock;
import mtr.data.TicketSystem;
import mtr.mappings.BlockEntityMapper;
import mtr.mappings.EntityBlockMapper;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.scoreboard.ScoreboardPlayerScore;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 公交计费器：可以当普通道闸用（两次刷卡模式），也可以作为固定金额扣费器（一次售票模式）。
 * 用刷子右键打开设置界面切换模式与金额。
 */
public class BlockBusTicketProcessor extends BlockTicketProcessor implements EntityBlockMapper {

	public static final int MODE_TWO_TAP = 0;
	public static final int MODE_FIXED_AMOUNT = 1;
	public static final int DEFAULT_AMOUNT = 5;

	public BlockBusTicketProcessor() {
		super(false, false, false);
	}

	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		if (world.isClient) {
			return ActionResult.SUCCESS;
		}

		// 方块为上下两格，方块实体位于下半格
		final BlockPos entityPos = IBlock.getStatePropertySafe(state, IBlock.HALF) == DoubleBlockHalf.UPPER ? pos.down() : pos;

		if (player.isHolding(Items.BRUSH.get())) {
			Registry.sendPacketToClient((ServerPlayerEntity) player, PacketHandler.PACKET_OPEN_BUS_TICKET_PROCESSOR, buf -> {
				buf.writeBlockPos(entityPos);
				buf.writeInt(getMode(world, entityPos));
				buf.writeInt(getAmount(world, entityPos));
			});
			return ActionResult.SUCCESS;
		}

		if (getMode(world, entityPos) == MODE_TWO_TAP) {
			TicketSystem.passThrough(world, entityPos, player, true, true,
					SoundEvents.CRT_TICKET.get(), SoundEvents.CRT_TICKET.get(), SoundEvents.CRT_TICKET.get(), SoundEvents.CRT_TICKET.get(), SoundEvents.CRT_TICKET_ERROR.get(), true);
		} else {
			final int amount = getAmount(world, entityPos);
			if (getBalance(world, player) >= amount) {
				final ScoreboardPlayerScore score = getPlayerScore(world, player);
				if (score != null) {
					score.setScore(score.getScore() - amount);
				}
				world.playSound(null, entityPos, SoundEvents.CRT_TICKET.get(), SoundCategory.BLOCKS, 1.0F, 1.0F);
				player.sendMessage(Text.translatable("gui.mtr.balance", getBalance(world, player)), true);
			} else {
				world.playSound(null, entityPos, SoundEvents.CRT_TICKET_ERROR.get(), SoundCategory.BLOCKS, 1.0F, 1.0F);
				player.sendMessage(Text.translatable("gui.nanbin.ticket_processer.insufficient_balance"), true);
			}
		}

		return ActionResult.SUCCESS;
	}

	public static int getMode(World world, BlockPos pos) {
		return world.getBlockEntity(pos) instanceof BlockEntity entity ? entity.getMode() : MODE_TWO_TAP;
	}

	public static int getAmount(World world, BlockPos pos) {
		return world.getBlockEntity(pos) instanceof BlockEntity entity ? entity.getAmount() : DEFAULT_AMOUNT;
	}

	private static ScoreboardPlayerScore getPlayerScore(World world, PlayerEntity player) {
		TicketSystem.addObjectivesIfMissing(world);
		return TicketSystem.getPlayerScore(world, player, TicketSystem.BALANCE_OBJECTIVE);
	}

	private static long getBalance(World world, PlayerEntity player) {
		final ScoreboardPlayerScore score = getPlayerScore(world, player);
		return score == null ? 0 : score.getScore();
	}

	@Override
	public BlockEntityMapper createBlockEntity(BlockPos pos, BlockState state) {
		return new BlockEntity(pos, state);
	}

	@Override
	public void appendTooltip(ItemStack stack, @Nullable BlockView world, List<Text> tooltip, TooltipContext options) {
		tooltip.add(TranslationProvider.BRUSH_USE.getText().copy().formatted(Formatting.DARK_GRAY));
	}

	/** 方块实体：保存计费模式与固定金额。 */
	public static class BlockEntity extends BlockEntityMapper {

		private static final String KEY_MODE = "mode";
		private static final String KEY_AMOUNT = "amount";

		private int mode = MODE_TWO_TAP;
		private int amount = DEFAULT_AMOUNT;

		public BlockEntity(BlockPos pos, BlockState state) {
			super(BlockEntityTypes.BUS_TICKET_PROCESSOR.get(), pos, state);
		}

		public int getMode() {
			return mode;
		}

		public int getAmount() {
			return amount;
		}

		public void setData(int mode, int amount) {
			this.mode = mode;
			this.amount = amount;
			markDirty();
		}

		@Override
		public void readCompoundTag(NbtCompound nbt) {
			super.readCompoundTag(nbt);
			mode = nbt.getInt(KEY_MODE);
			amount = nbt.getInt(KEY_AMOUNT);
		}

		@Override
		public void writeCompoundTag(NbtCompound nbt) {
			super.writeCompoundTag(nbt);
			nbt.putInt(KEY_MODE, mode);
			nbt.putInt(KEY_AMOUNT, amount);
		}
	}
}
