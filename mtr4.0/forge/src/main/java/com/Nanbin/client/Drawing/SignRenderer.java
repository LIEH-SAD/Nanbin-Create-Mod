package com.Nanbin.client.Drawing;

import com.Nanbin.client.RouteMap.RoundMapDrawing;
import org.mtr.mapping.holder.NativeImage;

import java.awt.*;
import java.awt.image.BufferedImage;

import static com.Nanbin.InitClient.LOGGER;

/**
 * 站名牌纹理的通用绘制方法。仅包含绘制算法，不内置任何数值；
 * 所有布局/字号/圆参数均由调用方（各 Render 类）传入。
 */
public final class SignRenderer {

	private SignRenderer() {
	}

	/**
	 * @param stationName      already-extracted visible text (no pipe rules), "|" splits CJK/Latin segments
	 * @param stationColor     ARGB color, -1 to use white
	 * @param routeColor       ARGB color, -1 to use station color
	 * @param aspect           texture width as a ratio of {@link FontRenderUtils#BASE_TEXTURE_HEIGHT}
	 * @param rawFont          由字体缓存加载好的字体（站名用）
	 * @param fontSize         站名 CJK 行字号
	 * @param circleRawFont    由字体缓存加载好的圆内文字字体（可为 null，则回退到 rawFont）
	 * @param topBarEnd        顶条底缘（相对 H），0 表示不画
	 * @param textEnd          文字区底缘（相对 H）
	 * @param middleBarStart   中间条上缘（相对 H），middleBarEnd &gt; 0 时启用
	 * @param middleBarEnd     中间条下缘（相对 H）
	 * @param middleBarWidth   中间条宽度（相对 H），&lt;=0 表示整宽
	 * @param bottomBarStart   底条上缘（相对 H），&gt;=1 表示不画
	 * @param latinFontRatio   拉丁行字号 = fontSize * ratio
	 * @param gapRatio         中/拉两行间距 = fontSize * ratio
	 * @param verticalBias     文字块在文字区内的垂直偏置（0~1，越大越靠下）
	 * @param extraOffsetRatio 文字块额外下移 = H * ratio
	 * @param textMarginDefault 文字较长时的水平留边（比例）
	 * @param textMarginMedium  文字中等时的水平留边（比例）
	 * @param textMarginShort   文字很短时的水平留边（比例）
	 * @param circleCenterYOffset 圆心相对基准位置的下移量 = H * ratio（基准：中间条中心；无中间条时为底条上缘）
	 * @param circleRadiusHRatio  圆半径上限 = H * ratio
	 * @param circleRadiusWRatio  圆半径上限 = W * ratio
	 * @param strokeHRatio        描边宽度 = max(H * ratio, 2)
	 * @param circleFontRatio     圆内文字字号 = fontSize * ratio
	 * @param invertedCircle      true 时使用反色圆
	 */
	public static NativeImage render(String stationName, int stationColor, int routeColor, String routeNumber, String platformNumber, float aspect, Font rawFont, int fontSize, Font circleRawFont, float topBarEnd, float textEnd, float middleBarStart, float middleBarEnd, float middleBarWidth, float bottomBarStart, float latinFontRatio, float gapRatio, float verticalBias, float extraOffsetRatio, float textMarginDefault, float textMarginMedium, float textMarginShort, float circleCenterYOffset, float circleRadiusHRatio, float circleRadiusWRatio, float strokeHRatio, float circleFontRatio, boolean invertedCircle, boolean whiteBackground) {
		try {
			// Scale texture dimensions by resolution
			final float resolutionScale = FontRenderUtils.getResolutionScale();
			final int H = FontRenderUtils.getTextureHeight();
			final int W = Math.round(H * aspect);
			if (W <= 0) return null;

			final BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
			final Graphics2D g = img.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

			// Theme color: use station color for bars and circle border
			final Color themeAwtColor = stationColor == -1 ? Color.WHITE : new Color(stationColor | 0xFF000000, true);
			final Color routeAwtColor = routeColor == -1 ? themeAwtColor : new Color(routeColor | 0xFF000000, true);
			final Color textColor = Color.BLACK;

			// ---- 0. Background (white by default, generic) ----
			if (whiteBackground) {
				g.setColor(Color.WHITE);
				g.fillRect(0, 0, W, H);
			}

			// ---- 1. Top bar (station theme color) ----
			if (topBarEnd > 0) {
				g.setColor(themeAwtColor);
				g.fillRect(0, 0, W, Math.round(H * topBarEnd));
			}

			// ---- 2. Station name ----
			// Split into CJK / Latin lines
			final String[] segments = stationName.split("\\|", -1);
			final StringBuilder cjkLine = new StringBuilder();
			final StringBuilder latinLine = new StringBuilder();
			for (final String seg : segments) {
				if (seg.isEmpty()) continue;
				if (FontRenderUtils.containsCjk(seg)) {
					cjkLine.append(seg);
				} else {
					latinLine.append(seg);
				}
			}
			final boolean hasCjk = cjkLine.length() > 0;
			final boolean hasLatin = latinLine.length() > 0;

			if (hasCjk || hasLatin) {
				// Scale font sizes proportionally with texture resolution
				final int scaledFontSize = Math.round(fontSize * resolutionScale);
				final int scaledLatinFontSize = Math.max(Math.round(fontSize * latinFontRatio * resolutionScale), Math.round(8 * resolutionScale));
				final Font cjkFont = rawFont.deriveFont(Font.PLAIN, (float) scaledFontSize);
				final Font latinFont = rawFont.deriveFont(Font.PLAIN, (float) scaledLatinFontSize);

				final int cjkW = hasCjk ? FontRenderUtils.getStringWidth(cjkLine.toString(), cjkFont) : 0;
				final int latinW = hasLatin ? FontRenderUtils.getStringWidth(latinLine.toString(), latinFont) : 0;
				final int cjkH = hasCjk ? FontRenderUtils.getFontHeight(cjkFont) : 0;
				final int latinH = hasLatin ? FontRenderUtils.getFontHeight(latinFont) : 0;
				final int gap = (hasCjk && hasLatin) ? Math.round(fontSize * gapRatio * resolutionScale) : 0;

				final int maxLineW = Math.max(cjkW, latinW);
				final int totalTextH = (hasCjk ? cjkH : 0) + gap + (hasLatin ? latinH : 0);

				// Uniform scale if exceeds text zone
				final int textZoneH = Math.round(H * (textEnd - topBarEnd));
				// When text is short, add more horizontal padding so it doesn't stretch
				float horizontalMargin = textMarginDefault;
				if (maxLineW > 0 && maxLineW < W * 0.3F) {
					horizontalMargin = textMarginShort;
				} else if (maxLineW > 0 && maxLineW < W * 0.5F) {
					horizontalMargin = textMarginMedium;
				}
				final int textZoneW = W - Math.round(W * horizontalMargin);
				final float scale = Math.min(1.0F, Math.min(
						(float) textZoneW / (float) maxLineW,
						(float) textZoneH / (float) totalTextH
				));

				// Center text block vertically in the text zone, biased slightly downward
				final int textZoneTop = Math.round(H * topBarEnd);
				final float scaledTextH = totalTextH * scale;
				final float baseY = textZoneTop + (textZoneH - scaledTextH) * verticalBias + H * extraOffsetRatio;

				float cursorY = baseY;
				if (hasCjk) {
					final float sx = (W - cjkW * scale) / 2.0F;
					g.translate(sx, 0);
					g.scale(scale, scale);
					g.setFont(cjkFont);
					g.setColor(textColor);
					g.drawString(cjkLine.toString(), 0, cursorY + FontRenderUtils.getAscent(cjkFont));
					g.scale(1.0 / scale, 1.0 / scale);
					g.translate(-sx, 0);
					cursorY += (FontRenderUtils.getAscent(cjkFont) + FontRenderUtils.getDescent(cjkFont)) * scale + gap * scale;
				}
				if (hasLatin) {
					final float sx = (W - latinW * scale) / 2.0F;
					g.translate(sx, 0);
					g.scale(scale, scale);
					g.setFont(latinFont);
					g.setColor(textColor);
					g.drawString(latinLine.toString(), 0, cursorY / scale + FontRenderUtils.getAscent(latinFont));
					g.scale(1.0 / scale, 1.0 / scale);
					g.translate(-sx, 0);
				}
			}

			// ---- 3. Middle bar (station theme color, only when enabled) ----
			if (middleBarEnd > 0) {
				g.setColor(themeAwtColor);
				final int middleStart = Math.round(H * middleBarStart);
				final int middleHeight = Math.round(H * middleBarEnd) - middleStart;
				final int barWidth = middleBarWidth > 0 ? Math.round(H * middleBarWidth) : W;
				g.fillRect((W - barWidth) / 2, middleStart, barWidth, middleHeight);
			}

			// ---- 4. Bottom bar (route color) ----
			if (bottomBarStart < 1) {
				g.setColor(themeAwtColor);
				final int bottomStart = Math.round(H * bottomBarStart);
				g.fillRect(0, bottomStart, W, H - bottomStart);
			}

			// ---- 5. Circle icon with route/platform numbers ----
			// middleBar layout always draws circle; other layouts only when route/platform data exists
			if (middleBarEnd > 0 || !routeNumber.isEmpty() || !platformNumber.isEmpty()) {
				// Circle center based on middle bar center (or bottom bar top), then offset by circleCenterYOffset
				final float circleCenterYRatio = (middleBarEnd > 0 ? (middleBarStart + middleBarEnd) / 2F : bottomBarStart) + circleCenterYOffset;
				final int circleCenterY = Math.round(H * circleCenterYRatio);
				final int circleRadius = Math.min(Math.round(H * circleRadiusHRatio), Math.round(W * circleRadiusWRatio));
				final int circleCenterX = W / 2;
				final int strokeWidth = Math.max(Math.round(H * strokeHRatio), 2);
				final int scaledCircleFontSize = Math.max(Math.round(fontSize * circleFontRatio * resolutionScale), Math.round(12 * resolutionScale));
				final Font circleFont = (circleRawFont != null ? circleRawFont : rawFont).deriveFont(Font.PLAIN, (float) scaledCircleFontSize);
				// 圆内文字：纯数字用 SOURCE_SANS_3（circleFont），非数字用主字体（rawFont 同字号）
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