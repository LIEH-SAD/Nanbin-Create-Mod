package com.Nanbin.Registry.RegBlock;

import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.DirectionHelper;
import org.mtr.mapping.tool.HolderBase;
import org.mtr.mod.block.BlockPSDAPGGlassBase;
import org.mtr.mod.block.IBlock;

import javax.annotation.Nonnull;
import java.util.List;

import static com.Nanbin.mapping.IBlockExtension.EnumGlassMode;
import static com.Nanbin.mapping.IBlockExtension.GLASS_MODE;

public class BlockCRTAPGGlass2 extends BlockPSDAPGGlassBase implements DirectionHelper {

    public static final IntegerProperty SIDE_COUNT = IntegerProperty.of("side_count", 1, 3);

    public BlockCRTAPGGlass2() {
        super();
    }

    @Nonnull
    @Override
    public ActionResult onUse2(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        final double y = hit.getPos().getYMapped();
        if (y - Math.floor(y) > 0.21875) {
            final boolean isUpper = IBlock.getStatePropertySafe(state, HALF) == DoubleBlockHalf.UPPER;
            final BlockPos targetPos = isUpper ? pos : pos.up();
            final BlockState targetState = isUpper ? state : world.getBlockState(targetPos);

            return IBlock.checkHoldingBrush(world, player, () -> {
                final Direction facing = IBlock.getStatePropertySafe(targetState, FACING);
                final Direction left = facing.rotateYCounterclockwise();
                final Direction right = facing.rotateYClockwise();

                int leftCount = 0;
                for (int i = 1; i <= 2; i++) {
                    if (isSameGlass(world, targetPos.offset(left, i))) {
                        leftCount = i;
                    } else {
                        break;
                    }
                }
                int rightCount = 0;
                for (int i = 1; i <= 2; i++) {
                    if (isSameGlass(world, targetPos.offset(right, i))) {
                        rightCount = i;
                    } else {
                        break;
                    }
                }
                int totalCount = leftCount + rightCount + 1;

                if (totalCount == 1) {
                    setBothHalves(world, targetPos, targetState, 1, EnumGlassMode.SINGLE);
                } else if (totalCount == 2) {
                    if (leftCount == 1) {
                        setBothHalves(world, targetPos.offset(left, 1), world.getBlockState(targetPos.offset(left, 1)), 2, EnumGlassMode.LEFT);
                        setBothHalves(world, targetPos, targetState, 2, EnumGlassMode.RIGHT);
                    } else {
                        setBothHalves(world, targetPos, targetState, 2, EnumGlassMode.LEFT);
                        setBothHalves(world, targetPos.offset(right, 1), world.getBlockState(targetPos.offset(right, 1)), 2, EnumGlassMode.RIGHT);
                    }
                } else if (totalCount == 3 && !isSameGlass(world, targetPos.offset(left, 3)) && !isSameGlass(world, targetPos.offset(right, 3))) {
                    if (leftCount == 0 && rightCount == 2) {
                        setBothHalves(world, targetPos, targetState, 3, EnumGlassMode.LEFT);
                        setBothHalves(world, targetPos.offset(right, 1), world.getBlockState(targetPos.offset(right, 1)), 3, EnumGlassMode.MIDDLE);
                        setBothHalves(world, targetPos.offset(right, 2), world.getBlockState(targetPos.offset(right, 2)), 3, EnumGlassMode.RIGHT);
                    } else if (leftCount == 2 && rightCount == 0) {
                        setBothHalves(world, targetPos.offset(left, 2), world.getBlockState(targetPos.offset(left, 2)), 3, EnumGlassMode.LEFT);
                        setBothHalves(world, targetPos.offset(left, 1), world.getBlockState(targetPos.offset(left, 1)), 3, EnumGlassMode.MIDDLE);
                        setBothHalves(world, targetPos, targetState, 3, EnumGlassMode.RIGHT);
                    } else {
                        setBothHalves(world, targetPos.offset(left, 1), world.getBlockState(targetPos.offset(left, 1)), 3, EnumGlassMode.LEFT);
                        setBothHalves(world, targetPos, targetState, 3, EnumGlassMode.MIDDLE);
                        setBothHalves(world, targetPos.offset(right, 1), world.getBlockState(targetPos.offset(right, 1)), 3, EnumGlassMode.RIGHT);
                    }
                } else {
                    if (leftCount >= rightCount) {
                        setBothHalves(world, targetPos.offset(left, 1), world.getBlockState(targetPos.offset(left, 1)), 2, EnumGlassMode.LEFT);
                        setBothHalves(world, targetPos, targetState, 2, EnumGlassMode.RIGHT);
                    } else {
                        setBothHalves(world, targetPos, targetState, 2, EnumGlassMode.LEFT);
                        setBothHalves(world, targetPos.offset(right, 1), world.getBlockState(targetPos.offset(right, 1)), 2, EnumGlassMode.RIGHT);
                    }
                }
            });
        }
        return super.onUse2(state, world, pos, player, hand, hit);
    }

    private void setBothHalves(World world, BlockPos pos, BlockState state, int side, EnumGlassMode mode) {
        world.setBlockState(pos, state.with(new Property<>(SIDE_COUNT.data), side).with(new Property<>(GLASS_MODE.data), mode));
        BlockPos downPos = pos.down();
        world.setBlockState(downPos, world.getBlockState(downPos).with(new Property<>(SIDE_COUNT.data), side).with(new Property<>(GLASS_MODE.data), mode));
    }

    private boolean isSameGlass(World world, BlockPos pos) {
        return world.getBlockState(pos).getBlock().data instanceof BlockCRTAPGGlass2;
    }

    @Nonnull
    public VoxelShape getOutlineShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        Direction facing = IBlock.getStatePropertySafe(state, FACING);
        boolean half = IBlock.getStatePropertySafe(state, HALF) == DoubleBlockHalf.UPPER;
        return half ? IBlock.getVoxelShapeByDirection(0, 0, 0, 16, 8, 4, facing) : IBlock.getVoxelShapeByDirection(0, 0, 0, 16, 16, 4, facing);
    }

    @Nonnull
    public VoxelShape getCollisionShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        Direction facing = IBlock.getStatePropertySafe(state, FACING);
        boolean half = IBlock.getStatePropertySafe(state, HALF) == DoubleBlockHalf.UPPER;
        return half ? IBlock.getVoxelShapeByDirection(0, 0, 0, 16, 8, 4, facing) : IBlock.getVoxelShapeByDirection(0, 0, 0, 16, 16, 4, facing);
    }

    @Override
    public void addBlockProperties(List<HolderBase<?>> properties) {
        properties.add(FACING);
        properties.add(HALF);
        properties.add(SIDE_EXTENDED);
        properties.add(SIDE_COUNT);
        properties.add(GLASS_MODE);
    }
}