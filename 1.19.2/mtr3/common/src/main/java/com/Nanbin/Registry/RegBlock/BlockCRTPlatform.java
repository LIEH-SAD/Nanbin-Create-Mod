package com.Nanbin.Registry.RegBlock;

import mtr.block.*;
import mtr.mappings.BlockDirectionalMapper;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Property;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;

public class BlockCRTPlatform extends BlockDirectionalMapper {
    private final boolean isIndented;
    public static final EnumProperty<EnumDoorType> DOOR_TYPE = EnumProperty.of("door_type", EnumDoorType.class);
    public static final IntProperty SIDE = IntProperty.of("side", 0, 4);

    public BlockCRTPlatform(AbstractBlock.Settings settings, boolean isIndented) {
        super(settings);
        this.isIndented = isIndented;
        this.setDefaultState((BlockState)this.getDefaultState().with(DOOR_TYPE, BlockCRTPlatform.EnumDoorType.NONE));
    }

    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState newState, WorldAccess world, BlockPos pos, BlockPos posFrom) {
        return this.getActualState(world, pos, state);
    }

    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return (BlockState)this.getDefaultState().with(FACING, ctx.getPlayerFacing());
    }

    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        if (this.isIndented) {
            Direction facing = (Direction) IBlock.getStatePropertySafe(state, FACING);
            return VoxelShapes.union(
                    IBlock.getVoxelShapeByDirection((double)0.0F, (double)0.0F, (double)6.0F, (double)16.0F, (double)13.0F, (double)16.0F, facing),
                    Block.createCuboidShape((double)0.0F, (double)12.0F, (double)0.0F, (double)16.0F, (double)16.0F, (double)16.0F));
        } else {
            return super.getOutlineShape(state, world, pos, context);
        }
    }

    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{FACING, DOOR_TYPE, SIDE});
    }

    private BlockState getActualState(BlockView world, BlockPos pos, BlockState state) {
        Direction facing = (Direction)IBlock.getStatePropertySafe(state, FACING);
        BlockState stateAbove = world.getBlockState(pos.up());
        Block blockAbove = stateAbove.getBlock();
        EnumDoorType doorType;
        if (!(blockAbove instanceof BlockPSDDoor) && !(blockAbove instanceof BlockPSDGlass) && !(blockAbove instanceof BlockPSDGlassEnd)) {
            if (!(blockAbove instanceof BlockAPGDoor) && !(blockAbove instanceof BlockAPGGlass) && !(blockAbove instanceof BlockAPGGlassEnd)) {
                doorType = BlockCRTPlatform.EnumDoorType.NONE;
            } else {
                doorType = BlockCRTPlatform.EnumDoorType.APG;
                facing = (Direction)IBlock.getStatePropertySafe(stateAbove, FACING);
            }
        } else {
            doorType = BlockCRTPlatform.EnumDoorType.PSD;
            facing = (Direction)IBlock.getStatePropertySafe(stateAbove, FACING);
        }

        boolean aboveIsDoor = blockAbove instanceof BlockPSDAPGDoorBase;
        BlockState stateLeftAbove = world.getBlockState(pos.up().offset(facing.rotateYCounterclockwise()));
        boolean leftAboveIsDoor = stateLeftAbove.getBlock() instanceof BlockPSDAPGDoorBase;
        BlockState stateRightAbove = world.getBlockState(pos.up().offset(facing.rotateYClockwise()));
        boolean rightAboveIsDoor = stateRightAbove.getBlock() instanceof BlockPSDAPGDoorBase;
        int side;
        if (aboveIsDoor && rightAboveIsDoor) {
            side = 2;
        } else if (aboveIsDoor && leftAboveIsDoor) {
            side = 3;
        } else if (rightAboveIsDoor) {
            side = 1;
            facing = (Direction)IBlock.getStatePropertySafe(stateRightAbove, FACING);
        } else if (leftAboveIsDoor) {
            side = 4;
            facing = (Direction)IBlock.getStatePropertySafe(stateLeftAbove, FACING);
        } else {
            side = 0;
        }

        return (BlockState)((BlockState)((BlockState)state.with(FACING, facing)).with(DOOR_TYPE, doorType)).with(SIDE, side);
    }

    private static enum EnumDoorType implements StringIdentifiable {
        NONE("none"),
        PSD("psd"),
        APG("apg");

        private final String name;

        private EnumDoorType(String nameIn) {
            this.name = nameIn;
        }

        public String asString() {
            return this.name;
        }
    }
}
