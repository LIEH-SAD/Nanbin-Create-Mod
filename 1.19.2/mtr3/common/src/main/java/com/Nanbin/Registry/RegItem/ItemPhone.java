package com.Nanbin.Registry.RegItem;

import mtr.packet.PacketTrainDataGuiServer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class ItemPhone extends Item {
    public ItemPhone(Settings settings) {
        super(settings);
    }

    public TypedActionResult<ItemStack> use (World world, PlayerEntity player, Hand interactionHand) {
        if (!world.isClient()) {
            PacketTrainDataGuiServer.openTicketMachineScreenS2C(world, (ServerPlayerEntity)player);
        }
        return super.use(world, player, interactionHand);
    }
}
