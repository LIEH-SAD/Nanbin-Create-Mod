package com.Nanbin.Registry.RegBlock;

import com.Nanbin.mapping.BlockExtension;
import com.Nanbin.mapping.DirectionHelper;
import com.Nanbin.mapping.IBlockExtension;
import com.Nanbin.mapping.TranslationProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.mtr.block.IBlock;

import java.util.List;

/**
 * CRT 版铁路告示牌立柱。
 * <p>
 * 4.1 适配：不继承 MTR 的 {@code org.mtr.block.BlockRailwaySignPole} —— 4.1 那个版本多了
 * {@code type} 属性（IntProperty 0-3，用于贴合不同长度的告示牌）并依赖 {@code BlockPoleCheckBase}，
 * 而 CRT 的资源（{@code crt_railway_sign_pole.json} 等）只有 {@code facing} 一个变体，
 * 所以沿用 4.0 的简单实现：只有朝向 + 刷子旋转。
 */
public class BlockCRTRailwaySignPole extends BlockExtension implements DirectionHelper {

	public BlockCRTRailwaySignPole(net.minecraft.block.AbstractBlock.Settings settings) {
		super(settings);
	}

	@Override
	public BlockState getPlacementState2(ItemPlacementContext ctx) {
		return getDefaultState2().with(FACING, ctx.getHorizontalPlayerFacing());
	}

	@Override
	public VoxelShape getOutlineShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return IBlock.getVoxelShapeByDirection(7.5, 0, 0, 8.5, 16, 1, IBlock.getStatePropertySafe(state, FACING));
	}

	@Override
	public ActionResult onUse2(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		return IBlockExtension.checkHoldingBrush(world, player, () -> {
			final Direction facing = IBlock.getStatePropertySafe(state, FACING);
			world.setBlockState(pos, state.with(FACING, facing.rotateYClockwise()));
		});
	}

	@Override
	public void addBlockProperties(List<Property<?>> properties) {
		super.addBlockProperties(properties);
		properties.add(Properties.HORIZONTAL_FACING);
	}

	@Override
	public void addTooltips(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(TranslationProvider.BRUSH_USE.getMutableText().formatted(Formatting.DARK_GRAY));
	}
}
