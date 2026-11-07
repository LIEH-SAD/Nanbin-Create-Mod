package com.Nanbin.client.Render;

import com.Nanbin.Registry.RegBlock.BlockCRTPSDTop1;
import com.Nanbin.client.Drawing.CustomFontTextureCache;
import com.Nanbin.client.Drawing.CustomFontTextureCache.FontType;
import com.Nanbin.client.Drawing.LineMapData;
import com.Nanbin.client.Drawing.LineMapDataResolver;
import mtr.block.BlockPSDAPGDoorBase;
import mtr.block.BlockPSDAPGGlassEndBase;
import mtr.block.BlockPSDTop;
import mtr.block.IBlock;
import mtr.client.ClientData;
import mtr.client.IDrawing;
import mtr.data.Platform;
import mtr.data.Route;
import mtr.data.Station;
import mtr.render.MoreRenderLayers;
import mtr.render.RenderRouteBase;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.joml.Quaternionf;
import net.minecraft.util.math.MathHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CRT 第一代高屏蔽门吊板渲染器：吊板本身承载 MTR 标准导视 —— 门上方画站牌
 * （本站 / 上一站 / 下一站 / 线路编号），玻璃上方画整条线路图。
 *
 * <p>MTR 4 → 3.6.3 适配：{@code MainRenderer.scheduleRender} → 直接用 {@link MoreRenderLayers} +
 * {@link IDrawing} 在当前矩阵栈上绘制；{@code InitClient.findClosePlatform} → MTR 3 的
 * {@code BlockPSDTop.TileEntityRouteBase#getPlatformId}；{@code MinecraftClientData.simplifiedRoutes} →
 * {@link ClientData#DATA_CACHE} 的 {@code requestPlatformIdToRoutes}（MTR 3.6.3 无 SimplifiedRoute）。</p>
 */
public class RenderCRTPSDTop1 implements BlockEntityRenderer<BlockCRTPSDTop1.BlockEntity> {

	private static final float Z_FACE = 1.95F / 16F;
	private static final float CONTENT_BOTTOM = 0.4375F;
	private static final float CONTENT_TOP = CONTENT_BOTTOM - 0.6F;
	private static final float PERSISTENT_BOTTOM = (CONTENT_TOP + CONTENT_BOTTOM) / 2F;
	private static final int PLATFORM_SEARCH_Y_OFFSET = 3;
	private static final int PLATFORM_SEARCH_DISTANCE = 5;
	private static final int VERTICAL_OFFSET_TO_PSD = 2;
	private static final int PLATFORM_SEARCH_FALLBACK_DISTANCE = 16;
	private static final FontType SIGN_FONT_TYPE = FontType.ALIBABA;
	private static final FontType SIGN_CIRCLE_FONT_TYPE = FontType.SOURCE_SANS_3;
	private static final int SIGN_STATION_FONT_SIZE = 105;
	private static final int SIGN_SIDE_FONT_SIZE = 70;
	private static final int SIGN_BADGE_FONT_SIZE = 46;
	private static final float SIGN_LATIN_FONT_RATIO = 0.58F;
	private static final float SIGN_GAP_RATIO = 0.05F;
	private static final int SIGN_TEXT_COLOR = 0xFF000000;
	private static final FontType MAP_NAME_FONT_TYPE = FontType.SOURCE_HAN;
	private static final FontType MAP_NUMBER_FONT_TYPE = FontType.SOURCE_SANS_3;
	private static final int MAP_NAME_FONT_SIZE = 114;
	private static final int MAP_NUMBER_FONT_SIZE = 60;
	private static final float MAP_LATIN_FONT_RATIO = 13F / 24F;
	private static final float MAP_GAP_RATIO = 0.45F;
	private static final int MAP_TEXT_COLOR = 0xFF111111;

	public RenderCRTPSDTop1(BlockEntityRendererFactory.Context context) {
	}

	@Override
	public void render(BlockCRTPSDTop1.BlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
		final World world = entity.getWorld();
		if (world == null) {
			return;
		}
		final BlockPos pos = entity.getPos();
		final BlockState state = world.getBlockState(pos);
		final Direction facing = IBlock.getStatePropertySafe(state, net.minecraft.block.HorizontalFacingBlock.FACING);
		final BlockPSDTop.EnumPersistent persistent = IBlock.getStatePropertySafe(state, BlockPSDTop.PERSISTENT);
		final boolean reverse = IBlock.getStatePropertySafe(state, BlockPSDTop.ARROW_DIRECTION) % 2 != 0;
		final long platformId = entity.getPlatformId(ClientData.PLATFORMS, ClientData.DATA_CACHE);
		if (platformId == 0) {
			return;
		}

		final net.minecraft.block.Block blockBelow = world.getBlockState(pos.down()).getBlock();
		final boolean door = blockBelow instanceof BlockPSDAPGDoorBase;
		if (!door && blockBelow instanceof BlockPSDAPGGlassEndBase) {
			// 端玻璃上方不显示
			return;
		}
		if (persistent == BlockPSDTop.EnumPersistent.BLANK) {
			// 刷子把吊板切成 blank 时不显示内容（与 MTR 相同）
			return;
		}

		final Group group = findGroup(world, pos, facing);
		final boolean persistentNone = persistent == BlockPSDTop.EnumPersistent.NONE;
		final float contentBottom = persistentNone ? CONTENT_BOTTOM : PERSISTENT_BOTTOM;
		final float bandHeight = contentBottom - CONTENT_TOP;

		final Identifier texture;
		if (door) {
			final PSDStationData data = resolvePSDStationData(platformId);
			if (data == null || data.stationName().isEmpty()) {
				return;
			}
			texture = CustomFontTextureCache.instance.getPSDStationSignTexture(
					data.stationName(), data.previousStationName(), data.nextStationName(), data.routeNumber(), data.stationNumber(),
					data.lineColor(), SIGN_TEXT_COLOR, group.count(), bandHeight,
					SIGN_FONT_TYPE, SIGN_CIRCLE_FONT_TYPE, SIGN_STATION_FONT_SIZE, SIGN_SIDE_FONT_SIZE, SIGN_BADGE_FONT_SIZE,
					SIGN_LATIN_FONT_RATIO, SIGN_GAP_RATIO, reverse);
		} else {
			final LineMapData data = LineMapDataResolver.resolve(platformId);
			if (data == null || data.stations().isEmpty()) {
				return;
			}
			texture = CustomFontTextureCache.instance.getFasciaLineMapTexture(data, group.count(), bandHeight,
					MAP_NAME_FONT_TYPE, MAP_NUMBER_FONT_TYPE, MAP_NAME_FONT_SIZE, MAP_NUMBER_FONT_SIZE,
					MAP_LATIN_FONT_RATIO, MAP_GAP_RATIO, MAP_TEXT_COLOR, reverse);
		}

		final int shadingColor = RenderRouteBase.getShadingColor(facing, 0xFFFFFFFF);
		// 按「本格下标 i / 组内总格数 N」切纹理：一扇门（2 格）上方的两块吊板各画一半，
		// 合起来才是横跨整扇门的大站牌。
		final float u1 = (float) group.index() / group.count();
		final float u2 = (float) (group.index() + 1) / group.count();

		matrices.push();
		// vanilla 的 BlockEntityRenderDispatcher 已将 matrices 平移到本方块坐标原点，因此用相对坐标。
		matrices.translate(0.5, 0.5, 0.5);
		matrices.multiply(new Quaternionf().rotationY(MathHelper.RADIANS_PER_DEGREE * -facing.asRotation()));
		matrices.multiply(new Quaternionf().rotationZ(MathHelper.RADIANS_PER_DEGREE * 180.0F));
		matrices.translate(0.0, 0.0, Z_FACE);
		final RenderLayer renderLayer = MoreRenderLayers.getLight(texture, true);
		final VertexConsumer vertexConsumer = vertexConsumers.getBuffer(renderLayer);
		IDrawing.drawTexture(matrices, vertexConsumer, -0.5F, CONTENT_TOP, 1.0F, bandHeight, u1, 0.0F, u2, 1.0F, facing, shadingColor, light);
		matrices.pop();
	}

	/** 吊板分组：本格在组内的下标（从乘客左手边数起，0 起）与组内总格数。 */
	private static final class Group {

		private final int index;
		private final int count;

		private Group(int index, int count) {
			this.index = index;
			this.count = count;
		}

		private int index() {
			return index;
		}

		private int count() {
			return count;
		}
	}

	/**
	 * 扫描本行吊板得到分组。分组以下方门／玻璃的分组属性为准（乘客视角最左端 LEFT、中间 MIDDLE、最右端 RIGHT，
	 * 与 MTR 的刷子分组一致），吊板自身无需同步该属性。
	 */
	private static Group findGroup(World world, BlockPos pos, Direction facing) {
		final Object kind = world.getBlockState(pos.down()).getBlock();
		final int left = countTowards(world, pos, facing.rotateYCounterclockwise(), kind, IBlock.EnumSide.LEFT);
		final int right = countTowards(world, pos, facing.rotateYClockwise(), kind, IBlock.EnumSide.RIGHT);
		return new Group(left, left + 1 + right);
	}

	/**
	 * 从 pos 出发沿 direction 数出本组的格数：遇到 MIDDLE 计入并继续，遇到 endSide（分组端点）计入并停止，
	 * 其它情况（非同类吊板、非同类下方方块、另一分组端点）立即停止。
	 */
	private static int countTowards(World world, BlockPos pos, Direction direction, Object kind, IBlock.EnumSide endSide) {
		int count = 0;
		BlockPos current = pos.offset(direction);
		while (world.getBlockState(current).getBlock() instanceof BlockCRTPSDTop1) {
			final BlockState below = world.getBlockState(current.down());
			if (below.getBlock() != kind) {
				break;
			}
			final IBlock.EnumSide side = sideOf(below);
			if (side == IBlock.EnumSide.MIDDLE) {
				count++;
			} else {
				if (side == endSide) {
					count++;
				}
				break;
			}
			current = current.offset(direction);
		}
		return count;
	}

	/** 取分组属性：门用 SIDE，玻璃／端玻璃用 SIDE_EXTENDED。 */
	private static IBlock.EnumSide sideOf(BlockState state) {
		return state.getBlock() instanceof BlockPSDAPGDoorBase
				? IBlock.getStatePropertySafe(state, IBlock.SIDE)
				: IBlock.getStatePropertySafe(state, IBlock.SIDE_EXTENDED);
	}

	/** 吊板导视数据：来源的 RouteMapGenerator.PSDStationData 在目标侧尚未提供，这里用 MTR 3 客户端数据现算。 */
	private record PSDStationData(String stationName, String previousStationName, String nextStationName, String routeNumber, String stationNumber, int lineColor) {
	}

	private static PSDStationData resolvePSDStationData(long platformId) {
		try {
			final Platform platform = ClientData.DATA_CACHE.platformIdMap.get(platformId);
			final Station station = ClientData.DATA_CACHE.platformIdToStation.get(platformId);
			final String stationName = station == null ? "" : station.name;
			final String stationNumber = platform == null || platform.name == null ? "" : platform.name;
			String previousStationName = null;
			String nextStationName = null;
			String routeNumber = "";
			int lineColor = station == null ? 0 : station.color;

			final List<mtr.client.ClientCache.PlatformRouteDetails> details = ClientData.DATA_CACHE.requestPlatformIdToRoutes(platformId);
			for (final mtr.client.ClientCache.PlatformRouteDetails detail : details) {
				lineColor = detail.routeColor;
				final int index = detail.currentStationIndex;
				final boolean circular = detail.circularState != Route.CircularState.NONE;
				final int stationCount = detail.stationDetails.size();
				if (index > 0) {
					previousStationName = detail.stationDetails.get(index - 1).stationName;
				} else if (index == 0 && circular && stationCount > 0) {
					// 环线：首站的前一站是末站
					previousStationName = detail.stationDetails.get(stationCount - 1).stationName;
				}
				if (index + 1 < stationCount) {
					nextStationName = detail.stationDetails.get(index + 1).stationName;
				} else if (circular && stationCount > 0) {
					// 环线：末站的后一站是首站
					nextStationName = detail.stationDetails.get(0).stationName;
				}
				for (final Route route : ClientData.ROUTES) {
					if (route.color == lineColor && route.lightRailRouteNumber != null && !route.lightRailRouteNumber.isEmpty()) {
						routeNumber = route.lightRailRouteNumber;
						break;
					}
				}
				break;
			}
			return new PSDStationData(stationName, previousStationName, nextStationName, routeNumber, stationNumber, lineColor);
		} catch (Exception e) {
			return null;
		}
	}

	/** 调试用状态去重表：每个位置只在「状态关键字」变化时更新一次（与 RenderCRTStationName1 的调试做法一致）。 */
	private static final Map<Long, String> DEBUG_STATES = new HashMap<>();

	private static void debug(BlockPos pos, String message) {
		final long key = pos.asLong();
		if (message.equals(DEBUG_STATES.get(key))) {
			return;
		}
		if (DEBUG_STATES.size() > 512) {
			DEBUG_STATES.clear();
		}
		DEBUG_STATES.put(key, message);
	}
}
