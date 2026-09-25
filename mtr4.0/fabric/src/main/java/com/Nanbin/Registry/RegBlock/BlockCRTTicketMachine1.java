package com.Nanbin.Registry.RegBlock;

import com.Nanbin.mapping.TicketMachineHelper;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.tool.HolderBase;
import org.mtr.mod.Items;
import org.mtr.mod.block.BlockDirectionalDoubleBlockBase;
import org.mtr.mod.block.IBlock;

import javax.annotation.Nonnull;
import java.util.List;

public class BlockCRTTicketMachine1 extends BlockDirectionalDoubleBlockBase {
    public BlockCRTTicketMachine1(BlockSettings blockSettings) {
        super(blockSettings);
    }

    @Nonnull
    @Override
    public ActionResult onUse2(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (player.isHolding(Items.BRUSH.get())) {
            return IBlock.checkHoldingBrush(world, player, () -> toggleFault(world, pos, state, player));
        }
        if (TicketFault.isFault(state)) {
            if (!world.isClient()) {
                TicketFault.notifyInUse(world, player);
            }
            return ActionResult.SUCCESS;
        }
        if (!world.isClient()) {
            TicketMachineHelper.openTicketMachineScreen(world, player);
        }
        return ActionResult.SUCCESS;
    }

    /** 服务端：上下两格一并切换故障状态，故障期间售票机停止服务。 */
    private static void toggleFault(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        final boolean fault = !TicketFault.isFault(state);
        world.setBlockState(pos, state.with(new Property<>(TicketFault.FAULT.data), fault), 3);
        final BlockPos otherPos = pos.offset(IBlock.getStatePropertySafe(state, HALF) == DoubleBlockHalf.UPPER ? Direction.DOWN : Direction.UP);
        final BlockState otherState = world.getBlockState(otherPos);
        if (otherState.isOf(state.getBlock())) {
            world.setBlockState(otherPos, otherState.with(new Property<>(TicketFault.FAULT.data), fault), 3);
        }
        TicketFault.notifyToggle(player, fault);
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
        properties.add(TicketFault.FAULT);
    }
}
