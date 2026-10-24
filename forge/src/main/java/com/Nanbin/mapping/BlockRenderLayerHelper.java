package com.Nanbin.mapping;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import org.mtr.mapping.holder.RenderLayer;
import org.mtr.mapping.registry.BlockRegistryObject;

public final class BlockRenderLayerHelper {

	private BlockRenderLayerHelper() {
	}

	@SuppressWarnings("removal")
	public static void putBlock(BlockRegistryObject block, RenderLayer renderLayer) {
		ItemBlockRenderTypes.setRenderLayer(block.get().data, renderLayer.data);
	}
}
