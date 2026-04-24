package com.client;

import com.Nanbin.Blocks.Blocks;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.render.RenderLayer;


public class nanbinClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        //透明度定义类
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.CRT_LIFT_TIPS.get().data, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.LOGO.get().data, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.CRT_FENCE1.get().data, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.CRT_FENCE8.get().data, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.CRT_FENCE9.get().data, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.CRT_TICKET_2_ENTER.get().data, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.CRT_TICKET_2_EXIT.get().data, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.BLUEFENCE.get().data, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.BLUEFENCE_TOP.get().data, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.GREENFENCE.get().data, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.GREENFENCE_TOP.get().data, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.METALFENCE.get().data, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.METALFENCE_TOP.get().data, RenderLayer.getCutout());
    }

}
