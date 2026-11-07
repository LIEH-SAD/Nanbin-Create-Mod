package com.Nanbin.Registry.RegBlock.base;

import com.Nanbin.Init;
import com.Nanbin.mapping.IBlockExtension;
import mtr.MTRClient;
import mtr.mappings.BlockEntityMapper;
import mtr.mappings.BlockMapper;
import mtr.mappings.EntityBlockMapper;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 自定义颜色方块基类（MTR 4 {@code com.Nanbin.Registry.RegBlock.base.BlockCustomColorBase} 的 MTR 3.6.3 版）。
 *
 * <p>MTR 4 的 {@code BlockExtension + BlockWithEntity + BlockEntityExtension} 在 MTR 3 对应
 * {@link BlockMapper} + {@link EntityBlockMapper} + {@link BlockEntityMapper}；
 * {@code InitClient.getStationColor} 换成 MTR 3 的 {@link MTRClient#getStationColor(BlockPos)}。</p>
 *
 * <p>刷子点击打开编辑界面的数据包通道由 Lead 的 packet 层统一接入（MTR 4 的
 * {@code PacketOpenBlockEntityScreen} 在目标侧尚无等价物），因此本基类暂不接管右键。</p>
 */
public abstract class BlockCustomColorBase extends BlockMapper implements EntityBlockMapper {

	public BlockCustomColorBase(Settings settings) {
		super(settings);
	}

	/** 子类必须提供方块实体；显示声明以消除接口默认方法与抽象方法的重叠。 */
	@Override
	public abstract BlockEntityMapper createBlockEntity(BlockPos pos, BlockState state);

	@Override
	public String getTranslationKey() {
		return super.getTranslationKey().replace("block.tjmetro.custom_color_", "block.minecraft.");
	}

	@Override
	public void appendTooltip(ItemStack stack, @Nullable BlockView world, List<Text> tooltip, TooltipContext options) {
		tooltip.add(Text.translatable("tooltip.tjmetro.custom_color").formatted(Formatting.GRAY));
	}

	public static class BlockEntityBase extends BlockEntityMapper {

		public final String COLOR_ID = "color";
		public int color = -1;

		public BlockEntityBase(BlockEntityType<?> type, BlockPos blockPos, BlockState blockState) {
			super(type, blockPos, blockState);
		}

		public int getDefaultColor(BlockPos pos) {
			return MTRClient.getStationColor(pos);
		}

		@Override
		public void readCompoundTag(NbtCompound compoundTag) {
			color = compoundTag.getInt(COLOR_ID);
			super.readCompoundTag(compoundTag);
		}

		@Override
		public void writeCompoundTag(NbtCompound compoundTag) {
			compoundTag.putInt(COLOR_ID, color);
			super.writeCompoundTag(compoundTag);
		}

		public void setData(int color) {
			this.color = color;
			markDirty();
		}
	}
}
