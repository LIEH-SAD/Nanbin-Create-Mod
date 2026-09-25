package com.Nanbin.Registry.RegBlock;

import mtr.SoundEvents;
import mtr.block.IBlock;
import mtr.data.TicketSystem;
import mtr.data.TicketSystem.EnumTicketBarrierOpen;
import mtr.mappings.BlockDirectionalMapper;
import mtr.mappings.Utilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlockCRTTicketBarrier1 extends BlockDirectionalMapper {
    private final boolean isEntrance;
    public static final EnumProperty<TicketSystem.EnumTicketBarrierOpen> OPEN = EnumProperty.create("open", TicketSystem.EnumTicketBarrierOpen.class);

    public BlockCRTTicketBarrier1(boolean isEntrance) {
        super(Properties.of().requiresCorrectToolForDrops().strength(2.0F).lightLevel((state) -> 5).noOcclusion());
        this.isEntrance = isEntrance;
    }

    public void entityInside(BlockState state, Level world, BlockPos pos, Entity entity) {
        if (!world.isClientSide && entity instanceof Player) {
            Direction facing = (Direction) IBlock.getStatePropertySafe(state, FACING);
            Vec3 playerPosRotated = entity.position().subtract((double)pos.getX() + (double)0.5F, (double)0.0F, (double)pos.getZ() + (double)0.5F).yRot((float)Math.toRadians((double)facing.toYRot()));
            TicketSystem.EnumTicketBarrierOpen open = (TicketSystem.EnumTicketBarrierOpen)IBlock.getStatePropertySafe(state, OPEN);
            if (open.isOpen() && playerPosRotated.z > (double)0.0F) {
                world.setBlockAndUpdate(pos, (BlockState)state.setValue(OPEN, EnumTicketBarrierOpen.CLOSED));
            } else if (!open.isOpen() && playerPosRotated.z < (double)0.0F) {
                TicketSystem.EnumTicketBarrierOpen newOpen = TicketSystem.passThrough(world, pos, (Player)entity, this.isEntrance, !this.isEntrance, SoundEvents.TICKET_BARRIER, SoundEvents.TICKET_BARRIER_CONCESSIONARY, SoundEvents.TICKET_BARRIER, SoundEvents.TICKET_BARRIER_CONCESSIONARY, (SoundEvent)null, false);
                world.setBlockAndUpdate(pos, (BlockState)state.setValue(OPEN, newOpen));
                if (newOpen != EnumTicketBarrierOpen.CLOSED && !world.getBlockTicks().hasScheduledTick(pos, this)) {
                    Utilities.scheduleBlockTick(world, pos, this, 40);
                }
            }
        }

    }

    public void tick(BlockState state, ServerLevel world, BlockPos pos) {
        world.setBlockAndUpdate(pos, (BlockState)state.setValue(OPEN, EnumTicketBarrierOpen.CLOSED));
    }

    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return (BlockState)((BlockState)this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection())).setValue(OPEN, EnumTicketBarrierOpen.CLOSED);
    }

    public VoxelShape getShape(BlockState state, BlockGetter blockGetter, BlockPos pos, CollisionContext collisionContext) {
        Direction facing = (Direction)IBlock.getStatePropertySafe(state, FACING);
        return IBlock.getVoxelShapeByDirection(12, 0, -7, 16, 16, 23, facing);
    }

    public VoxelShape getCollisionShape(BlockState state, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
        Direction facing = (Direction)IBlock.getStatePropertySafe(state, FACING);
        TicketSystem.EnumTicketBarrierOpen open = (TicketSystem.EnumTicketBarrierOpen)IBlock.getStatePropertySafe(state, OPEN);
        VoxelShape base = IBlock.getVoxelShapeByDirection((double)15.0F, (double)0.0F, (double)0.0F, (double)16.0F, (double)24.0F, (double)16.0F, facing);
        return open.isOpen() ? base : Shapes.or(IBlock.getVoxelShapeByDirection((double)0.0F, (double)0.0F, (double)7.0F, (double)16.0F, (double)24.0F, (double)9.0F, facing), base);
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{FACING, OPEN});
    }
}
