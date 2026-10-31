package com.Nanbin.client.Drawing;

import org.mtr.mapping.holder.NativeImage;

import java.awt.*;
import java.awt.image.BufferedImage;

import static com.Nanbin.InitClient.LOGGER;

public final class CRTSignRenderer {

	private CRTSignRenderer() {
	}

	public static NativeImage render(String stationName, int stationColor, int routeColor, String routeNumber, String platformNumber, float aspect, Font rawFont, int fontSize, Font circleRawFont, float topBarEnd, float textEnd, float middleBarStart, float middleBarEnd, float middleBarWidth, float bottomBarStart, float latinFontRatio, float gapRatio, float verticalBias, float extraOffsetRatio, float textMarginDefault, float textMarginMedium, float textMarginShort, float circleCenterYOffset, float circleRadiusHRatio, float circleRadiusWRatio, float strokeHRatio, float circleFontRatio, boolean invertedCircle, boolean whiteBackground) {
		try {
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

			if (whiteBackground) {
				g.setColor(Color.WHITE);
				g.fillRect(0, 0, W, H);
			}

			if (topBarEnd > 0) {
				g.setColor(themeAwtColor);
				g.fillRect(0, 0, W, Math.round(H * topBarEnd));
			}

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

				// Shrink uniformly if it overflows the text zone
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

			if (middleBarEnd > 0) {
				g.setColor(themeAwtColor);
				final int middleStart = Math.round(H * middleBarStart);
				final int middleHeight = Math.round(H * middleBarEnd) - middleStart;
				final int barWidth = middleBarWidth > 0 ? Math.round(H * middleBarWidth) : W;
				g.fillRect((W - barWidth) / 2, middleStart, barWidth, middleHeight);
			}

			if (bottomBarStart < 1) {
				g.setColor(themeAwtColor);
				final int bottomStart = Math.round(H * bottomBarStart);
				g.fillRect(0, bottomStart, W, H - bottomStart);
			}

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