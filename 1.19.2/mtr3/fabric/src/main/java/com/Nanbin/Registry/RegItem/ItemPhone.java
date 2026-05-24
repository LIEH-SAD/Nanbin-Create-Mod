package com.Nanbin.Registry.RegItem;

import mtr.packet.PacketTrainDataGuiServer;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ItemPhone extends Item{
    public ItemPhone(Item.Settings settings) {
        super(settings);
    }

    public ActionResult use(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand interactionHand, BlockHitResult blockHitResult) {
        if (!world.isClient) {
            PacketTrainDataGuiServer.openTicketMachineScreenS2C(world, (ServerPlayerEntity)player);
        }
        return ActionResult.SUCCESS;
    }
}
