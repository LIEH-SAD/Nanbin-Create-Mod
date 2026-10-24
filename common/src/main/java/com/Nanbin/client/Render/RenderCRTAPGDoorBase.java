package com.Nanbin.client.Render;

import com.Nanbin.Init;
import com.Nanbin.Registry.RegBlock.APGDoorNumbering;
import com.Nanbin.Registry.RegBlock.BlockCRTAPGDoorBase;
import com.Nanbin.client.Drawing.CustomFontTextureCache;
import com.Nanbin.mapping.DirectionHelper;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.mtr.block.BlockPSDAPGDoorBase;
import org.mtr.block.IBlock;
import org.mtr.client.MinecraftClientData;
import org.mtr.core.data.Platform;
import org.mtr.core.data.Position;
import org.mtr.core.data.SimplifiedRoute;
import org.mtr.core.operation.ArrivalResponse;
import org.mtr.data.ArrivalsCacheClient;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.render.BlockEntityRendererExtension;
import org.mtr.render.StoredMatrixTransformations;
import org.mtr.tool.Drawing;

import java.util.HashMap;
import java.util.Map;

/**
 * CRT 屏蔽门渲染器的公共基类。
 * <p>
 * 源项目 4.0 里一代/二代是两个近乎逐行相同的类（各 371 行），差别只在：
 * <ul>
 *     <li>用的模型/贴图不同（一代整扇门在下半格模型里，二代拆成 top/bottom）；</li>
 *     <li>提示灯的材质与闪烁规则（一代地面灯会随列车接近闪烁，二代不闪）；</li>
 *     <li>判断旁边是不是玻璃方块的类型集合与偏移方向。</li>
 * </ul>
 * 所以这里把<b>公共骨架</b>（矩阵、状态读取、缓存、线路色/站台查询）收在基类，
 * 把两处真正不同的地方留成抽象方法 {@link #renderLighting} / {@link #renderDoorBody}。
 * <p>
 * 4.1 适配要点：
 * <ul>
 *     <li>{@code BlockEntityRenderer}（4.0 映射层）→ {@link BlockEntityRendererExtension}，
 *     render 签名换成 {@code (T, MatrixStack, VertexConsumerProvider, ClientWorld, ClientPlayerEntity, float, int, int)}；</li>
 *     <li>模型层：4.0 的 {@code EntityModelExtension}/{@code ModelPartExtension} 在 4.1 已被删除，
 *     改用原版 {@code ModelPart}，工具在 {@link DoorModels}；</li>
 *     <li>{@code directions.asRotation()} → {@link DirectionHelper#asRotation}；</li>
 *     <li>{@code entity.getWorld2()/getPos2()} → {@code getWorld()/getPos()}。</li>
 * </ul>
 */
public abstract class RenderCRTAPGDoorBase<T extends BlockCRTAPGDoorBase.DoorBlockEntity> extends BlockEntityRendererExtension<T> implements IBlock {

	/** 一代门的「列车接近」提示灯闪烁周期（tick）；二代门地面灯不闪。 */
	protected static final double ARRIVAL_WARNING_BLINK_PERIOD = 40.0;
	/** 开门过程中提示灯的闪烁周期（tick）。 */
	protected static final double DOOR_MOVING_BLINK_PERIOD = 20.0;
	/** 列车到站前多少毫秒开始亮「接近」提示（客户端到站缓存能给出时间时用）。 */
	protected static final long ARRIVAL_WARNING_TIME_MS = 60000;
	/** 判定门是否属于某站台时，允许的边界外扩格数。 */
	protected static final int PLATFORM_SEARCH_RANGE = 3;
	/** 门编号/线路色结果缓存时长：沿墙扫描开销较大，最多每秒重算一次。 */
	protected static final long DOOR_NUMBER_CACHE_TTL_MS = 1000;

	/** MTR 门未解锁时贴在门上的告示牌贴图。 */
	protected static final net.minecraft.util.Identifier DOOR_NOT_IN_USE_TEXTURE = net.minecraft.util.Identifier.of("mtr", "textures/block/sign/door_not_in_use.png");

	protected final int type;

	private final Map<BlockPos, Double> previousOpenMap = new HashMap<>();
	/** doorPos -> {number, cacheTimeMillis}。 */
	private final Map<BlockPos, long[]> doorNumberCache = new HashMap<>();
	/** doorPos -> {lineColor, cacheTimeMillis}。 */
	private final Map<BlockPos, long[]> doorColorCache = new HashMap<>();

	protected RenderCRTAPGDoorBase(int type) {
		this.type = type;
	}

	@Override
	public void render(T entity, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, ClientWorld world, ClientPlayerEntity player, float tickDelta, int light, int overlay) {
		if (world == null) {
			return;
		}
		entity.tick(tickDelta);
		final BlockPos blockPos = entity.getPos();

		final Direction facing = IBlock.getStatePropertySafe(world, blockPos, DirectionHelper.FACING);
		final boolean side = IBlock.getStatePropertySafe(world, blockPos, IBlock.SIDE) == IBlock.EnumSide.RIGHT;
		final boolean half = IBlock.getStatePropertySafe(world, blockPos, IBlock.HALF) == IBlock.DoubleBlockHalf.UPPER;
		final boolean end = IBlock.getStatePropertySafe(world, blockPos, BlockPSDAPGDoorBase.END);
		final boolean unlocked = IBlock.getStatePropertySafe(world, blockPos, BlockPSDAPGDoorBase.UNLOCKED);
		final double open = Math.min(entity.getDoorValue(), type >= 3 ? 0.75 : 1.0);

		final boolean doorFullyOpen = open >= (type >= 3 ? 0.75 : 1.0) - 0.01;
		final double previousOpen = previousOpenMap.getOrDefault(blockPos, -1.0);
		final boolean doorMoving = previousOpen >= 0 && Math.abs(open - previousOpen) > 0.001;
		previousOpenMap.put(blockPos, open);
		final boolean trainApproaching = isTrainApproaching(blockPos);

		final DoorState state = new DoorState(world, blockPos, facing, side, half, end, unlocked, open, doorFullyOpen, doorMoving, trainApproaching, light, overlay);

		state.storedLight = new StoredMatrixTransformations(0.5 + blockPos.getX(), blockPos.getY(), 0.5 + blockPos.getZ());
		state.storedLight.add(matrices -> {
			Drawing.rotateYDegrees(matrices, -DirectionHelper.asRotation(facing));
			Drawing.rotateXDegrees(matrices, 180);
		});
		state.storedGround = new StoredMatrixTransformations(0.5 + blockPos.getX(), blockPos.getY(), 0.5 + blockPos.getZ());
		state.storedGround.add(matrices -> Drawing.rotateYDegrees(matrices, -DirectionHelper.asRotation(facing)));

		renderLighting(state);

		// 门体：在朝向矩阵之后追加「开门位移」，左右两页反向滑开
		state.storedBody = state.storedLight.copy();
		state.storedBody.add(matrices -> matrices.translate(open * (side ? -1 : 1), 0, 0));

		renderDoorBody(state);
	}

	/**
	 * 提示灯 / 地面灯的绘制（一代、二代规则不同）。
	 * <p>调用时机：在 {@link DoorState#storedBody} 计算之前，所以要用 {@link DoorState#storedLight}
	 * 或 {@link DoorState#storedGround}（不带开门位移）。
	 */
	protected abstract void renderLighting(DoorState state);

	/** 门页本体（含「未解锁」告示牌）的绘制，用 {@link DoorState#storedBody}。 */
	protected abstract void renderDoorBody(DoorState state);

	/** 列车是否即将进站：按门所在站台的到站缓存判断（拿不到数据时一律 false）。 */
	protected boolean isTrainApproaching(BlockPos doorPos) {
		try {
			final MinecraftClientData clientData = MinecraftClientData.getInstance();
			final LongAVLTreeSet platformIds = new LongAVLTreeSet();

			for (final Platform platform : clientData.platformIdMap.values()) {
				if (isNearPlatform(doorPos, platform)) {
					platformIds.add(platform.getId());
				}
			}

			if (platformIds.isEmpty()) {
				return false;
			}

			final ObjectArrayList<ArrivalResponse> arrivals = ArrivalsCacheClient.INSTANCE.requestArrivals(platformIds);
			final long millisOffset = ArrivalsCacheClient.INSTANCE.getMillisOffset();
			final long now = System.currentTimeMillis();

			long earliestArrival = Long.MAX_VALUE;
			for (final ArrivalResponse response : arrivals) {
				final long timeUntilArrival = response.getArrival() - millisOffset - now;
				if (timeUntilArrival > 0 && timeUntilArrival < earliestArrival) {
					earliestArrival = timeUntilArrival;
				}
			}

			if (earliestArrival < Long.MAX_VALUE) {
				return earliestArrival <= ARRIVAL_WARNING_TIME_MS;
			}
		} catch (Exception e) {
			LOG_ERR("isTrainApproaching error", e);
		}
		return false;
	}

	/** 判断门是否位于站台沿线（按站台两端坐标的 XZ 包围盒判定，含 {@link #PLATFORM_SEARCH_RANGE} 裕量）。 */
	protected boolean isNearPlatform(BlockPos doorPos, Platform platform) {
		final Position position1 = platform.getRandomPosition();
		final Position position2 = platform.getOtherPosition(position1);
		final long minX = Math.min(position1.getX(), position2.getX()) - PLATFORM_SEARCH_RANGE;
		final long maxX = Math.max(position1.getX(), position2.getX()) + PLATFORM_SEARCH_RANGE;
		final long minZ = Math.min(position1.getZ(), position2.getZ()) - PLATFORM_SEARCH_RANGE;
		final long maxZ = Math.max(position1.getZ(), position2.getZ()) + PLATFORM_SEARCH_RANGE;
		return doorPos.getX() >= minX && doorPos.getX() <= maxX && doorPos.getZ() >= minZ && doorPos.getZ() <= maxZ;
	}

	/** 带 TTL 缓存的门编号：避免每帧沿墙全量扫描（最多 512×2 格），最多每秒重算一次。 */
	protected int getDoorNumberCached(World world, BlockPos blockPos) {
		final long now = System.currentTimeMillis();
		final long[] cached = doorNumberCache.get(blockPos);
		if (cached != null && now - cached[1] < DOOR_NUMBER_CACHE_TTL_MS) {
			return (int) cached[0];
		}
		final int number = APGDoorNumbering.getDoorNumber(world, blockPos, IBlock.getStatePropertySafe(world, blockPos, BlockCRTAPGDoorBase.SORT_FROM_RIGHT));
		doorNumberCache.put(blockPos, new long[]{number, now});
		if (doorNumberCache.size() > 512) {
			doorNumberCache.entrySet().removeIf(entry -> now - entry.getValue()[1] >= DOOR_NUMBER_CACHE_TTL_MS);
		}
		return number;
	}

	/** 解析门所在站台线路的颜色（RGB）；无线路时返回灰色。 */
	protected int getDoorLineColor(BlockPos doorPos) {
		try {
			final MinecraftClientData clientData = MinecraftClientData.getInstance();
			for (final Platform platform : clientData.platformIdMap.values()) {
				if (isNearPlatform(doorPos, platform)) {
					final long platformId = platform.getId();
					for (final SimplifiedRoute route : clientData.simplifiedRoutes) {
						if (route.getPlatformIndex(platformId) >= 0) {
							return route.getColor() | 0xFF000000;
						}
					}
				}
			}
		} catch (Exception e) {
			LOG_ERR("failed to resolve door line color", e);
		}
		return 0xFF929498;
	}

	/** 带 TTL 缓存的线路色：避免每帧遍历全部站台与线路。 */
	protected int getDoorLineColorCached(BlockPos doorPos) {
		final long now = System.currentTimeMillis();
		final long[] cached = doorColorCache.get(doorPos);
		if (cached != null && now - cached[1] < DOOR_NUMBER_CACHE_TTL_MS) {
			return (int) cached[0];
		}
		final int color = getDoorLineColor(doorPos);
		doorColorCache.put(doorPos, new long[]{color, now});
		if (doorColorCache.size() > 512) {
			doorColorCache.entrySet().removeIf(entry -> now - entry.getValue()[1] >= DOOR_NUMBER_CACHE_TTL_MS);
		}
		return color;
	}

	/**
	 * 在门页外立面渲染「屏蔽门编号铭牌」（线路色方块 + 白色编号）。
	 * <p>
	 * <b>当前不绘制</b>：上游 4.0 里这段 {@code MainRenderer.scheduleRender} 是被整段注释掉的
	 * （一代、二代两个文件都一样），也就是说这个功能在源项目里从来没有真正生效过
	 * —— 编号只在 {@link APGDoorNumbering} 里算出来，没画出去。
	 * 这里保持同样的行为：编号与线路色照算（有 TTL 缓存），绘制留待做视觉校对时再打开。
	 * <p>
	 * 打开时要做的事：贴图由 {@link CustomFontTextureCache#getDoorNumberPlateTexture} 生成，
	 * 模型是 {@code DoorModels.createSingleCube(16, 16, 10, 4, 2, 3, 3, 0.1F)}，
	 * 需要跟着门页一起做开门平移，所以要挂在 {@link DoorState#storedBody} 上。
	 * {@link DoorModels#createSingleCube} 目前只收整数尺寸，厚度 0.1 需要先加一个 float 重载。
	 */
	protected void renderDoorNumberPlate(DoorState state) {
		final int doorNumber = getDoorNumberCached(state.world, state.pos);
		if (doorNumber > 0) {
			// 保留调用：一旦启用绘制，这两个值就是贴图参数
			getDoorLineColorCached(state.pos);
		}
	}

	protected static void LOG_ERR(String message, Exception e) {
		Init.LOGGER.error("[RenderCRTAPGDoor] " + message, e);
	}

	/** 一帧里算好的门状态，避免把十几个参数在抽象方法之间来回传。 */
	protected static final class DoorState {

		public final ClientWorld world;
		public final BlockPos pos;
		public final Direction facing;
		/** true = 右门页（SIDE == RIGHT）。 */
		public final boolean side;
		/** true = 上半格。 */
		public final boolean half;
		public final boolean end;
		public final boolean unlocked;
		/** 0..1 的开门进度。 */
		public final double open;
		public final boolean doorFullyOpen;
		public final boolean doorMoving;
		public final boolean trainApproaching;
		public final int light;
		public final int overlay;

		public StoredMatrixTransformations storedLight;
		public StoredMatrixTransformations storedGround;
		/** 在 {@link #storedLight} 之上追加了开门位移，门页本体用它。 */
		public StoredMatrixTransformations storedBody;

		private DoorState(ClientWorld world, BlockPos pos, Direction facing, boolean side, boolean half, boolean end, boolean unlocked, double open, boolean doorFullyOpen, boolean doorMoving, boolean trainApproaching, int light, int overlay) {
			this.world = world;
			this.pos = pos;
			this.facing = facing;
			this.side = side;
			this.half = half;
			this.end = end;
			this.unlocked = unlocked;
			this.open = open;
			this.doorFullyOpen = doorFullyOpen;
			this.doorMoving = doorMoving;
			this.trainApproaching = trainApproaching;
			this.light = light;
			this.overlay = overlay;
		}
	}
}
