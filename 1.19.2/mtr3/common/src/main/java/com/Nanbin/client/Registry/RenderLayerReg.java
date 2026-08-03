package com.Nanbin.client.Registry;

import com.Nanbin.Blocks.Blocks;
import mtr.RegistryClient;
import net.minecraft.block.Block;
import net.minecraft.client.render.RenderLayer;

public class RenderLayerReg {
    public static void init() {
        RegistryClient.registerBlockRenderType(RenderLayer.getCutout(), (Block) Blocks.CRT_FENCE1.get());
        RegistryClient.registerBlockRenderType(RenderLayer.getCutout(), (Block) Blocks.CRT_FENCE8.get());
        RegistryClient.registerBlockRenderType(RenderLayer.getCutout(), (Block) Blocks.CRT_FENCE9.get());
        RegistryClient.registerBlockRenderType(RenderLayer.getCutout(), (Block) Blocks.CRT_FENCE10.get());
        RegistryClient.registerBlockRenderType(RenderLayer.getCutout(), (Block) Blocks.CRT_FENCE_LIFT_TIPS_1.get());
        RegistryClient.registerBlockRenderType(RenderLayer.getCutout(), (Block) Blocks.CRT_FENCE_TICKET.get());
        RegistryClient.registerBlockRenderType(RenderLayer.getCutout(), (Block) Blocks.CRT_TICKET_1_EXIT.get());
        RegistryClient.registerBlockRenderType(RenderLayer.getCutout(), (Block) Blocks.CRT_TICKET_1_ENTER.get());
        RegistryClient.registerBlockRenderType(RenderLayer.getCutout(), (Block) Blocks.CRT_TICKET_2_EXIT.get());
        RegistryClient.registerBlockRenderType(RenderLayer.getCutout(), (Block) Blocks.CRT_TICKET_2_ENTER.get());
        RegistryClient.registerBlockRenderType(RenderLayer.getCutout(), (Block) Blocks.CRT_TICKET_3_EXIT.get());
        RegistryClient.registerBlockRenderType(RenderLayer.getCutout(), (Block) Blocks.CRT_TICKET_3_ENTER.get());
        RegistryClient.registerBlockRenderType(RenderLayer.getCutout(), (Block) Blocks.CRT_TICKET_MACHINE_1.get());
        RegistryClient.registerBlockRenderType(RenderLayer.getCutout(), (Block) Blocks.NRT_TICKET_1_EXIT.get());
        RegistryClient.registerBlockRenderType(RenderLayer.getCutout(), (Block) Blocks.NRT_TICKET_1_ENTER.get());
        RegistryClient.registerBlockRenderType(RenderLayer.getCutout(), (Block) Blocks.SOUNDPROOFNET_GLASS.get());
        RegistryClient.registerBlockRenderType(RenderLayer.getCutout(), (Block) Blocks.SOUNDPROOFNET_GLASS_2.get());
        RegistryClient.registerBlockRenderType(RenderLayer.getCutout(), (Block) Blocks.BLUEFENCE.get());
        RegistryClient.registerBlockRenderType(RenderLayer.getCutout(), (Block) Blocks.BLUEFENCE_TOP.get());
        RegistryClient.registerBlockRenderType(RenderLayer.getCutout(), (Block) Blocks.GREENFENCE.get());
        RegistryClient.registerBlockRenderType(RenderLayer.getCutout(), (Block) Blocks.GREENFENCE_TOP.get());
        RegistryClient.registerBlockRenderType(RenderLayer.getCutout(), (Block) Blocks.METALFENCE.get());
        RegistryClient.registerBlockRenderType(RenderLayer.getCutout(), (Block) Blocks.METALFENCE_TOP.get());
        RegistryClient.registerBlockRenderType(RenderLayer.getCutout(), (Block) Blocks.ORDINARY_PSD_CAB_DOOR.get());
        RegistryClient.registerBlockRenderType(RenderLayer.getCutout(), (Block) Blocks.CRT_APG_CAB_DOOR_OLD.get());
        RegistryClient.registerBlockRenderType(RenderLayer.getCutout(), (Block) Blocks.CRT_APG_CAB_FENCE_OLD.get());
        RegistryClient.registerBlockRenderType(RenderLayer.getCutout(), (Block) Blocks.CRT_APG_CAB_FENCE_OLD_CONNECT.get());
        RegistryClient.registerBlockRenderType(RenderLayer.getCutout(), (Block) Blocks.CRT_APG_CAB_DOOR_NEW.get());
        RegistryClient.registerBlockRenderType(RenderLayer.getCutout(), (Block) Blocks.CRT_PSD_CAB_DOOR.get());
        RegistryClient.registerBlockRenderType(RenderLayer.getCutout(), (Block) Blocks.CRT_FENCE_LIFT_TIPS_1.get());
        RegistryClient.registerBlockRenderType(RenderLayer.getCutout(), (Block) Blocks.CRT_LIFT_TIPS_3.get());
    }
}
