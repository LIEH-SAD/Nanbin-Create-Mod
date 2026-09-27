package com.Nanbin.client.Drawing;

import net.minecraft.client.texture.NativeImage;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.Nanbin.InitClient.LOGGER;

/**
 * 双语（CJK + 拉丁）文字渲染成紧贴内容的纹理。
 * <ul>
 *     <li>CJK 片段拼成一行大字，不换行（只等比缩放）</li>
 *     <li>拉丁片段用小字号拼行，最多折成 2 行</li>
 * </ul>
 * 纹理紧密贴合内容，调用方可以按显示框等比缩放。
 */
public final class BilingualFittedRenderer {

	private BilingualFittedRenderer() {
	}

	public static NativeImage render(String text, Font rawFont, int fontSize, float latinFontRatio, int maxWidth, Color textColor) {
		return render(text, rawFont, fontSize, latinFontRatio, maxWidth, textColor, 0.04F);
	}

	/**
	 * @param text           已按管道规则提取过的可见文字（无 "|" 规则），"|" 分隔 CJK / 拉丁片段
	 * @param fontSize       CJK 字号（像素，未做分辨率缩放）；拉丁字号 = fontSize × latinFontRatio
	 * @param latinFontRatio 拉丁字号与 CJK 字号的比值（例如 0.4）
	 * @param maxWidth       拉丁行折行用的最大宽度（像素，最多 2 行）
	 * @param gapRatio       CJK 与拉丁行之间的间隙，按 fontSize 的比例（0 = 紧贴）
	 */
	public static NativeImage render(String text, Font rawFont, int fontSize, float latinFontRatio, int maxWidth, Color textColor, float gapRatio) {
		try {
			final float resolutionScale = FontRenderUtils.getResolutionScale();
			final int cjkFontSize = Math.max(1, Math.round(fontSize * resolutionScale));
			final int latinFontSize = Math.max(1, Math.round(fontSize * latinFontRatio * resolutionScale));
			final Font cjkFont = rawFont.deriveFont(Font.PLAIN, (float) cjkFontSize);
			final Font latinFont = rawFont.deriveFont(Font.PLAIN, (float) latinFontSize);

			final String[] segments = text.split("\\|", -1);
			final StringBuilder cjkBuilder = new StringBuilder();
			final StringBuilder latinBuilder = new StringBuilder();
			for (final String segment : segments) {
				if (segment.isEmpty()) {
					continue;
				}
				if (FontRenderUtils.containsCjk(segment)) {
					cjkBuilder.append(segment);
				} else {
					if (latinBuilder.length() > 0) {
						latinBuilder.append(' ');
					}
					latinBuilder.append(segment);
				}
			}

			final String cjk = cjkBuilder.toString();
			final String latin = latinBuilder.toString();
			final boolean hasCjk = cjk.length() > 0;
			final boolean hasLatin = latin.length() > 0;
			if (!hasCjk && !hasLatin) {
				return null;
			}

			final List<String> latinLines = hasLatin ? wrapLatin(latin, latinFont, maxWidth) : Collections.emptyList();

			final int cjkWidth = hasCjk ? FontRenderUtils.getStringWidth(cjk, cjkFont) : 0;
			final int cjkAscent = hasCjk ? FontRenderUtils.getAscent(cjkFont) : 0;
			final int cjkDescent = hasCjk ? FontRenderUtils.getDescent(cjkFont) : 0;
			int latinMaxWidth = 0;
			int latinAscent = 0;
			int latinDescent = 0;
			for (final String line : latinLines) {
				latinMaxWidth = Math.max(latinMaxWidth, FontRenderUtils.getStringWidth(line, latinFont));
				latinAscent = Math.max(latinAscent, FontRenderUtils.getAscent(latinFont));
				latinDescent = Math.max(latinDescent, FontRenderUtils.getDescent(latinFont));
			}

			final int gap = hasCjk && hasLatin ? Math.max(0, Math.round(fontSize * gapRatio * resolutionScale)) : 0;
			final int maxLineWidth = Math.max(cjkWidth, latinMaxWidth);
			final int totalContentHeight = (hasCjk ? cjkAscent + cjkDescent : 0)
					+ gap
					+ (hasLatin ? (latinAscent + latinDescent) * latinLines.size() : 0);

			final int padding = Math.max(2, Math.round(fontSize * 0.04F * resolutionScale));
			final int textureWidth = Math.max(1, maxLineWidth + padding * 2);
			final int textureHeight = Math.max(1, totalContentHeight + padding * 2);

			final BufferedImage img = new BufferedImage(textureWidth, textureHeight, BufferedImage.TYPE_INT_ARGB);
			final Graphics2D g = img.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

			float cursorY = padding;
			if (hasCjk) {
				g.setFont(cjkFont);
				g.setColor(textColor);
				g.drawString(cjk, (textureWidth - cjkWidth) / 2, cursorY + cjkAscent);
				cursorY += cjkAscent + cjkDescent + gap;
			}
			for (final String line : latinLines) {
				g.setFont(latinFont);
				g.setColor(textColor);
				g.drawString(line, (textureWidth - FontRenderUtils.getStringWidth(line, latinFont)) / 2, cursorY + latinAscent);
				cursorY += latinAscent + latinDescent;
			}
			g.dispose();

			return FontRenderUtils.toNativeImage(img);
		} catch (Exception e) {
			LOGGER.error("Failed to render bilingual text \"{}\" to texture", text, e);
			return null;
		}
	}

	/** 贪心折行，最多 2 行：第 1 行填到 maxWidth，剩下的进第 2 行。 */
	private static List<String> wrapLatin(String text, Font font, int maxWidth) {
		final List<String> lines = new ArrayList<>();
		final String[] words = text.trim().split("\\s+");
		StringBuilder current = new StringBuilder();
		for (final String word : words) {
			final String candidate = current.length() == 0 ? word : current + " " + word;
			if (current.length() == 0 || FontRenderUtils.getStringWidth(candidate, font) <= maxWidth) {
				current = new StringBuilder(candidate);
			} else {
				if (lines.isEmpty()) {
					lines.add(current.toString());
					current = new StringBuilder(word);
				} else {
					// 已经是第 1 行：溢出的都并进第 2 行（可能自身也超宽，由调用方等比缩放）
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
}
