package com.Nanbin.mapping;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import org.mtr.mapping.holder.BlockSettings;

public final class BlockSettingsFactory {

	private BlockSettingsFactory() {
	}

	public static BlockSettings createPushOnly() {
		return new BlockSettings(BlockBehaviour.Properties.of().pushReaction(PushReaction.PUSH_ONLY));
	}

	public static BlockSettings createBurnableNonOpaqueSolid() {
		return new BlockSettings(BlockBehaviour.Properties.of().ignitedByLava().noOcclusion().forceSolidOn());
	}
}
