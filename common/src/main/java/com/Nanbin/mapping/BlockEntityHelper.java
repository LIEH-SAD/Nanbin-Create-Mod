package com.Nanbin.mapping;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.world.World;

/**
 * 方块实体的「标脏 + 同步」辅助，取代 4.0 的 {@code markDirty2()}。
 * <p>
 * 4.0 的 {@code org.mtr.mapping.mapper.BlockEntityExtension#markDirty2()} 除了 {@code markDirty()}
 * 之外，还会在服务端调用 {@code world.updateListeners(pos, state, state, 2)}，把方块实体的 NBT
 * 推送给正在观看该方块的客户端（MTR 的 {@code BlockEntityExtension#getUpdatePacket()} 返回
 * {@code ClientboundBlockEntityDataPacket}，正好被这一句触发）。
 * <p>
 * 4.1 去掉了这层包装，直接调原版 {@code markDirty()} 只会把区块标记为待保存，客户端拿不到新数据：
 * 路名文字、站牌线路、站台信息、下趟车到站时间等在编辑完成后都不会刷新（要重新进出世界才出现）。
 * <p>
 * 所以方块实体里凡是原来调 {@code markDirty2()} 的地方，都改成 {@code BlockEntityHelper.markDirty(this)}
 * （给别的方块实体标脏时传那个实体）。
 */
public final class BlockEntityHelper {

	private BlockEntityHelper() {
	}

	/** 标记存档，并在服务端把方块实体数据同步给客户端。 */
	public static void markDirty(BlockEntity blockEntity) {
		blockEntity.markDirty();
		final World world = blockEntity.getWorld();
		if (world == null || world.isClient()) {
			return;
		}
		final BlockState state = blockEntity.getCachedState();
		if (state != null) {
			world.updateListeners(blockEntity.getPos(), state, state, Block.NOTIFY_LISTENERS);
		}
	}
}
