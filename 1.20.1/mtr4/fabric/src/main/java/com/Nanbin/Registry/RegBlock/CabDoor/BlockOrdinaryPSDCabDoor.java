package com.Nanbin.Registry.RegBlock.CabDoor;

import net.minecraft.block.BlockSetType;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.mtr.mapping.holder.BlockView;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.holder.TextFormatting;
import org.mtr.mapping.holder.TooltipContext;
import org.mtr.mapping.mapper.TextHelper;

import java.util.List;

import static org.mtr.mod.Items.*;

public class BlockOrdinaryPSDCabDoor extends DoorBlock {

    public BlockOrdinaryPSDCabDoor(Settings settings) {
        super(settings, BlockSetType.OAK);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }

        ItemStack handStack = player.getStackInHand(hand);
        Item handItem = handStack.getItem();

        if (handItem == CREATIVE_DRIVER_KEY.get().data || handItem == ADVANCED_DRIVER_KEY.get().data) {
            BlockState newState = state.cycle(OPEN);
            world.setBlockState(pos, newState, 10);

            if (newState.get(OPEN)) {
                player.sendMessage(Text.translatable("tips.cabdoor.open"), true);
            } else {
                player.sendMessage(Text.translatable("tips.cabdoor.close"), true);
            }
            return ActionResult.SUCCESS;
        }

        if (handItem == GUARD_KEY.get().data || handItem == BASIC_DRIVER_KEY.get().data) {
            player.sendMessage(Text.translatable("tips.cabdoor.has.low"), true);
            return ActionResult.FAIL;
        }

        player.sendMessage(Text.translatable("tips.cabdoor.has.nokey"), true);
        return ActionResult.FAIL;
    }

    public void addTooltips(org.mtr.mapping.holder.ItemStack stack, @Nullable BlockView world, List<MutableText> tooltip, TooltipContext options) {
        tooltip.add(TextHelper.translatable("tooltip.block.cab_door", new Object[0]).formatted(TextFormatting.GRAY));
    }
}