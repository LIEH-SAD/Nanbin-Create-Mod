package com.Nanbin.Registry.RegBlock.flag;

import com.Nanbin.mapping.TranslationProvider;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.BooleanProperty;
import org.mtr.mapping.holder.PlayerEntity;
import org.mtr.mapping.holder.Text;
import org.mtr.mapping.holder.TextFormatting;
import org.mtr.mapping.holder.World;
import org.mtr.mod.block.IBlock;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TicketFault {

    public static final BooleanProperty FAULT = BooleanProperty.of("fault");
    private static final int MESSAGE_COOLDOWN_TICKS = 40;
    private static final Map<UUID, Long> LAST_MESSAGE_TIME = new HashMap<>();

    private TicketFault() {
    }

    public static boolean isFault(BlockState state) {
        return IBlock.getStatePropertySafe(state, FAULT);
    }

    public static void notifyToggle(PlayerEntity player, boolean fault) {
        player.sendMessage(Text.cast((fault ? TranslationProvider.TICKET_FAULT_ON : TranslationProvider.TICKET_FAULT_OFF).getMutableText(new Object[0]).formatted(fault ? TextFormatting.RED : TextFormatting.GREEN)), true);
    }

    // 服务端提示玩家设备故障
    public static void notifyInUse(World world, PlayerEntity player) {
        final long time = world.getTime();
        final Long lastMessageTime = LAST_MESSAGE_TIME.get(player.getUuid());
        if (lastMessageTime != null && time - lastMessageTime < MESSAGE_COOLDOWN_TICKS) {
            return;
        }
        LAST_MESSAGE_TIME.put(player.getUuid(), time);
        player.sendMessage(Text.cast(TranslationProvider.TICKET_FAULT_IN_USE.getMutableText(new Object[0]).formatted(TextFormatting.RED)), true);
    }
}
