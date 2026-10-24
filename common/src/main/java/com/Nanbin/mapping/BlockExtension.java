package com.Nanbin.mapping;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * 方块基类适配层。
 * <p>
 * MTR 4.1 去掉了 4.0 的 {@code org.mtr.mapping.mapper.BlockExtension} 包装，方块直接继承原版
 * {@link Block} 并自己实现 {@code appendProperties} / {@code getPlacementState} / {@code getOutlineShape}。
 * 这里保留 4.0 时代的 {@code *2} 钩子方法名，让移植过来的方块实现只需要最小改动。
 * <p>
 * 4.0 时代很多装饰性方块直接写 {@code new BlockExtension(settings)}，所以这个类不是抽象类。
 */
public class BlockExtension extends Block {

	public BlockExtension(Settings settings) {
		super(settings);
	}

	public BlockState getDefaultState2() {
		return getDefaultState();
	}

	public BlockState getPlacementState2(ItemPlacementContext ctx) {
		return getDefaultState();
	}

	public VoxelShape getOutlineShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return super.getOutlineShape(state, world, pos, context);
	}

	public VoxelShape getCollisionShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return super.getCollisionShape(state, world, pos, context);
	}

	/** 子类在这里声明自己用到的方块状态属性。 */
	public void addBlockProperties(List<Property<?>> properties) {
	}

	/** 子类在这里追加物品提示文本。 */
	public void addTooltips(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
	}

	/** 放置完成钩子（4.0 命名），默认转发给原版。 */
	public void onPlaced2(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
	}

	/** 方块被替换钩子（4.0 命名）。 */
	public void onStateReplaced2(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
	}

	/** 右键钩子（4.0 命名）。返回 PASS 时回退原版行为。 */
	public ActionResult onUse2(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		return ActionResult.PASS;
	}

	/** 实体碰撞钩子（4.0 命名）。 */
	public void onEntityCollision2(BlockState state, World world, BlockPos pos, Entity entity) {
	}

	/** 计划刻钩子（4.0 命名）。 */
	public void scheduledTick2(BlockState state, ServerWorld world, BlockPos pos, Random random) {
	}

	/** 破坏钩子（4.0 命名）。 */
	public void onBreak2(World world, BlockPos pos, BlockState state, PlayerEntity player) {
	}

	/** 相机碰撞轮廓钩子（4.0 命名）；返回 null 表示沿用原版。 */
	public VoxelShape getCameraCollisionShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return null;
	}

	// TODO(第二批)：相邻方块更新钩子。1.21.4 把 Block#getStateForNeighborUpdate 换成了
	// getStateForNeighborUpdate(WorldView, ScheduledTickView, BlockPos, Direction, BlockPos, BlockState, Random)，
	// 与 1.21.1 不兼容，需要按版本分别实现（放到各平台/版本源码集里）。

	@Override
	public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
		addTooltips(stack, context, tooltip, type);
	}

	@Override
	public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		super.onPlaced(world, pos, state, placer, stack);
		onPlaced2(world, pos, state, placer, stack);
	}

	@Override
	public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
		onStateReplaced2(state, world, pos, newState, moved);
		super.onStateReplaced(state, world, pos, newState, moved);
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		final ActionResult result = onUse2(state, world, pos, player, hit);
		return result == ActionResult.PASS ? super.onUse(state, world, pos, player, hit) : result;
	}

	@Override
	protected void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
		super.onEntityCollision(state, world, pos, entity);
		onEntityCollision2(state, world, pos, entity);
	}

	@Override
	protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		super.scheduledTick(state, world, pos, random);
		scheduledTick2(state, world, pos, random);
	}

	@Override
	public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
		onBreak2(world, pos, state, player);
		return super.onBreak(world, pos, state, player);
	}

	@Override
	protected VoxelShape getCameraCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		final VoxelShape result = getCameraCollisionShape2(state, world, pos, context);
		return result == null ? super.getCameraCollisionShape(state, world, pos, context) : result;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		final List<Property<?>> properties = new ArrayList<>();
		addBlockProperties(properties);
		properties.forEach(builder::add);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		return getPlacementState2(ctx);
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return getOutlineShape2(state, world, pos, context);
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return getCollisionShape2(state, world, pos, context);
	}
}
