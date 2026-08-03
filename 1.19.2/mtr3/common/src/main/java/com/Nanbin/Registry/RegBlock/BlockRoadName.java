package com.Nanbin.Registry.RegBlock;

import com.Nanbin.entity.BlockEntityTypes;
import com.Nanbin.mapping.Registry;
import com.Nanbin.packet.PacketHandler;
import mtr.Items;
import mtr.block.IBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * @author LIEH-SAD
 * @see BlockEntity
 * @since 1.0.0
 */
public class BlockRoadName extends HorizontalFacingBlock implements BlockEntityProvider {

	public static final DirectionProperty FACING = HorizontalFacingBlock.FACING;
	public static final EnumProperty<EnumSide> SIDE = EnumProperty.of("side", EnumSide.class);

	public BlockRoadName(Settings settings) {
		super(settings);
		setDefaultState(getDefaultState().with(FACING, Direction.NORTH).with(SIDE, EnumSide.SINGLE));
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		final Direction direction = ctx.getPlayerFacing();
		final BlockState state = getDefaultState().with(FACING, direction);
		final Direction right = direction.rotateYClockwise();
		final Direction left = direction.rotateYCounterclockwise();
		// 放置点即为中间格：左右各延伸一格
		if (isReplaceable(ctx, left) && isReplaceable(ctx, right)) {
			ctx.getWorld().setBlockState(ctx.getBlockPos().offset(left, 1), state.with(SIDE, EnumSide.LEFT));
			ctx.getWorld().setBlockState(ctx.getBlockPos().offset(right, 1), state.with(SIDE, EnumSide.RIGHT));
			return state.with(SIDE, EnumSide.MIDDLE);
		}
		return null;
	}

	private static boolean isReplaceable(ItemPlacementContext ctx, Direction dir) {
		return ctx.getWorld().getBlockState(ctx.getBlockPos().offset(dir, 1)).canReplace(ctx);
	}

	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		if (player.isHolding(Items.BRUSH.get())) {
			if (!world.isClient) {
				String[] texts = new String[]{"", "", "", ""};
				final net.minecraft.block.entity.BlockEntity blockEntity = world.getBlockEntity(pos);
				if (blockEntity instanceof BlockEntity entity) {
					texts = entity.getTexts();
				}
				final String[] finalTexts = texts;
				Registry.sendPacketToClient((ServerPlayerEntity) player, PacketHandler.PACKET_OPEN_ROAD_NAME, buf -> {
					buf.writeBlockPos(pos);
					buf.writeVarInt(finalTexts.length);
					for (final String text : finalTexts) {
						buf.writeString(text);
					}
				});
			}
			return ActionResult.SUCCESS;
		}
		return ActionResult.PASS;
	}

	@Override
	public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
		final Direction direction = state.get(FACING);
		final Direction right = direction.rotateYClockwise();
		final Direction left = direction.rotateYCounterclockwise();
		final EnumSide side = state.get(SIDE);
		if (side == EnumSide.MIDDLE) {
			world.breakBlock(pos.offset(left, 1), true);
			world.breakBlock(pos.offset(right, 1), true);
		} else if (side == EnumSide.LEFT) {
			world.breakBlock(pos.offset(right, 1), true);
			world.breakBlock(pos.offset(right, 2), true);
		} else if (side == EnumSide.RIGHT) {
			world.breakBlock(pos.offset(left, 1), true);
			world.breakBlock(pos.offset(left, 2), true);
		}
		super.onBreak(world, pos, state, player);
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		final Direction direction = state.get(FACING);
		final EnumSide side = state.get(SIDE);
		// 每格碰撞箱与各自模型一致：left/right 为半块，middle 为整块，z 7..9 薄板
		if (side == EnumSide.LEFT) {
			return IBlock.getVoxelShapeByDirection(8, 0, 7, 16, 16, 9, direction);
		} else if (side == EnumSide.RIGHT) {
			return IBlock.getVoxelShapeByDirection(0, 0, 7, 8, 16, 9, direction);
		} else {
			return IBlock.getVoxelShapeByDirection(0, 0, 7, 16, 16, 9, direction);
		}
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING, SIDE);
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new BlockEntity(pos, state);
	}

	public enum EnumSide implements StringIdentifiable {
		LEFT("left"), RIGHT("right"), MIDDLE("middle"), SINGLE("single");

		private final String name;

		EnumSide(String name) {
			this.name = name;
		}

		@Override
		public String asString() {
			return name;
		}
	}

	public static class BlockEntity extends net.minecraft.block.entity.BlockEntity {

		private static final String KEY_TEXT_1 = "roadNameText1";
		private static final String KEY_TEXT_2 = "roadNameText2";
		private static final String KEY_TEXT_3 = "roadNameText3";
		private static final String KEY_TEXT_4 = "roadNameText4";

		private String text1 = "";
		private String text2 = "";
		private String text3 = "";
		private String text4 = "";

		public BlockEntity(BlockPos pos, BlockState state) {
			super(BlockEntityTypes.ROAD_NAME.get(), pos, state);
		}

		/** 获取四个文本框的内容（索引 0-3）。 */
		public String[] getTexts() {
			return new String[]{text1, text2, text3, text4};
		}

		/** 设置四个文本框的内容，并同步到另外两个方块。 */
		public void setTexts(String[] texts) {
			setTextsInternal(texts);
			for (final BlockPos otherPos : getOtherPositions()) {
				if (world != null) {
					final net.minecraft.block.entity.BlockEntity blockEntity = world.getBlockEntity(otherPos);
					if (blockEntity instanceof BlockEntity entity) {
						entity.setTextsInternal(texts);
					}
				}
			}
			syncToClients();
		}

		private void setTextsInternal(String[] texts) {
			text1 = texts.length > 0 ? texts[0] : "";
			text2 = texts.length > 1 ? texts[1] : "";
			text3 = texts.length > 2 ? texts[2] : "";
			text4 = texts.length > 3 ? texts[3] : "";
			markDirty();
			syncToClients();
		}

		/**
		 * 主动向客户端发送方块实体更新包。
		 * 仅 markDirty() 只会写盘，不会触发 toUpdatePacket()，导致客户端永远显示旧数据。
		 */
		private void syncToClients() {
			if (world != null && !world.isClient) {
				world.updateListeners(getPos(), getCachedState(), getCachedState(), 3);
			}
		}

		/** 获取另外两个方块的坐标。 */
		private BlockPos[] getOtherPositions() {
			final Direction facing = getCachedState().get(FACING);
			final EnumSide side = getCachedState().get(SIDE);
			final Direction right = facing.rotateYClockwise();
			final Direction left = facing.rotateYCounterclockwise();
			final BlockPos pos = getPos();
			if (side == EnumSide.MIDDLE) {
				return new BlockPos[]{pos.offset(left, 1), pos.offset(right, 1)};
			} else if (side == EnumSide.LEFT) {
				return new BlockPos[]{pos.offset(right, 1), pos.offset(right, 2)};
			} else {
				return new BlockPos[]{pos.offset(left, 1), pos.offset(left, 2)};
			}
		}

		@Override
		public void readNbt(NbtCompound nbt) {
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

		@Override
		public Packet<ClientPlayPacketListener> toUpdatePacket() {
			return BlockEntityUpdateS2CPacket.create(this);
		}

		@Override
		public NbtCompound toInitialChunkDataNbt() {
			return createNbt();
		}
	}
}