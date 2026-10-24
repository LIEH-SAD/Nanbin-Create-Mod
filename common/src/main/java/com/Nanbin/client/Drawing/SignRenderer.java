package com.Nanbin.client.Drawing;

import com.Nanbin.client.RouteMap.RoundMapDrawing;
import net.minecraft.client.texture.NativeImage;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import static com.Nanbin.InitClient.LOGGER;

/**
 * 站名牌纹理的通用绘制方法。仅包含绘制算法，不内置任何数值；
 * 所有布局 / 字号 / 圆参数均由调用方（各 Render 类）传入。
 */
public final class SignRenderer {

	private SignRenderer() {
	}

	/**
	 * @param stationName       已按管道规则提取过的可见文字（无 "|" 规则），"|" 分隔 CJK / 拉丁片段
	 * @param stationColor      ARGB 颜色，-1 表示用白色
	 * @param routeColor        ARGB 颜色，-1 表示用站台色
	 * @param aspect            纹理宽度，作为 {@link FontRenderUtils#BASE_TEXTURE_HEIGHT} 的比例
	 * @param fontSize          站名 CJK 行字号
	 * @param circleRawFont     圆内文字字体（可为 null，则回退到 rawFont）
	 * @param topBarEnd         顶条底缘（相对 H），0 表示不画
	 * @param textEnd           文字区底缘（相对 H）
	 * @param middleBarStart    中间条上缘（相对 H），middleBarEnd &gt; 0 时启用
	 * @param middleBarEnd      中间条下缘（相对 H）
	 * @param middleBarWidth    中间条宽度（相对 H），&lt;=0 表示整宽
	 * @param bottomBarStart    底条上缘（相对 H），&gt;=1 表示不画
	 * @param latinFontRatio    拉丁行字号 = fontSize × ratio
	 * @param gapRatio          中/拉两行间距 = fontSize × ratio
	 * @param verticalBias      文字块在文字区内的垂直偏置（0~1，越大越靠下）
	 * @param extraOffsetRatio  文字块额外下移 = H × ratio
	 * @param circleCenterYOffset 圆心相对基准位置的下移量 = H × ratio（基准：中间条中心；无中间条时为底条上缘）
	 * @param circleRadiusHRatio  圆半径上限 = H × ratio
	 * @param circleRadiusWRatio  圆半径上限 = W × ratio
	 * @param strokeHRatio        描边宽度 = max(H × ratio, 2)
	 * @param circleFontRatio     圆内文字字号 = fontSize × ratio
	 * @param invertedCircle      true 时使用反色圆
	 */
	public static NativeImage render(String stationName, int stationColor, int routeColor, String routeNumber, String platformNumber, float aspect, Font rawFont, int fontSize, Font circleRawFont, float topBarEnd, float textEnd, float middleBarStart, float middleBarEnd, float middleBarWidth, float bottomBarStart, float latinFontRatio, float gapRatio, float verticalBias, float extraOffsetRatio, float textMarginDefault, float textMarginMedium, float textMarginShort, float circleCenterYOffset, float circleRadiusHRatio, float circleRadiusWRatio, float strokeHRatio, float circleFontRatio, boolean invertedCircle, boolean whiteBackground) {
		try {
			final float resolutionScale = FontRenderUtils.getResolutionScale();
			final int height = FontRenderUtils.getTextureHeight();
			final int width = Math.round(height * aspect);
			if (width <= 0) {
				return null;
			}

			final BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			final Graphics2D g = img.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

			// 主题色：条带与圆用站台色
			final Color themeAwtColor = stationColor == -1 ? Color.WHITE : new Color(stationColor | 0xFF000000, true);
			final Color routeAwtColor = routeColor == -1 ? themeAwtColor : new Color(routeColor | 0xFF000000, true);
			final Color textColor = Color.BLACK;

			// ---- 0. 背景（默认白底） ----
			if (whiteBackground) {
				g.setColor(Color.WHITE);
				g.fillRect(0, 0, width, height);
			}

			// ---- 1. 顶条（站台主题色） ----
			if (topBarEnd > 0) {
				g.setColor(themeAwtColor);
				g.fillRect(0, 0, width, Math.round(height * topBarEnd));
			}

			// ---- 2. 站名 ----
			final String[] segments = stationName.split("\\|", -1);
			final StringBuilder cjkLine = new StringBuilder();
			final StringBuilder latinLine = new StringBuilder();
			for (final String segment : segments) {
				if (segment.isEmpty()) {
					continue;
				}
				if (FontRenderUtils.containsCjk(segment)) {
					cjkLine.append(segment);
				} else {
					latinLine.append(segment);
				}
			}
			final boolean hasCjk = cjkLine.length() > 0;
			final boolean hasLatin = latinLine.length() > 0;

			if (hasCjk || hasLatin) {
				final int scaledFontSize = Math.round(fontSize * resolutionScale);
				final int scaledLatinFontSize = Math.max(Math.round(fontSize * latinFontRatio * resolutionScale), Math.round(8 * resolutionScale));
				final Font cjkFont = rawFont.deriveFont(Font.PLAIN, (float) scaledFontSize);
				final Font latinFont = rawFont.deriveFont(Font.PLAIN, (float) scaledLatinFontSize);

				final int cjkWidth = hasCjk ? FontRenderUtils.getStringWidth(cjkLine.toString(), cjkFont) : 0;
				final int latinWidth = hasLatin ? FontRenderUtils.getStringWidth(latinLine.toString(), latinFont) : 0;
				final int cjkHeight = hasCjk ? FontRenderUtils.getFontHeight(cjkFont) : 0;
				final int latinHeight = hasLatin ? FontRenderUtils.getFontHeight(latinFont) : 0;
				final int gap = hasCjk && hasLatin ? Math.round(fontSize * gapRatio * resolutionScale) : 0;

				final int maxLineWidth = Math.max(cjkWidth, latinWidth);
				final int totalTextHeight = (hasCjk ? cjkHeight : 0) + gap + (hasLatin ? latinHeight : 0);

				// 超出文字区时整体等比缩放
				final int textZoneHeight = Math.round(height * (textEnd - topBarEnd));
				// 文字较短时加大水平留边，避免被拉伸
				float horizontalMargin = textMarginDefault;
				if (maxLineWidth > 0 && maxLineWidth < width * 0.3F) {
					horizontalMargin = textMarginShort;
				} else if (maxLineWidth > 0 && maxLineWidth < width * 0.5F) {
					horizontalMargin = textMarginMedium;
				}
				final int textZoneWidth = width - Math.round(width * horizontalMargin);
				final float scale = Math.min(1.0F, Math.min(
						(float) textZoneWidth / (float) maxLineWidth,
						(float) textZoneHeight / (float) totalTextHeight));

				final int textZoneTop = Math.round(height * topBarEnd);
				final float scaledTextHeight = totalTextHeight * scale;
				float cursorY = textZoneTop + (textZoneHeight - scaledTextHeight) * verticalBias + height * extraOffsetRatio;

				if (hasCjk) {
					final float offsetX = (width - cjkWidth * scale) / 2.0F;
					g.translate(offsetX, 0);
					g.scale(scale, scale);
					g.setFont(cjkFont);
					g.setColor(textColor);
					g.drawString(cjkLine.toString(), 0, cursorY + FontRenderUtils.getAscent(cjkFont));
					g.scale(1.0 / scale, 1.0 / scale);
					g.translate(-offsetX, 0);
					cursorY += (FontRenderUtils.getAscent(cjkFont) + FontRenderUtils.getDescent(cjkFont)) * scale + gap * scale;
				}
				if (hasLatin) {
					final float offsetX = (width - latinWidth * scale) / 2.0F;
					g.translate(offsetX, 0);
					g.scale(scale, scale);
					g.setFont(latinFont);
					g.setColor(textColor);
					g.drawString(latinLine.toString(), 0, cursorY / scale + FontRenderUtils.getAscent(latinFont));
					g.scale(1.0 / scale, 1.0 / scale);
					g.translate(-offsetX, 0);
				}
			}

			// ---- 3. 中间条（站台主题色，启用时才有） ----
			if (middleBarEnd > 0) {
				g.setColor(themeAwtColor);
				final int middleStart = Math.round(height * middleBarStart);
				final int middleHeight = Math.round(height * middleBarEnd) - middleStart;
				final int barWidth = middleBarWidth > 0 ? Math.round(height * middleBarWidth) : width;
				g.fillRect((width - barWidth) / 2, middleStart, barWidth, middleHeight);
			}

			// ---- 4. 底条（线路色） ----
			if (bottomBarStart < 1) {
				g.setColor(themeAwtColor);
				final int bottomStart = Math.round(height * bottomBarStart);
				g.fillRect(0, bottomStart, width, height - bottomStart);
			}

			// ---- 5. 圆形图标（线路编号 / 站台号） ----
			if (middleBarEnd > 0 || !routeNumber.isEmpty() || !platformNumber.isEmpty()) {
				final float circleCenterYRatio = (middleBarEnd > 0 ? (middleBarStart + middleBarEnd) / 2F : bottomBarStart) + circleCenterYOffset;
				final int circleCenterY = Math.round(height * circleCenterYRatio);
				final int circleRadius = Math.min(Math.round(height * circleRadiusHRatio), Math.round(width * circleRadiusWRatio));
				final int circleCenterX = width / 2;
				final int strokeWidth = Math.max(Math.round(height * strokeHRatio), 2);
				final int scaledCircleFontSize = Math.max(Math.round(fontSize * circleFontRatio * resolutionScale), Math.round(12 * resolutionScale));
				final Font circleFont = (circleRawFont != null ? circleRawFont : rawFont).deriveFont(Font.PLAIN, (float) scaledCircleFontSize);
				// 圆内文字：纯数字用 SOURCE_SANS_3（circleFont），非数字用主字体
				final Font circleTextFont = rawFont.deriveFont(Font.PLAIN, (float) scaledCircleFontSize);

				if (invertedCircle) {
					RoundMapDrawing.drawInverted(g, circleCenterX, circleCenterY, circleRadius, strokeWidth, themeAwtColor, routeAwtColor, circleFont, circleTextFont, routeNumber, platformNumber);
				} else {
					RoundMapDrawing.draw(g, circleCenterX, circleCenterY, circleRadius, strokeWidth, themeAwtColor, routeAwtColor, circleFont, circleTextFont, routeNumber, platformNumber);
				}
			}

			g.dispose();
			return FontRenderUtils.toNativeImage(img);
		} catch (Exception e) {
			LOGGER.error("Failed to render sign for \"{}\"", stationName, e);
			return null;
		}
	}
}
