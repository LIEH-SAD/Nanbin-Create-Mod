package com.Nanbin.client.Registry;

import com.Nanbin.Blocks.Blocks;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;

public class RenderLayer {

    public static void init() {
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.CRT_LIFT_TIPS.get().data, net.minecraft.client.render.RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.LOGO.get().data, net.minecraft.client.render.RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.CRT_FENCE1.get().data, net.minecraft.client.render.RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.CRT_FENCE8.get().data, net.minecraft.client.render.RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.CRT_FENCE9.get().data, net.minecraft.client.render.RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.CRT_TICKET_1_ENTER.get().data, net.minecraft.client.render.RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.CRT_TICKET_1_EXIT.get().data, net.minecraft.client.render.RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.CRT_TICKET_2_ENTER.get().data, net.minecraft.client.render.RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.CRT_TICKET_2_EXIT.get().data, net.minecraft.client.render.RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.CRT_TICKET_MACHINE_1.get().data, net.minecraft.client.render.RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.BLUEFENCE.get().data, net.minecraft.client.render.RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.BLUEFENCE_TOP.get().data, net.minecraft.client.render.RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.GREENFENCE.get().data, net.minecraft.client.render.RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.GREENFENCE_TOP.get().data, net.minecraft.client.render.RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.METALFENCE.get().data, net.minecraft.client.render.RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.METALFENCE_TOP.get().data, net.minecraft.client.render.RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.ORDINARY_PSD_CAB_DOOR.get().data, net.minecraft.client.render.RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.CRT_APG_CAB_DOOR_OLD.get().data, net.minecraft.client.render.RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.CRT_APG_CAB_FENCE_OLD.get().data, net.minecraft.client.render.RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.CRT_APG_CAB_FENCE_OLD_CONNECT.get().data, net.minecraft.client.render.RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.CRT_APG_CAB_DOOR_NEW.get().data, net.minecraft.client.render.RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.CRT_PSD_CAB_DOOR.get().data, net.minecraft.client.render.RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.SOUNDPROOFNET_GLASS.get().data, net.minecraft.client.render.RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.SOUNDPROOFNET_GLASS_2.get().data, net.minecraft.client.render.RenderLayer.getCutout());
    }
}
