package com.Nanbin.client.Registry;

import com.Nanbin.Blocks.Blocks;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.render.RenderLayer;

public class RenderLayerMap {

    public static void onInitialize(){
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.CRT_LIFT_TIPS, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.LOGO, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.CRT_FENCE1, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.CRT_FENCE8, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.CRT_FENCE9, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.CRT_TICKET_1_ENTER, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.CRT_TICKET_1_EXIT, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.CRT_TICKET_2_ENTER, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.CRT_TICKET_2_EXIT, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.CRT_TICKET_MACHINE_1, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.BLUEFENCE, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.BLUEFENCE_TOP, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.GREENFENCE, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.GREENFENCE_TOP, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.METALFENCE, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.METALFENCE_TOP, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.ORDINARY_PSD_CAB_DOOR, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.CRT_APG_CAB_DOOR_OLD, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.CRT_APG_CAB_DOOR_NEW, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.SOUNDPROOFNET_GLASS, RenderLayer.getCutout());
    }
}
