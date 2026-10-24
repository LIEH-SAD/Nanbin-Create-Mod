package com.Nanbin.Registry.RegBlock;

import com.Nanbin.Init;
import com.Nanbin.entity.BlockEntityTypes;
import com.Nanbin.mapping.BlockEntityHelper;
import com.Nanbin.mapping.BlockExtension;
import com.Nanbin.mapping.IBlockExtension;
import com.Nanbin.mapping.Registry;
import com.Nanbin.mapping.TranslationProvider;
import com.Nanbin.packet.PacketOpenRoadNameScreen;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
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
import org.mtr.block.BlockRouteSignBase;
import org.mtr.block.IBlock;

import java.util.List;

import static org.mtr.block.IBlock.SIDE_EXTENDED;

/**
 * 路名标识：中间格放置，左右自动各延伸一格。
 * <p>
 * 4.1 适配：{@code BlockWithEntity} → 原版 {@link BlockEntityProvider}；
 * {@code readCompoundTag}/{@code writeCompoundTag} → {@code readNbt}/{@code writeNbt} + {@link NbtCompound}；
 * {@code getPos2}/{@code getWorld2}/{@code getCachedState2} → 原版方法名；
 * {@code onBreak2} 钩子由 {@link BlockExtension} 转发。
 */
public class BlockRoadName extends BlockExtension implements BlockEntityProvider {

	public BlockRoadName(AbstractBlock.Settings settings) {
		super(settings.luminance(state -> 10));
	}

	@Override
	public BlockState getPlacementState2(ItemPlacementContext ctx) {
		final Direction direction = ctx.getHorizontalPlayerFacing();
		final BlockState state = getDefaultState().with(Properties.HORIZONTAL_FACING, direction);
		final Direction right = direction.rotateYClockwise();
		final Direction left = direction.rotateYCounterclockwise();
		// 放置点即为中间格：左右各延伸一格
		if (IBlock.isReplaceable(ctx, left, 1) && IBlock.isReplaceable(ctx, right, 1)) {
			ctx.getWorld().setBlockState(ctx.getBlockPos().offset(left, 1), state.with(SIDE_EXTENDED, IBlock.EnumSide.LEFT));
			ctx.getWorld().setBlockState(ctx.getBlockPos().offset(right, 1), state.with(SIDE_EXTENDED, IBlock.EnumSide.RIGHT));
			return state.with(SIDE_EXTENDED, IBlock.EnumSide.MIDDLE);
		}
		return null;
	}

	@Override
	public ActionResult onUse2(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		return IBlockExtension.checkHoldingBrush(world, player, () -> {
			String[] texts = {"", "", "", ""};
			final net.minecraft.block.entity.BlockEntity blockEntity = world.getBlockEntity(pos);
			if (blockEntity instanceof BlockEntity entity) {
				texts = entity.getTexts();
			}
			Registry.sendPacketToClient((ServerPlayerEntity) player, new PacketOpenRoadNameScreen(pos, texts));
		});
	}

	@Override
	public void onBreak2(World world, BlockPos pos, BlockState state, PlayerEntity player) {
		final Direction direction = IBlock.getStatePropertySafe(state, Properties.HORIZONTAL_FACING);
		final Direction right = direction.rotateYClockwise();
		final Direction left = direction.rotateYCounterclockwise();
		final IBlock.EnumSide side = IBlock.getStatePropertySafe(state, SIDE_EXTENDED);
		if (side == IBlock.EnumSide.MIDDLE) {
			IBlockExtension.breakBlock(world, pos.offset(left, 1));
			IBlockExtension.breakBlock(world, pos.offset(right, 1));
		} else if (side == IBlock.EnumSide.LEFT) {
			IBlockExtension.breakBlock(world, pos.offset(right, 1));
			IBlockExtension.breakBlock(world, pos.offset(right, 2));
		} else if (side == IBlock.EnumSide.RIGHT) {
			IBlockExtension.breakBlock(world, pos.offset(left, 1));
			IBlockExtension.breakBlock(world, pos.offset(left, 2));
		}
	}

	@Override
	public VoxelShape getOutlineShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		final Direction direction = IBlock.getStatePropertySafe(state, Properties.HORIZONTAL_FACING);
		final IBlock.EnumSide side = IBlock.getStatePropertySafe(state, SIDE_EXTENDED);
		// 每格碰撞箱与各自模型一致：left/right 为半格，middle 为整格，z 7..9 薄板
		if (side == IBlock.EnumSide.LEFT) {
			return IBlock.getVoxelShapeByDirection(8, 0, 7, 16, 16, 9, direction);
		} else if (side == IBlock.EnumSide.RIGHT) {
			return IBlock.getVoxelShapeByDirection(0, 0, 7, 8, 16, 9, direction);
		} else {
			return IBlock.getVoxelShapeByDirection(0, 0, 7, 16, 16, 9, direction);
		}
	}

	@Override
	public void addBlockProperties(List<Property<?>> properties) {
		properties.add(Properties.HORIZONTAL_FACING);
		properties.add(SIDE_EXTENDED);
	}

	@Override
	public net.minecraft.block.entity.BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new BlockEntity(pos, state);
	}

	@Override
	public void addTooltips(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(TranslationProvider.BRUSH_USE.getMutableText().formatted(Formatting.DARK_GRAY));
	}

	public static class BlockEntity extends BlockRouteSignBase.BlockEntityBase {

		private static final String KEY_TEXT_1 = "roadNameText1";
		private static final String KEY_TEXT_2 = "roadNameText2";
		private static final String KEY_TEXT_3 = "roadNameText3";
		private static final String KEY_TEXT_4 = "roadNameText4";

		private String text1 = "";
		private String text2 = "";
		private String text3 = "";
		private String text4 = "";

		public BlockEntity(BlockPos pos, BlockState state) {
			super(BlockEntityTypes.BLOCK_ROAD_NAME.get(), pos, state);
		}

		/** 获取四个文本框的内容（索引 0-3）。 */
		public String[] getTexts() {
			return new String[]{text1, text2, text3, text4};
		}

		/** 设置四个文本框的内容，并同步到另外两个方块。 */
		public void setTexts(String[] texts) {
			setTextsInternal(texts);
			for (final BlockPos otherPos : getOtherPositions()) {
				final net.minecraft.block.entity.BlockEntity blockEntity = getWorld().getBlockEntity(otherPos);
				if (blockEntity instanceof BlockEntity entity) {
					entity.setTextsInternal(texts);
				}
			}
			BlockEntityHelper.markDirty(this);
		}

		private void setTextsInternal(String[] texts) {
			text1 = texts.length > 0 ? texts[0] : "";
			text2 = texts.length > 1 ? texts[1] : "";
			text3 = texts.length > 2 ? texts[2] : "";
			text4 = texts.length > 3 ? texts[3] : "";
			BlockEntityHelper.markDirty(this);
		}

		/** 获取另外两个方块的坐标。 */
		private BlockPos[] getOtherPositions() {
			final Direction facing = IBlock.getStatePropertySafe(getCachedState(), Properties.HORIZONTAL_FACING);
			final IBlock.EnumSide side = IBlock.getStatePropertySafe(getCachedState(), SIDE_EXTENDED);
			final Direction right = facing.rotateYClockwise();
			final Direction left = facing.rotateYCounterclockwise();
			final BlockPos pos = getPos();
			if (side == IBlock.EnumSide.MIDDLE) {
				return new BlockPos[]{pos.offset(left, 1), pos.offset(right, 1)};
			} else if (side == IBlock.EnumSide.LEFT) {
				return new BlockPos[]{pos.offset(right, 1), pos.offset(right, 2)};
			} else {
				return new BlockPos[]{pos.offset(left, 1), pos.offset(left, 2)};
			}
		}

		@Override
		protected void readNbt(NbtCompound nbt) {
			super.readNbt(nbt);
			text1 = nbt.getString(KEY_TEXT_1);
			text2 = nbt.getString(KEY_TEXT_2);
			text3 = nbt.getString(KEY_TEXT_3);
			text4 = nbt.getString(KEY_TEXT_4);
		}

		@Override
		protected void writeNbt(NbtCompound nbt) {
			super.writeNbt(nbt);
			nbt.putString(KEY_TEXT_1, text1);
			nbt.putString(KEY_TEXT_2, text2);
			nbt.putString(KEY_TEXT_3, text3);
			nbt.putString(KEY_TEXT_4, text4);
		}

		public void setData(long platformId) {
			setPlatformId(platformId);
			for (final BlockPos otherPos : getOtherPositions()) {
				final net.minecraft.block.entity.BlockEntity blockEntity = getWorld().getBlockEntity(otherPos);
				if (blockEntity instanceof BlockEntity entity) {
					entity.setPlatformId(platformId);
					BlockEntityHelper.markDirty(entity);
				} else {
					Init.LOGGER.error("BlockRoadName.BlockEntity: Unable to set data for block entity at {}", otherPos.toShortString());
				}
			}
			BlockEntityHelper.markDirty(this);
		}
	}
}
