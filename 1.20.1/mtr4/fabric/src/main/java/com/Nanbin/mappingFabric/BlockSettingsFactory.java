package com.Nanbin.mappingFabric;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.piston.PistonBehavior;
import org.mtr.mapping.holder.BlockSettings;

public final class BlockSettingsFactory {

	private BlockSettingsFactory() {
	}

	public static BlockSettings createPushOnly() {
		return new BlockSettings(AbstractBlock.Settings.create().pistonBehavior(PistonBehavior.PUSH_ONLY));
	}

	public static BlockSettings createBurnableNonOpaqueSolid() {
		return new BlockSettings(AbstractBlock.Settings.create().burnable().nonOpaque().solid());
	}
}
