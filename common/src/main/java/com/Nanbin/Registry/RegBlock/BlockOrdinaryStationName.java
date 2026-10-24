package com.Nanbin.Registry.RegBlock;

import com.Nanbin.entity.BlockEntityTypes;
import com.Nanbin.mapping.Registry;
import com.Nanbin.mapping.TranslationProvider;
import com.Nanbin.packet.PacketHandler;
import mtr.Items;
import mtr.block.BlockRouteSignBase;
import net.minecraft.block.BlockState;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
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
 * 普通站名牌：白底黑字的贴墙站名牌。用刷子右键打开 MTR 的站台/线路选择器，
 * 渲染由 {@link com.Nanbin.client.Render.RenderOrdinaryStationName} 完成。
 */
public class BlockOrdinaryStationName extends BlockStationNameBase {

	public BlockOrdinaryStationName(Settings settings) {
		super(settings);
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new BlockEntity(pos, state);
	}

	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		if (player.isHolding(Items.BRUSH.get())) {
			if (!world.isClient) {
				Registry.sendPacketToClient((ServerPlayerEntity) player, PacketHandler.PACKET_OPEN_CRT_STATION_NAME, buf -> buf.writeBlockPos(pos));
			}
			return ActionResult.SUCCESS;
		}
		return ActionResult.PASS;
	}

	@Override
	public void appendTooltip(ItemStack stack, @Nullable BlockView world, List<Text> tooltip, TooltipContext options) {
		tooltip.add(TranslationProvider.TOOLTIP_STATION_COLOR.getText().copy().formatted(Formatting.DARK_GRAY));
		tooltip.add(TranslationProvider.BRUSH_USE.getText().copy().formatted(Formatting.DARK_GRAY));
	}

	/**
	 * 方块实体：继承 MTR 的 {@link BlockRouteSignBase.TileEntityRouteSignBase}，
	 * 站台 ID 由 MTR 保存并同步；线路编号额外缓存一份（MTR 客户端数据不含完整线路编号）。
	 */
	public static class BlockEntity extends BlockRouteSignBase.TileEntityRouteSignBase {

		private static final String KEY_ROUTE_NUMBER = "routeNumber";

		private String routeNumber = "";

		public BlockEntity(BlockPos pos, BlockState state) {
			super(BlockEntityTypes.ORDINARY_STATION_NAME.get(), pos, state);
		}

		/** 站名颜色：普通站名牌为白底黑字，固定返回白色。 */
		public int getColor(BlockState state) {
			return -1;
		}

		public String getRouteNumber() {
			return routeNumber == null ? "" : routeNumber;
		}

		/** 解析主题色 / 线路色 / 线路编号（无线路数据时回退为站名色）。 */
		public BlockCRTStationName1.BlockEntity.ResolvedRouteData getResolvedData(int fallbackColor) {
			return BlockCRTStationName1.BlockEntity.resolveRouteData(getPlatformId(), fallbackColor, routeNumber);
		}

		@Override
		public void readCompoundTag(NbtCompound nbt) {
			super.readCompoundTag(nbt);
			routeNumber = nbt.getString(KEY_ROUTE_NUMBER);
		}

		@Override
		public void writeCompoundTag(NbtCompound nbt) {
			super.writeCompoundTag(nbt);
			nbt.putString(KEY_ROUTE_NUMBER, routeNumber == null ? "" : routeNumber);
		}
	}
}
