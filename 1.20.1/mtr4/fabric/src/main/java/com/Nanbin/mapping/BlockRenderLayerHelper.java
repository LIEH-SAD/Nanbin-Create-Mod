package com.Nanbin.mapping;

import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import org.mtr.mapping.holder.RenderLayer;
import org.mtr.mapping.registry.BlockRegistryObject;

public final class BlockRenderLayerHelper {

	private BlockRenderLayerHelper() {
	}

	public static void putBlock(BlockRegistryObject block, RenderLayer renderLayer) {
		BlockRenderLayerMap.INSTANCE.putBlock(block.get().data, renderLayer.data);
	}
}
