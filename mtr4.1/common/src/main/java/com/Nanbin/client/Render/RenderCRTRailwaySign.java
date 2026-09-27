package com.Nanbin.client.Render;

import com.Nanbin.Registry.RegBlock.BlockCRTRailwaySign;
import com.Nanbin.client.Drawing.CustomFontTextureCache;
import com.Nanbin.client.Drawing.SignTextStyleConfig;
import com.Nanbin.client.JavaScriptSupport.JSDrawContext;
import com.Nanbin.client.JavaScriptSupport.JSSignConfig;
import com.Nanbin.client.JavaScriptSupport.JSSignEngine;
import com.Nanbin.mapping.DirectionHelper;
import org.mtr.block.BlockRailwaySign;
import org.mtr.block.IBlock;
import org.mtr.client.CustomResourceLoader;
import org.mtr.client.MinecraftClientData;
import org.mtr.core.data.Station;
import org.mtr.data.IGui;
import org.mtr.generated.lang.TranslationProvider;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import org.mtr.render.BlockEntityRendererExtension;
import org.mtr.render.StoredMatrixTransformations;
import org.mtr.resource.SignResource;
import org.mtr.resource.SignType;
import org.mtr.tool.Drawing;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Properties;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.network.ClientPlayerEntity;

/**
 * CRT 铁路告示牌渲染器。
 * <p>
 * <b>与 4.0 的实现思路不同（重要）：</b>4.0 里 MTR 的 {@code RenderRailwaySign} 不可扩展，
 * 所以 CRT 把「贴图、自定义文字、特殊标志类型」整套绘制都抄了一遍
 * （{@code drawSignCRT} / {@code renderCustomTextCRT} 加起来 300+ 行）。
 * <p>
 * MTR 4.1 有两点变化让那套重写不再必要：
 * <ol>
 *     <li>{@code RenderRailwaySign} 变成 <b>final</b>（不能继承），但它自己也变成只有 33 行的薄壳，
 *     直接委托给 {@link SignResource#render}；</li>
 *     <li><b>每格独立配色成了 MTR 的原生能力</b>（{@code selectedIds[]} 是一格一个集合），
 *     {@code SignResource.render} 已经会分别处理背景、逐格贴图、自定义文字与 EXIT/PLATFORM/ROUTE/STATION
 *     四类特殊标志。</li>
 * </ol>
 * 所以这里改成：普通格子直接复用 MTR 的 {@link SignResource#render}，CRT 只负责
 * 「整块由 JS 脚本渲染」这一条 MTR 没有的能力，以及给站台信息屏提供共用的格子绘制入口。
 * <p>
 * <b>关于 {@code crt_route_name} 的「线路编号」：</b>这些 CRT 标志在
 * {@code assets/nanbin/signs.json} 里是普通标志（带静态占位文字），MTR 的
 * {@code SignResource.render} 只会画出底图 + 占位文字，不会自己变成线路编号。
 * 4.0 用 {@code renderRouteNumberV2Regions} 系列（线路色竖条 + 白底编号块 + 线路名）
 * 叠在画好的告示牌上，本移植按同样思路保留，见 {@link #renderRouteNumberOverlay}。
 * 编号取值见 {@link SignRouteData}：MTR 4.1 的 {@code Route#getRouteNumber()} 是公开 API，
 * 所以不需要 4.0 那套「服务端解析 + NBT 同步」。
 */
public class RenderCRTRailwaySign extends BlockEntityRendererExtension<BlockCRTRailwaySign.BlockEntityCRTRailwaySign> {

	/** 单格的世界尺寸，与 MTR 4.1 自己的告示牌渲染保持一致。 */
	public static final float SIGN_SIZE = 0.5F;
	/** 带文字的小标志里，图标只占 0.75 格（4.0 的 {@code drawSignCRT}）。 */
	private static final float SMALL_SIGN_SCALE = 0.75F;

	/** 线路编号牌标志 id（4.0 的 {@code ROUTE_NUMBER_V2} / {@code ROUTE_NUMBER_V2_FLIPPED}）。 */
	private static final String ROUTE_NUMBER_SIGN = "crt_route_name";
	private static final String ROUTE_NUMBER_SIGN_FLIPPED = "crt_route_name_flipped";
	/** 逐格线路编号标志。 */
	private static final String ROUTE_NUMBER_VALUE = "crt_route_number";
	private static final String ROUTE_NUMBER_VALUE_FLIPPED = "crt_route_number_flipped";
	/** CRT 站名标志。 */
	private static final String ROUTE_STATION_NAME = "crt_station_name";

	/** MTR 的 Render 加载早于 BlockEntity，且字体纹理有缓存，需定时强制刷新。 */
	private static final long REFRESH_INTERVAL_MS = 1000L;
	private static final long JS_REFRESH_INTERVAL_MS = 500L;

	private static long lastJSRefreshTime = 0;
	private long lastRefreshTime = 0;

	@Override
	public void render(BlockCRTRailwaySign.BlockEntityCRTRailwaySign entity, net.minecraft.client.util.math.MatrixStack matrixStack, net.minecraft.client.render.VertexConsumerProvider vertexConsumerProvider, net.minecraft.client.world.ClientWorld clientWorld, net.minecraft.client.network.ClientPlayerEntity clientPlayerEntity, float tickDelta, int light, int overlay) {
		final long now = System.currentTimeMillis();
		if (now - lastRefreshTime >= REFRESH_INTERVAL_MS) {
			lastRefreshTime = now;
			CustomFontTextureCache.instance.clearFittedTextureCache();
		}

		final net.minecraft.util.math.BlockPos pos = entity.getPos();
		final net.minecraft.block.BlockState state = clientWorld.getBlockState(pos);
		if (!(state.getBlock() instanceof BlockRailwaySign blockRailwaySign)) {
			return;
		}

		final String[] signIds = entity.getSignIds();
		if (signIds == null || signIds.length != blockRailwaySign.length) {
			return;
		}
		// 整块全空时不渲染（MTR 4.1 自己的渲染器也会在这种情况下画不出东西）
		if (allCellsEmpty(signIds)) {
			return;
		}

		final net.minecraft.util.math.Direction facing = IBlock.getStatePropertySafe(state, net.minecraft.state.property.Properties.HORIZONTAL_FACING);

		// 与 MTR 4.1 的 RenderRailwaySign 完全相同的矩阵变换
		matrixStack.push();
		Drawing.rotateYDegrees(matrixStack, -DirectionHelper.asRotation(facing));
		Drawing.rotateZDegrees(matrixStack, 180);
		matrixStack.translate(blockRailwaySign.getXStart() / 16.0F - 0.5F, -0.53125, -0.0625F - IGui.SMALL_OFFSET);

		final String styleScriptId = JSSignConfig.getStyleScriptId(signIds);
		if (styleScriptId != null) {
			// 整块由 JS 脚本渲染：MTR 没有这个能力，只能自己来
			renderJSStyleLine(styleScriptId, signTransformations(pos, blockRailwaySign, facing), pos, signIds, flattenSelectedIds(entity.getSelectedIds()),
					entity.getRouteNumbers(), facing, 0, SIGN_SIZE, 0, 0, true, buildRouteNumberMap(entity));
		} else {
			try {
				drawSignRow(matrixStack, vertexConsumerProvider, SignDrawing.immediate(matrixStack, vertexConsumerProvider),
						pos, signIds, entity.getSelectedIds(), entity.getRouteNumbers(), SIGN_SIZE, false, false);
			} catch (Exception e) {
				com.Nanbin.Init.LOGGER.error("[RenderCRTRailwaySign] 绘制失败", e);
			}
		}

		matrixStack.pop();
	}

	/**
	 * 画一整行/一整块 CRT 告示牌：4.0 {@code RenderCRTRailwaySign.render} 的移植。
	 * <p>
	 * 绘制顺序（与 4.0 一致，也是「预览 = 实际渲染」的关键）：
	 * <ol>
	 *     <li>先让 MTR 自己画一遍整行 —— EXIT / PLATFORM / LINE / STATION 这四类「动态标志」
	 *     只有 MTR 的 {@link SignResource#render} 会画（出口字母、站台号、线路名、站名），
	 *     而且它需要整行数组才能算出文字可用宽度；</li>
	 *     <li>再逐格用 CRT 自己的画法覆盖：不透明底色 + 贴图 + 本项目字体（Alibaba）的文字。
	 *     这一步同时把 MTR 那层可能不对的整块底色盖掉；</li>
	 *     <li>最后叠线路编号区段（{@code crt_route_name}）。</li>
	 * </ol>
	 * 第 2 步只画「本 CRT 自己的标志」（含空格子），四类动态标志留给 MTR，所以不会互相打架。
	 *
	 * @param poseStack     已经摆到告示牌平面的矩阵（原点 = 最左格的左下角）
	 * @param sink          贴图落点：世界渲染传立即绘制（与 MTR 的告示牌同一条缓冲），
	 *                      GUI 预览传 GUI 的缓冲；两者的几何完全一致
	 * @param transparentBlack 4.0 的语义：只画居中的文字、不画贴图也不画底色（站台信息屏的双层格用）
	 */
	public static void drawSignRow(
			net.minecraft.client.util.math.MatrixStack poseStack,
			net.minecraft.client.render.VertexConsumerProvider vertexConsumerProvider,
			SignDrawing.QuadSink sink,
			net.minecraft.util.math.BlockPos pos,
			String[] signIds,
			LongAVLTreeSet[] selectedIds,
			String[] routeNumbers,
			float size,
			boolean transparentBlack,
			boolean isGui) {
		if (signIds == null || signIds.length == 0) {
			return;
		}

		final int[] cellBackgroundColors = new int[signIds.length];
		final SignResource[] signResources = new SignResource[signIds.length];
		final boolean[] mtrSpecialSigns = new boolean[signIds.length];
		boolean anyMtrSpecialSign = false;
		for (int i = 0; i < signIds.length; i++) {
			final SignResource sign = CustomResourceLoader.getSignById(signIds[i]);
			signResources[i] = sign;
			if (sign != null && sign.getBackgroundColor() != 0) {
				cellBackgroundColors[i] = sign.getBackgroundColor();
			}
			if (isMtrSpecialSign(signIds[i], sign)) {
				mtrSpecialSigns[i] = true;
				anyMtrSpecialSign = true;
			}
		}

		// 1) MTR 只负责四类「动态标志」（出口字母 / 站台号 / 线路名 / 站名）——
		// 它们的绘制（含按整行计算文字可用宽度）只有 MTR 会做。
		// 其余格子必须把资源置空交给 CRT：否则 MTR 会把自己的文字/底色画一遍，
		// 再被我们的逐格绘制盖住一半，就出现「不同格子底色 + 文字重叠」。
		if (anyMtrSpecialSign && !transparentBlack) {
			final SignResource[] mtrResources = new SignResource[signIds.length];
			for (int i = 0; i < signIds.length; i++) {
				mtrResources[i] = mtrSpecialSigns[i] ? signResources[i] : null;
			}
			SignResource.render(poseStack, vertexConsumerProvider, pos, 0, 0, nonNullSelectedIds(selectedIds, signIds.length), mtrResources, size, isGui ? 0 : IGui.SMALL_OFFSET, isGui);
		}

		// 2) CRT 逐格覆盖
		for (int i = 0; i < signIds.length; i++) {
			if (mtrSpecialSigns[i]) {
				continue;
			}
			drawSignCellCrt(poseStack, vertexConsumerProvider, sink, pos, signIds[i], size * i, 0, size,
					getMaxWidth(signIds, i, false), getMaxWidth(signIds, i, true),
					selectedIds == null || i >= selectedIds.length || selectedIds[i] == null ? new LongAVLTreeSet() : selectedIds[i],
					routeNumbers, cellBackgroundColors[i], transparentBlack, signIds, isGui);
		}

		// 3) 线路编号区段
		renderRouteNumberRegions(sink, pos, signIds, selectedIds, routeNumbers, size, cellBackgroundColors);
	}

	/**
	 * 单格 CRT 绘制（4.0 的 {@code drawSignCRT}）。
	 * <p>
	 * 四类动态标志（exit_letter / line / platform / station）以及 MTR 自己那几类
	 * 特殊标志类型交给 MTR（{@link #drawSignRow} 第 1 步已经画过），这里直接返回。
	 */
	public static void drawSignCellCrt(
			net.minecraft.client.util.math.MatrixStack poseStack,
			net.minecraft.client.render.VertexConsumerProvider vertexConsumerProvider,
			SignDrawing.QuadSink sink,
			net.minecraft.util.math.BlockPos pos,
			String signId,
			float x,
			float y,
			float size,
			float maxWidthLeft,
			float maxWidthRight,
			LongAVLTreeSet selectedIds,
			String[] routeNumbers,
			int cellBackgroundColor,
			boolean transparentBlack,
			String[] signIds,
			boolean isGui) {

		final SignResource sign = CustomResourceLoader.getSignById(signId);
		// 线路编号牌：底色由这里铺，编号内容由 renderRouteNumberRegions 叠上去
		if (isRouteNumberSign(signId)) {
			if (!transparentBlack) {
				SignDrawing.rect(sink, x, y, size, size, withAlpha(cellBackgroundColor), SignDrawing.defaultLight());
			}
			return;
		}
		if (sign == null) {
			// 空格子：铺一层底色，把 MTR 那层整块底色盖掉
			if (!transparentBlack) {
				SignDrawing.rect(sink, x, y, size, size, withAlpha(cellBackgroundColor), SignDrawing.defaultLight());
			}
			return;
		}

		if (isMtrSpecialSign(signId, sign)) {
			// 四类动态标志由 MTR 负责，见 drawSignRow / drawSignLine
			return;
		}

		if (!sign.hasCustomText) {
			if (!transparentBlack) {
				SignDrawing.rect(sink, x, y, size, size, withAlpha(cellBackgroundColor), SignDrawing.defaultLight());
			}
			final float signSize = (sign.getSmall() ? SMALL_SIGN_SCALE : 1.0F) * size;
			final float margin = (size - signSize) / 2.0F;
			final boolean flipTexture = sign.getFlipTexture();
			sink.quad(sign.textureId, x + margin, y + margin, signSize, signSize,
					flipTexture ? 1 : 0, 0, flipTexture ? 0 : 1, 1, -1, SignDrawing.defaultLight(), true);
			return;
		}

		final boolean flipCustomText = sign.getFlipCustomText();
		final boolean isSmall = sign.getSmall();
		final boolean isStationNameCrt = ROUTE_STATION_NAME.equals(signId);
		final boolean isRouteNumber = ROUTE_NUMBER_VALUE.equals(signId) || ROUTE_NUMBER_VALUE_FLIPPED.equals(signId);
		SignTextStyleConfig.SignTextStyle style = SignTextStyleConfig.get(signId);
		if (style == null && signId != null && signId.endsWith("_flipped")) {
			style = SignTextStyleConfig.get(signId.substring(0, signId.length() - "_flipped".length()));
		}
		final String signText = resolveSignText(signId, sign, selectedIds, routeNumbers, isStationNameCrt, isRouteNumber);

		if (transparentBlack) {
			// 站台信息屏的双层格：不画底色，只画贴图与居中文字
			if (!isRouteNumber && !isStationNameCrt) {
				final float signSize = (isSmall ? SMALL_SIGN_SCALE : 1.0F) * size;
				final float margin = (size - signSize) / 2.0F;
				final boolean flipTexture = sign.getFlipTexture();
				sink.quad(sign.textureId, x + margin, y + margin, signSize, signSize,
						flipTexture ? 1 : 0, 0, flipTexture ? 0 : 1, 1, -1, SignDrawing.defaultLight(), true);
				SignDrawing.drawCustomText(signText, sink, size, x, false, size, 0,
						SignDrawing.fontTypeFor(signText),
						null, false, true, Color.BLACK, y, 0, 1.0F, isRouteNumber);
			} else {
				SignDrawing.drawCustomText(signText, sink, size, x, false, size, 0,
						SignDrawing.fontTypeFor(signText),
						null, false, true, Color.BLACK, y, 0, 1.0F, isRouteNumber);
			}
			return;
		}

		SignDrawing.rect(sink, x, y, size, size, withAlpha(cellBackgroundColor), SignDrawing.defaultLight());

		if (!isRouteNumber && !isStationNameCrt) {
			final float signSize = (isSmall ? SMALL_SIGN_SCALE : 1.0F) * size;
			final float margin = (size - signSize) / 2.0F;
			final boolean flipTexture = sign.getFlipTexture();
			sink.quad(sign.textureId, x + margin, y + margin, signSize, signSize,
					flipTexture ? 1 : 0, 0, flipTexture ? 0 : 1, 1, -1, SignDrawing.defaultLight(), true);
		}

		final float fixedMargin = size * 0.25F / 2.0F;
		final float maxWidth;
		final float start;
		if (!isRouteNumber && !isStationNameCrt) {
			maxWidth = Math.max(0, (flipCustomText ? maxWidthLeft : maxWidthRight) * size - fixedMargin * (isSmall ? 1 : 2));
			start = flipCustomText ? x - (isSmall ? 0 : fixedMargin) : x + size + (isSmall ? 0 : fixedMargin);
		} else {
			maxWidth = size;
			start = x;
		}

		SignDrawing.drawCustomText(signText, sink, size, start, flipCustomText, maxWidth, withAlpha(cellBackgroundColor),
				SignDrawing.fontTypeFor(signText),
				style, false, isRouteNumber || isStationNameCrt, null, 0, 0, 1.0F, isRouteNumber);
	}

	/** 四类由 MTR 自己画（带动态内容）的标志。 */
	private static boolean isMtrSpecialSign(String signId, SignResource sign) {
		if ("exit_letter".equals(signId) || "exit_letter_flipped".equals(signId)
				|| "line".equals(signId) || "line_flipped".equals(signId)
				|| "platform".equals(signId) || "platform_flipped".equals(signId)) {
			return true;
		}
		if (sign == null) {
			return false;
		}
		return switch (sign.getSignType()) {
			case EXIT, PLATFORM, ROUTE, STATION -> true;
			default -> false;
		};
	}

	private static int withAlpha(int color) {
		return color == 0 ? 0xFFFFFFFF : color | 0xFF000000;
	}

	/** MTR 的整行绘制不接受 null 的选中数组。 */
	private static LongAVLTreeSet[] nonNullSelectedIds(LongAVLTreeSet[] selectedIds, int length) {
		final LongAVLTreeSet[] result = new LongAVLTreeSet[length];
		for (int i = 0; i < length; i++) {
			result[i] = selectedIds != null && i < selectedIds.length && selectedIds[i] != null ? selectedIds[i] : new LongAVLTreeSet();
		}
		return result;
	}

	/** 4.0 的 {@code resolveSignText}：自定义文本 / 站名 / 线路编号 / 标志自带的文字。 */
	private static String resolveSignText(String signId, SignResource sign, LongAVLTreeSet selectedIds, String[] routeNumbers, boolean isStationName, boolean isRouteNumber) {
		if ("nanbin_custom_text".equals(signId) || "nanbin_custom_text_flipped".equals(signId)) {
			final String customText = JSSignEngine.readCustomText(selectedIds);
			if (customText != null && !customText.isEmpty()) {
				return customText;
			}
		}

		if (isStationName) {
			try {
				final MinecraftClientData clientData = MinecraftClientData.getInstance();
				final List<String> names = new ArrayList<>();
				// LongAVLTreeSet 本身就是有序的，等价于 4.0 的 .sorted()
				for (final long id : selectedIds) {
					final Station station = clientData.stationIdMap.get(id);
					if (station != null) {
						names.add(IGui.insertTranslation(TranslationProvider.GUI_MTR_STATION_CJK, TranslationProvider.GUI_MTR_STATION, 1, station.getName()));
					}
				}
				return names.isEmpty() ? "" : IGui.mergeStations(names);
			} catch (Exception e) {
				return "";
			}
		}

		if (isRouteNumber) {
			return routeNumbers == null ? "" : String.join("/", routeNumbers);
		}
		return sign.getCustomText() == null ? "" : sign.getCustomText();
	}

	/**
	 * 4.0 的 {@code RenderRailwaySign.getMaxWidth}：某一格左右两侧还有多少「单位」可以放文字。
	 * <p>
	 * WTR 4.1 的 {@code SignResource.getTextSpace} 不对外，这里按同样的算法重算一遍：
	 * 从相邻格开始数，空格子算 1 个单位，小标志让出 0.125，遇到反向的自定义文字标志就折半。
	 */
	private static float getMaxWidth(String[] signIds, int index, boolean right) {
		if (signIds == null || index < 0 || index >= signIds.length) {
			return 0;
		}
		final SignResource sign = CustomResourceLoader.getSignById(signIds[index]);
		if (sign == null) {
			return 0;
		}
		final int direction = right ? 1 : -1;
		final boolean isBigSign = (sign.getSignType() == SignType.EXIT || sign.getSignType() == SignType.ROUTE) && !sign.getSmall();
		float space = isBigSign ? 0 : -0.125F * (sign.getSmall() ? 1 : 2);

		for (int i = index + direction; i >= 0 && i < signIds.length; i += direction) {
			final SignResource other = CustomResourceLoader.getSignById(signIds[i]);
			if (other == null) {
				space += 1;
				continue;
			}
			if (!isBigSign && other.getSmall()) {
				space += 0.125F;
			}
			if (other.hasCustomText && other.getFlipCustomText() != sign.getFlipCustomText()) {
				space = (space - 0.125F) / 2.0F;
			}
			break;
		}
		return space;
	}

	/**
	 * 供站台信息屏等复用：在给定的 {MatrixStack, VertexConsumerProvider} 下画**一行**告示牌格子。
	 * <p>
	 * 站台信息屏的双层格是「一格一格贴着线路色矩形排」的，起点不在矩阵原点上，
	 * 所以这里逐格给 x/y 偏移，与 4.0 的 {@code drawSignCRT(..., x, y, ...)} 一致。
	 * 行里若混了四类动态标志（exit_letter / line / platform / station），
	 * 只有 MTR 会画它们，所以额外用「平移后的矩阵」让 MTR 补画一次。
	 *
	 * @param transparentBlack 4.0 的语义：不画底色，只画贴图与居中文字（站台信息屏用）
	 */
	public static void drawSignLine(net.minecraft.client.util.math.MatrixStack poseStack, net.minecraft.client.render.VertexConsumerProvider vertexConsumerProvider, SignDrawing.QuadSink sink,
			net.minecraft.util.math.BlockPos pos, String[] lineIds, LongAVLTreeSet selectedIds, String[] routeNumbers, float size,
			float startX, float startY, boolean transparentBlack) {
		if (lineIds == null || lineIds.length == 0) {
			return;
		}

		boolean hasMtrSpecialSign = false;
		for (final String lineId : lineIds) {
			if (isMtrSpecialSign(lineId, CustomResourceLoader.getSignById(lineId))) {
				hasMtrSpecialSign = true;
				break;
			}
		}
		if (hasMtrSpecialSign) {
			final LongAVLTreeSet[] perCell = new LongAVLTreeSet[lineIds.length];
			for (int j = 0; j < lineIds.length; j++) {
				perCell[j] = selectedIds;
			}
			poseStack.push();
			poseStack.translate(startX, startY, 0);
			try {
				SignResource.render(poseStack, vertexConsumerProvider, pos, perCell, lineIds, size, IGui.SMALL_OFFSET, false);
			} catch (Exception ignored) {
			}
			poseStack.pop();
		}

		for (int j = 0; j < lineIds.length; j++) {
			if (isRouteNumberSign(lineIds[j])) {
				continue;
			}
			drawSignCellCrt(poseStack, vertexConsumerProvider, sink, pos, lineIds[j], startX + j * size, startY, size,
					size, size, selectedIds, routeNumbers, 0, transparentBlack, lineIds, false);
		}
	}

	/** 整块告示牌由 JS 脚本渲染（4.0 的 {@code renderJSStyleSign}）。 */
	public static void renderJSStyleSign(String scriptId, StoredMatrixTransformations storedMatrixTransformations, net.minecraft.util.math.BlockPos pos, String[] signIds, LongAVLTreeSet selectedIds, String[] routeNumbers, net.minecraft.util.math.Direction facing, int backgroundColor, float cellSize, Map<Long, String> routeNumberMap) {
		renderJSStyleLine(scriptId, storedMatrixTransformations, pos, signIds, selectedIds, routeNumbers, facing, backgroundColor, cellSize, 0, 0, false, routeNumberMap);
	}

	/**
	 * 整块/某一行由 JS 脚本渲染。
	 * <p>
	 * 与 4.0 完全一致：组一个 {@link JSSignEngine.JSSignContext} + {@link JSDrawContext}，
	 * 交给 {@link JSSignEngine#render}。绘制原语（drawText / drawTexture / drawRect / drawLine）
	 * 已经在 {@link SignDrawing} 里实现好了。
	 */
	public static void renderJSStyleLine(String scriptId, StoredMatrixTransformations storedMatrixTransformations, net.minecraft.util.math.BlockPos pos, String[] lineIds, LongAVLTreeSet selectedIds, String[] routeNumbers, net.minecraft.util.math.Direction facing, int backgroundColor, float cellSize, float startX, float startY, boolean transparentBlack, Map<Long, String> routeNumberMap) {
		refreshJSCache();
		final String signKey = scriptId + "_" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ() + "_" + startX + "_" + startY + "_" + facing;
		final JSSignEngine.JSSignContext context = new JSSignEngine.JSSignContext(lineIds, 0, selectedIds, routeNumbers, backgroundColor, cellSize, signKey, pos, routeNumberMap);
		final JSDrawContext drawContext = new JSDrawContext(storedMatrixTransformations, facing, startX, startY, cellSize, lineIds.length, transparentBlack, backgroundColor);
		JSSignEngine.render(scriptId, signKey, drawContext, new JSSignEngine.JSSignAPI(context, scriptId));
	}

	public static boolean isJSSign(String signId) {
		return signId != null && signId.startsWith("crt_js_");
	}

	// ---- CRT 线路编号渲染（4.0 的 renderRouteNumberV2Regions 系列） ----

	/** 线路编号牌标志（4.0 的 {@code isRouteNumberV2}）。 */
	private static boolean isRouteNumberSign(String signId) {
		return ROUTE_NUMBER_SIGN.equals(signId) || ROUTE_NUMBER_SIGN_FLIPPED.equals(signId);
	}

	/**
	 * 叠加「CRT 线路编号」区段（4.0 的 {@code renderRouteNumberV2Regions}）。
	 * <p>
	 * 4.0 每格的线路色彩来自 {@code entity.getCellColors(c)}（附加的覆盖数组）；
	 * 4.1 的告示牌方块实体本身就<b>一格一个集合</b>，直接用 {@code selectedIds[cell]}。
	 * 线路编号优先用 {@link SignRouteData} 的真实值，猜法只作为兜底保留。
	 */
	private static void renderRouteNumberRegions(SignDrawing.QuadSink sink, net.minecraft.util.math.BlockPos pos, String[] signIds, LongAVLTreeSet[] selectedIds, String[] routeNumbers, float size, int[] cellBackgroundColors) {
		if (selectedIds == null) {
			return;
		}

		for (int i = 0; i < signIds.length; i++) {
			if (signIds[i] == null || !isRouteNumberSign(signIds[i])) {
				continue;
			}

			// 连续同标志的区段
			int start = i;
			while (start > 0 && signIds[start - 1] != null && signIds[start - 1].equals(signIds[i])) {
				start--;
			}
			int end = i;
			while (end < signIds.length - 1 && signIds[end + 1] != null && signIds[end + 1].equals(signIds[i])) {
				end++;
			}

			final boolean regionFlipped = ROUTE_NUMBER_SIGN_FLIPPED.equals(signIds[start]);

			long[] colors = null;
			for (int cell = start; cell <= end && colors == null; cell++) {
				final long[] cellColors = cell < selectedIds.length && selectedIds[cell] != null ? selectedIds[cell].toLongArray() : null;
				if (cellColors != null && cellColors.length > 0) {
					colors = cellColors;
				}
			}
			if (colors == null) {
				final LongAVLTreeSet merged = new LongAVLTreeSet();
				for (final LongAVLTreeSet set : selectedIds) {
					if (set != null) {
						merged.addAll(set);
					}
				}
				colors = merged.toLongArray();
			}

			if (colors.length == 0) {
				i = end;
				continue;
			}

			// 线路数比区段格子多时，向相邻空格借位
			final int extraBlanks = Math.max(0, colors.length - (end - start + 1));
			int regionStart = start;
			int regionEnd = end;
			for (int cell = end + 1; cell < signIds.length && cell <= end + extraBlanks; cell++) {
				final String nextSignId = signIds[cell];
				if (nextSignId != null && !nextSignId.isEmpty()) {
					break;
				}
				regionEnd = cell;
			}
			if (regionFlipped) {
				final int rightAbsorbed = regionEnd - end;
				for (int cell = start - 1; cell >= 0 && cell >= start - (extraBlanks - rightAbsorbed); cell--) {
					final String nextSignId = signIds[cell];
					if (nextSignId != null && !nextSignId.isEmpty()) {
						break;
					}
					regionStart = cell;
				}
			}

			for (int cell = regionStart; cell <= regionEnd; cell++) {
				final int cellBg = cellBackgroundColors != null && cell < cellBackgroundColors.length && cellBackgroundColors[cell] != 0
						? cellBackgroundColors[cell] : 0xFFFFFF;
				renderRouteNumberCell(sink, signIds, cell, regionStart, regionEnd, colors, pos, size, regionFlipped, cellBg);
			}
			i = regionEnd;
		}
	}

	private static void renderRouteNumberCell(SignDrawing.QuadSink sink, String[] signIds, int cellIndex, int regionStart, int regionEnd, long[] colors, net.minecraft.util.math.BlockPos pos, float size, boolean regionFlipped, int backgroundColor) {
		final String cellSignId = signIds[cellIndex];
		final boolean flipped = ROUTE_NUMBER_SIGN_FLIPPED.equals(cellSignId) || ((cellSignId == null || cellSignId.isEmpty()) && regionFlipped);
		final int shownRoutes = Math.min(regionEnd - regionStart + 1, colors.length);
		final int routeIndex = flipped ? regionEnd - cellIndex : cellIndex - regionStart;
		if (routeIndex < 0 || routeIndex >= shownRoutes) {
			return;
		}

		final long routeColorLong = colors[routeIndex];
		final int routeColor = (int) routeColorLong | 0xFF000000;
		final String routeName = SignRouteData.getRouteName(pos, routeColorLong);
		String number = SignRouteData.getRouteNumber(pos, routeColorLong);
		if (number.isEmpty()) {
			number = deriveNumberFromName(routeName);
		}
		final String text = stripNumberFromName(routeName, number);
		final boolean isTextCell = flipped ? cellIndex == regionEnd - shownRoutes + 1 : cellIndex == regionStart + shownRoutes - 1;
		final float cellX = cellIndex * size;

		if (flipped) {
			renderRouteNumberCellFlip(sink, signIds, cellIndex, size, cellX, routeColor, number, text, isTextCell, backgroundColor);
		} else {
			renderRouteNumberCellNormal(sink, signIds, cellIndex, size, cellX, routeColor, number, text, isTextCell, backgroundColor);
		}
	}

	private static void renderRouteNumberCellNormal(SignDrawing.QuadSink sink, String[] signIds, int cellIndex, float size, float cellX, int routeColor, String number, String text, boolean isLast, int backgroundColor) {
		final float stripeY = 0.2F * size;
		final float stripeW = 0.3F * size;
		final float stripeH = 0.8F * size;
		SignDrawing.rect(sink, cellX, stripeY, stripeW, stripeH, routeColor, SignDrawing.defaultLight());

		final boolean hasNumber = !number.isEmpty();
		if (hasNumber) {
			drawNumberBlock(sink, cellX + 0.3F * size, 0, 0.7F * size, size, number);
		}

		if (isLast && !text.isEmpty()) {
			final float textStart = cellX + (hasNumber ? 1.0F : 0.4F) * size;
			SignDrawing.drawCustomText(text, sink, size, textStart, false, 100.0F, backgroundColor,
					SignDrawing.fontTypeFor(text), SignTextStyleConfig.get(signIds[cellIndex]),
					false, false, null, 0, 0, 0.8F, false);
		}
	}

	private static void renderRouteNumberCellFlip(SignDrawing.QuadSink sink, String[] signIds, int cellIndex, float size, float cellX, int routeColor, String number, String text, boolean isTextCell, int backgroundColor) {
		final float stripeX = cellX + 0.7F * size;
		final float stripeY = 0.2F * size;
		final float stripeW = 0.3F * size;
		final float stripeH = 0.8F * size;
		SignDrawing.rect(sink, stripeX, stripeY, stripeW, stripeH, routeColor, SignDrawing.defaultLight());

		final boolean hasNumber = !number.isEmpty();
		if (hasNumber) {
			drawNumberBlock(sink, cellX, 0, 0.7F * size, size, number);
		}

		if (isTextCell && !text.isEmpty()) {
			final float textStart = cellX + (hasNumber ? 0.0F : 0.7F) * size;
			SignTextStyleConfig.SignTextStyle style = SignTextStyleConfig.get(signIds[cellIndex]);
			if (style == null) {
				style = SignTextStyleConfig.get(ROUTE_NUMBER_SIGN);
			}
			SignDrawing.drawCustomText(text, sink, size, textStart, true, 100.0F, backgroundColor,
					SignDrawing.fontTypeFor(text), style, false, false, null, 0, 0, 0.8F, false);
		}
	}

	/** 白底编号块：先铺白底，再把编号贴图居中画上去。 */
	private static void drawNumberBlock(SignDrawing.QuadSink sink, float x, float y, float width, float height, String number) {
		SignDrawing.rect(sink, x, y, width, height, 0xFFFFFFFF, SignDrawing.defaultLight());
		SignDrawing.drawNumberInBox(sink, x, y, width, height, number);
	}

	/** 从线路名里猜编号：取「||」前的可见部分，再取第一个「|」之前的片段。 */
	public static String deriveNumberFromName(String rawName) {
		if (rawName == null || rawName.isEmpty()) {
			return "";
		}
		final int hideIndex = rawName.indexOf("||");
		final String visible = hideIndex >= 0 ? rawName.substring(0, hideIndex) : rawName;
		final int separatorIndex = visible.indexOf('|');
		return (separatorIndex >= 0 ? visible.substring(0, separatorIndex) : visible).trim();
	}

	/** 从线路名里去掉编号部分，剩下的就是「线路名称」。 */
	public static String stripNumberFromName(String rawName, String number) {
		if (rawName == null || rawName.isEmpty()) {
			return "";
		}
		final int hideIndex = rawName.indexOf("||");
		final String visible = hideIndex >= 0 ? rawName.substring(0, hideIndex) : rawName;
		final String[] numberSegments = number == null || number.isEmpty() ? new String[0] : number.split("\\|");
		final StringBuilder rest = new StringBuilder();

		for (final String segment : visible.split("\\|")) {
			String stripped = segment.trim();
			for (final String numberSegment : numberSegments) {
				stripped = stripped.replace(numberSegment, "");
			}
			stripped = stripped.replaceAll("\\d", "").trim();
			if (!stripped.isEmpty()) {
				if (rest.length() > 0) {
					rest.append('|');
				}
				rest.append(stripped);
			}
		}
		return rest.toString();
	}

	/** 供界面/混入判断某一行是否是 JS 样式行。 */
	public static boolean isJSStyleLine(String[] signIds) {
		return JSSignConfig.getStyleScriptId(signIds) != null;
	}

	private static void refreshJSCache() {
		final long now = System.currentTimeMillis();
		if (now - lastJSRefreshTime >= JS_REFRESH_INTERVAL_MS) {
			lastJSRefreshTime = now;
			JSSignEngine.clearLegacyResultCache();
		}
	}

	private static boolean allCellsEmpty(String[] signIds) {
		for (final String signId : signIds) {
			if (signId != null && !signId.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 4.0 的 JS 侧拿的是「一个全局 selectedIds 集合」，4.1 是「一格一个集合」，
	 * 所以这里合成一个并集给 JS 用（JS API 本来就没有暴露逐格颜色，行为等价）。
	 */
	private static LongAVLTreeSet flattenSelectedIds(LongAVLTreeSet[] selectedIds) {
		final LongAVLTreeSet merged = new LongAVLTreeSet();
		if (selectedIds != null) {
			for (final LongAVLTreeSet set : selectedIds) {
				if (set != null) {
					merged.addAll(set);
				}
			}
		}
		return merged;
	}

	/** 颜色 → 线路编号，供 JS 脚本按颜色取编号（4.0 同样取自客户端线路数据）。 */
	private static Map<Long, String> buildRouteNumberMap(BlockCRTRailwaySign.BlockEntityCRTRailwaySign entity) {
		final Map<Long, String> routeNumberMap = new HashMap<>();
		final net.minecraft.util.math.BlockPos pos = entity.getPos();
		try {
			for (final LongAVLTreeSet set : entity.getSelectedIds()) {
				for (final long color : set) {
					routeNumberMap.put(color, resolveRouteNumber(entity, pos, color));
				}
			}
			if (routeNumberMap.isEmpty()) {
				// 没选任何线路时给全部可用线路，脚本仍能按颜色查到编号
				for (final Map.Entry<Long, String> entry : SignRouteData.getRouteNames(pos).entrySet()) {
					routeNumberMap.put(entry.getKey(), resolveRouteNumber(entity, pos, entry.getKey()));
				}
			}
		} catch (Exception ignored) {
			// 数据未同步时按空处理，脚本会走到自己的兜底分支
		}
		return routeNumberMap;
	}

	/**
	 * 单个颜色的线路编号：优先用客户端线路数据里的真实编号
	 * （MTR 4.1 的 {@code Route#getRouteNumber()}，见 {@link SignRouteData}），
	 * 其次是方块实体里同步过来的编号，最后才从线路名里猜。
	 */
	private static String resolveRouteNumber(BlockCRTRailwaySign.BlockEntityCRTRailwaySign entity, net.minecraft.util.math.BlockPos pos, long color) {
		final String live = SignRouteData.getRouteNumber(pos, color);
		if (!live.isEmpty()) {
			return live;
		}
		final String synced = entity.getRouteNumber(color);
		if (synced != null && !synced.isEmpty()) {
			return synced;
		}
		return deriveNumberFromName(SignRouteData.getRouteName(pos, color));
	}

	/** 与 4.0 一致：以告示牌为原点的 StoredMatrixTransformations，供延后渲染的绘制原语使用。 */
	private static StoredMatrixTransformations signTransformations(net.minecraft.util.math.BlockPos pos, BlockRailwaySign blockRailwaySign, net.minecraft.util.math.Direction facing) {
		final StoredMatrixTransformations storedMatrixTransformations = new StoredMatrixTransformations(0.5 + pos.getX(), 0.53125 + pos.getY(), 0.5 + pos.getZ());
		storedMatrixTransformations.add(matrices -> {
			Drawing.rotateYDegrees(matrices, -DirectionHelper.asRotation(facing));
			Drawing.rotateZDegrees(matrices, 180);
			matrices.translate(blockRailwaySign.getXStart() / 16.0F - 0.5F, 0, -0.06875000009313226);
		});
		return storedMatrixTransformations;
	}
}
