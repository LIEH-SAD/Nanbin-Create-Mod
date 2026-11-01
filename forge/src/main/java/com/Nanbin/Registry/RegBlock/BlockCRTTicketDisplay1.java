package com.Nanbin.Registry.RegBlock;

import com.Nanbin.Registry.RegBlock.flag.TicketFault;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockExtension;
import org.mtr.mapping.mapper.DirectionHelper;
import org.mtr.mapping.tool.HolderBase;
import org.mtr.mod.Blocks;
import org.mtr.mod.block.IBlock;
import org.mtr.mod.data.TicketSystem.EnumTicketBarrierOpen;

import javax.annotation.Nonnull;
import java.util.List;

public class BlockCRTTicketDisplay1 extends BlockExtension implements DirectionHelper {

    private static final int MAX_SCAN_DISTANCE = 6;
    private static final int REFRESH_INTERVAL = 4;

    public static final EnumProperty<EnumDisplayState> DISPLAY = EnumProperty.of("display", EnumDisplayState.class);

    public BlockCRTTicketDisplay1() {
        super(Blocks.createDefaultBlockSettings(true, blockState -> 5).nonOpaque());
        setDefaultState2(getDefaultState2().with(new Property<>(DISPLAY.data), EnumDisplayState.CLOSED));
    }

    @Override
    public BlockState getPlacementState2(ItemPlacementContext ctx) {
        final BarrierInfo barrier = findBarrier(ctx.getWorld(), ctx.getBlockPos());
        final Direction facing = barrier == null ? ctx.getPlayerFacing() : barrier.facing;
        final EnumDisplayState display = barrier == null ? EnumDisplayState.CLOSED : barrier.display;
        return getDefaultState2()
                .with(new Property<>(FACING.data), facing.data)
                .with(new Property<>(DISPLAY.data), display);
    }

    @Override
    public void onBlockAdded2(BlockState state, World world, BlockPos pos, BlockState oldState, boolean moved) {
        if (!world.isClient()) {
            scheduleRefresh(world, pos);
        }
    }

    @Override
    public void scheduledTick2(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        final World mappedWorld = new World(world.data);
        refresh(mappedWorld, pos, state);
        scheduleRefresh(mappedWorld, pos);
    }

    @Override
    public void neighborUpdate2(BlockState state, World world, BlockPos pos, Block block, BlockPos sourcePos, boolean notify) {
        if (!world.isClient()) {
            refresh(world, pos, state);
            scheduleRefresh(world, pos);
        }
    }

    @Nonnull
    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return getDisplayShape(state);
    }

    @Nonnull
    @Override
    public VoxelShape getCollisionShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return getDisplayShape(state);
    }

    @Override
    public void addBlockProperties(List<HolderBase<?>> properties) {
        properties.add(FACING);
        properties.add(DISPLAY);
    }

    private static VoxelShape getDisplayShape(BlockState state) {
        return IBlock.getVoxelShapeByDirection(0, 4, 6, 16, 16, 10, IBlock.getStatePropertySafe(state, FACING));
    }

    private void refresh(World world, BlockPos pos, BlockState state) {
        final BarrierInfo barrier = findBarrier(world, pos);
        final BlockState newState = barrier == null
                ? state.with(new Property<>(DISPLAY.data), EnumDisplayState.CLOSED)
                : state.with(new Property<>(FACING.data), barrier.facing.data)
                .with(new Property<>(DISPLAY.data), barrier.display);
        if (!newState.equals(state)) {
            world.setBlockState(pos, newState, 3);
        }
    }

    private void scheduleRefresh(World world, BlockPos pos) {
        scheduleBlockTick(world, pos, new Block(this), REFRESH_INTERVAL);
    }

    private static BarrierInfo findBarrier(World world, BlockPos pos) {
        for (int distance = 1; distance <= MAX_SCAN_DISTANCE; distance++) {
            final BlockState barrierState = world.getBlockState(pos.down(distance));
            final EnumDisplayState display = getDisplayState(barrierState);
            if (display != null) {
                return new BarrierInfo(IBlock.getStatePropertySafe(barrierState, FACING), display);
            }
        }
        return null;
    }

    private static EnumDisplayState getDisplayState(BlockState state) {
        final Block block = state.getBlock();
        final EnumProperty<EnumTicketBarrierOpen> openProperty;

        if (block.data instanceof BlockCRTTicketBarrier1) {
            openProperty = BlockCRTTicketBarrier1.OPEN;
        } else if (block.data instanceof BlockCRTTicketBarrier2) {
            openProperty = BlockCRTTicketBarrier2.OPEN;
        } else if (block.data instanceof BlockCRTTicketBarrier3) {
            openProperty = BlockCRTTicketBarrier3.OPEN;
        } else if (block.data instanceof BlockNRTTicketBarrier1) {
            openProperty = BlockNRTTicketBarrier1.OPEN;
        } else {
            return null;
        }

        if (TicketFault.isFault(state)) {
            return EnumDisplayState.FAULT;
        }

        final EnumTicketBarrierOpen open = IBlock.getStatePropertySafe(state, openProperty);
        return open == EnumTicketBarrierOpen.OPEN || open == EnumTicketBarrierOpen.OPEN_CONCESSIONARY
                ? EnumDisplayState.OPEN
                : EnumDisplayState.CLOSED;
    }

    private static final class BarrierInfo {
        private final Direction facing;
        private final EnumDisplayState display;

        private BarrierInfo(Direction facing, EnumDisplayState display) {
            this.facing = facing;
            this.display = display;
        }
    }

    public enum EnumDisplayState implements StringIdentifiable {
        CLOSED("closed"),
        OPEN("open"),
        FAULT("fault");

        private final String name;

        EnumDisplayState(String name) {
            this.name = name;
        }

        @Nonnull
        @Override
        public String asString2() {
            return name;
        }
    }
}
