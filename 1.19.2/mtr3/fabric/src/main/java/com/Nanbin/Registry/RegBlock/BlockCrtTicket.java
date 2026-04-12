
package com.Nanbin.Registry.RegBlock;

import com.Nanbin.Registry.NanbinSoundEvents;
import mtr.block.IBlock;
import mtr.data.TicketSystem;
import mtr.data.TicketSystem.EnumTicketBarrierOpen;
import mtr.mappings.BlockDirectionalMapper;
import mtr.mappings.Utilities;
import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class BlockCrtTicket extends BlockDirectionalMapper {
    private final boolean isEntrance;
    public static final EnumProperty<TicketSystem.EnumTicketBarrierOpen> OPEN = EnumProperty.of("open", TicketSystem.EnumTicketBarrierOpen.class);

    public BlockCrtTicket(boolean isEntrance) {
        super(Settings.of(Material.METAL, MapColor.GRAY).requiresTool().strength(2.0F).luminance((state) -> 5).nonOpaque());
        this.isEntrance = isEntrance;
    }

    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!world.isClient && entity instanceof PlayerEntity) {
            Direction facing = (Direction) IBlock.getStatePropertySafe(state, FACING);
            Vec3d playerPosRotated = entity.getPos().subtract((double)pos.getX() + (double)0.5F, (double)0.0F, (double)pos.getZ() + (double)0.5F).rotateY((float)Math.toRadians((double)facing.asRotation()));
            TicketSystem.EnumTicketBarrierOpen open = (TicketSystem.EnumTicketBarrierOpen)IBlock.getStatePropertySafe(state, OPEN);
            if (open.isOpen() && playerPosRotated.z > (double)0.0F) {
                world.setBlockState(pos, (BlockState)state.with(OPEN, EnumTicketBarrierOpen.CLOSED));
            } else if (!open.isOpen() && playerPosRotated.z < (double)0.0F) {
                TicketSystem.EnumTicketBarrierOpen newOpen = TicketSystem.passThrough(world, pos, (PlayerEntity)entity, this.isEntrance, !this.isEntrance, NanbinSoundEvents.CRT_TICKET_BARRIER, NanbinSoundEvents.CRT_TICKET_BARRIER, NanbinSoundEvents.CRT_TICKET_BARRIER, NanbinSoundEvents.CRT_TICKET_BARRIER, (SoundEvent)null, false);
                world.setBlockState(pos, (BlockState)state.with(OPEN, newOpen));
                if (newOpen != EnumTicketBarrierOpen.CLOSED && !world.getBlockTickScheduler().isQueued(pos, this)) {
                    Utilities.scheduleBlockTick(world, pos, this, 40);
                }
            }
        }

    }

    public void tick(BlockState state, ServerWorld world, BlockPos pos) {
        world.setBlockState(pos, (BlockState)state.with(OPEN, EnumTicketBarrierOpen.CLOSED));
    }

    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return (BlockState)((BlockState)this.getDefaultState().with(FACING, ctx.getPlayerFacing())).with(OPEN, EnumTicketBarrierOpen.CLOSED);
    }

    public VoxelShape getOutlineShape(BlockState state, BlockView blockGetter, BlockPos pos, ShapeContext collisionContext) {
        Direction facing = (Direction)IBlock.getStatePropertySafe(state, FACING);
        return IBlock.getVoxelShapeByDirection((double)12.0F, (double)0.0F, (double)0.0F, (double)16.0F, (double)15.0F, (double)16.0F, facing);
    }

    public VoxelShape getCollisionShape(BlockState state, BlockView blockGetter, BlockPos blockPos, ShapeContext collisionContext) {
        Direction facing = (Direction)IBlock.getStatePropertySafe(state, FACING);
        TicketSystem.EnumTicketBarrierOpen open = (TicketSystem.EnumTicketBarrierOpen)IBlock.getStatePropertySafe(state, OPEN);
        VoxelShape base = IBlock.getVoxelShapeByDirection((double)15.0F, (double)0.0F, (double)0.0F, (double)16.0F, (double)24.0F, (double)16.0F, facing);
        return open.isOpen() ? base : VoxelShapes.union(IBlock.getVoxelShapeByDirection((double)0.0F, (double)0.0F, (double)7.0F, (double)16.0F, (double)24.0F, (double)9.0F, facing), base);
    }

    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{FACING, OPEN});
    }
}
