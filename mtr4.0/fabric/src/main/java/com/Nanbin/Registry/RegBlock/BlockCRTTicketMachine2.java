package com.Nanbin.Registry.RegBlock;

import com.Nanbin.mapping.TicketMachineHelper;
import org.mtr.mapping.holder.ActionResult;
import org.mtr.mapping.holder.BlockHitResult;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockSettings;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.BlockView;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.Hand;
import org.mtr.mapping.holder.PlayerEntity;
import org.mtr.mapping.holder.Property;
import org.mtr.mapping.holder.ShapeContext;
import org.mtr.mapping.holder.VoxelShape;
import org.mtr.mapping.holder.World;
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
