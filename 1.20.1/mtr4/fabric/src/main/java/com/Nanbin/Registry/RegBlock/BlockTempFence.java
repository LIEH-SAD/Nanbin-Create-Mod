package com.Nanbin.Registry.RegBlock;

import net.minecraft.block.*;

public class BlockTempFence extends FenceBlock {
    public BlockTempFence(Settings settings) {
        super(settings);
    }

    private boolean canConnectToFence(BlockState state) {
        return true;
    }
}
