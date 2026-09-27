package com.Nanbin.Registry.RegBlock.CabDoor;

import com.Nanbin.mapping.DoorBlockExtension;
import com.Nanbin.mapping.TranslationProvider;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.mtr.block.IBlock;
import org.mtr.registry.Items;

import java.util.List;

import static com.Nanbin.mapping.DirectionHelper.FACING;

/**
 * 普通 APG 轿厢门（用司机钥匙开关）。
 * <p>
 * 4.1 相比 4.0：{@code org.mtr.mod.Items} → {@link Items}；{@code TextHelper.translatable} → {@link Text#translatable}；
 * MTR 4.1 已经没有 GUARD_KEY，所以只保留「普通钥匙权限不足」与「没有钥匙」两条提示。
 */
public class BlockOrdinaryAPGCabDoor extends DoorBlockExtension {

	protected static final VoxelShape NORTH_SHAPE;
	protected static final VoxelShape SOUTH_SHAPE;
	protected static final VoxelShape EAST_SHAPE;
	protected static final VoxelShape WEST_SHAPE;
	protected static final VoxelShape NORTH_TOP_SHAPE;
	protected static final VoxelShape SOUTH_TOP_SHAPE;
	protected static final VoxelShape EAST_TOP_SHAPE;
	protected static final VoxelShape WEST_TOP_SHAPE;

	public BlockOrdinaryAPGCabDoor(AbstractBlock.Settings settings) {
		super(settings.nonOpaque());
	}

	@Override
	public VoxelShape getOutlineShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		final Direction direction = IBlock.getStatePropertySafe(state, FACING);
		final boolean closed = !isOpen(state);
		final boolean rightHinge = isRightHinge(state);
		final boolean upper = isUpperHalf(state);
		return switch (direction) {
			case SOUTH -> {
				if (!upper) {
					yield closed ? NORTH_SHAPE : (rightHinge ? WEST_SHAPE : EAST_SHAPE);
				} else {
					yield closed ? NORTH_TOP_SHAPE : (rightHinge ? WEST_TOP_SHAPE : EAST_TOP_SHAPE);
				}
			}
			case WEST -> {
				if (!upper) {
					yield closed ? EAST_SHAPE : (rightHinge ? NORTH_SHAPE : SOUTH_SHAPE);
				} else {
					yield closed ? EAST_TOP_SHAPE : (rightHinge ? NORTH_TOP_SHAPE : SOUTH_TOP_SHAPE);
				}
			}
			case NORTH -> {
				if (!upper) {
					yield closed ? SOUTH_SHAPE : (rightHinge ? EAST_SHAPE : WEST_SHAPE);
				} else {
					yield closed ? SOUTH_TOP_SHAPE : (rightHinge ? EAST_TOP_SHAPE : WEST_TOP_SHAPE);
				}
			}
			default -> {
				if (!upper) {
					yield closed ? WEST_SHAPE : (rightHinge ? SOUTH_SHAPE : NORTH_SHAPE);
				} else {
					yield closed ? WEST_TOP_SHAPE : (rightHinge ? SOUTH_TOP_SHAPE : NORTH_TOP_SHAPE);
				}
			}
		};
	}

	@Override
	public ActionResult onUse2(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (world.isClient) {
			return ActionResult.SUCCESS;
		}

		final Item handItem = player.getStackInHand(Hand.MAIN_HAND).getItem();
		if (handItem == Items.CREATIVE_DRIVER_KEY.get() || handItem == Items.ADVANCED_DRIVER_KEY.get()) {
			final BlockState newState = cycleOpenState(state);
			world.setBlockState(pos, newState, 10);
			player.sendMessage(Text.translatable(isOpen(newState) ? "tips.cabdoor.open" : "tips.cabdoor.close"), true);
			return ActionResult.SUCCESS;
		}

		if (handItem == Items.BASIC_DRIVER_KEY.get()) {
			player.sendMessage(Text.translatable("tips.cabdoor.has.low"), true);
			return ActionResult.FAIL;
		}

		player.sendMessage(Text.translatable("tips.cabdoor.has.nokey"), true);
		return ActionResult.FAIL;
	}

	@Override
	public void addTooltips(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(TranslationProvider.BLOCK_CAB_DOOR.getMutableText().formatted(Formatting.GRAY));
	}

	static {
		NORTH_SHAPE = Block.createCuboidShape(0, 0, 12, 16, 16, 16);
		SOUTH_SHAPE = Block.createCuboidShape(0, 0, 0, 16, 16, 4);
		EAST_SHAPE = Block.createCuboidShape(0, 0, 0, 4, 16, 16);
		WEST_SHAPE = Block.createCuboidShape(14, 0, 0, 16, 16, 16);
		NORTH_TOP_SHAPE = Block.createCuboidShape(0, 0, 12, 16, 10, 16);
		SOUTH_TOP_SHAPE = Block.createCuboidShape(0, 0, 0, 16, 10, 4);
		EAST_TOP_SHAPE = Block.createCuboidShape(0, 0, 0, 4, 10, 16);
		WEST_TOP_SHAPE = Block.createCuboidShape(12, 0, 0, 16, 10, 16);
	}
}
