package com.Nanbin.client.Drawing;

import org.mtr.mapping.holder.NativeImage;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.Nanbin.InitClient.LOGGER;

/**
 * Renders bilingual (CJK + Latin) text into a tightly-fitted texture.
 * - CJK segments are concatenated into a single large line and never wrap (only scale proportionally).
 * - Latin segments are concatenated into a smaller-font line, which may wrap into at most 2 lines.
 * The texture is tightly bounded to its content so the caller can scale it proportionally to fit a display box.
 */
public final class BilingualFittedRenderer {

	private BilingualFittedRenderer() {
	}

	/**
	 * @param text            already-extracted visible text (no pipe rules), "|" splits CJK/Latin segments
	 * @param rawFont         the loaded font (e.g. Alibaba)
	 * @param fontSize        CJK font size (pixels, before resolution scaling); Latin = fontSize * latinFontRatio
	 * @param latinFontRatio  ratio of Latin font size to CJK font size (e.g. 0.4)
	 * @param maxWidth        max width in pixels used for wrapping the Latin line (up to 2 lines)
	 * @param textColor       text color
	 */
	public static NativeImage render(String text, Font rawFont, int fontSize, float latinFontRatio, int maxWidth, Color textColor) {
		return render(text, rawFont, fontSize, latinFontRatio, maxWidth, textColor, 0.04F);
	}

	/**
	 * @param text            already-extracted visible text (no pipe rules), "|" splits CJK/Latin segments
	 * @param rawFont         the loaded font (e.g. Alibaba)
	 * @param fontSize        CJK font size (pixels, before resolution scaling); Latin = fontSize * latinFontRatio
	 * @param latinFontRatio  ratio of Latin font size to CJK font size (e.g. 0.4)
	 * @param maxWidth        max width in pixels used for wrapping the Latin line (up to 2 lines)
	 * @param textColor       text color
	 * @param gapRatio        gap between CJK and Latin lines as a ratio of fontSize (0 = tightly packed)
	 */
	public static NativeImage render(String text, Font rawFont, int fontSize, float latinFontRatio, int maxWidth, Color textColor, float gapRatio) {
		try {
			final float resolutionScale = FontRenderUtils.getResolutionScale();
			final int cjkFontSize = Math.max(1, Math.round(fontSize * resolutionScale));
			final int latinFontSize = Math.max(1, Math.round(fontSize * latinFontRatio * resolutionScale));
			final Font cjkFont = rawFont.deriveFont(Font.PLAIN, (float) cjkFontSize);
			final Font latinFont = rawFont.deriveFont(Font.PLAIN, (float) latinFontSize);

			// Split into CJK / Latin
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
				return null;
			}

			// Wrap Latin into at most 2 lines
			final List<String> latinLines = hasLatin ? wrapLatin(latin, latinFont, maxWidth) : Collections.emptyList();

			// Measure content
			final int cjkW = hasCjk ? FontRenderUtils.getStringWidth(cjk, cjkFont) : 0;
			final int cjkAscent = hasCjk ? FontRenderUtils.getAscent(cjkFont) : 0;
			final int cjkDescent = hasCjk ? FontRenderUtils.getDescent(cjkFont) : 0;
			int latinMaxW = 0;
			int latinAscent = 0;
			int latinDescent = 0;
			for (final String line : latinLines) {
				latinMaxW = Math.max(latinMaxW, FontRenderUtils.getStringWidth(line, latinFont));
				latinAscent = Math.max(latinAscent, FontRenderUtils.getAscent(latinFont));
				latinDescent = Math.max(latinDescent, FontRenderUtils.getDescent(latinFont));
			}
			final int gap = (hasCjk && hasLatin) ? Math.max(0, Math.round(fontSize * gapRatio * resolutionScale)) : 0;
			final int maxLineW = Math.max(cjkW, latinMaxW);
			final int totalContentH = (hasCjk ? cjkAscent + cjkDescent : 0) + gap + (hasLatin ? (latinAscent + latinDescent) * latinLines.size() : 0);

			final int padding = Math.max(2, Math.round(fontSize * 0.04F * resolutionScale));
			final int texW = Math.max(1, maxLineW + padding * 2);
			final int texH = Math.max(1, totalContentH + padding * 2);

			final BufferedImage img = new BufferedImage(texW, texH, BufferedImage.TYPE_INT_ARGB);
			final Graphics2D g = img.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

			float cursorY = padding;
			if (hasCjk) {
				g.setFont(cjkFont);
				g.setColor(textColor);
				final int x = (texW - cjkW) / 2;
				g.drawString(cjk, x, cursorY + cjkAscent);
				cursorY += cjkAscent + cjkDescent + gap;
			}
			for (final String line : latinLines) {
				g.setFont(latinFont);
				g.setColor(textColor);
				final int x = (texW - FontRenderUtils.getStringWidth(line, latinFont)) / 2;
				g.drawString(line, x, cursorY + latinAscent);
				cursorY += latinAscent + latinDescent;
			}
			g.dispose();

			return FontRenderUtils.toNativeImage(img);
		} catch (Exception e) {
			LOGGER.error("Failed to render bilingual text \"{}\" to texture", text, e);
			return null;
		}
	}

	/** Greedy word wrap into at most 2 lines: line 1 fills until maxWidth, the rest goes to line 2. */
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
					// Already 1 line: overflow onto line 2 (may itself overflow; caller scales proportionally)
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
