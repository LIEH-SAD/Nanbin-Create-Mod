package com.Nanbin.Registry.RegBlock;

import com.Nanbin.mapping.Registry;
import com.Nanbin.packet.PacketHandler;
import mtr.Items;
import mtr.block.BlockDirectionalDoubleBlockBase;
import mtr.block.IBlock;
import mtr.data.TicketSystem;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.ScoreboardPlayerScore;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Property;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class BlockCRTTicketMachine1 extends BlockDirectionalDoubleBlockBase {
    public BlockCRTTicketMachine1(AbstractBlock.Settings settings) {
        super(settings);
        setDefaultState((BlockState)this.getDefaultState().with(TicketFault.FAULT, false));
    }

    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand interactionHand, BlockHitResult blockHitResult) {
        if (player.isHolding(Items.BRUSH.get())) {
            return IBlock.checkHoldingBrush(world, player, () -> toggleFault(world, pos, state, player));
        }
        if (TicketFault.isFault(state)) {
            if (!world.isClient) {
                TicketFault.notifyInUse(world, player);
            }
            return ActionResult.SUCCESS;
        }
        if (!world.isClient) {
            TicketSystem.addObjectivesIfMissing(world);
            final ScoreboardPlayerScore score = TicketSystem.getPlayerScore(world, player, TicketSystem.BALANCE_OBJECTIVE);
            final int balance = score == null ? 0 : score.getScore();
            Registry.sendPacketToClient((ServerPlayerEntity) player, PacketHandler.PACKET_OPEN_TICKET_MENU, buf -> buf.writeInt(balance));
        }

        return ActionResult.SUCCESS;
    }

    /** 服务端：上下两格一并切换故障状态，故障期间售票机停止服务。 */
    private static void toggleFault(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        final boolean fault = !TicketFault.isFault(state);
        world.setBlockState(pos, (BlockState)state.with(TicketFault.FAULT, fault), 3);
        final BlockPos otherPos = pos.offset(IBlock.getStatePropertySafe(state, HALF) == DoubleBlockHalf.UPPER ? Direction.DOWN : Direction.UP);
        final BlockState otherState = world.getBlockState(otherPos);
        if (otherState.isOf(state.getBlock())) {
            world.setBlockState(otherPos, (BlockState)otherState.with(TicketFault.FAULT, fault), 3);
        }
        TicketFault.notifyToggle(player, fault);
    }

    public VoxelShape getOutlineShape(BlockState state, BlockView blockGetter, BlockPos pos, ShapeContext collisionContext) {
        Direction facing = (Direction)IBlock.getStatePropertySafe(state, FACING);
        int height = IBlock.getStatePropertySafe(state, HALF) == DoubleBlockHalf.UPPER ? 14 : 16;
        return IBlock.getVoxelShapeByDirection(2, 0, 0, 14, 16, 11, facing);
    }

    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{FACING, HALF, TicketFault.FAULT});
    }
}
