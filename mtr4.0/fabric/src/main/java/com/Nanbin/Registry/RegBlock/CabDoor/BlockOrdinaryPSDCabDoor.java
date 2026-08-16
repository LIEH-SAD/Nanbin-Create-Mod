package com.Nanbin.Registry.RegBlock.CabDoor;

import com.Nanbin.mapping.DoorBlockExtension;
import com.Nanbin.mapping.TranslationProvider;
import org.jetbrains.annotations.Nullable;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.DirectionHelper;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mod.block.IBlock;

import java.util.List;

import static org.mtr.mod.Items.*;

public class BlockOrdinaryPSDCabDoor extends DoorBlockExtension {

    public BlockOrdinaryPSDCabDoor() {
        super(false, blockSettings -> blockSettings.nonOpaque());
    }

    protected static final VoxelShape NORTH_SHAPE;
    protected static final VoxelShape SOUTH_SHAPE;
    protected static final VoxelShape EAST_SHAPE;
    protected static final VoxelShape WEST_SHAPE;

    public VoxelShape getOutlineShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        Direction direction = IBlock.getStatePropertySafe(state, DirectionHelper.FACING);
        boolean bl = !isOpen(state);
        boolean bl2 = isRightHinge(state);
        switch (direction) {
            case WEST:
            default:
                return bl ? WEST_SHAPE : (bl2 ? SOUTH_SHAPE : NORTH_SHAPE);
            case NORTH:
                return bl ? NORTH_SHAPE : (bl2 ? WEST_SHAPE : EAST_SHAPE);
            case EAST:
                return bl ? EAST_SHAPE : (bl2 ? NORTH_SHAPE : SOUTH_SHAPE);
            case SOUTH:
                return bl ? SOUTH_SHAPE : (bl2 ? EAST_SHAPE : WEST_SHAPE);
        }
    }

    @Override
    public ActionResult onUse2(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }

        ItemStack handStack = player.getStackInHand(hand);
        Item handItem = handStack.getItem();

        if (handItem.data == CREATIVE_DRIVER_KEY.get().data || handItem.data == ADVANCED_DRIVER_KEY.get().data) {
            BlockState newState = cycleOpen(state);
            world.setBlockState(pos, newState, 10);

            if (isOpen(newState)) {
                player.sendMessage(Text.cast(TextHelper.translatable("tips.cabdoor.open")), true);
            } else {
                player.sendMessage(Text.cast(TextHelper.translatable("tips.cabdoor.close")), true);
            }
            return ActionResult.SUCCESS;
        }

        if (handItem.data == GUARD_KEY.get().data || handItem.data == BASIC_DRIVER_KEY.get().data) {
            player.sendMessage(Text.cast(TextHelper.translatable("tips.cabdoor.has.low")), true);
            return ActionResult.FAIL;
        }

        player.sendMessage(Text.cast(TextHelper.translatable("tips.cabdoor.has.nokey")), true);
        return ActionResult.FAIL;
    }

    public void addTooltips(ItemStack stack, @Nullable BlockView world, List<MutableText> tooltip, TooltipContext options) {
        tooltip.add(TranslationProvider.BLOCK_CAB_DOOR.getMutableText(new Object[0]).formatted(TextFormatting.GRAY));
    }

    static {
        NORTH_SHAPE = Block.createCuboidShape(0, 0, 0, 16, 16, 4);
        SOUTH_SHAPE = Block.createCuboidShape(0, 0, 12, 16, 16, 16);
        EAST_SHAPE = Block.createCuboidShape(12, 0, 0, 16, 16, 16);
        WEST_SHAPE = Block.createCuboidShape(0, 0, 0, 4, 16, 16);
    }
}
