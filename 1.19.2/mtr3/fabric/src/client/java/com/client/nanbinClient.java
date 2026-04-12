package com.client;

import com.Nanbin.Blocks.Blocks;
import com.Nanbin.Blocks.PlatformDoors;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.render.RenderLayer;

public class nanbinClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        //方块透明度定义类
        BlockRenderLayerMap.INSTANCE.putBlock(PlatformDoors.PLATFORM_DOOR, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.LOGO, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.CRT_FENCE1, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.CRT_FENCE2, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.CRT_FENCE3, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.CRT_FENCE4, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.CRT_FENCE5, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.CRT_FENCE6, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.CRT_FENCE7, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.CRT_FENCE8, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.CRT_FENCE9, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.CRT_FENCE10, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.GREENFENCE, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.BLUEFENCE, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.METALFENCE, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.GREENFENCE_TOP, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.BLUEFENCE_TOP, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.METALFENCE_TOP, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.SOUNDPROOFNET_GLASS, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.CRT_TICKET_2_ENTER, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.CRT_TICKET_2_EXIT, RenderLayer.getCutout());

        //渲染类

    }
}
