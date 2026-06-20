package com.Nanbin.Registry.RegBlock.CabDoor;

import net.minecraft.block.*;
import net.minecraft.block.enums.DoorHinge;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.mtr.mapping.holder.BlockView;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.holder.TextFormatting;
import org.mtr.mapping.holder.TooltipContext;
import org.mtr.mapping.mapper.TextHelper;

import java.util.List;

import static org.mtr.mod.Items.*;

public class BlockOrdinaryAPGCabDoor extends DoorBlock {

    public BlockOrdinaryAPGCabDoor(Settings settings) {
        super(settings, BlockSetType.IRON);
    }

    protected static final VoxelShape NORTH_SHAPE;
    protected static final VoxelShape SOUTH_SHAPE;
    protected static final VoxelShape EAST_SHAPE;
    protected static final VoxelShape WEST_SHAPE;
    protected static final VoxelShape NORTH_TOP_SHAPE;
    protected static final VoxelShape SOUTH_TOP_SHAPE;
    protected static final VoxelShape EAST_TOP_SHAPE;
    protected static final VoxelShape WEST_TOP_SHAPE;

    public VoxelShape getOutlineShape(BlockState state, net.minecraft.world.BlockView world, BlockPos pos, ShapeContext context) {
        Direction direction = (Direction)state.get(FACING);
        boolean bl = !(Boolean)state.get(OPEN);
        boolean bl2 = state.get(HINGE) == DoorHinge.RIGHT;
        DoubleBlockHalf half = state.get(HALF);
        VoxelShape northShape, southShape, eastShape, westShape;
        if (half != DoubleBlockHalf.UPPER) {
            northShape = NORTH_TOP_SHAPE;
            southShape = SOUTH_TOP_SHAPE;
            eastShape  = EAST_TOP_SHAPE;
            westShape  = WEST_TOP_SHAPE;
        } else {
            northShape = NORTH_SHAPE;
            southShape = SOUTH_SHAPE;
            eastShape  = EAST_SHAPE;
            westShape  = WEST_SHAPE;
        }
        switch (direction) {
            case EAST:
            default:
                if (half != DoubleBlockHalf.UPPER) {
                    return bl ? WEST_SHAPE : (bl2 ? SOUTH_SHAPE : NORTH_SHAPE);
                } else {
                    return bl ? WEST_TOP_SHAPE : (bl2 ? SOUTH_TOP_SHAPE : NORTH_TOP_SHAPE);
                }
            case SOUTH:
                if (half != DoubleBlockHalf.UPPER) {
                return bl ? NORTH_SHAPE : (bl2 ? WEST_SHAPE : EAST_SHAPE);
                } else {
                    return bl ? NORTH_TOP_SHAPE : (bl2 ? WEST_TOP_SHAPE : EAST_TOP_SHAPE);
                }
            case WEST:
                if (half != DoubleBlockHalf.UPPER) {
                return bl ? EAST_SHAPE : (bl2 ? NORTH_SHAPE : SOUTH_SHAPE);
                } else {
                    return bl ? EAST_TOP_SHAPE : (bl2 ? NORTH_TOP_SHAPE : SOUTH_TOP_SHAPE);
                }
            case NORTH:
                if (half != DoubleBlockHalf.UPPER) {
                return bl ? SOUTH_SHAPE : (bl2 ? EAST_SHAPE : WEST_SHAPE);
                } else {
                    return bl ? SOUTH_TOP_SHAPE : (bl2 ? EAST_TOP_SHAPE : WEST_TOP_SHAPE);
                }
        }
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }

        ItemStack handStack = player.getStackInHand(hand);
        Item handItem = handStack.getItem();

        if (handItem == CREATIVE_DRIVER_KEY.get().data || handItem == ADVANCED_DRIVER_KEY.get().data) {
            BlockState newState = state.cycle(OPEN);
            world.setBlockState(pos, newState, 10);

            if (newState.get(OPEN)) {
                player.sendMessage(Text.translatable("tips.cabdoor.open"), true);
            } else {
                player.sendMessage(Text.translatable("tips.cabdoor.close"), true);
            }
            return ActionResult.SUCCESS;
        }

        if (handItem == GUARD_KEY.get().data || handItem == BASIC_DRIVER_KEY.get().data) {
            player.sendMessage(Text.translatable("tips.cabdoor.has.low"), true);
            return ActionResult.FAIL;
        }

        player.sendMessage(Text.translatable("tips.cabdoor.has.nokey"), true);
        return ActionResult.FAIL;
    }

    public void addTooltips(ItemStack stack, @Nullable BlockView world, List<MutableText> tooltip, TooltipContext options) {
        tooltip.add(TextHelper.translatable("tooltip.block.cab_door", new Object[0]).formatted(TextFormatting.YELLOW));
    }

    static {
        NORTH_SHAPE = Block.createCuboidShape(0, 0, 12, 16, 16, 16);
        SOUTH_SHAPE = Block.createCuboidShape(0, 0, 0, 16, 16, 4);
        EAST_SHAPE = Block.createCuboidShape(0, 0, 0, 4, 16, 16);
        WEST_SHAPE = Block.createCuboidShape(14, 0, 0, 16, 16, 16);
        NORTH_TOP_SHAPE = Block.createCuboidShape(0, 0, 14, 16, 10, 16);
        SOUTH_TOP_SHAPE = Block.createCuboidShape(0, 0, 0, 16, 10, 2);
        EAST_TOP_SHAPE = Block.createCuboidShape(0, 0, 0, 4, 10, 16);
        WEST_TOP_SHAPE = Block.createCuboidShape(12, 0, 0, 16, 10, 16);
        }
}