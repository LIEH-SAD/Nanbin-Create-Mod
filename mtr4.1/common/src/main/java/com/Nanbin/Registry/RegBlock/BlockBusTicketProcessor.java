package com.Nanbin.Registry.RegBlock;

import com.Nanbin.Registry.SoundEvents;
import com.Nanbin.entity.BlockEntityTypes;
import com.Nanbin.mapping.BlockEntityHelper;
import com.Nanbin.mapping.IBlockExtension;
import com.Nanbin.mapping.Registry;
import com.Nanbin.mapping.TranslationProvider;
import com.Nanbin.packet.PacketOpenBusTicketProcessorScreen;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.mtr.block.BlockTicketProcessor;
import org.mtr.block.IBlock;
import org.mtr.data.TicketSystem;

import java.util.List;

/**
 * 公交售票机：两种模式（两次刷卡 / 固定金额扣费）。
 * <p>
 * 4.1 适配：{@code BlockWithEntity} → {@link BlockEntityProvider}；
 * {@code Init.REGISTRY.sendPacketToClient} → {@code Registry.sendPacketToClient}；
 * {@code readCompoundTag}/{@code writeCompoundTag} → {@code readNbt}/{@code writeNbt}；
 * {@code playSound} 用原版签名（4.1 的 passThrough 需要 5 个音效参数）。
 */
public class BlockBusTicketProcessor extends BlockTicketProcessor implements BlockEntityProvider {

	public static final int MODE_TWO_TAP = 0;
	public static final int MODE_FIXED_AMOUNT = 1;
	public static final int DEFAULT_AMOUNT = 5;

	public BlockBusTicketProcessor(net.minecraft.block.AbstractBlock.Settings settings) {
		super(settings, false, false, false);
	}

	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos blockPos, PlayerEntity player, BlockHitResult hit) {
		if (world.isClient) {
			return ActionResult.SUCCESS;
		}

		final BlockPos entityPos = IBlock.getStatePropertySafe(state, IBlock.HALF) == IBlock.DoubleBlockHalf.UPPER ? blockPos.down() : blockPos;

		// 手持刷子：打开配置界面
		final boolean[] brushHeld = {false};
		IBlockExtension.checkHoldingBrush(world, player, () -> {
			brushHeld[0] = true;
			Registry.sendPacketToClient((ServerPlayerEntity) player,
					new PacketOpenBusTicketProcessorScreen(entityPos, getMode(world, entityPos), getAmount(world, entityPos)));
		});
		if (brushHeld[0]) {
			return ActionResult.SUCCESS;
		}

		final int mode = getMode(world, entityPos);
		if (mode == MODE_TWO_TAP) {
			final var ticketSound = SoundEvents.CRT_TICKET.get();
			TicketSystem.passThrough(world, entityPos, player, true, true,
					ticketSound, ticketSound, ticketSound, ticketSound, SoundEvents.CRT_TICKET_ERROR.get(), true, open -> {
					});
		} else {
			final int amount = getAmount(world, entityPos);
			if (TicketSystem.getBalance(world, player) >= amount) {
				TicketSystem.addBalance(world, player, -amount);
				world.playSound(null, entityPos, SoundEvents.CRT_TICKET.get(), SoundCategory.BLOCKS, 1.0F, 1.0F);
				player.sendMessage(org.mtr.generated.lang.TranslationProvider.GUI_MTR_BALANCE.getText(String.valueOf(TicketSystem.getBalance(world, player))), true);
			} else {
				world.playSound(null, entityPos, SoundEvents.CRT_TICKET_ERROR.get(), SoundCategory.BLOCKS, 1.0F, 1.0F);
				player.sendMessage(Text.translatable("gui.nanbin.ticket_processer.insufficient_balance"), true);
			}
		}

		return ActionResult.SUCCESS;
	}

	private static int getMode(World world, BlockPos pos) {
		final net.minecraft.block.entity.BlockEntity blockEntity = world.getBlockEntity(pos);
		return blockEntity instanceof BlockEntity entity ? entity.getMode() : MODE_TWO_TAP;
	}

	private static int getAmount(World world, BlockPos pos) {
		final net.minecraft.block.entity.BlockEntity blockEntity = world.getBlockEntity(pos);
		return blockEntity instanceof BlockEntity entity ? entity.getAmount() : DEFAULT_AMOUNT;
	}

	@Override
	public net.minecraft.block.entity.BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new BlockEntity(pos, state);
	}

	@Override
	public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(TranslationProvider.BRUSH_USE.getMutableText().formatted(Formatting.DARK_GRAY));
	}

	/** MTR 的余额提示文案已改用 generated TranslationProvider，这里不再需要包装。 */

	public static class BlockEntity extends org.mtr.block.BlockEntityExtension {

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
			BlockEntityHelper.markDirty(this);
		}

		@Override
		protected void readNbt(NbtCompound nbt) {
			super.readNbt(nbt);
			mode = nbt.getInt("mode");
			amount = nbt.getInt("amount");
		}

		@Override
		protected void writeNbt(NbtCompound nbt) {
			super.writeNbt(nbt);
			nbt.putInt("mode", mode);
			nbt.putInt("amount", amount);
		}
	}
}
