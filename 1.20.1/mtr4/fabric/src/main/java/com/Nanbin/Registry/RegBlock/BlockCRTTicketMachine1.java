package com.Nanbin.Registry.RegBlock;

import com.Nanbin.Registry.RegMean.TicketMachineHelper;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.tool.HolderBase;
import org.mtr.mod.block.BlockDirectionalDoubleBlockBase;
import org.mtr.mod.block.IBlock;

import javax.annotation.Nonnull;
import java.util.List;

public class BlockCRTTicketMachine1 extends BlockDirectionalDoubleBlockBase {
    public BlockCRTTicketMachine1(BlockSettings blockSettings) {
        super(blockSettings);
    }

    @Nonnull
    public ActionResult onUse2(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient()) {
            TicketMachineHelper.openTicketMachineScreen(world, player);
        }
        return ActionResult.SUCCESS;
    }

    @Nonnull
    public VoxelShape getOutlineShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        Direction facing = IBlock.getStatePropertySafe(state, FACING);
        int height = IBlock.getStatePropertySafe(state, HALF) == DoubleBlockHalf.UPPER ? 14 : 16;
        return IBlock.getVoxelShapeByDirection(2, 0, 0, 14, 16, 11,facing);
    }

    public void addBlockProperties(List<HolderBase<?>> properties) {
        properties.add(FACING);
        properties.add(HALF);
    }
}
