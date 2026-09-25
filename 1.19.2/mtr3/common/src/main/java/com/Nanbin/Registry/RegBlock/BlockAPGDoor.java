package com.Nanbin.Registry.RegBlock;

import mtr.BlockEntityTypes;
import mtr.Items;
import mtr.block.BlockPSDAPGDoorBase;
import mtr.mappings.BlockEntityMapper;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;

public class BlockAPGDoor extends BlockPSDAPGDoorBase {
    public BlockAPGDoor() {
    }

    public BlockEntityMapper createBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityAPGDoor(pos, state);
    }

    public Item asItem() {
        return (Item)Items.APG_DOOR.get();
    }

    public static class TileEntityAPGDoor extends BlockPSDAPGDoorBase.TileEntityPSDAPGDoorBase {
        public TileEntityAPGDoor(BlockPos pos, BlockState state) {
            super((BlockEntityType)BlockEntityTypes.APG_DOOR_TILE_ENTITY.get(), pos, state);
        }
    }
}
