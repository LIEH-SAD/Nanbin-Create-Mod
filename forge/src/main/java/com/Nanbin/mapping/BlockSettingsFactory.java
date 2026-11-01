package com.Nanbin.mapping;

// pushReaction/ignitedByLava and the no-arg Properties.of() are only available from 1.20
//? if >=1.20 {

import org.mtr.mapping.holder.BlockSettings;

public final class BlockSettingsFactory {

	private BlockSettingsFactory() {
	}

	public static BlockSettings createPushOnly() {
		return new BlockSettings(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().pushReaction(net.minecraft.world.level.material.PushReaction.PUSH_ONLY));
	}

	public static BlockSettings createBurnableNonOpaqueSolid() {
		return new BlockSettings(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().ignitedByLava().noOcclusion().forceSolidOn());
	}
}
//?}