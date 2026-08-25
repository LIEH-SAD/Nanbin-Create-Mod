package com.Nanbin.Registry.RegBlock;

import com.Nanbin.Init;
import com.Nanbin.Registry.SoundEvents;
import com.Nanbin.entity.BlockEntityTypes;
import com.Nanbin.mapping.IBlockExtension;
import com.Nanbin.packet.PacketOpenBusTicketProcessorScreen;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mapping.mapper.BlockWithEntity;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mod.block.BlockTicketProcessor;
import org.mtr.mod.block.IBlock;
import org.mtr.mod.data.TicketSystem;
import org.mtr.mod.generated.lang.TranslationProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class BlockBusTicketProcessor extends BlockTicketProcessor implements BlockWithEntity {
    public static final int MODE_TWO_TAP = 0;
    public static final int MODE_FIXED_AMOUNT = 1;

    public BlockBusTicketProcessor() {
        super(false, false, false);
    }

    @Nonnull
    @Override
    public ActionResult onUse2(BlockState state, World world, BlockPos blockPos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }

        final BlockPos entityPos;
        if (IBlock.getStatePropertySafe(state, IBlock.HALF) == DoubleBlockHalf.UPPER) {
            entityPos = blockPos.down();
        } else {
            entityPos = blockPos;
        }

        final boolean[] brushHeld = {false};
        IBlockExtension.checkHoldingBrush(world, player, () -> {
            brushHeld[0] = true;
            Init.REGISTRY.sendPacketToClient(ServerPlayerEntity.cast(player),
                    new PacketOpenBusTicketProcessorScreen(entityPos, getMode(world, entityPos), getAmount(world, entityPos)));
        });

        if (brushHeld[0]) {
            return ActionResult.SUCCESS;
        }

        int mode = getMode(world, entityPos);
        if (mode == MODE_TWO_TAP) {
            TicketSystem.passThrough(world, entityPos, player, true, true,
                    SoundEvents.CRT_TICKET.get(),
                    SoundEvents.CRT_TICKET.get(),
                    SoundEvents.CRT_TICKET.get(),
                    SoundEvents.CRT_TICKET.get(),
                    SoundEvents.CRT_TICKET_ERROR.get(),
                    true, (open) -> {});
        } else {
            int amount = getAmount(world, entityPos);
            long balance = TicketSystem.getBalance(world, player);
            if (balance >= amount) {
                TicketSystem.addBalance(world, player, -amount);
                world.playSound((PlayerEntity) null, entityPos, SoundEvents.CRT_TICKET.get(), SoundCategory.BLOCKS, 1.0F, 1.0F);
                player.sendMessage(TranslationProvider.GUI_MTR_BALANCE.getText(new Object[]{String.valueOf(TicketSystem.getBalance(world, player))}), true);
            } else {
                world.playSound((PlayerEntity) null, entityPos, SoundEvents.CRT_TICKET_ERROR.get(), SoundCategory.BLOCKS, 1.0F, 1.0F);
                player.sendMessage(Text.cast(TextHelper.translatable("gui.nanbin.ticket_processer.insufficient_balance")), true);
            }
        }

        return ActionResult.SUCCESS;
    }

    private static int getMode(World world, BlockPos pos) {
        org.mtr.mapping.holder.BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity != null && blockEntity.data instanceof BlockEntity entity) {
            return entity.getMode();
        }
        return MODE_TWO_TAP;
    }

    private static int getAmount(World world, BlockPos pos) {
        org.mtr.mapping.holder.BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity != null && blockEntity.data instanceof BlockEntity entity) {
            return entity.getAmount();
        }
        return 5;
    }

    @Override
    public BlockEntityExtension createBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new BlockEntity(blockPos, blockState);
    }

    public static class BlockEntity extends BlockEntityExtension {
        private int mode = MODE_TWO_TAP;
        private int amount = 5;

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
            this.markDirty2();
        }

        @Override
        public void readCompoundTag(CompoundTag compoundTag) {
            super.readCompoundTag(compoundTag);
            mode = compoundTag.getInt("mode");
            amount = compoundTag.getInt("amount");
        }

        @Override
        public void writeCompoundTag(CompoundTag compoundTag) {
            super.writeCompoundTag(compoundTag);
            compoundTag.putInt("mode", mode);
            compoundTag.putInt("amount", amount);
        }
    }

    public void addTooltips(ItemStack stack, @Nullable BlockView world, List<MutableText> tooltip, TooltipContext options) {
        tooltip.add(com.Nanbin.mapping.TranslationProvider.BRUSH_USE.getMutableText(new Object[0]).formatted(TextFormatting.DARK_GRAY));
    }
}