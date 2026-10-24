package com.Nanbin.client.Drawing;

import org.mtr.mapping.holder.NativeImage;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.Nanbin.InitClient.LOGGER;


public final class CRTStationInfoBandRenderer {

	private CRTStationInfoBandRenderer() {
	}

	public static NativeImage render(String stationName, String prevStationName, String nextStationName, int lineColor, boolean flip, float bandWidth, float bandHeight, Font rawFont, int fontSize, float latinFontRatio, float gapRatio, int textColor, int rectColor, float rectLeft, float rectLength, float rectHeight, float rightGap, float margin, int prevFontSize, float prevMaxWidth, float prevMaxHeight) {
		try {
			final int resolution = org.mtr.mod.config.Config.getClient().getDynamicTextureResolution();
			final float scale = Math.min((float) Math.pow(2, resolution + 5), 1024.0F);
			final float resolutionScale = scale / 60.0F;
			final float pxPerBlock = 120.0F * resolutionScale;
			final int W = Math.max(1, Math.round(bandWidth * pxPerBlock));
			final int H = Math.max(1, Math.round(bandHeight * pxPerBlock));

			final BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
			final Graphics2D g = img.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

			final Color textAwt = new Color(textColor, true);
			final Color rectAwt = new Color(rectColor, true);
			final Color lineAwt = new Color(lineColor | 0xFF000000, true);

			// 站名
			final float zoneLeft = margin * pxPerBlock;
			final float zoneTop = margin * pxPerBlock;
			final float zoneW = W - margin * pxPerBlock * 2;
			final float zoneH = H - margin * pxPerBlock * 2;
			final TextBlock mainText = buildTextBlock(stationName, rawFont, fontSize, latinFontRatio, gapRatio, textAwt, resolutionScale, Math.round(zoneW), false, false);
			final float mainScale = mainText.height > 0 ? Math.min(1.0F, Math.min(zoneW / mainText.width, zoneH / mainText.height)) : 1.0F;
			final int mainDrawW = Math.max(1, Math.round(mainText.width * mainScale));
			final int mainDrawH = Math.max(1, Math.round(mainText.height * mainScale));
			final int mainLeft = Math.round(zoneLeft + (zoneW - mainDrawW) / 2.0F);
			final int mainTop = Math.round(zoneTop + (zoneH - mainDrawH) / 2.0F);
			drawTextBlock(g, mainText, mainLeft, mainTop, mainScale, mainDrawW);

			// 指示组件
			final int rectW = Math.max(1, Math.round(rectLength * pxPerBlock));
			final int rectH = Math.max(1, Math.round(rectHeight * pxPerBlock));
			final int rectTop = (H - rectH) / 2;
			final int triW = Math.max(1, Math.round(rectHeight * pxPerBlock));
			final int cy = H / 2;
			final int mainRight = mainLeft + mainDrawW;
			final int pad = Math.max(1, Math.round(0.04F * pxPerBlock));

			final int greyW = rectW;
			final int rectX0 = Math.max(0, Math.round(rectLeft * pxPerBlock));
			int greyX;
			if (!flip) {
				greyX = rectX0;
				if (mainRight > greyX && mainLeft < greyX + greyW) {
					greyX = Math.min(mainLeft + mainDrawW + pad, W - greyW);
				}
			} else {
				greyX = (W - rectX0) - greyW; // 镜像：距右缘 rectLeft
				if (mainRight > greyX && mainLeft < greyX + greyW) {
					greyX = Math.max(mainLeft - pad - greyW, 0);
				}
			}
			g.setColor(rectAwt);
			g.fillRect(greyX, rectTop, greyW, rectH);

			// 线路色组合图
			final int lineRectW = Math.max(1, rectW - triW);
			int lineRectX;
			if (!flip) {
				final int bandRight = W - Math.max(0, Math.round(rightGap * pxPerBlock));
				lineRectX = bandRight - triW - lineRectW;
			} else {
				final int bandLeft = Math.max(0, Math.round(rightGap * pxPerBlock)); // 镜像：三角顶点距左缘 rightGap
				lineRectX = bandLeft + triW;
			}
			g.setColor(lineAwt);
			g.fillRect(lineRectX, rectTop, lineRectW, rectH);
			if (nextStationName != null && !nextStationName.isEmpty()) {
				final Polygon triangle = new Polygon();
				if (!flip) {
					final int rightRectRight = lineRectX + lineRectW;
					triangle.addPoint(rightRectRight + triW, cy);                    // 顶点（向右）
					triangle.addPoint(rightRectRight, cy - triW / 2);                // 底边右上
					triangle.addPoint(rightRectRight, cy + triW / 2);                // 底边右下
				} else {
					triangle.addPoint(lineRectX - triW, cy);                         // 顶点（向左）
					triangle.addPoint(lineRectX, cy - triW / 2);                     // 底边左上
					triangle.addPoint(lineRectX, cy + triW / 2);                     // 底边左下
				}
				g.fillPolygon(triangle);
			}

			// 上一站名
			if (prevStationName != null && !prevStationName.isEmpty()) {
				final float prevZoneW = Math.max(1, prevMaxWidth * pxPerBlock);
				final float prevZoneH = Math.max(1, prevMaxHeight * pxPerBlock);
				final TextBlock prevText = buildTextBlock(prevStationName, rawFont, prevFontSize, latinFontRatio, 0F, rectAwt, resolutionScale, Math.round(prevZoneW), !flip, flip);
				final float prevScale = prevText.height > 0 ? Math.min(1.0F, Math.min(prevZoneW / prevText.width, prevZoneH / prevText.height)) : 1.0F;
				final int prevX = flip ? greyX + greyW : greyX;
				final int prevY = rectTop + rectH;
				drawTextBlock(g, prevText, prevX, prevY, prevScale, prevZoneW);
			}

			// 下一站名
			if (nextStationName != null && !nextStationName.isEmpty()) {
				final float nextZoneW = Math.max(1, prevMaxWidth * pxPerBlock);
				final float nextZoneH = Math.max(1, prevMaxHeight * pxPerBlock);
				final TextBlock nextText = buildTextBlock(nextStationName, rawFont, prevFontSize, latinFontRatio, 0F, lineAwt, resolutionScale, Math.round(nextZoneW), flip, !flip);
				final float nextScale = nextText.height > 0 ? Math.min(1.0F, Math.min(nextZoneW / nextText.width, nextZoneH / nextText.height)) : 1.0F;
				final int nextX = flip ? lineRectX : lineRectX + lineRectW; // 对齐锚点：矩形外缘（三角底边）
				final int nextY = rectTop + rectH;
				drawTextBlock(g, nextText, nextX, nextY, nextScale, nextZoneW);
			}

			g.dispose();
			return FontRenderUtils.toNativeImage(img);
		} catch (Exception e) {
			LOGGER.error("Failed to render StationInfo1 band for \"{}\" / \"{}\"", stationName, prevStationName, e);
			return null;
		}
	}

	static void drawTextBlock(Graphics2D g, TextBlock block, int x, int y, float scale, float zoneW) {
		if (!block.hasCjk && block.latinLines.isEmpty()) {
			return;
		}
		float cursorY = y;
		if (block.hasCjk) {
			final float lineW = block.cjkW * scale;
			final float sx = block.leftAlign ? x : (block.rightAlign ? x - lineW : x + (zoneW - lineW) / 2.0F);
			g.translate(sx, 0);
			g.scale(scale, scale);
			g.setFont(block.cjkFont);
			g.setColor(block.color);
			g.drawString(block.cjk, 0, cursorY / scale + FontRenderUtils.getAscent(block.cjkFont));
			g.scale(1.0F / scale, 1.0F / scale);
			g.translate(-sx, 0);
			cursorY += (FontRenderUtils.getAscent(block.cjkFont) + FontRenderUtils.getDescent(block.cjkFont)) * scale + block.gap * scale;
		}
		for (final String line : block.latinLines) {
			final float lineW = FontRenderUtils.getStringWidth(line, block.latinFont) * scale;
			final float sx = block.leftAlign ? x : (block.rightAlign ? x - lineW : x + (zoneW - lineW) / 2.0F);
			g.translate(sx, 0);
			g.scale(scale, scale);
			g.setFont(block.latinFont);
			g.setColor(block.color);
			g.drawString(line, 0, cursorY / scale + FontRenderUtils.getAscent(block.latinFont));
			g.scale(1.0F / scale, 1.0F / scale);
			g.translate(-sx, 0);
			cursorY += (FontRenderUtils.getAscent(block.latinFont) + FontRenderUtils.getDescent(block.latinFont)) * scale;
		}
	}

	static TextBlock buildTextBlock(String text, Font rawFont, int fontSize, float latinFontRatio, float gapRatio, Color color, float resolutionScale, int wrapMaxWidthPx, boolean leftAlign, boolean rightAlign) {
		final String[] segments = text.split("\\|", -1);
		final StringBuilder cjkBuilder = new StringBuilder();
		final StringBuilder latinBuilder = new StringBuilder();
		for (final String seg : segments) {
			if (seg.isEmpty()) {
				continue;
			}
			if (FontRenderUtils.containsCjk(seg)) {
				cjkBuilder.append(seg);
			} else {
				if (latinBuilder.length() > 0) {
					latinBuilder.append(' ');
				}
				latinBuilder.append(seg);
			}
		}
		final String cjk = cjkBuilder.toString();
		final String latin = latinBuilder.toString();
		final boolean hasCjk = cjk.length() > 0;
		final boolean hasLatin = latin.length() > 0;
		if (!hasCjk && !hasLatin) {
			return new TextBlock("", cjkFont(rawFont, fontSize, resolutionScale), latinFont(rawFont, fontSize, latinFontRatio, resolutionScale), Collections.emptyList(), color, 0, 0, 0, 0, false, leftAlign, rightAlign);
		}

		final Font cjkF = cjkFont(rawFont, fontSize, resolutionScale);
		final Font latinF = latinFont(rawFont, fontSize, latinFontRatio, resolutionScale);
		final List<String> latinLines = hasLatin ? wrapLatin(latin, latinF, wrapMaxWidthPx) : Collections.emptyList();

		final int cjkW = hasCjk ? FontRenderUtils.getStringWidth(cjk, cjkF) : 0;
		int latinMaxW = 0;
		for (final String line : latinLines) {
			latinMaxW = Math.max(latinMaxW, FontRenderUtils.getStringWidth(line, latinF));
		}
		final int gap = (hasCjk && hasLatin) ? Math.max(0, Math.round(fontSize * gapRatio * resolutionScale)) : 0;
		final int width = Math.max(cjkW, latinMaxW);
		final int height = (hasCjk ? FontRenderUtils.getFontHeight(cjkF) : 0) + gap + (hasLatin ? FontRenderUtils.getFontHeight(latinF) * latinLines.size() : 0);
		return new TextBlock(cjk, cjkF, latinF, latinLines, color, cjkW, width, height, gap, hasCjk, leftAlign, rightAlign);
	}

	private static Font cjkFont(Font rawFont, int fontSize, float resolutionScale) {
		return rawFont.deriveFont(Font.PLAIN, Math.max(1, Math.round(fontSize * resolutionScale)));
	}

	private static Font latinFont(Font rawFont, int fontSize, float latinFontRatio, float resolutionScale) {
		return rawFont.deriveFont(Font.PLAIN, Math.max(1, Math.round(fontSize * latinFontRatio * resolutionScale)));
	}

	/** 按单词贪心换行：单词放不下就另起一行；单个单词本身就超宽时独占一行。 */
	private static List<String> wrapLatin(String text, Font font, int maxWidth) {
		final List<String> lines = new ArrayList<>();
		if (text == null || text.trim().isEmpty()) {
			return lines;
		}
		final String[] words = text.trim().split("\\s+");
		final int limit = Math.max(1, maxWidth);
		StringBuilder current = new StringBuilder();
		for (final String word : words) {
			final String candidate = current.length() == 0 ? word : current + " " + word;
			if (current.length() > 0 && FontRenderUtils.getStringWidth(candidate, font) > limit) {
				lines.add(current.toString());
				current = new StringBuilder(word);
			} else {
				current = new StringBuilder(candidate);
			}
		}
		if (current.length() > 0) {
			lines.add(current.toString());
		}
		return lines;
	}

	static final class TextBlock {
		final String cjk;
		final Font cjkFont;
		final Font latinFont;
		final List<String> latinLines;
		final Color color;
		final int cjkW;
		final int width;
		final int height;
		final int gap;
		final boolean hasCjk;
		final boolean leftAlign;
		final boolean rightAlign;

		TextBlock(String cjk, Font cjkFont, Font latinFont, List<String> latinLines, Color color, int cjkW, int width, int height, int gap, boolean hasCjk, boolean leftAlign, boolean rightAlign) {
			this.cjk = cjk;
			this.cjkFont = cjkFont;
			this.latinFont = latinFont;
			this.latinLines = latinLines;
			this.color = color;
			this.cjkW = cjkW;
			this.width = width;
			this.height = height;
			this.gap = gap;
			this.hasCjk = hasCjk;
			this.leftAlign = leftAlign;
			this.rightAlign = rightAlign;
		}
	}
}
