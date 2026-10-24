package com.Nanbin.mapping;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockSetType;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.enums.DoorHinge;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import java.util.List;

/**
 * 门方块适配层，取代 MTR 4.0 的 {@code org.mtr.mapping.mapper.DoorBlockExtension}。
 * <p>
 * 4.0 的那个类是包在 {@code DoorBlock} 外面的包装类（所以可以定义静态的 {@code isOpen}）；
 * 这里直接继承原版 {@link DoorBlock}，因此 {@code isOpen} 用原版已有的实例方法，
 * 开关循环用 {@link #cycleOpenState}。
 */
public abstract class DoorBlockExtension extends DoorBlock {

	protected DoorBlockExtension(Settings settings) {
		super(BlockSetType.IRON, settings);
	}

	/** 子类返回自定义轮廓；返回 null 表示沿用原版门轮廓。 */
	public VoxelShape getOutlineShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return null;
	}

	/** 子类实现在这里处理右键（4.0 命名的钩子）。 */
	public ActionResult onUse2(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		return ActionResult.PASS;
	}

	/** 子类在这里追加物品提示文本。 */
	public void addTooltips(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
	}

	@Override
	public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
		addTooltips(stack, context, tooltip, type);
	}

	protected static BlockState cycleOpenState(BlockState state) {
		return state.cycle(OPEN);
	}

	protected static boolean isRightHinge(BlockState state) {
		return state.get(HINGE) == DoorHinge.RIGHT;
	}

	protected static boolean isUpperHalf(BlockState state) {
		return state.get(HALF) == DoubleBlockHalf.UPPER;
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		final VoxelShape result = getOutlineShape2(state, world, pos, context);
		return result == null ? super.getOutlineShape(state, world, pos, context) : result;
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		final ActionResult result = onUse2(state, world, pos, player, hit);
		return result == ActionResult.PASS ? super.onUse(state, world, pos, player, hit) : result;
	}
}
