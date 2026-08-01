package com.Nanbin.Registry.RegBlock;

import com.Nanbin.Init;
import com.Nanbin.entity.BlockEntityTypes;
import com.Nanbin.mapping.IBlockExtension;
import com.Nanbin.packet.PacketOpenRoadNameScreen;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mapping.mapper.BlockExtension;
import org.mtr.mapping.mapper.BlockWithEntity;
import org.mtr.mapping.mapper.DirectionHelper;
import org.mtr.mapping.tool.HolderBase;
import org.mtr.mod.Blocks;
import org.mtr.mod.block.BlockRouteSignBase;
import org.mtr.mod.block.IBlock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

import static org.mtr.mod.block.IBlock.SIDE_EXTENDED;

/**
 * @author ZiYueCommentary
 * @see BlockEntity
 * @since 1.0.0
 */

public class BlockRoadName extends BlockExtension implements DirectionHelper, BlockWithEntity
{
    public BlockRoadName() {
        this(Blocks.createDefaultBlockSettings(true, state -> 10));
    }

    public BlockRoadName(BlockSettings blockSettings) {
        super(blockSettings);
    }

    @Override
    public @Nullable BlockState getPlacementState2(ItemPlacementContext ctx) {
        final Direction direction = ctx.getPlayerFacing();
        final BlockState state = getDefaultState2().with(new Property<>(FACING.data), direction.data);
        final Direction right = direction.rotateYClockwise();
        final Direction left = direction.rotateYCounterclockwise();
        // 放置点即为中间格：左右各延伸一格
        if (IBlock.isReplaceable(ctx, left, 1) && IBlock.isReplaceable(ctx, right, 1)) {
            ctx.getWorld().setBlockState(ctx.getBlockPos().offset(left, 1), state.with(new Property<>(SIDE_EXTENDED.data), IBlock.EnumSide.LEFT));
            ctx.getWorld().setBlockState(ctx.getBlockPos().offset(right, 1), state.with(new Property<>(SIDE_EXTENDED.data), IBlock.EnumSide.RIGHT));
            return state.with(new Property<>(SIDE_EXTENDED.data), IBlock.EnumSide.MIDDLE);
        }
        return null;
    }

    @Override
    public @Nonnull ActionResult onUse2(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        return IBlockExtension.checkHoldingBrush(world, player, () -> {
            String[] texts = new String[]{"", "", "", ""};
            final org.mtr.mapping.holder.BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity != null && blockEntity.data instanceof BlockEntity entity) {
                texts = entity.getTexts();
            }
            Init.REGISTRY.sendPacketToClient(ServerPlayerEntity.cast(player), new PacketOpenRoadNameScreen(pos, texts));
        });
    }

    @Override
    public void onBreak2(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        final Direction direction = IBlock.getStatePropertySafe(state, FACING);
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
        super.onBreak2(world, pos, state, player);
    }

    @Override
    public @Nonnull VoxelShape getOutlineShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        final Direction direction = IBlock.getStatePropertySafe(state, FACING);
        final IBlock.EnumSide side = IBlock.getStatePropertySafe(state, SIDE_EXTENDED);
        // 每格碰撞箱与各自模型一致：left/right 为半块，middle 为整块，z 7..9 薄板
        if (side == IBlock.EnumSide.LEFT) {
            return IBlock.getVoxelShapeByDirection(8, 0, 7, 16, 16, 9, direction);
        } else if (side == IBlock.EnumSide.RIGHT) {
            return IBlock.getVoxelShapeByDirection(0, 0, 7, 8, 16, 9, direction);
        } else {
            return IBlock.getVoxelShapeByDirection(0, 0, 7, 16, 16, 9, direction);
        }
    }

    @Override
    public void addBlockProperties(List<HolderBase<?>> properties) {
        properties.add(FACING);
        properties.add(SIDE_EXTENDED);
    }

    @Override
    public BlockEntityExtension createBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new BlockEntity(blockPos, blockState);
    }

    public static class BlockEntity extends BlockRouteSignBase.BlockEntityBase
    {
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
                final org.mtr.mapping.holder.BlockEntity blockEntity = this.getWorld2().getBlockEntity(otherPos);
                if (blockEntity != null && blockEntity.data instanceof BlockEntity entity) {
                    entity.setTextsInternal(texts);
                }
            }
            markDirty2();
        }

        private void setTextsInternal(String[] texts) {
            text1 = texts.length > 0 ? texts[0] : "";
            text2 = texts.length > 1 ? texts[1] : "";
            text3 = texts.length > 2 ? texts[2] : "";
            text4 = texts.length > 3 ? texts[3] : "";
            markDirty2();
        }

        /** 获取另外两个方块的坐标。 */
        private BlockPos[] getOtherPositions() {
            final Direction facing = IBlock.getStatePropertySafe(this.getCachedState2(), FACING);
            final IBlock.EnumSide side = IBlock.getStatePropertySafe(this.getCachedState2(), SIDE_EXTENDED);
            final Direction right = facing.rotateYClockwise();
            final Direction left = facing.rotateYCounterclockwise();
            final BlockPos pos = this.getPos2();
            if (side == IBlock.EnumSide.MIDDLE) {
                return new BlockPos[]{pos.offset(left, 1), pos.offset(right, 1)};
            } else if (side == IBlock.EnumSide.LEFT) {
                return new BlockPos[]{pos.offset(right, 1), pos.offset(right, 2)};
            } else {
                return new BlockPos[]{pos.offset(left, 1), pos.offset(left, 2)};
            }
        }

        @Override
        public void readCompoundTag(CompoundTag compoundTag) {
            super.readCompoundTag(compoundTag);
            text1 = compoundTag.getString(KEY_TEXT_1);
            text2 = compoundTag.getString(KEY_TEXT_2);
            text3 = compoundTag.getString(KEY_TEXT_3);
            text4 = compoundTag.getString(KEY_TEXT_4);
        }

        @Override
        public void writeCompoundTag(CompoundTag compoundTag) {
            super.writeCompoundTag(compoundTag);
            compoundTag.putString(KEY_TEXT_1, text1);
            compoundTag.putString(KEY_TEXT_2, text2);
            compoundTag.putString(KEY_TEXT_3, text3);
            compoundTag.putString(KEY_TEXT_4, text4);
        }

        public void setData(long platformId) {
            this.setPlatformId(platformId);
            for (final BlockPos otherPos : getOtherPositions()) {
                final org.mtr.mapping.holder.BlockEntity blockEntity = this.getWorld2().getBlockEntity(otherPos);
                if (blockEntity != null && blockEntity.data instanceof BlockEntity entity) {
                    entity.setPlatformId(platformId);
                    entity.markDirty2();
                } else {
                    Init.LOGGER.error("BlockRoadName.BlockEntity: Unable to set data for block entity at {}", otherPos.toShortString());
                }
            }
            markDirty2();
        }
    }
}
