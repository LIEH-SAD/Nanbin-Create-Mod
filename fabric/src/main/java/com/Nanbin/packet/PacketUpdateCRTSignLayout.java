package com.Nanbin.packet;

import com.Nanbin.Registry.RegBlock.BlockCRTRailwaySign;
import com.Nanbin.Registry.RegBlock.BlockCRTRailwaySignWall1;
import org.mtr.mapping.holder.BlockEntity;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.MinecraftServer;
import org.mtr.mapping.holder.ServerPlayerEntity;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;

/**
 * 保存 CRT 导视牌的"自由布局"数据（网页编辑器的元素/行布局 JSON）。
 *
 * <p>数据存在方块实体 NBT 里（{@code crt_sign_layout}），随方块实体更新自动同步给其他客户端；
 * 渲染端读到后用 CRTSignLayout 渲染，读不到时回退到旧的 signIds 格子渲染，旧存档不受影响。</p>
 */
public final class PacketUpdateCRTSignLayout extends PacketHandler {

	/** 布局 JSON 长度上限（MTR 包字符串上限 32767，这里留余量；正常牌面远小于此值）。 */
	private static final int MAX_LAYOUT_JSON_LENGTH = 30000;

	private final BlockPos blockPos;
	private final String layoutJson;

	public PacketUpdateCRTSignLayout(PacketBufferReceiver packetBufferReceiver) {
		blockPos = BlockPos.fromLong(packetBufferReceiver.readLong());
		layoutJson = packetBufferReceiver.readString();
	}

	public PacketUpdateCRTSignLayout(BlockPos blockPos, String layoutJson) {
		this.blockPos = blockPos;
		this.layoutJson = layoutJson == null ? "" : layoutJson;
	}

	@Override
	public void write(PacketBufferSender sender) {
		sender.writeLong(blockPos.asLong());
		sender.writeString(layoutJson);
	}

	@Override
	public void runServer(MinecraftServer minecraftServer, ServerPlayerEntity serverPlayerEntity) {
		// 布局 JSON 会写进方块实体 NBT，服务端做一次长度兜底，避免异常大包写坏存档
		if (layoutJson.length() > MAX_LAYOUT_JSON_LENGTH) {
			org.mtr.mod.Init.LOGGER.warn("Rejected CRT sign layout at {}: JSON too long ({} chars)", blockPos.toShortString(), layoutJson.length());
			return;
		}
		final World world = serverPlayerEntity.getEntityWorld();
		if (!org.mtr.mod.Init.isChunkLoaded(world, blockPos)) {
			return;
		}
		final BlockEntity blockEntity = world.getBlockEntity(blockPos);
		if (blockEntity == null) {
			return;
		}
		boolean changed = false;
		if (blockEntity.data instanceof BlockCRTRailwaySign.BlockEntityCRTRailwaySign) {
			((BlockCRTRailwaySign.BlockEntityCRTRailwaySign) blockEntity.data).setSignLayout(layoutJson);
			changed = true;
		} else if (blockEntity.data instanceof BlockCRTRailwaySignWall1.BlockEntity) {
			((BlockCRTRailwaySignWall1.BlockEntity) blockEntity.data).setSignLayout(layoutJson);
			changed = true;
		}
		if (changed) {
			// 方块实体本身不会自动同步给其他玩家，手动广播一次让同服玩家立即看到新牌面
			final org.mtr.mapping.holder.BlockState state = world.getBlockState(blockPos);
			world.updateListeners(blockPos, state, state, 3);
		}
	}
}
