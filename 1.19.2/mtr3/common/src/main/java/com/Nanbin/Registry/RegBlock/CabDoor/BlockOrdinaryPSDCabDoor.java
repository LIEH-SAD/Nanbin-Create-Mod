package com.Nanbin.Registry.RegBlock.CabDoor;

import mtr.Items;
import mtr.block.IBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.enums.DoorHinge;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Style;
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

import java.util.List;

public class BlockOrdinaryPSDCabDoor extends DoorBlock {
    protected static final VoxelShape NORTH_SHAPE;
    protected static final VoxelShape SOUTH_SHAPE;
    protected static final VoxelShape EAST_SHAPE;
    protected static final VoxelShape WEST_SHAPE;

    static {
        NORTH_SHAPE = Block.createCuboidShape(0, 0, 0, 16, 16, 4);
        SOUTH_SHAPE = Block.createCuboidShape(0, 0, 12, 16, 16, 16);
        EAST_SHAPE = Block.createCuboidShape(12, 0, 0, 16, 16, 16);
        WEST_SHAPE = Block.createCuboidShape(0, 0, 0, 4, 16, 16);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        boolean hasKey = player.getMainHandStack().isOf(Items.DRIVER_KEY.get()) || player.getOffHandStack().isOf(Items.DRIVER_KEY.get());
        if (!hasKey) {
            player.sendMessage(Text.translatable("tips.cabdoor.has.nokey"), true);
            return ActionResult.FAIL;
        }
        if (!world.isClient) {
            state = state.cycle(OPEN);
            world.setBlockState(pos, state, 10);
        }
        player.sendMessage(Text.translatable("tips.cabdoor.open"), true);
        return ActionResult.SUCCESS;
    }

    public BlockOrdinaryPSDCabDoor(Settings settings) {
        super(settings);
    }

    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        Direction direction = (Direction)state.get(FACING);
        boolean bl = !(Boolean)state.get(OPEN);
        boolean bl2 = state.get(HINGE) == DoorHinge.RIGHT;
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

    public void appendTooltip(ItemStack itemStack, BlockView blockGetter, List<Text> tooltip, TooltipContext tooltipFlag) {
        tooltip.add(mtr.mappings.Text.translatable("tooltip.nanbin.block.cab_door", new Object[0]).setStyle(Style.EMPTY.withColor(Formatting.GRAY)));
    }
}