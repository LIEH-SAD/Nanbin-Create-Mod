package com.Nanbin.client.Render;

import com.Nanbin.client.Drawing.CustomFontTextureCache;
import com.Nanbin.client.Drawing.FontRenderUtils;
import com.Nanbin.client.Drawing.SignTextStyleConfig;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import org.mtr.render.MainRenderer;
import org.mtr.render.QueuedRenderLayer;
import org.mtr.render.StoredMatrixTransformations;
import org.mtr.tool.Drawing;
import org.mtr.tool.GuiHelper;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * CRT 告示牌的绘制原语（延后渲染 + 自定义字体贴图）。
 * <p>
 * 取代 4.0 的 {@code org.mtr.mapping.mapper.IDrawing} + {@code GraphicsHolder} 组合。
 * MTR 4.1 去掉了那层包装，改成 {@link MainRenderer#scheduleRender} 的延后回调
 * （{@code (MatrixStack, VertexConsumer, Vec3d offset)}）配合 {@link Drawing} 这个
 * 「顶点/UV/颜色/光照 → draw()」的构建器，所以这里把 4.0 里满天飞的
 * {@code IDrawing.drawTexture(gh, x, y, w, h, u1, v1, u2, v2, facing, color, light)}
 * 收拢成 {@link #scheduleTexture} / {@link #scheduleRect} 两个入口。
 * <p>
 * 注意朝向：4.0 直接把 {@code facing} 传给 drawTexture 去做镜像；4.1 里调用方都已经用
 * {@link StoredMatrixTransformations} 把矩阵转到正确朝向了（和 MTR 自己的 SignResource 一样），
 * 所以这里不再单独处理 facing。
 * <p>
 * 告示牌绘制与 JS 站牌引擎共用这些方法，所以单独放一个类。
 */
public final class SignDrawing {

	/** 纯白贴图，用来画实心矩形。 */
	public static final Identifier WHITE_TEXTURE = Identifier.of("mtr", "textures/block/white.png");

	private static final float FONT_SIZE_DEFAULT = 88.0F;
	private static final float FONT_SIZE_LATIN = 44.0F;
	private static final float LATIN_RAISE_RATIO = 0.7F;
	private static final float LINE_GAP_RATIO = 0.0F;

	private SignDrawing() {
	}

	/**
	 * 贴图四边形的落点。
	 * <p>
	 * 同一份绘制代码要服务两种场合：
	 * <ul>
	 *     <li><b>世界渲染</b>：走 MTR 的延后渲染队列（{@link #deferred}），矩阵由
	 *     {@link StoredMatrixTransformations} 提供，实际绘制发生在 MTR 的渲染阶段；</li>
	 *     <li><b>GUI 预览</b>：必须立刻画进当前的 GUI 缓冲（{@link #immediate}），
	 *     否则在界面里什么都看不到——MTR 自己的 {@code SignPreviewComponent} 也是这么做的
	 *     （拿 GUI 的 {@code PoseStack} + {@code Minecraft.renderBuffers().bufferSource()}）。</li>
	 * </ul>
	 */
	@FunctionalInterface
	public interface QuadSink {

		/** 在调用方已经摆好的矩阵下画一个贴图四边形；{@code priority} 只在延后渲染时有效。 */
		void quad(Identifier texture, float x, float y, float width, float height, float u1, float v1, float u2, float v2, int color, int light, boolean priority);
	}

	/** 延后渲染（世界）。 */
	public static QuadSink deferred(StoredMatrixTransformations storedMatrixTransformations) {
		return (texture, x, y, width, height, u1, v1, u2, v2, color, light, priority) ->
				scheduleTexture(storedMatrixTransformations, texture, priority, QueuedRenderLayer.LIGHT_TRANSLUCENT, x, y, width, height, u1, v1, u2, v2, color, light);
	}

	/** 立即绘制（GUI 预览）。 */
	public static QuadSink immediate(net.minecraft.client.util.math.MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider) {
		return (texture, x, y, width, height, u1, v1, u2, v2, color, light, priority) -> {
			if (width <= 0 || height <= 0) {
				return;
			}
			final VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(GuiHelper.getGuiTexturedRenderType(texture));
			new Drawing(matrixStack, vertexConsumer)
					.setVerticesWH(x, y, width, height)
					.setUv(u1, v1, u2, v2)
					.setColor(color)
					.setLight(light)
					.draw();
		};
	}

	/** 实心矩形（通过任意 sink）。 */
	public static void rect(QuadSink sink, float x, float y, float width, float height, int color, int light) {
		sink.quad(WHITE_TEXTURE, x, y, width, height, 0, 0, 1, 1, color, light, false);
	}

	/** 世界空间下的默认光照（等价于 4.0 的 {@code GraphicsHolder.getDefaultLight()}）。 */
	public static int defaultLight() {
		return LightmapTextureManager.MAX_LIGHT_COORDINATE;
	}

	/** 在给定的矩阵变换下延后绘制一个贴图四边形。 */
	public static void scheduleTexture(StoredMatrixTransformations storedMatrixTransformations, Identifier texture, boolean priority, QueuedRenderLayer queuedRenderLayer, float x, float y, float width, float height, float u1, float v1, float u2, float v2, int color, int light) {
		if (width <= 0 || height <= 0) {
			return;
		}
		MainRenderer.scheduleRender(texture, priority, queuedRenderLayer, (matrixStack, vertexConsumer, offset) -> {
			storedMatrixTransformations.transform(matrixStack, offset);
			new Drawing(matrixStack, vertexConsumer)
					.setVerticesWH(x, y, width, height)
					.setUv(u1, v1, u2, v2)
					.setColor(color)
					.setLight(light)
					.draw();
			matrixStack.pop();
		});
	}

	/** 实心矩形（用白贴图着色）。 */
	public static void scheduleRect(StoredMatrixTransformations storedMatrixTransformations, float x, float y, float width, float height, int color, int light) {
		scheduleTexture(storedMatrixTransformations, WHITE_TEXTURE, false, QueuedRenderLayer.LIGHT_TRANSLUCENT, x, y, width, height, 0, 0, 1, 1, color, light);
	}

	private static void scheduleRect(StoredMatrixTransformations storedMatrixTransformations, float x, float y, float width, float height, int color) {
		scheduleRect(storedMatrixTransformations, x, y, width, height, color, defaultLight());
	}

	/** 画一条有粗细的线段（先平移到中点、按角度旋转，再画一个矩形）。 */
	public static void scheduleLine(StoredMatrixTransformations storedMatrixTransformations, float x1, float y1, float x2, float y2, float thickness, int color) {
		final float dx = x2 - x1;
		final float dy = y2 - y1;
		final float length = (float) Math.sqrt(dx * dx + dy * dy);
		if (length <= 1.0E-4F || thickness <= 0) {
			return;
		}
		final float angle = (float) Math.atan2(dy, dx);
		final float midX = (x1 + x2) / 2.0F;
		final float midY = (y1 + y2) / 2.0F;
		MainRenderer.scheduleRender(WHITE_TEXTURE, false, QueuedRenderLayer.LIGHT_TRANSLUCENT, (matrixStack, vertexConsumer, offset) -> {
			storedMatrixTransformations.transform(matrixStack, offset);
			matrixStack.translate(midX, midY, 0);
			Drawing.rotateZRadians(matrixStack, angle);
			new Drawing(matrixStack, vertexConsumer)
					.setVerticesWH(-length / 2.0F, -thickness / 2.0F, length, thickness)
					.setUv(0, 0, 1, 1)
					.setColor(color)
					.setLight(defaultLight())
					.draw();
			matrixStack.pop();
		});
	}

	// ---- 文本 ----

	/**
	 * CJK 判定：直接复用 Drawing 包统一的那份（{@link FontRenderUtils#containsCjk}）。
	 * <p>
	 * 之前这里抄的是 4.0 CRT 渲染器自带的 {@code UnicodeScript.HAN} 判定，会把中文标点、
	 * 全角符号排除在外，与 Drawing 包里其它渲染器（站名牌 / 路名牌 / 双语渲染）的口径不一致。
	 */
	public static boolean containsCJK(String text) {
		return FontRenderUtils.containsCjk(text);
	}

	/**
	 * 字体规则（Drawing 包的统一约定）：纯数字用 Source Sans 3，其余（含 CJK 与拉丁）用 Alibaba。
	 */
	public static CustomFontTextureCache.FontType fontTypeFor(String text) {
		return FontRenderUtils.isNumeric(text)
				? CustomFontTextureCache.FontType.SOURCE_SANS_3
				: CustomFontTextureCache.FontType.ALIBABA;
	}

	public static boolean isLightBackground(int argbColor) {
		final int r = argbColor >> 16 & 255;
		final int g = argbColor >> 8 & 255;
		final int b = argbColor & 255;
		return (r * 299 + g * 587 + b * 114) / 1000 > 128;
	}

	/**
	 * 量一段文本按当前字体渲染后会占多宽（世界坐标）。
	 * 4.0 的 {@code RenderCRTRailwaySign.measureTextWidth}，JS 侧 {@code JSText} 也要用。
	 */
	public static float measureTextWidth(String text, float boxHeight) {
		if (text == null || text.isEmpty() || boxHeight <= 0) {
			return 0;
		}
		int totalGlyphHeight = 0;
		int maxPxWidth = 0;
		int lineCount = 0;
		for (final String line : text.split("\\|")) {
			if (!line.isEmpty()) {
				final int lineFontSize = Math.round(containsCJK(line) ? FONT_SIZE_DEFAULT : FONT_SIZE_LATIN);
				final CustomFontTextureCache.FittedTextTexture fitted = CustomFontTextureCache.instance.getFittedTextTexture(line, fontTypeFor(line), lineFontSize, Color.BLACK);
				if (fitted.width > 0 && fitted.height > 0) {
					final int padding = Math.max(2, Math.round(lineFontSize * 0.08F));
					totalGlyphHeight += Math.max(1, fitted.height - padding * 2);
					maxPxWidth = Math.max(maxPxWidth, fitted.width);
					lineCount++;
				}
			}
		}
		if (lineCount == 0 || maxPxWidth <= 0) {
			return 0;
		}
		final float pixelToWorld = boxHeight / totalGlyphHeight;
		return maxPxWidth * pixelToWorld;
	}

	/** 简版：把文本画进一个盒子（JS 的 drawText 用）。 */
	public static void drawTextInBox(String text, StoredMatrixTransformations storedMatrixTransformations, float boxX, float boxY, float boxWidth, float boxHeight, int color, float scale, boolean bold, boolean centered) {
		drawTextInBox(text, deferred(storedMatrixTransformations), boxX, boxY, boxWidth, boxHeight, color, scale, bold, centered);
	}

	/** 简版：把文本画进一个盒子（sink 版）。 */
	public static void drawTextInBox(String text, QuadSink sink, float boxX, float boxY, float boxWidth, float boxHeight, int color, float scale, boolean bold, boolean centered) {
		if (text == null || text.isEmpty() || boxWidth <= 0 || boxHeight <= 0) {
			return;
		}
		drawCustomText(text, sink, boxHeight, boxX, false, boxWidth, 0,
				CustomFontTextureCache.FontType.ALIBABA, new SignTextStyleConfig.SignTextStyle(color, scale, bold), false,
				centered, new Color(color, true), boxY, boxWidth, 0, false);
	}

	/** 4.0 的 {@code RenderCRTRailwaySign.renderCustomTextCRT}：按 | 分行、CJK 与拉丁分别取字号、自动缩放居中。 */
	public static void drawCustomText(String signText, StoredMatrixTransformations storedMatrixTransformations, float size, float start, boolean flipCustomText, float maxWidth, int backgroundColor, CustomFontTextureCache.FontType fontType, SignTextStyleConfig.SignTextStyle style, boolean drawBackground, boolean centerInCell, Color forcedTextColor, float yOffset, float centerWidth, float cjkScale, boolean forceFullSizeLines) {
		drawCustomText(signText, deferred(storedMatrixTransformations), size, start, flipCustomText, maxWidth, backgroundColor, fontType, style, drawBackground, centerInCell, forcedTextColor, yOffset, centerWidth, cjkScale, forceFullSizeLines);
	}

	/** {@link #drawCustomText} 的 sink 版，世界渲染与 GUI 预览共用。 */
	public static void drawCustomText(String signText, QuadSink sink, float size, float start, boolean flipCustomText, float maxWidth, int backgroundColor, CustomFontTextureCache.FontType fontType, SignTextStyleConfig.SignTextStyle style, boolean drawBackground, boolean centerInCell, Color forcedTextColor, float yOffset, float centerWidth, float cjkScale, boolean forceFullSizeLines) {
		if (maxWidth <= 0) {
			return;
		}
		final int textBackgroundColor = backgroundColor | 0xFF000000;
		final Color textColor = forcedTextColor != null ? forcedTextColor : (style != null && style.hasColor() ? new Color(style.textColor) : (isLightBackground(textBackgroundColor) ? Color.BLACK : Color.WHITE));
		final float sizeMult = style != null ? style.textSize : 1.0F;
		final boolean bold = style != null && style.textBold;
		final List<CustomFontTextureCache.FittedTextTexture> fittedLines = new ArrayList<>();
		final List<Integer> linePaddings = new ArrayList<>();
		final List<Integer> lineRaises = new ArrayList<>();
		int totalGlyphHeight = 0;
		int maxPxWidth = 0;
		int maxPadding = 0;

		for (final String line : signText.split("\\|")) {
			if (!line.isEmpty()) {
				final boolean cjk = containsCJK(line);
				final int lineFontSize = !cjk && !forceFullSizeLines ? Math.round(FONT_SIZE_LATIN) : Math.round(FONT_SIZE_DEFAULT * cjkScale);
				// 字体也逐行按 Drawing 包的规则选：纯数字走 Source Sans 3，其余走调用方给的字体
				final CustomFontTextureCache.FontType lineFontType = bold ? CustomFontTextureCache.FontType.SOURCE_HAN : (FontRenderUtils.isNumeric(line) ? CustomFontTextureCache.FontType.SOURCE_SANS_3 : fontType);
				final CustomFontTextureCache.FittedTextTexture fitted = CustomFontTextureCache.instance.getFittedTextTexture(line, lineFontType, lineFontSize, textColor, bold);
				if (fitted.width <= 0 || fitted.height <= 0) {
					return;
				}
				final int padding = Math.max(2, Math.round(lineFontSize * 0.08F));
				fittedLines.add(fitted);
				linePaddings.add(padding);
				lineRaises.add(!cjk && !forceFullSizeLines ? Math.round(lineFontSize * LATIN_RAISE_RATIO) : 0);
				totalGlyphHeight += Math.max(1, fitted.height - padding * 2);
				maxPxWidth = Math.max(maxPxWidth, fitted.width);
				maxPadding = Math.max(maxPadding, padding);
			}
		}

		if (fittedLines.isEmpty()) {
			return;
		}
		final int lineGapPx = Math.round(LINE_GAP_RATIO);
		totalGlyphHeight += lineGapPx * (fittedLines.size() - 1);
		final float pixelToWorld = size / totalGlyphHeight;
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
		final float x1;
		if (centerInCell) {
			final float glyphWidth = Math.max(1, maxPxWidth - maxPadding * 2) * pixelToWorld * scaleX;
			x1 = start + (centerWidth > 0 ? (centerWidth - glyphWidth) / 2.0F : (size - glyphWidth) / 2.0F);
		} else {
			x1 = flipCustomText ? start - width : start;
		}

		final float firstRaise = lineRaises.isEmpty() ? 0 : lineRaises.get(0) * pixelToWorld * scaleY;
		final float y1 = yOffset + (size - height) / 2.0F + firstRaise / 2.0F;

		if (drawBackground) {
			final float backgroundX = centerInCell ? start : x1;
			final float backgroundY = centerInCell ? yOffset : y1;
			final float backgroundW = centerInCell ? (centerWidth > 0 ? centerWidth : size) : width;
			final float backgroundH = centerInCell ? size : height;
			rect(sink, backgroundX, backgroundY, backgroundW, backgroundH, textBackgroundColor, defaultLight());
		}

		float lineY = y1;
		for (int i = 0; i < fittedLines.size(); i++) {
			final CustomFontTextureCache.FittedTextTexture lineTexture = fittedLines.get(i);
			final int padding = linePaddings.get(i);
			final int raise = lineRaises.get(i);
			final float lineW = lineTexture.width * pixelToWorld * scaleX;
			final float lineH = lineTexture.height * pixelToWorld * scaleY;
			final float lineX;
			if (centerInCell) {
				final float lineGlyphWidth = Math.max(1, lineTexture.width - padding * 2) * pixelToWorld * scaleX;
				final float lineCenterX = start + (centerWidth > 0 ? (centerWidth - lineGlyphWidth) / 2.0F : (size - lineGlyphWidth) / 2.0F);
				lineX = lineCenterX - padding * pixelToWorld * scaleX;
			} else {
				lineX = flipCustomText ? start - lineW + padding * pixelToWorld * scaleX : x1 - padding * pixelToWorld * scaleX;
			}
			final float finalLineY = lineY - (padding + raise) * pixelToWorld * scaleY;
			final float glyphLineHeight = Math.max(1, lineTexture.height - padding * 2) * pixelToWorld * scaleY;
			sink.quad(lineTexture.identifier, lineX, finalLineY, lineW, lineH, 0, 0, 1, 1, -1, defaultLight(), true);
			lineY += glyphLineHeight + lineGapPx * pixelToWorld * scaleY;
		}
	}

	/** 把线路编号画进一个盒子（带背景块）。 */
	public static void drawNumberInBox(StoredMatrixTransformations storedMatrixTransformations, float boxX, float boxY, float boxW, float boxH, String number) {
		drawNumberInBox(deferred(storedMatrixTransformations), boxX, boxY, boxW, boxH, number);
	}

	/** 把线路编号画进一个盒子（sink 版）。 */
	public static void drawNumberInBox(QuadSink sink, float boxX, float boxY, float boxW, float boxH, String number) {
		if (number == null || number.isEmpty()) {
			return;
		}
		final CustomFontTextureCache.FontType fontType = com.Nanbin.client.Drawing.FontRenderUtils.isNumeric(number)
				? CustomFontTextureCache.FontType.SOURCE_SANS_3
				: CustomFontTextureCache.FontType.ALIBABA;
		final CustomFontTextureCache.FittedTextTexture fitted = CustomFontTextureCache.instance.getFittedTextTexture(number, fontType, Math.round(FONT_SIZE_DEFAULT), Color.BLACK);
		if (fitted.width <= 0 || fitted.height <= 0) {
			return;
		}
		final int padding = Math.max(1, Math.round(1.76F));
		final float glyphW = Math.max(1, fitted.width - padding * 2);
		final float glyphH = Math.max(1, fitted.height - padding * 2);
		final float boost = com.Nanbin.client.Drawing.FontRenderUtils.isNumeric(number) ? 1.35F : 1.0F;
		final float scale = Math.min(boxH / glyphH, boxW / glyphW) * boost;
		final float textW = fitted.width * scale;
		final float textH = fitted.height * scale;
		final float textX = boxX + (boxW - textW) / 2.0F;
		final float textY = boxY + (boxH - textH) / 2.0F;
		sink.quad(fitted.identifier, textX, textY, textW, textH, 0, 0, 1, 1, -1, defaultLight(), true);
	}

	/** 供需要直接拿 buffer 的自定义绘制使用。 */
	public static void runWith(PoseStackConsumer consumer, StoredMatrixTransformations storedMatrixTransformations, Identifier texture, boolean priority, QueuedRenderLayer queuedRenderLayer) {
		MainRenderer.scheduleRender(texture, priority, queuedRenderLayer, (matrixStack, vertexConsumer, offset) -> {
			storedMatrixTransformations.transform(matrixStack, offset);
			consumer.accept(matrixStack, vertexConsumer);
			matrixStack.pop();
		});
	}

	@FunctionalInterface
	public interface PoseStackConsumer {
		void accept(net.minecraft.client.util.math.MatrixStack matrixStack, VertexConsumer vertexConsumer);
	}

	/** 朝向对应的水平旋转角度（度），4.1 里 Direction 的方法名换过，走 DirectionHelper 的垫片。 */
	public static void rotateToFacing(net.minecraft.client.util.math.MatrixStack matrixStack, Direction facing) {
		Drawing.rotateYDegrees(matrixStack, -com.Nanbin.mapping.DirectionHelper.asRotation(facing));
	}
}
