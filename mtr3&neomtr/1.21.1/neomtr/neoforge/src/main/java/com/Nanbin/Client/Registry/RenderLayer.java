package com.Nanbin.Client.Registry;

import com.Nanbin.Blocks.Blocks;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;

public class RenderLayer {

    public static void init(){
        ItemBlockRenderTypes.setRenderLayer(Blocks.CRT_TICKET_MACHINE_1.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(Blocks.CRT_TICKET_1_ENTER.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(Blocks.CRT_TICKET_1_EXIT.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(Blocks.CRT_TICKET_2_ENTER.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(Blocks.CRT_TICKET_2_EXIT.get(), RenderType.cutout());
    }
}
