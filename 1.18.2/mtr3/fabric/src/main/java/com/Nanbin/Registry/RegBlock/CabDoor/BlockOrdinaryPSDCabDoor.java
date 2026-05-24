package com.Nanbin.Registry.RegBlock.CabDoor;

import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockOrdinaryPSDCabDoor extends DoorBlock {
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        boolean hasKey = player.getMainHandStack().isOf(mtr.Items.DRIVER_KEY.get()) || player.getOffHandStack().isOf(mtr.Items.DRIVER_KEY.get());
        if (!hasKey) {
            player.sendMessage(new TranslatableText("tips.cabdoor.has.nokey"), true);
            return ActionResult.FAIL;
        }
        if (!world.isClient) {
            state = state.cycle(OPEN);
            world.setBlockState(pos, state, 10);
        }
        player.sendMessage(new TranslatableText("tips.cabdoor.open"), true);
        return ActionResult.SUCCESS;
    }

    public BlockOrdinaryPSDCabDoor(Settings settings) {
        super(settings);
    }
}