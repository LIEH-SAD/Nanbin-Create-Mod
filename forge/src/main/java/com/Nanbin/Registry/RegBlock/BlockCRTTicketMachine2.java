package com.Nanbin.Registry.RegBlock;

import com.Nanbin.Registry.RegBlock.flag.TicketFault;
import com.Nanbin.mapping.TicketMachineHelper;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.tool.HolderBase;
import org.mtr.mod.Items;
import org.mtr.mod.block.BlockDirectionalDoubleBlockBase;
import org.mtr.mod.block.IBlock;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * CRT 第二代售票机：与第一代外观不同，行为一致。
 * 刷子右键可切换故障状态，故障期间停止售票并提示玩家。
 */
public class BlockCRTTicketMachine2 extends BlockDirectionalDoubleBlockBase {

    public BlockCRTTicketMachine2(BlockSettings blockSettings) {
        super(blockSettings);
        // 布尔属性默认值为 true，必须显式把默认状态改为未故障，否则新放置的售票机默认就是故障状态。
        setDefaultState2(getDefaultState2().with(new Property<>(TicketFault.FAULT.data), false));
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

    /** 服务端调用：上下两格一并切换故障状态。 */
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
    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        final Direction facing = IBlock.getStatePropertySafe(state, FACING);
        return IBlock.getVoxelShapeByDirection(2, 0, 0, 14, 16, 11, facing);
    }

    @Override
    public void addBlockProperties(List<HolderBase<?>> properties) {
        properties.add(FACING);
        properties.add(HALF);
        properties.add(TicketFault.FAULT);
    }
}
