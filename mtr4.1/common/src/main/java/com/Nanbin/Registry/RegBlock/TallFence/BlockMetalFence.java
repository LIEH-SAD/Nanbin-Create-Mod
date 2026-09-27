package com.Nanbin.Registry.RegBlock.TallFence;

import com.Nanbin.Blocks.Blocks;
import com.Nanbin.mapping.BlockExtension;
import com.Nanbin.mapping.TranslationProvider;
import com.Nanbin.mapping.WorldHelper;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.mtr.block.IBlock;

import java.util.List;
import java.util.stream.Stream;

public class BlockMetalFence extends BlockExtension {

	public static final Property<Direction> FACING = Properties.HORIZONTAL_FACING;

	private static final int MAX_FENCE_HEIGHT = 4;
	private static final int BOTTOM_SECTION_COUNT = 3;

	private static final VoxelShape SHAPE_SOUTH = createFenceShape(0, 2, 0, 1);
	private static final VoxelShape SHAPE_NORTH = createFenceShape(14, 16, 15, 16);
	private static final VoxelShape SHAPE_WEST = Block.createCuboidShape(14, 0, 0, 16, 16, 16);
	private static final VoxelShape SHAPE_EAST = Block.createCuboidShape(0, 0, 0, 2, 16, 16);

	public BlockMetalFence(AbstractBlock.Settings settings) {
		super(settings);
	}

	@Nullable
	@Override
	public BlockState getPlacementState2(ItemPlacementContext context) {
		final World world = context.getWorld();
		final BlockPos placementPos = context.getBlockPos();
		final PlayerEntity player = context.getPlayer();

		for (int i = 1; i <= MAX_FENCE_HEIGHT; i++) {
			if (!world.getBlockState(placementPos.up(i)).isAir()) {
				sendPlayerMessage(world, player, "tips.fence_no_space");
				return null;
			}
		}

		if (placementPos.getY() + MAX_FENCE_HEIGHT - 1 > WorldHelper.getTopY(world)) {
			sendPlayerMessage(world, player, "tips.fence_too_high");
			return null;
		}

		return getDefaultState().with(FACING, context.getHorizontalPlayerFacing().getOpposite());
	}

	@Override
	public void onPlaced2(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		if (world.isClient) {
			return;
		}

		final BlockPos bottom = findBottomFencePos(world, pos);
		final Direction facing = IBlock.getStatePropertySafe(state, FACING);

		for (int i = 0; i < MAX_FENCE_HEIGHT; i++) {
			final BlockPos targetPos = bottom.up(i);
			if (canReplace(world.getBlockState(targetPos))) {
				world.setBlockState(targetPos, createFenceState(facing, i), 3);
			}
		}
	}

	@Override
	public void onStateReplaced2(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
		if (world.isClient || isFenceBlock(newState.getBlock())) {
			return;
		}
		destroyWholeFence(world, pos);
	}

	@Override
	public void addBlockProperties(List<Property<?>> properties) {
		properties.add(FACING);
	}

	@Override
	public VoxelShape getOutlineShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
		return switch (IBlock.getStatePropertySafe(state, FACING)) {
			case SOUTH -> SHAPE_SOUTH;
			case EAST -> SHAPE_EAST;
			case WEST -> SHAPE_WEST;
			default -> SHAPE_NORTH;
		};
	}

	@Override
	public VoxelShape getCollisionShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
		return getOutlineShape2(state, world, pos, ctx);
	}

	@Override
	public void addTooltips(ItemStack stack, net.minecraft.item.Item.TooltipContext context, List<Text> tooltip, net.minecraft.item.tooltip.TooltipType type) {
		tooltip.add(TranslationProvider.BLOCK_TALL_FENCE.getMutableText().formatted(Formatting.DARK_GRAY));
	}

	private void sendPlayerMessage(World world, @Nullable PlayerEntity player, String key) {
		if (!world.isClient && player != null) {
			player.sendMessage(Text.translatable(key), true);
		}
	}

	private boolean isFenceBlock(Block block) {
		return block instanceof BlockMetalFence || block instanceof BlockMetalFenceTop;
	}

	private BlockPos findBottomFencePos(World world, BlockPos pos) {
		BlockPos bottom = pos;
		while (bottom.getY() > WorldHelper.getBottomY(world)) {
			final BlockPos down = bottom.down();
			if (isFenceBlock(world.getBlockState(down).getBlock())) {
				bottom = down;
			} else {
				break;
			}
		}
		return bottom;
	}

	private BlockState createFenceState(Direction facing, int heightIndex) {
		if (heightIndex < BOTTOM_SECTION_COUNT) {
			return getDefaultState().with(FACING, facing);
		} else {
			return Blocks.METALFENCE_TOP.get().getDefaultState().with(BlockMetalFenceTop.FACING, facing);
		}
	}

	private boolean canReplace(BlockState state) {
		return state.isAir() || isFenceBlock(state.getBlock());
	}

	private void destroyWholeFence(World world, BlockPos pos) {
		final BlockPos bottom = findBottomFencePos(world, pos);
		for (int i = 0; i < MAX_FENCE_HEIGHT; i++) {
			world.breakBlock(bottom.up(i), false);
		}
	}

	private static VoxelShape createFenceShape(int zStart, int zEnd, int middleZStart, int middleZEnd) {
		return Stream.of(
				Block.createCuboidShape(15, 0, zStart, 16, 16, zEnd),
				Block.createCuboidShape(0, 0, zStart, 1, 16, zEnd),
				Block.createCuboidShape(1, 0, middleZStart, 15, 16, middleZEnd)
		).reduce((v1, v2) -> VoxelShapes.combineAndSimplify(v1, v2, BooleanBiFunction.OR)).orElse(VoxelShapes.empty());
	}
}
