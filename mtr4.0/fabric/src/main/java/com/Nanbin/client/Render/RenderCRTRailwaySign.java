package com.Nanbin.client.Render;

import com.Nanbin.Registry.RegBlock.BlockCRTRailwaySign;
import com.Nanbin.client.Drawing.CustomFontTextureCache;
import com.Nanbin.client.Drawing.CustomFontTextureCache.FittedTextTexture;
import com.Nanbin.client.Drawing.CustomFontTextureCache.FontType;
import com.Nanbin.client.Drawing.SignTextStyleConfig;
import com.Nanbin.client.Drawing.SignTextStyleConfig.SignTextStyle;
import org.mtr.core.data.SimplifiedRoute;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockEntityRenderer;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mod.Init;
import org.mtr.mod.block.BlockRailwaySign;
import org.mtr.mod.block.BlockStationNameBase;
import org.mtr.mod.block.IBlock;
import org.mtr.mod.client.IDrawing;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.data.IGui;
import org.mtr.mod.generated.lang.TranslationProvider;
import org.mtr.mod.render.MainRenderer;
import org.mtr.mod.render.QueuedRenderLayer;
import org.mtr.mod.render.RenderRailwaySign;
import org.mtr.mod.render.StoredMatrixTransformations;
import org.mtr.mod.resource.SignResource;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RenderCRTRailwaySign extends RenderRailwaySign<BlockCRTRailwaySign.BlockEntityCRTRailwaySign> {

	private static final FontType FONT_TYPE = FontType.ALIBABA;
	private static final int FONT_SIZE = 88;
	/** 模块2 数字放大系数：数字字形在字盒中偏小（上下大量空白），需放大更多才能填满方框。 */
	private static final float NUMBER_SCALE_BOOST = 1.35F;
	/** 模块2 中文放大系数：汉字为方块字，字形已充满字盒，无需放大（1.0 = 刚好填满不溢出）。 */
	private static final float CJK_SCALE_BOOST = 1.0F;
	/** 模块3 中文行字号比例：汉字缩小到原来的 80%。 */
	private static final float MODULE3_CJK_SCALE = 0.8F;
	/** 默认文字颜色（白底时黑色；深色自定义背景时自动切换为白色）。 */
	private static final Color TEXT_COLOR = new Color(0, 0, 0);
	private static final long REFRESH_INTERVAL_MS = 1000L;
	/** 英文/数字行的字号比例（中文字号 = 英文字号 × 2，即英文 = 中文 / 2）。 */
	private static final float LATIN_SCALE = 1.0F / 2.0F;
	/** 行距与字号的比例（中文行与英文行之间不留净间距）。 */
	private static final float LINE_GAP_RATIO = 0.0F;
	/** 纯英文/数字行相对其上移的比例（拉丁字形在文本框内偏下，上移后视觉上与中文行更对齐）。 */
	private static final float LATIN_RAISE_RATIO = 0.7F;

	private static final Identifier WHITE_TEXTURE = new Identifier(Init.MOD_ID, "textures/block/white.png");

	/** 模块化线路编号牌（模块1 色条 + 模块2 数字 + 模块3 线路名文字）。 */
	private static final String ROUTE_NUMBER_V2 = "crt_route_name";
	private static final String ROUTE_NUMBER_V2_FLIPPED = "crt_route_name_flipped";

	private long lastRefreshTime = 0;

	public RenderCRTRailwaySign(Argument dispatcher) {
		super(dispatcher);
	}

	@Override
	public void render(BlockCRTRailwaySign.BlockEntityCRTRailwaySign entity, float tickDelta, GraphicsHolder graphicsHolder, int light, int overlay) {
		// 定时强制刷新字体纹理缓存，确保站名等内容更新后立即生效
		final long now = System.currentTimeMillis();
		if (now - lastRefreshTime >= REFRESH_INTERVAL_MS) {
			lastRefreshTime = now;
			CustomFontTextureCache.instance.clearFittedTextureCache();
		}

		final World world = entity.getWorld2();
		if (world == null) {
			return;
		}

		final BlockPos pos = entity.getPos2();
		final BlockState state = world.getBlockState(pos);
		if (!(state.getBlock().data instanceof BlockRailwaySign)) {
			return;
		}
		final BlockRailwaySign block = (BlockRailwaySign) state.getBlock().data;
		final String[] signIds = entity.getSignIds();
		if (signIds == null || signIds.length != block.length) {
			Init.LOGGER.warn("CRT Railway Sign render skipped: signIds.length={}, block.length={}", signIds == null ? "null" : signIds.length, block.length);
			return;
		}
		final Direction facing = IBlock.getStatePropertySafe(state, BlockStationNameBase.FACING);

		boolean renderBackground = false;
		int backgroundColor = 0;
		for (final String signId : signIds) {
			if (signId != null) {
				final SignResource sign = RenderRailwaySign.getSign(signId);
				if (sign != null) {
					renderBackground = true;
					if (sign.getBackgroundColor() != 0) {
						backgroundColor = sign.getBackgroundColor();
						break;
					}
				}
			}
		}

		// 线路名按颜色索引（客户端 SimplifiedRoute 不含 routeNumber，但含线路名）
		final Map<Long, String> routeNameByColor = new HashMap<>();
		try {
			for (final SimplifiedRoute route : MinecraftClientData.getInstance().simplifiedRoutes) {
				routeNameByColor.put((long) route.getColor(), route.getName());
			}
		} catch (Exception ignored) {
		}

		final StoredMatrixTransformations storedMatrixTransformations = new StoredMatrixTransformations(0.5 + pos.getX(), 0.53125 + pos.getY(), 0.5 + pos.getZ());
		storedMatrixTransformations.add(graphicsHolderNew -> {
			graphicsHolderNew.rotateYDegrees(-facing.asRotation());
			graphicsHolderNew.rotateZDegrees(180);
			graphicsHolderNew.translate(block.getXStart() / 16F - 0.5, 0, -0.0625 - SMALL_OFFSET * 2);
		});

		graphicsHolder.push();
		graphicsHolder.translate(0.5, 0.53125, 0.5);
		graphicsHolder.rotateYDegrees(-facing.asRotation());
		graphicsHolder.rotateZDegrees(180);
		graphicsHolder.translate(block.getXStart() / 16F - 0.5, 0, -0.0625 - SMALL_OFFSET * 2);

		if (renderBackground) {
			// 默认背景底色为白色（原版为黑）
			final int newBackgroundColor = backgroundColor == 0 ? ARGB_WHITE : backgroundColor | ARGB_BLACK;
			MainRenderer.scheduleRender(WHITE_TEXTURE, false, QueuedRenderLayer.LIGHT, (graphicsHolderNew, offset) -> {
				storedMatrixTransformations.transform(graphicsHolderNew, offset);
				IDrawing.drawTexture(graphicsHolderNew, 0, 0, SMALL_OFFSET, 0.5F * (signIds.length), 0.5F, SMALL_OFFSET, facing, newBackgroundColor, GraphicsHolder.getDefaultLight());
				graphicsHolderNew.pop();
			});
		}
		for (int i = 0; i < signIds.length; i++) {
			if (signIds[i] != null && !isRouteNumberV2(signIds[i])) {
				drawSignCRT(
						graphicsHolder,
						storedMatrixTransformations,
						pos,
						signIds[i],
						0.5F * i,
						0,
						0.5F,
						RenderRailwaySign.getMaxWidth(signIds, i, false),
						RenderRailwaySign.getMaxWidth(signIds, i, true),
						entity.getSelectedIds(),
						entity.getRouteNumbers(),
						facing,
						backgroundColor,
						(textureId, x, y, size, flipTexture) -> MainRenderer.scheduleRender(textureId, true, QueuedRenderLayer.LIGHT_TRANSLUCENT, (graphicsHolderNew, offset) -> {
							storedMatrixTransformations.transform(graphicsHolderNew, offset);
							IDrawing.drawTexture(graphicsHolderNew, x, y, size, size, flipTexture ? 1 : 0, 0, flipTexture ? 0 : 1, 1, facing, -1, GraphicsHolder.getDefaultLight());
							graphicsHolderNew.pop();
						}),
						false
				);
			}
		}

		// 模块化线路编号牌最后绘制，保证模块3 文字盖在其他内容之上
		renderRouteNumberV2Regions(storedMatrixTransformations, entity, signIds, facing, 0.5F, routeNameByColor, backgroundColor);

		graphicsHolder.pop();
	}

	/**
	 * 复制原版 {@link RenderRailwaySign#drawSign} 的全部逻辑，仅将“自定义文字”槽位的渲染
	 * 替换为 CRT 版（白底 + 阿里巴巴字体 + 等比缩小）。出口字母、线路、站台等非文字槽位
	 * 仍然调用原版静态方法，保证逻辑一致。
	 */
	/**
	 * 渲染单个指示牌格子。
	 * <p>
	 * transparentBlack 为 true 时用于 StationInfo1 正面双格指示牌：不画任何背景矩形、
	 * 不画贴图图标，文字居中填满格子且强制黑色（其余槽位仍按原版逻辑绘制）。
	 */
	public static void drawSignCRT(GraphicsHolder graphicsHolder, @Nullable StoredMatrixTransformations storedMatrixTransformations, BlockPos pos, String signId, float x, float y, float size, float maxWidthLeft, float maxWidthRight, LongAVLTreeSet selectedIds, String[] routeNumbers, Direction facing, int backgroundColor, DrawTexture drawTexture, boolean transparentBlack) {
		final SignResource sign = RenderRailwaySign.getSign(signId);
		if (sign == null) {
			return;
		}

		final boolean isExit = signId.equals("exit_letter") || signId.equals("exit_letter_flipped");
		final boolean isLine = signId.equals("line") || signId.equals("line_flipped");
		final boolean isPlatform = signId.equals("platform") || signId.equals("platform_flipped");

		// GUI 预览（storedMatrixTransformations == null）：完全沿用原版绘制，保证屏幕逻辑一致
		if (storedMatrixTransformations == null) {
			RenderRailwaySign.drawSign(graphicsHolder, null, pos, signId, x, y, size, maxWidthLeft, maxWidthRight, selectedIds, facing, transparentBlack ? ARGB_BLACK : backgroundColor | ARGB_BLACK, drawTexture);
			return;
		}

		// 非文字槽位（出口字母 / 线路 / 站台 / 纯贴图图标）：完全保留原版逻辑（背景色沿用原版的黑底规则）
		if (isExit || isLine || isPlatform) {
			RenderRailwaySign.drawSign(graphicsHolder, storedMatrixTransformations, pos, signId, x, y, size, maxWidthLeft, maxWidthRight, selectedIds, facing, transparentBlack ? ARGB_BLACK : backgroundColor | ARGB_BLACK, drawTexture);
			return;
		}
		if (!sign.hasCustomText) {
			RenderRailwaySign.drawSign(graphicsHolder, storedMatrixTransformations, pos, signId, x, y, size, maxWidthLeft, maxWidthRight, selectedIds, facing, transparentBlack ? ARGB_BLACK : backgroundColor | ARGB_BLACK, drawTexture);
			return;
		}

		// ---- 自定义文字槽位：CRT 渲染（阿里巴巴字体 + 等比缩小） ----
		final boolean flipCustomText = sign.getFlipCustomText();
		final boolean isSmall = sign.getSmall();
		final boolean isStation = signId.equals("station") || signId.equals("station_flipped");
		final boolean isRouteNumber = signId.equals("crt_route_number") || signId.equals("crt_route_number_flipped");
		// 样式按 signs.json 中的 id 注册；_flipped 变体回退到基础 id
		SignTextStyle style = SignTextStyleConfig.get(signId);
		if (style == null && signId.endsWith("_flipped")) {
			style = SignTextStyleConfig.get(signId.substring(0, signId.length() - "_flipped".length()));
		}

		final String signText = resolveSignText(signId, sign, selectedIds, routeNumbers, isStation, isRouteNumber);

		// 透明黑字模式（StationInfo1 双格指示牌）：文字居中填满格子，不画背景、不画贴图
		if (transparentBlack) {
			renderCustomTextCRT(signText, storedMatrixTransformations, facing, size, x, false, size, 0, isRouteNumber ? getNumberFontType(signText) : FONT_TYPE, isRouteNumber, true, false, null, false, 1.0F, TEXT_COLOR);
			return;
		}

		// 站名牌：白底黑字，不画原版红板；小图标文字牌（如 lift_1_text）则保留图标贴图
		// 线路编号：纯文字槽位，不画（不存在的）贴图，避免出现空白透明贴图图标
		if (!isStation && !isRouteNumber) {
			final float signSize = (sign.getSmall() ? BlockRailwaySign.SMALL_SIGN_PERCENTAGE : 1) * size;
			final float margin = (size - signSize) / 2;
			drawTexture.drawTexture(sign.getTexture(), x + margin, y + margin, signSize, sign.getFlipTexture());
		}

		final float fixedMargin = size * (1 - BlockRailwaySign.SMALL_SIGN_PERCENTAGE) / 2;
		// 线路编号：文字应填满当前单个格子（start = x，maxWidth = 单格宽），
		// 其余自定义文字沿用原版"图标 + 右侧文字"布局
		final float maxWidth;
		final float start;
		if (isRouteNumber) {
			maxWidth = size;
			start = x;
		} else {
			maxWidth = Math.max(0, (flipCustomText ? maxWidthLeft : maxWidthRight) * size - fixedMargin * (isSmall ? 1 : 2));
			start = flipCustomText ? x - (isSmall ? 0 : fixedMargin) : x + size + (isSmall ? 0 : fixedMargin);
		}

		renderCustomTextCRT(signText, storedMatrixTransformations, facing, size, start, flipCustomText, maxWidth, backgroundColor, isRouteNumber ? getNumberFontType(signText) : FONT_TYPE, isRouteNumber, isRouteNumber, isRouteNumber, style, true, 1.0F, null);
	}

	/** 解析指示牌格子要显示的文字（站名合并 / 线路编号 / 自定义文字）。 */
	private static String resolveSignText(String signId, SignResource sign, LongAVLTreeSet selectedIds, String[] routeNumbers, boolean isStation, boolean isRouteNumber) {
		if (isStation) {
			return IGui.mergeStations(selectedIds.longStream()
					.filter(MinecraftClientData.getInstance().stationIdMap::containsKey)
					.sorted()
					.mapToObj(stationId -> IGui.insertTranslation(TranslationProvider.GUI_MTR_STATION_CJK, TranslationProvider.GUI_MTR_STATION, 1, MinecraftClientData.getInstance().stationIdMap.get(stationId).getName()))
					.collect(Collectors.toList())
			);
		} else if (isRouteNumber) {
			// 同时选中多条线路时用 "/" 同一行分隔显示（"|" 是本渲染器的换行符，不能用来连接编号）
			return String.join("/", routeNumbers);
		} else {
			return sign.getCustomText().getString();
		}
	}

	/**
	 * CRT 版自定义文字渲染：使用阿里巴巴字体预渲染为贴紧文字的纹理，
	 * 以槽位高度为基准，宽度超出可用空间时按相同比例同时缩放宽高（等比缩小），
	 * 不再像原版那样把高度固定为槽位高而把宽度截断造成横向拉伸。
	 */
	private static void renderCustomTextCRT(String signText, StoredMatrixTransformations storedMatrixTransformations, Direction facing, float size, float start, boolean flipCustomText, float maxWidth, int backgroundColor, FontType fontType, boolean forceFullSizeLines, boolean centerInCell, boolean forceWhiteBackground, @Nullable SignTextStyle style, boolean drawBackground, float cjkScale, @Nullable Color forcedTextColor) {
		if (maxWidth <= 0) {
			return;
		}

		// 线路编号等纯文字格子强制白底黑字；其余格子默认白色，sign 资源配置了 backgroundColor 时使用该自定义色，仿照 MTR 逻辑
		final int textBackgroundColor = forceWhiteBackground || backgroundColor == 0 ? ARGB_WHITE : backgroundColor | ARGB_BLACK;
		// 文字颜色：forcedTextColor 优先级最高（透明背景黑字场景）；否则 signs.json 配置了 textColor 时优先使用；
		// 再否则根据背景亮度自适应（深色背景用白色文字，浅色背景用黑色文字）
		final Color textColor = forcedTextColor != null ? forcedTextColor : (style != null && style.hasColor() ? new Color(style.textColor) : (isLightBackground(textBackgroundColor) ? TEXT_COLOR : new Color(255, 255, 255)));
		final float sizeMult = style != null ? style.textSize : 1.0F;
		final boolean bold = style != null && style.textBold;
		// 规定：不加粗用阿里巴巴字体，加粗用思源黑体（思源黑体本身为 Bold 字重）
		final FontType renderFontType = bold ? FontType.SOURCE_HAN : fontType;

		// 原版以 “|” 作为换行符（getTextPixels 会按 “|” 分行），逐行预渲染为贴紧文字的纹理。
		// 中文为主、英文为辅：含中文的行用基准字号，纯英文/数字行用中文的 1/2，保证中文=英文×2。
		// 注意：纹理高度 = ascent + descent + padding×2，堆叠时必须剔除上下 padding，
		// 否则每行会多出 2×padding 的透明边距，导致实际行距远大于设定值。
		final List<FittedTextTexture> fittedLines = new ArrayList<>();
		final List<Integer> linePaddings = new ArrayList<>();
		final List<Integer> lineRaises = new ArrayList<>(); // 英文行上移量（像素）
		int totalGlyphHeight = 0; // 所有行的字形高度（纹理高 - 上下 padding）之和
		int maxPxWidth = 0;
		int maxPadding = 0;
		for (final String line : signText.split("\\|")) {
			if (line.isEmpty()) {
				continue;
			}
			final boolean cjk = containsCJK(line);
			// 线路编号等需要全字号显示的内容：所有行都用基准字号（× cjkScale），不做中英缩放；
			// 否则默认按中文字号=英文×2 的规则处理。cjkScale 用于模块3 等需要缩小汉字字号的场景。
			final int lineFontSize = cjk || forceFullSizeLines ? Math.round(FONT_SIZE * cjkScale) : Math.round(FONT_SIZE * LATIN_SCALE);
			final FittedTextTexture fitted = CustomFontTextureCache.instance.getFittedTextTexture(line, renderFontType, lineFontSize, textColor, bold);
			if (fitted.width <= 0 || fitted.height <= 0) {
				return;
			}
			final int padding = Math.max(2, Math.round(lineFontSize * 0.08F));
			fittedLines.add(fitted);
			linePaddings.add(padding);
			lineRaises.add(cjk || forceFullSizeLines ? 0 : Math.round(lineFontSize * LATIN_RAISE_RATIO));
			totalGlyphHeight += Math.max(1, fitted.height - padding * 2);
			maxPxWidth = Math.max(maxPxWidth, fitted.width);
			maxPadding = Math.max(maxPadding, padding);
		}
		if (fittedLines.isEmpty()) {
			return;
		}

		// 行间距：字形底到下一行字形顶的净间距（以中文基准字号比例设定；0 表示完全无间距）
		final int lineGapPx = Math.round(FONT_SIZE * LINE_GAP_RATIO);
		totalGlyphHeight += lineGapPx * (fittedLines.size() - 1);

		// 以整个槽位高度为文字框基准（与原版 renderCustomText 一致，高度为 size），
		// 先按整块文字的自然宽高比换算，再对宽高做缩放。
		// textSize（默认 1.0）控制文字相对槽位的整体大小；线路编号等单格内容（centerInCell）
		// 默认填满整格（scaleY=1），宽度超出时仅横向压缩，不再等比缩小。
		final float textHeight = size;
		final float pixelToWorld = textHeight / totalGlyphHeight;
		final float blockWidth = maxPxWidth * pixelToWorld;
		final float blockHeight = totalGlyphHeight * pixelToWorld;
		final float fitScale = Math.min(1.0F, maxWidth / blockWidth);
		final float scaleY;
		final float scaleX;
		if (centerInCell) {
			scaleY = sizeMult;
			scaleX = Math.min(sizeMult, maxWidth / blockWidth);
		} else {
			scaleY = fitScale * sizeMult;
			scaleX = fitScale * sizeMult;
		}
		final float width = blockWidth * scaleX;
		final float height = blockHeight * scaleY;

		// 线路编号等单格内容：以字形宽度（剔除左右 padding）水平居中；
		// 否则沿用 flip 布局
		final float x1;
		if (centerInCell) {
			// 纹理宽含左右 padding，字形实际宽 = 最大纹理宽 - 2×padding
			final float glyphWidth = Math.max(1, maxPxWidth - maxPadding * 2) * pixelToWorld * scaleX;
			x1 = start + (size - glyphWidth) / 2;
		} else {
			x1 = flipCustomText ? start - width : start;
		}
		// 垂直居中，保证美观
		final float y1 = (size - height) / 2;

		// 文字区底色（白底或自定义背景色）：线路编号格子铺满整个格子，其余紧贴字形区域。
		// 模块化线路编号牌的模块3 直接画在告示牌已有背景上，不额外铺底，避免盖住模块1 色条。
		if (drawBackground) {
			final float backgroundX = centerInCell ? start : x1;
			final float backgroundY = centerInCell ? 0 : y1;
			final float backgroundW = centerInCell ? size : width;
			final float backgroundH = centerInCell ? size : height;
			MainRenderer.scheduleRender(WHITE_TEXTURE, false, QueuedRenderLayer.LIGHT_TRANSLUCENT, (graphicsHolderNew, offset) -> {
				storedMatrixTransformations.transform(graphicsHolderNew, offset);
				IDrawing.drawTexture(graphicsHolderNew, backgroundX, backgroundY, backgroundW, backgroundH, 0, 0, 1, 1, facing, textBackgroundColor, GraphicsHolder.getDefaultLight());
				graphicsHolderNew.pop();
			});
		}

		// 逐行绘制：纹理的 y 需减去 padding 缩放值，使字形顶部正好对齐 lineY；
		// 英文行额外上移 raise 像素，让拉丁字形在视觉上与中文行对齐。
		// 行距 = 上一行字形高 + 净行距 lineGapPx，保证字间距精确可控。
		float lineY = y1;
		for (int i = 0; i < fittedLines.size(); i++) {
			final FittedTextTexture lineTexture = fittedLines.get(i);
			final int padding = linePaddings.get(i);
			final int raise = lineRaises.get(i);
			final float lineW = lineTexture.width * pixelToWorld * scaleX;
			final float lineH = lineTexture.height * pixelToWorld * scaleY;
			// 以字形左缘（/右缘）对齐：各行纹理左侧 padding 因字号而异，必须按各自 padding 平移，
			// 使所有行字形的最左（/最右）边缘对齐到同一位置，而不是让纹理框对齐。
			final float lineX;
			if (centerInCell) {
				// 单格内容：始终按字形左缘对齐到 x1（水平居中）
				lineX = x1 - padding * pixelToWorld * scaleX;
			} else {
				lineX = flipCustomText ? start - lineW + padding * pixelToWorld * scaleX : x1 - padding * pixelToWorld * scaleX;
			}
			final float finalLineY = lineY - (padding + raise) * pixelToWorld * scaleY;
			final float glyphLineHeight = Math.max(1, lineTexture.height - padding * 2) * pixelToWorld * scaleY;
			MainRenderer.scheduleRender(lineTexture.identifier, true, QueuedRenderLayer.LIGHT_TRANSLUCENT, (graphicsHolderNew, offset) -> {
				storedMatrixTransformations.transform(graphicsHolderNew, offset);
				IDrawing.drawTexture(graphicsHolderNew, lineX, finalLineY, lineW, lineH, 0, 0, 1, 1, facing, -1, GraphicsHolder.getDefaultLight());
				graphicsHolderNew.pop();
			});
			lineY += glyphLineHeight + lineGapPx * pixelToWorld * scaleY;
		}
	}

	/** 判断文本中是否包含 CJK（中文）字符。 */
	private static boolean containsCJK(String text) {
		for (int i = 0; i < text.length(); i++) {
			final Character.UnicodeScript script = Character.UnicodeScript.of(text.charAt(i));
			if (script == Character.UnicodeScript.HAN) {
				return true;
			}
		}
		return false;
	}

	/** 判断 ARGB 背景色是否为浅色（亮度 > 128 视为浅色，用黑字；否则用白字）。 */
	private static boolean isLightBackground(int argbColor) {
		final int r = (argbColor >> 16) & 0xFF;
		final int g = (argbColor >> 8) & 0xFF;
		final int b = argbColor & 0xFF;
		return (r * 299 + g * 587 + b * 114) / 1000 > 128;
	}

	// ---- 模块化线路编号牌（crt_route_name / _flipped） ----

	private static boolean isRouteNumberV2(String signId) {
		return ROUTE_NUMBER_V2.equals(signId) || ROUTE_NUMBER_V2_FLIPPED.equals(signId);
	}

	/**
	 * 从线路名推导编号：先忽略 "||"（或更多连续竖线）之后的内容，再取首个 "|" 前的
	 * 主要段作为编号。例如 "2号线|Branch Line 2" → "2号线"。可能返回中文。
	 */
	private static String deriveNumberFromName(@Nullable String rawName) {
		if (rawName == null || rawName.isEmpty()) {
			return "";
		}
		final int hideIdx = rawName.indexOf("||");
		final String visible = hideIdx >= 0 ? rawName.substring(0, hideIdx) : rawName;
		final int sepIdx = visible.indexOf('|');
		final String primary = sepIdx >= 0 ? visible.substring(0, sepIdx) : visible;
		return primary.trim();
	}

	/**
	 * 线路名去掉编号后的剩余文字：先忽略 "||"（或更多连续竖线）之后的内容，
	 * 再将编号与线路名按单 "|" 分段，逐段移除对应的编号子串（编号可能为中文，
	 * 也支持 "环|Loop" 这类双语编号，各段独立匹配），随后去掉残余数字、丢弃空段，
	 * 中英文按出现顺序用 "|" 重新连接。
	 * 例如 "环线|Loop Line"，编号 "环|Loop" → "线|Line"。
	 */
	private static String stripNumberFromName(@Nullable String rawName, String number) {
		if (rawName == null || rawName.isEmpty()) {
			return "";
		}
		final int hideIdx = rawName.indexOf("||");
		final String visible = hideIdx >= 0 ? rawName.substring(0, hideIdx) : rawName;
		final String[] numberSegments = number == null || number.isEmpty() ? new String[0] : number.split("\\|");
		final String[] nameSegments = visible.split("\\|");
		final StringBuilder rest = new StringBuilder();
		for (final String segment : nameSegments) {
			String stripped = segment.trim();
			for (final String numSeg : numberSegments) {
				stripped = stripped.replace(numSeg, "");
			}
			stripped = stripped.replaceAll("\\d", "").trim();
			if (stripped.isEmpty()) {
				continue;
			}
			if (rest.length() > 0) {
				rest.append("|");
			}
			rest.append(stripped);
		}
		return rest.toString();
	}

	/**
	 * 渲染所有模块化线路编号牌区域。用户把该牌放到多个连续格子里（每条线路一格）。
	 * <p>
	 * 普通版每格画 [模块1 色条][模块2 数字]，模块3（线路名文字）自动画在最后一格右侧；
	 * flip 版每格画 [模块2][模块1]，模块3 画在区域最左侧（镜像普通版）。
	 */
	private static void renderRouteNumberV2Regions(StoredMatrixTransformations storedMatrixTransformations, BlockCRTRailwaySign.BlockEntityCRTRailwaySign entity, String[] signIds, Direction facing, float size, Map<Long, String> routeNameByColor, int backgroundColor) {
		for (int i = 0; i < signIds.length; i++) {
			if (signIds[i] == null || !isRouteNumberV2(signIds[i])) {
				continue;
			}
			// 连续同一种 v2 格子（普通版或 flip 版，不混用）为一个区域
			int start = i;
			while (start > 0 && signIds[start - 1] != null && signIds[start - 1].equals(signIds[i])) {
				start--;
			}
			int v2End = i;
			while (v2End < signIds.length - 1 && signIds[v2End + 1] != null && signIds[v2End + 1].equals(signIds[i])) {
				v2End++;
			}
			final boolean regionFlipped = ROUTE_NUMBER_V2_FLIPPED.equals(signIds[start]);
			// 该区域使用的线路：区域内第一格有独立设置的优先，无则回退全局选择
			long[] colors = null;
			for (int c = start; c <= v2End && colors == null; c++) {
				final long[] cellColors = entity.getCellColors(c);
				if (cellColors != null && cellColors.length > 0) {
					colors = cellColors;
				}
			}
			if (colors == null) {
				colors = entity.getSelectedIds().longStream().toArray();
			}
			if (colors.length == 0) {
				i = v2End;
				continue;
			}
			// 空白格只吸收到能容纳所选线路数量为止（选 N 条线就吸收 N-1 个空白格），
			// 多余的空白格留给同一块告示牌上后续的线路名牌区域。
			// 优先向右扩展（与普通版一致）；flip 版右侧不够时再向左镜像扩展。
			final int extraBlanks = Math.max(0, colors.length - (v2End - start + 1));
			int regionStart = start;
			int end = v2End;
			for (int c = v2End + 1; c < signIds.length && c <= v2End + extraBlanks; c++) {
				final String nextSignId = signIds[c];
				if (nextSignId == null || nextSignId.isEmpty()) {
					end = c;
				} else {
					break;
				}
			}
			if (regionFlipped) {
				final int rightAbsorbed = end - v2End;
				for (int c = start - 1; c >= 0 && c >= start - (extraBlanks - rightAbsorbed); c--) {
					final String nextSignId = signIds[c];
					if (nextSignId == null || nextSignId.isEmpty()) {
						regionStart = c;
					} else {
						break;
					}
				}
			}
			for (int cell = regionStart; cell <= end; cell++) {
				renderRouteNumberV2Cell(storedMatrixTransformations, signIds, cell, regionStart, end, colors, routeNameByColor, entity, facing, size, backgroundColor, regionFlipped);
			}
			i = end;
		}
	}

	private static void renderRouteNumberV2Cell(StoredMatrixTransformations storedMatrixTransformations, String[] signIds, int cellIndex, int regionStart, int regionEnd, long[] colors, Map<Long, String> routeNameByColor, BlockCRTRailwaySign.BlockEntityCRTRailwaySign entity, Direction facing, float size, int backgroundColor, boolean regionFlipped) {
		// 空白格（自动并入区域的未设置格子）继承区域 flip 方向
		final String cellSignId = signIds[cellIndex];
		final boolean flipped = ROUTE_NUMBER_V2_FLIPPED.equals(cellSignId)
				|| ((cellSignId == null || cellSignId.isEmpty()) && regionFlipped);
		final int shownRoutes = Math.min(regionEnd - regionStart + 1, colors.length);
		// flip 版是镜像：从正确一侧阅读时线路顺序正确，故区域最左格显示最后一条线路、最右格显示第一条
		final int routeIndex = flipped ? (regionEnd - cellIndex) : (cellIndex - regionStart);
		if (routeIndex >= shownRoutes) {
			return;
		}
		final long routeColorLong = colors[routeIndex];
		final int routeColor = (int) routeColorLong | ARGB_BLACK;
		final String routeName = routeNameByColor.get(routeColorLong);
		// 模块2 线路编号：优先使用服务端同步的真实线路编号（可能为中文/字母/含 "|" 的双语编号，不一定只是数字），
		// 无编号时回退到线路名推导。
		String number = entity.getRouteNumber(routeColorLong);
		if (number.isEmpty()) {
			number = deriveNumberFromName(routeName);
		}
		// 模块3 线路名文字：线路名去掉编号后的剩余内容，中英文都保留（忽略 "||" 及更多连续竖线之后的内容）
		final String text = stripNumberFromName(routeName, number);
		// 模块3 文字所在格子：普通版在最后一个显示线路的格子右侧；
		// flip 版镜像普通版，在最后一个显示线路的格子（区域右侧第一格）左侧
		final boolean isTextCell = flipped
				? (cellIndex == regionEnd - shownRoutes + 1)
				: (cellIndex == regionStart + shownRoutes - 1);
		final float cellX = cellIndex * size;

		// 普通版与 flip 版布局差异大，拆分成两个独立方法，各自微调互不影响
		if (flipped) {
			renderRouteNumberV2CellFlip(storedMatrixTransformations, signIds, cellIndex, facing, size, cellX, routeColor, number, text, isTextCell, backgroundColor);
		} else {
			renderRouteNumberV2CellNormal(storedMatrixTransformations, signIds, cellIndex, facing, size, cellX, routeColor, number, text, isTextCell, backgroundColor);
		}
	}

	/**
	 * 普通版：从左到右 [模块1 色条][模块2 数字]，模块3 文字画在最后一格右侧。
	 * 坐标系 y=0 是格子顶部、+y 向下，格子底部 = 1.0*size。
	 */
	private static void renderRouteNumberV2CellNormal(StoredMatrixTransformations storedMatrixTransformations, String[] signIds, int cellIndex, Direction facing, float size, float cellX, int routeColor, String number, String text, boolean isLast, int backgroundColor) {
		// 模块1 色条：宽0.3格、高0.8格，底部贴告示牌底部（顶部 = 1.0 - 0.8 = 0.2格）
		final float m1X = cellX;
		final float m1Y = 0.2F * size;
		final float m1W = 0.3F * size;
		final float m1H = 0.8F * size;
		drawSolidRect(storedMatrixTransformations, facing, m1X, m1Y, m1W, m1H, routeColor);

		// 模块2 数字块：从 0.3 到 1.0 格（宽0.7），占满整格高度；无数字时整块不画
		final boolean hasNumber = !number.isEmpty();
		if (hasNumber) {
			drawNumberBlock(storedMatrixTransformations, facing, cellX + 0.3F * size, 0, 0.7F * size, 1.0F * size, number);
		}

		// 模块3 文字：仅最后一格；无数字（仅模块1+3）时从 0.4 起，与色条留 0.1 格空隙
		if (isLast && !text.isEmpty()) {
			final float textStart = cellX + (hasNumber ? 1.0F : 0.4F) * size;
			final SignTextStyle style = SignTextStyleConfig.get(signIds[cellIndex]);
			renderCustomTextCRT(text, storedMatrixTransformations, facing, size, textStart, false, 100F, backgroundColor, FontType.ALIBABA, false, false, false, style, false, MODULE3_CJK_SCALE, null);
		}
	}

	/**
	 * flip 版：镜像普通版。每格 [模块2 数字][模块1 色条]，模块3 文字画在区域最左格左侧。
	 */
	private static void renderRouteNumberV2CellFlip(StoredMatrixTransformations storedMatrixTransformations, String[] signIds, int cellIndex, Direction facing, float size, float cellX, int routeColor, String number, String text, boolean isTextCell, int backgroundColor) {
		// 模块1 色条：固定在最右侧 [0.7, 1.0]，底部贴告示牌底部
		final float m1X = cellX + 0.7F * size;
		final float m1Y = 0.2F * size;
		final float m1W = 0.3F * size;
		final float m1H = 0.8F * size;
		drawSolidRect(storedMatrixTransformations, facing, m1X, m1Y, m1W, m1H, routeColor);

		// 模块2 数字块：最左侧 [0, 0.7]，占满整格高度；无数字时整块不画
		final boolean hasNumber = !number.isEmpty();
		if (hasNumber) {
			drawNumberBlock(storedMatrixTransformations, facing, cellX, 0, 0.7F * size, size, number);
		}

		// 模块3 文字：flip 版画在区域最左格（即本格）的左侧，右对齐向左延伸，
		// 与普通版"最后一格右侧向左读"互为镜像。长文字向左超出告示牌边界。
		if (isTextCell && !text.isEmpty()) {
			final float textStart = cellX + (hasNumber ? 0.0F : 0.7F) * size;
			SignTextStyle style = SignTextStyleConfig.get(signIds[cellIndex]);
			if (style == null) {
				style = SignTextStyleConfig.get(ROUTE_NUMBER_V2);
			}
			renderCustomTextCRT(text, storedMatrixTransformations, facing, size, textStart, true, 100F, backgroundColor, FontType.ALIBABA, false, false, false, style, false, MODULE3_CJK_SCALE, null);
		}
	}

	/**
	 * 数字块：纯白底 + 思源黑体数字（无描边，用户确认）。
	 * 白底直接画在模块2 区域，白告示牌背景下不可见，仅作非白背景时的兜底。
	 */
	private static void drawNumberBlock(StoredMatrixTransformations storedMatrixTransformations, Direction facing, float x, float y, float w, float h, String number) {
		drawSolidRect(storedMatrixTransformations, facing, x, y, w, h, ARGB_WHITE);
		drawNumberInBox(storedMatrixTransformations, facing, x, y, w, h, number);
	}

	/** 用纯色矩形画一个纹理四边形（用于色条 / 数字块 / 描边）。 */
	private static void drawSolidRect(StoredMatrixTransformations storedMatrixTransformations, Direction facing, float x, float y, float width, float height, int color) {
		MainRenderer.scheduleRender(WHITE_TEXTURE, false, QueuedRenderLayer.LIGHT_TRANSLUCENT, (graphicsHolderNew, offset) -> {
			storedMatrixTransformations.transform(graphicsHolderNew, offset);
			IDrawing.drawTexture(graphicsHolderNew, x, y, width, height, 0, 0, 1, 1, facing, color, GraphicsHolder.getDefaultLight());
			graphicsHolderNew.pop();
		});
	}

	/**
	 * 线路编号的字体选择：纯数字/拉丁字符用 SOURCE_SANS_3，含中文时用阿里巴巴字体
	 * （Source Sans 3 不含中文字形，无法渲染中文）。
	 */
	private static FontType getNumberFontType(String text) {
		return containsCJK(text) ? FontType.ALIBABA : FontType.SOURCE_SANS_3;
	}

	/** 在指定方框内居中绘制线路编号（纯数字用 Source Sans 3，含中文用思源黑体）。 */
	private static void drawNumberInBox(StoredMatrixTransformations storedMatrixTransformations, Direction facing, float boxX, float boxY, float boxW, float boxH, String number) {
		if (number.isEmpty()) {
			return;
		}
		final FittedTextTexture fitted = CustomFontTextureCache.instance.getFittedTextTexture(number, getNumberFontType(number), FONT_SIZE, new Color(0, 0, 0));
		if (fitted.width <= 0 || fitted.height <= 0) {
			return;
		}
		// 数字/中文按方框等比缩放后，再按内容类型分开放大：
		// 汉字是方块字已填满字盒，放大 1.0 即可；数字字形偏小，需要更大倍数才显大。
		final int padding = Math.max(1, Math.round(FONT_SIZE * 0.02F));
		final float glyphW = Math.max(1, fitted.width - padding * 2);
		final float glyphH = Math.max(1, fitted.height - padding * 2);
		final float boost = containsCJK(number) ? CJK_SCALE_BOOST : NUMBER_SCALE_BOOST;
		final float scale = Math.min(boxH / glyphH, boxW / glyphW) * boost;
		final float textW = fitted.width * scale;
		final float textH = fitted.height * scale;
		final float textX = boxX + (boxW - textW) / 2;
		final float textY = boxY + (boxH - textH) / 2;
		MainRenderer.scheduleRender(fitted.identifier, true, QueuedRenderLayer.LIGHT_TRANSLUCENT, (graphicsHolderNew, offset) -> {
			storedMatrixTransformations.transform(graphicsHolderNew, offset);
			IDrawing.drawTexture(graphicsHolderNew, textX, textY, textW, textH, 0, 0, 1, 1, facing, -1, GraphicsHolder.getDefaultLight());
			graphicsHolderNew.pop();
		});
	}
}