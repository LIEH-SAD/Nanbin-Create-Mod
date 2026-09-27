package com.Nanbin.Registry.RegBlock;

import com.Nanbin.mapping.BlockExtension;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

import java.util.List;

/**
 * CRT 刚性接触网立柱。
 * <p>
 * 资源里的 {@code blockstates/crt_rigid_catenary_1.json} 是按四个 {@code facing} 变体写的，
 * 但源项目这个方块并没有 facing 属性，导致该方块状态一直加载失败、模型显示不出来。
 * 这里补上朝向属性，模型与旋转才能生效。
 */
public class BlockCRTRigidCatenary1 extends BlockExtension {

	public static final Property<Direction> FACING = Properties.HORIZONTAL_FACING;

	public BlockCRTRigidCatenary1(AbstractBlock.Settings settings) {
		super(settings);
	}

	@Override
	public BlockState getPlacementState2(ItemPlacementContext ctx) {
		return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing());
	}

	@Override
	public void addBlockProperties(List<Property<?>> properties) {
		properties.add(FACING);
	}

	@Override
	public VoxelShape getOutlineShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return getStationPoleShape();
	}

	public static VoxelShape getStationPoleShape() {
		return Block.createCuboidShape(6, 0, 6, 10, 16, 10);
	}
}
