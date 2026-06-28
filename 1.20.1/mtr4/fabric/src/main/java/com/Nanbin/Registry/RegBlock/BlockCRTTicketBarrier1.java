package com.Nanbin.Registry.RegBlock;

import com.Nanbin.Registry.SoundEvents;
import org.mtr.mapping.holder.*;
import com.Nanbin.Registry.RegMean.VoxelShapes;
import org.mtr.mapping.mapper.BlockExtension;
import org.mtr.mapping.mapper.DirectionHelper;
import org.mtr.mapping.tool.HolderBase;
import org.mtr.mod.Blocks;
import org.mtr.mod.block.IBlock;
import org.mtr.mod.data.TicketSystem;
import org.mtr.mod.data.TicketSystem.EnumTicketBarrierOpen;

import javax.annotation.Nonnull;
import java.util.List;

import static com.Nanbin.Registry.RegMean.VoxelShapes.union;

public class BlockCRTTicketBarrier1 extends BlockExtension implements DirectionHelper {
    private final boolean isEntrance;
    public static final EnumProperty<EnumTicketBarrierOpen> OPEN = EnumProperty.of("open", EnumTicketBarrierOpen.class);

    public BlockCRTTicketBarrier1(boolean isEntrance) {
        super(Blocks.createDefaultBlockSettings(true, (blockState) -> 5));
        this.isEntrance = isEntrance;
    }

    public void onEntityCollision2(BlockState state, World world, BlockPos blockPos, Entity entity) {
        if (!world.isClient() && PlayerEntity.isInstance(entity)) {
            Direction facing = IBlock.getStatePropertySafe(state, FACING);
            Vector3d playerPosRotated = entity.getPos().subtract((double)blockPos.getX() + (double)0.5F, (double)0.0F, (double)blockPos.getZ() + (double)0.5F).rotateY((float)Math.toRadians((double)facing.asRotation()));
            EnumTicketBarrierOpen open = (EnumTicketBarrierOpen)IBlock.getStatePropertySafe(state, new Property((net.minecraft.state.property.Property)OPEN.data));
            if ((open == EnumTicketBarrierOpen.OPEN || open == EnumTicketBarrierOpen.OPEN_CONCESSIONARY) && playerPosRotated.getZMapped() > (double)0.0F) {
                world.setBlockState(blockPos, state.with(new Property((net.minecraft.state.property.Property)OPEN.data), EnumTicketBarrierOpen.CLOSED));
            } else if (open == EnumTicketBarrierOpen.CLOSED && playerPosRotated.getZMapped() < (double)0.0F) {
                BlockPos blockPosCopy = new BlockPos(blockPos.getX(), blockPos.getY(), blockPos.getZ());
                world.setBlockState(blockPosCopy, state.with(new Property((net.minecraft.state.property.Property)OPEN.data), EnumTicketBarrierOpen.PENDING));
                TicketSystem.passThrough(world, blockPosCopy, PlayerEntity.cast(entity), this.isEntrance, !this.isEntrance, SoundEvents.CRT_TICKET.get(), SoundEvents.CRT_TICKET.get(), SoundEvents.CRT_TICKET.get(), SoundEvents.CRT_TICKET.get(), (SoundEvent)null, false, (newOpen) -> {
                    world.setBlockState(blockPosCopy, state.with(new Property((net.minecraft.state.property.Property)OPEN.data), newOpen));
                    if (newOpen != EnumTicketBarrierOpen.CLOSED && !hasScheduledBlockTick(world, blockPosCopy, new Block(this))) {
                        scheduleBlockTick(world, blockPosCopy, new Block(this), 40);
                    }

                });
            }
        }

    }

    public void scheduledTick2(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        world.setBlockState(pos, state.with(new Property((net.minecraft.state.property.Property)OPEN.data), EnumTicketBarrierOpen.CLOSED));
    }

    public BlockState getPlacementState2(ItemPlacementContext ctx) {
        return this.getDefaultState2().with(new Property((net.minecraft.state.property.Property)FACING.data), ctx.getPlayerFacing().data).with(new Property((net.minecraft.state.property.Property)OPEN.data), EnumTicketBarrierOpen.CLOSED);
    }

    @Nonnull
    public VoxelShape getOutlineShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        Direction facing = IBlock.getStatePropertySafe(state, FACING);
        return union(IBlock.getVoxelShapeByDirection(12, 0, -7, 16, 16, 23, facing), IBlock.getVoxelShapeByDirection(-4, 0, -7, 0, 16, 23, facing));
    }

    @Nonnull
    public VoxelShape getCollisionShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        Direction facing = IBlock.getStatePropertySafe(state, FACING);
        EnumTicketBarrierOpen open = (EnumTicketBarrierOpen)IBlock.getStatePropertySafe(state, new Property((net.minecraft.state.property.Property)OPEN.data));
        VoxelShape base = IBlock.getVoxelShapeByDirection((double)15.0F, (double)0.0F, (double)0.0F, (double)16.0F, (double)24.0F, (double)16.0F, facing);
        return open != EnumTicketBarrierOpen.OPEN && open != EnumTicketBarrierOpen.OPEN_CONCESSIONARY ? union(
                IBlock.getVoxelShapeByDirection(-4, 0, -7, 0, 24, 23, facing),
                IBlock.getVoxelShapeByDirection(12, 0, -7, 16, 24, 23, facing),
                IBlock.getVoxelShapeByDirection(0, 0, 6, 12, 24, 8, facing)) : base;
    }

    public void addBlockProperties(List<HolderBase<?>> properties) {
        properties.add(FACING);
        properties.add(OPEN);
    }
}
