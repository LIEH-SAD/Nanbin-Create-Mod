package com.Nanbin.Registry.RegBlock;

import com.Nanbin.mapping.IBlockExtension;
import com.Nanbin.mapping.TranslationProvider;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockExtension;
import org.mtr.mapping.mapper.DirectionHelper;
import org.mtr.mapping.tool.HolderBase;
import org.mtr.mod.block.IBlock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class BlockCRTRailwaySignPole extends BlockExtension implements DirectionHelper {
    public BlockCRTRailwaySignPole(BlockSettings blockSettings) {
        super(blockSettings);
    }

    public BlockState getPlacementState2(ItemPlacementContext ctx) {
        return this.getDefaultState2().with(new Property<>(FACING.data), ctx.getPlayerFacing().data);
    }

    @Nonnull
    public VoxelShape getOutlineShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return IBlock.getVoxelShapeByDirection(7.5, 0, 0, 8.5, 16, 1, IBlock.getStatePropertySafe(state, FACING));
    }

    @Nonnull
    @Override
    public ActionResult onUse2(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        return IBlockExtension.checkHoldingBrush(world, player, () -> {
            final Direction facing = IBlock.getStatePropertySafe(state, FACING);
            world.setBlockState(pos, state.with(new Property<>(FACING.data), facing.rotateYClockwise().data));
        });
    }

    public void addBlockProperties(List<HolderBase<?>> properties) {
        super.addBlockProperties(properties);
        properties.add(FACING);
    }

    public void addTooltips(ItemStack stack, @Nullable BlockView world, List<MutableText> tooltip, TooltipContext options) {
        tooltip.add(TranslationProvider.BRUSH_USE.getMutableText(new Object[0]).formatted(TextFormatting.DARK_GRAY));
    }
}