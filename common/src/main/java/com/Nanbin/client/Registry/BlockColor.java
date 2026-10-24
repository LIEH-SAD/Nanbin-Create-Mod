package com.Nanbin.client.Registry;

import com.Nanbin.Blocks.Blocks;
import mtr.RegistryClient;
import net.minecraft.block.Block;

public class BlockColor {
    public static void init() {
        RegistryClient.registerBlockColors((Block) Blocks.CRT_TICKET_1_ENTER.get());
        RegistryClient.registerBlockColors((Block) Blocks.CRT_TICKET_1_EXIT.get());
        RegistryClient.registerBlockColors((Block) Blocks.CRT_TICKET_3_ENTER.get());
        RegistryClient.registerBlockColors((Block) Blocks.CRT_TICKET_3_EXIT.get());
        RegistryClient.registerBlockColors((Block) Blocks.CRT_OLD_WALL1.get());
        RegistryClient.registerBlockColors((Block) Blocks.CRT_OLD_WALL2.get());
        RegistryClient.registerBlockColors((Block) Blocks.STATION_COLOR_CEILING.get());
        RegistryClient.registerBlockColors((Block) Blocks.STATION_COLOR_CEILING_2.get());
    }
}
