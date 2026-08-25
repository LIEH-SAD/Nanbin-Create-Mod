package com.Nanbin.Registry.RegBlock;

import com.Nanbin.Init;
import com.Nanbin.entity.BlockEntityTypes;
import com.Nanbin.mapping.TranslationProvider;
import org.mtr.core.data.Data;
import org.mtr.core.data.Platform;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mapping.mapper.BlockWithEntity;
import org.mtr.mapping.tool.HolderBase;
import org.mtr.mod.block.BlockPSDAPGDoorBase;
import org.mtr.mod.block.IBlock;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.List;

public class BlockCRTAPGDoor1 extends BlockPSDAPGDoorBase implements BlockWithEntity {
    /** 屏蔽门自动排序方向：true = 从右端开始编号，false = 从左端开始编号。 */
    public static final BooleanProperty SORT_FROM_RIGHT = BooleanProperty.of("sort_from_right");

    public BlockCRTAPGDoor1() {
    }

    @Nonnull
    public BlockEntityExtension createBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new BlockEntity(blockPos, blockState);
    }

    @Override
    public void addBlockProperties(List<HolderBase<?>> properties) {
        super.addBlockProperties(properties);
        properties.add(SORT_FROM_RIGHT);
    }

    @Nonnull
    @Override
    public ActionResult onUse2(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        // 顶端（上半格）：切换整排门的自动排序方向；底端（下半格）：保留原来的锁定/解锁
        if (IBlock.getStatePropertySafe(state, HALF) == IBlock.DoubleBlockHalf.UPPER) {
            return IBlock.checkHoldingBrush(world, player, () -> toggleSortDirection(world, pos, state, player));
        }
        return super.onUse2(state, world, pos, player, hand, hit);
    }

    /** 服务端：切换排序方向，并同步到同一排的所有门单元（上下两半 × 左右两块）。 */
    private static void toggleSortDirection(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        final boolean newValue = !IBlock.getStatePropertySafe(state, SORT_FROM_RIGHT);
        final Direction facing = IBlock.getStatePropertySafe(state, BlockPSDAPGDoorBase.FACING);
        final Direction axis = facing.rotateYClockwise();
        for (final BlockPos unitStart : APGDoorNumbering.findRowUnitStarts(world, pos)) {
            for (int x = 0; x < 2; x++) {
                final BlockPos base = unitStart.offset(axis, x);
                for (int y = 0; y < 2; y++) {
                    final BlockPos p = base.up(y);
                    final BlockState s = world.getBlockState(p);
                    if (APGDoorNumbering.isCRTAPGDoor(s)) {
                        world.setBlockState(p, s.with(new Property<>(SORT_FROM_RIGHT.data), newValue));
                    }
                }
            }
        }
        player.sendMessage((newValue ? TranslationProvider.APG_DOOR_SORT_FROM_RIGHT : TranslationProvider.APG_DOOR_SORT_FROM_LEFT).getText(), true);
    }

    @Nonnull
    public VoxelShape getOutlineShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        Direction facing = IBlock.getStatePropertySafe(state, FACING);
        boolean half = IBlock.getStatePropertySafe(state, HALF) == DoubleBlockHalf.UPPER;
        return half ? IBlock.getVoxelShapeByDirection(0, 0, 0, 16, 2, 4, facing) : IBlock.getVoxelShapeByDirection(0, 0, 0, 16, 16, 4, facing);
    }

    public static class BlockEntity extends BlockEntityBase {
        private static final String ARRIVAL_TIME_ID = "nextTrainArrivalTime";
        private long nextTrainArrivalTime = 0;
        private long lastSyncTime = 0;
        private static final long SYNC_INTERVAL = 1000;

        public BlockEntity(BlockPos pos, BlockState state) {
            super(BlockEntityTypes.CRT_APG_DOOR_1.get(), pos, state);
        }

        @Override
        public void readCompoundTag(CompoundTag compoundTag) {
            nextTrainArrivalTime = compoundTag.getLong(ARRIVAL_TIME_ID);
            super.readCompoundTag(compoundTag);
        }

        @Override
        public void writeCompoundTag(CompoundTag compoundTag) {
            compoundTag.putLong(ARRIVAL_TIME_ID, nextTrainArrivalTime);
            super.writeCompoundTag(compoundTag);
        }

        @Override
        public void tick(float tickDelta) {
            super.tick(tickDelta);
            if (getWorld2() != null && !getWorld2().isClient()) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastSyncTime >= SYNC_INTERVAL) {
                    lastSyncTime = currentTime;
                    long predictedArrival = predictNextArrival();
                    if (predictedArrival != nextTrainArrivalTime) {
                        setNextTrainArrivalTime(predictedArrival);
                    }
                }
            }
        }

        public boolean isTrainApproaching(double warningTime) {
            if (nextTrainArrivalTime <= 0) {
                return false;
            }
            long currentTime = System.currentTimeMillis();
            long timeUntilArrival = nextTrainArrivalTime - currentTime;
            return timeUntilArrival > 0 && timeUntilArrival <= warningTime * 1000;
        }

        public void setNextTrainArrivalTime(long arrivalTime) {
            this.nextTrainArrivalTime = arrivalTime;
            markDirty2();
        }

        public long getNextTrainArrivalTime() {
            return nextTrainArrivalTime;
        }

        /**
         * Server-side: try to predict next train arrival time via reflection into MTR internals.
         * Returns 0 if unable to determine.
         */
        private long predictNextArrival() {
            if (getWorld2() == null || getWorld2().isClient()) {
                return 0;
            }
            final BlockPos pos = getPos2();
            try {
                final Field mainField = Class.forName("org.mtr.mod.Init").getDeclaredField("main");
                mainField.setAccessible(true);
                final Object main = mainField.get(null);
                if (main == null) {
                    return 0;
                }

                final Field simulatorsField = main.getClass().getDeclaredField("simulators");
                simulatorsField.setAccessible(true);
                final Iterable<?> simulators = (Iterable<?>) simulatorsField.get(main);

                long earliestArrival = Long.MAX_VALUE;
                boolean foundPlatform = false;
                int totalPlatforms = 0;
                int skippedNoRoutes = 0;
                int skippedFar = 0;

                for (final Object simulator : simulators) {
                    final Data data = (Data) simulator;

                    for (final Platform platform : data.platformIdMap.values()) {
                        totalPlatforms++;
                        if (platform.routes == null || platform.routes.isEmpty()) {
                            skippedNoRoutes++;
                            continue;
                        }
                        foundPlatform = true;
                        boolean nearDoor = true;
                        try {
                            final Field minPosField = platform.getClass().getSuperclass().getDeclaredField("minPosition");
                            minPosField.setAccessible(true);
                            final Object minPos = minPosField.get(platform);
                            if (minPos != null) {
                                final String posStr = minPos.toString();
                                final int px = extractCoordinate(posStr, 'x');
                                final int pz = extractCoordinate(posStr, 'z');
                                final int dx = Math.abs(px - pos.getX());
                                final int dz = Math.abs(pz - pos.getZ());
                                if (dx > 10 || dz > 10) {
                                    nearDoor = false;
                                    skippedFar++;
                                    continue;
                                }
                            }
                        } catch (Exception ignored) {
                        }

                        if (nearDoor) {
                            final long arrival = tryGetNextArrival(platform);
                            if (arrival > 0 && arrival < earliestArrival) {
                                earliestArrival = arrival;
                            }
                        }
                    }
                }

                if (foundPlatform && earliestArrival < Long.MAX_VALUE) {
                    return earliestArrival;
                }
                if (foundPlatform) {
                    return System.currentTimeMillis() + 30000;
                }
            } catch (Exception e) {
                Init.LOGGER.error("[CRTAPGDoor1] predictNextArrival error", e);
            }
            return 0;
        }

        private static long tryGetNextArrival(Platform platform) {
            try {
                final java.lang.reflect.Method method = platform.getClass().getMethod("getNextArrival");
                final Object result = method.invoke(platform);
                if (result instanceof Number) {
                    return ((Number) result).longValue();
                }
            } catch (NoSuchMethodException ignored) {
            } catch (Exception e) {
                Init.LOGGER.error("[CRTAPGDoor1] tryGetNextArrival.getNextArrival error", e);
            }
            try {
                long earliestDeparture = Long.MAX_VALUE;
                for (final Object route : platform.routes) {
                    try {
                        final java.lang.reflect.Method getScheduleMethod = route.getClass().getMethod("getSchedule");
                        final Object schedule = getScheduleMethod.invoke(route);
                        if (schedule != null) {
                            try {
                                final java.lang.reflect.Method getNextMethod = schedule.getClass().getMethod("getNextDeparture", long.class);
                                final Object next = getNextMethod.invoke(schedule, System.currentTimeMillis());
                                if (next instanceof Number) {
                                    final long depTime = ((Number) next).longValue();
                                    if (depTime > System.currentTimeMillis() && depTime < earliestDeparture) {
                                        earliestDeparture = depTime;
                                    }
                                }
                            } catch (NoSuchMethodException ignored) {
                            } catch (Exception e) {
                                Init.LOGGER.error("[CRTAPGDoor1] tryGetNextArrival.getNextDeparture error", e);
                            }
                        }
                    } catch (NoSuchMethodException ignored) {
                    } catch (Exception e) {
                        Init.LOGGER.error("[CRTAPGDoor1] tryGetNextArrival.getSchedule error", e);
                    }
                }
                if (earliestDeparture > System.currentTimeMillis() && earliestDeparture < Long.MAX_VALUE) {
                    return earliestDeparture;
                }
            } catch (Exception e) {
                Init.LOGGER.error("[CRTAPGDoor1] tryGetNextArrival error", e);
            }
            return 0;
        }

        private static int extractCoordinate(String posStr, char coord) {
            try {
                final int idx = posStr.indexOf(coord + "=");
                if (idx >= 0) {
                    final int end = posStr.indexOf(",", idx);
                    final String val = end >= 0 ? posStr.substring(idx + 2, end) : posStr.substring(idx + 2);
                    return Integer.parseInt(val.trim());
                }
            } catch (Exception ignored) {
            }
            return Integer.MAX_VALUE;
        }
    }
}