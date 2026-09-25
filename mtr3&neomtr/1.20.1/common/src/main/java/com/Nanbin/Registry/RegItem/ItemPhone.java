package com.Nanbin.Registry.RegItem;

import com.Nanbin.mapping.Registry;
import com.Nanbin.packet.PacketHandler;
import mtr.data.TicketSystem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.ScoreboardPlayerScore;
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
            TicketSystem.addObjectivesIfMissing(world);
            final ScoreboardPlayerScore score = TicketSystem.getPlayerScore(world, player, TicketSystem.BALANCE_OBJECTIVE);
            final int balance = score == null ? 0 : score.getScore();
            Registry.sendPacketToClient((ServerPlayerEntity) player, PacketHandler.PACKET_OPEN_TICKET_MENU, buf -> buf.writeInt(balance));
        }
        return super.use(world, player, interactionHand);
    }
}
