package com.Nanbin.Registry.RegBlock;

import com.Nanbin.entity.BlockEntityTypes;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import org.mtr.block.IBlock;

/** CRT 屏蔽门（一代）。 */
public class BlockCRTAPGDoor1 extends BlockCRTAPGDoorBase {

	public BlockCRTAPGDoor1(AbstractBlock.Settings settings) {
		super(settings);
	}

	@Override
	protected VoxelShape getUpperHalfShape(Direction facing) {
		return IBlock.getVoxelShapeByDirection(0, 0, 0, 16, 2, 4, facing);
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new BlockEntity(pos, state);
	}

	public static class BlockEntity extends DoorBlockEntity {

		public BlockEntity(BlockPos pos, BlockState state) {
			super(BlockEntityTypes.CRT_APG_DOOR_1.get(), pos, state);
		}
	}
}
