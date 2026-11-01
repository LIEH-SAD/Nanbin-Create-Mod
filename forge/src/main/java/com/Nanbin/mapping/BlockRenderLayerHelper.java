package com.Nanbin.mapping;

import org.mtr.mapping.holder.RenderLayer;
import org.mtr.mapping.registry.BlockRegistryObject;

public final class BlockRenderLayerHelper {

	private BlockRenderLayerHelper() {
	}

	@SuppressWarnings("removal")
	public static void putBlock(BlockRegistryObject block, RenderLayer renderLayer) {
		//? if <1.17 {
		/*net.minecraft.client.renderer.RenderTypeLookup.setRenderLayer(block.get().data, renderLayer.data);
		*///?} else {
		net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(block.get().data, renderLayer.data);
		//?}
	}
}
