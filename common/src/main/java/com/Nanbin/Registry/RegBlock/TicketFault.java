package com.Nanbin.Registry.RegBlock;

import com.Nanbin.mapping.TranslationProvider;
import mtr.block.IBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 售检票设备的故障状态：用刷子右键即可切换。
 * 故障时设备无法正常使用，改为提示玩家设备故障；所有相关方块共用同一属性，
 * 并在方块状态中加入故障分支以使用各自独立的故障模型。
 */
public final class TicketFault {

	/** 故障状态属性，参与方块状态，因此故障时使用单独的模型。 */
	public static final BooleanProperty FAULT = BooleanProperty.of("fault");

	/** 同一玩家的故障提示冷却（游戏刻），避免持续接触设备时每刻刷屏。 */
	private static final int MESSAGE_COOLDOWN_TICKS = 40;
	private static final Map<UUID, Long> LAST_MESSAGE_TIME = new HashMap<>();

	private TicketFault() {
	}

	public static boolean isFault(BlockState state) {
		return IBlock.getStatePropertySafe(state, FAULT);
	}

	/** 切换故障状态后提示操作者当前结果。 */
	public static void notifyToggle(PlayerEntity player, boolean fault) {
		player.sendMessage((fault ? TranslationProvider.TICKET_FAULT_ON : TranslationProvider.TICKET_FAULT_OFF).getText().copy().formatted(fault ? Formatting.RED : Formatting.GREEN), true);
	}

	/** 服务端提示玩家设备故障。 */
	public static void notifyInUse(World world, PlayerEntity player) {
		final long time = world.getTime();
		final Long lastMessageTime = LAST_MESSAGE_TIME.get(player.getUuid());
		if (lastMessageTime != null && time - lastMessageTime < MESSAGE_COOLDOWN_TICKS) {
			return;
		}
		LAST_MESSAGE_TIME.put(player.getUuid(), time);
		player.sendMessage(TranslationProvider.TICKET_FAULT_IN_USE.getText().copy().formatted(Formatting.RED), true);
	}
}
