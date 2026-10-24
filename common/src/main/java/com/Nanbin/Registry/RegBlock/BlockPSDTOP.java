package com.Nanbin.Registry.RegBlock;

import com.Nanbin.entity.BlockEntityTypes;
import com.Nanbin.mapping.BlockExtension;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import org.mtr.block.IBlock;

import java.util.List;

/**
 * PSD 顶板。
 * <p>
 * 4.1 适配：{@code BlockWithEntity} → {@link BlockEntityProvider}；
 * 方块实体基类直接用 MTR 的 {@code org.mtr.block.BlockPSDTop$BlockEntityBase}。
 */
public class BlockPSDTOP extends BlockExtension implements BlockEntityProvider {

	public BlockPSDTOP(AbstractBlock.Settings settings) {
		super(settings);
	}

	@Override
	public BlockState getPlacementState2(ItemPlacementContext ctx) {
		return getDefaultState().with(Properties.HORIZONTAL_FACING, ctx.getHorizontalPlayerFacing());
	}

	@Override
	public VoxelShape getOutlineShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return IBlock.getVoxelShapeByDirection(0, 0, 0, 16, 16, 6, IBlock.getStatePropertySafe(state, Properties.HORIZONTAL_FACING));
	}

	@Override
	public void addBlockProperties(List<Property<?>> properties) {
		properties.add(Properties.HORIZONTAL_FACING);
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new BlockEntity(pos, state);
	}

	public static class BlockEntity extends org.mtr.block.BlockPSDTop.BlockEntityBase {

		public BlockEntity(BlockPos pos, BlockState state) {
			super(BlockEntityTypes.PSD_TOP.get(), pos, state);
		}
	}
}
