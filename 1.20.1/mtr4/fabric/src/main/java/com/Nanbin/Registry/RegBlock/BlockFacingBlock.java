
package com.Nanbin.Registry.RegBlock;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Property;

public class BlockFacingBlock extends HorizontalFacingBlock {
    public BlockFacingBlock(Settings settings) {
        super(settings);
    }

    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{FACING});
    }

    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return (BlockState)this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    public PistonBehavior getPistonBehavior(BlockState state) {
        return PistonBehavior.PUSH_ONLY;
    }
}
