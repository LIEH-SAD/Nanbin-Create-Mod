package com.Nanbin.client.Drawing;

import org.mtr.mapping.holder.NativeImage;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.Nanbin.InitClient.LOGGER;

/**
 * StationInfo1 顶部 0.5F 条带的整条纹理绘制方法：灰色矩形 + 本站名 + 线路上一站名。
 * 所有元素合成到一张纹理里，渲染器只需绘制一次。
 * 仅包含绘制算法，不内置任何数值；所有布局/字号/颜色参数均由调用方传入。
 */
public final class StationInfoBandRenderer {

	private StationInfoBandRenderer() {
	}

	/**
	 * @param stationName       本站名（已提取可见文本，| 分隔中英段）
	 * @param prevStationName   线路上一站名（可为空，则不画文字只画矩形）
	 * @param nextStationName   线路下一站名（可为空，则不画右侧三角与文字只画矩形）
	 * @param lineColor         线路颜色（RGB，无 alpha；线路色矩形/三角/下一站文字使用）
	 * @param flip              true = 镜像：线路色组合图在左（三角顶点向左）、灰色矩形在右
	 * @param bandWidth         条带宽度（块）
	 * @param bandHeight        条带高度（块）
	 * @param rawFont           站名字体
	 * @param fontSize          本站名 CJK 字号
	 * @param latinFontRatio    拉丁字号 = CJK 字号 × ratio
	 * @param gapRatio          本站名中/拉两行间距 = fontSize × ratio
	 * @param textColor         本站名文字颜色
	 * @param rectColor         灰色矩形与上一站文字颜色
	 * @param rectLeft          矩形左缘距条带左缘（块）
	 * @param rectLength        矩形长度（块）
	 * @param rectHeight        矩形高度（块）
	 * @param rightGap          线路色组合图距条带外侧边缘（块）
	 * @param margin            本站名与条带边缘最小留边（块）
	 * @param prevFontSize      上一站/下一站 CJK 字号
	 * @param prevMaxWidth      上一站/下一站文字最大宽度（块）
	 * @param prevMaxHeight     上一站/下一站文字最大高度（块）
	 */
	public static NativeImage render(String stationName, String prevStationName, String nextStationName, int lineColor, boolean flip, float bandWidth, float bandHeight, Font rawFont, int fontSize, float latinFontRatio, float gapRatio, int textColor, int rectColor, float rectLeft, float rectLength, float rectHeight, float rightGap, float margin, int prevFontSize, float prevMaxWidth, float prevMaxHeight) {
		try {
			// RailwaySign 精细度公式：纹理高度 = 2^(res+5)（res 0→32px，3→256px，7→4096px）。
			// 条带高 0.5 格、宽高比 6:1，取 scale 作为纹理高度基准；密度因子 = scale/60
			// （原 res3 条带高 60px 为基准），字体/布局随密度等比放大，视觉比例不变而清晰度对齐 RailwaySign。
			// 封顶 1024px（res≥5），避免 6:1 宽条带在 res6/7 生成 24576×4096 巨型纹理导致崩溃；
			// 也因此不再需要把全局精细度拉到 7，默认值下已足够清晰。
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

			// ---- 1. 本站名：在条带中央水平居中，可缩放到边距内 ----
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
			// 以文本块宽度 mainDrawW 为居中参考：mainLeft 已定位好块位置，块内各行再居中一次即可
			drawTextBlock(g, mainText, mainLeft, mainTop, mainScale, mainDrawW);

			// ---- 2/3/4. 两侧指示组件 ----
			// flip=false：左侧灰矩形(+上一站名)，右侧线路色矩形+等腰三角(顶点向右)(+下一站名)
			// flip=true ：镜像——左侧线路色组合图(三角顶点向左)(+下一站名)，右侧灰矩形(+上一站名)
			final int rectW = Math.max(1, Math.round(rectLength * pxPerBlock));
			final int rectH = Math.max(1, Math.round(rectHeight * pxPerBlock));
			final int rectTop = (H - rectH) / 2;
			final int triW = Math.max(1, Math.round(rectHeight * pxPerBlock)); // 三角横向长度 = 0.1F
			final int cy = H / 2;
			final int mainRight = mainLeft + mainDrawW;
			final int pad = Math.max(1, Math.round(0.04F * pxPerBlock));

			// ---- 灰矩形：非 flip 在左（右移避让本站名）；flip 在右（左移避让本站名） ----
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

			// ---- 线路色组合图：矩形 + 等腰三角 ----
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
				// 等腰三角形：非 flip 底边在矩形右缘、顶点向右；flip 底边在矩形左缘、顶点向左
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

			// ---- 上一站名：非 flip 在灰矩形下左对齐到其左缘；flip 在灰矩形下右对齐到其右缘 ----
			if (prevStationName != null && !prevStationName.isEmpty()) {
				final float prevZoneW = Math.max(1, prevMaxWidth * pxPerBlock);
				final float prevZoneH = Math.max(1, prevMaxHeight * pxPerBlock);
				final TextBlock prevText = buildTextBlock(prevStationName, rawFont, prevFontSize, latinFontRatio, 0F, rectAwt, resolutionScale, Math.round(prevZoneW), !flip, flip);
				final float prevScale = prevText.height > 0 ? Math.min(1.0F, Math.min(prevZoneW / prevText.width, prevZoneH / prevText.height)) : 1.0F;
				final int prevX = flip ? greyX + greyW : greyX;
				final int prevY = rectTop + rectH;
				drawTextBlock(g, prevText, prevX, prevY, prevScale, prevZoneW);
			}

			// ---- 下一站名：非 flip 在线路色矩形下右对齐到其右缘；flip 左对齐到其左缘 ----
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

	/** 在给定左上角 (x, y) 以 scale 绘制文本块。leftAlign=true 左对齐到 x；rightAlign=true 右对齐到 x；否则在 zoneW 内居中。 */
	private static void drawTextBlock(Graphics2D g, TextBlock block, int x, int y, float scale, float zoneW) {
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

	/** 拆分为 CJK 单行 + 拉丁多行（最多 2 行），并测量整体尺寸。 */
	private static TextBlock buildTextBlock(String text, Font rawFont, int fontSize, float latinFontRatio, float gapRatio, Color color, float resolutionScale, int wrapMaxWidthPx, boolean leftAlign, boolean rightAlign) {
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

	/** Greedy word wrap: 第一行尽量填满，其余放到第二行。 */
	private static List<String> wrapLatin(String text, Font font, int maxWidth) {
		final List<String> lines = new ArrayList<>();
		final String[] words = text.trim().split("\\s+");
		StringBuilder current = new StringBuilder();
		for (final String word : words) {
			final String candidate = current.length() == 0 ? word : current + " " + word;
			if (current.length() == 0 || FontRenderUtils.getStringWidth(candidate, font) <= maxWidth) {
				current = new StringBuilder(candidate);
			} else {
				if (lines.size() < 1) {
					lines.add(current.toString());
					current = new StringBuilder(word);
				} else {
					if (current.length() > 0) {
						current.append(' ');
					}
					current.append(word);
				}
			}
		}
		if (current.length() > 0) {
			lines.add(current.toString());
		}
		return lines;
	}

	/** 文本块数据：CJK 单行 + 拉丁多行，颜色统一，含整体测量尺寸。 */
	private static final class TextBlock {
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
