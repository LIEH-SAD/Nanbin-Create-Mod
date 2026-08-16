package com.Nanbin.client.Drawing;

import org.mtr.mapping.holder.NativeImage;

import java.awt.*;
import java.awt.image.BufferedImage;

import static com.Nanbin.InitClient.LOGGER;

/**
 * Renders text in two rows:
 * - Top row: CJK segments concatenated (fontSize), centered
 * - Bottom row: Latin segments concatenated (fontSize/2), centered
 * - Small gap between rows
 * - Uniformly scaled down if either row exceeds texture bounds
 */
public final class TextRenderer {

	private TextRenderer() {
	}

	/**
	 * @param text     already-extracted visible text (no pipe rules), "|" splits CJK/Latin segments
	 * @param maxWidth texture width as a ratio of {@link FontRenderUtils#BASE_TEXTURE_HEIGHT}
	 */
	public static NativeImage render(String text, float maxWidth, Font rawFont, int fontSize, Color textColor) {
		try {
			final int textureHeight = FontRenderUtils.getTextureHeight();
			final int textureWidth = Math.round(textureHeight * maxWidth);
			if (textureWidth <= 0) {
				return null;
			}

			// Split by "|" and classify segments into CJK and Latin groups
			final String[] segments = text.split("\\|", -1);
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

			if (!hasCjk && !hasLatin) return null;

			// Scale font sizes proportionally with texture resolution
			final float resolutionScale = FontRenderUtils.getResolutionScale();
			final int scaledCjkFontSize = Math.round(fontSize * resolutionScale);
			final int scaledLatinFontSize = Math.max(Math.round(fontSize * 0.6F * resolutionScale), Math.round(8 * resolutionScale));
			final float gapRatio = 0.15F;
			final Font cjkFont = rawFont.deriveFont(Font.PLAIN, (float) scaledCjkFontSize);
			final Font latinFont = rawFont.deriveFont(Font.PLAIN, (float) scaledLatinFontSize);

			final BufferedImage temp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
			final Graphics2D tempG = temp.createGraphics();

			int cjkTotalWidth = 0, cjkAscent = 0, cjkDescent = 0;
			if (hasCjk) {
				tempG.setFont(cjkFont);
				final FontMetrics m = tempG.getFontMetrics(cjkFont);
				cjkTotalWidth = m.stringWidth(cjkLine.toString());
				cjkAscent = m.getAscent();
				cjkDescent = m.getDescent();
			}

			int latinTotalWidth = 0, latinAscent = 0, latinDescent = 0;
			if (hasLatin) {
				tempG.setFont(latinFont);
				final FontMetrics m = tempG.getFontMetrics(latinFont);
				latinTotalWidth = m.stringWidth(latinLine.toString());
				latinAscent = m.getAscent();
				latinDescent = m.getDescent();
			}

			tempG.dispose();

			final int maxLineWidth = Math.max(cjkTotalWidth, latinTotalWidth);
			final int gap;
			if (hasCjk && hasLatin) {
				gap = Math.round(fontSize * gapRatio * resolutionScale);
			} else {
				gap = 0;
			}
			final int totalContentHeight = (hasCjk ? (cjkAscent + cjkDescent) : 0)
					+ gap
					+ (hasLatin ? (latinAscent + latinDescent) : 0);

			// Uniform scale: shrink to fit, never upscale
			final float scale = Math.min(1.0F, Math.min(
					(float) textureWidth / (float) maxLineWidth,
					(float) textureHeight / (float) totalContentHeight
			));

			// Render both rows centered into one texture
			final BufferedImage img = new BufferedImage(textureWidth, textureHeight, BufferedImage.TYPE_INT_ARGB);
			final Graphics2D g = img.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

			// Vertically center the two-row block
			final float scaledContentHeight = totalContentHeight * scale;
			final float baseY = (textureHeight - scaledContentHeight) / 2.0F / scale;

			float cursorY = baseY;
			if (hasCjk) {
				final float sx = (textureWidth - cjkTotalWidth * scale) / 2.0F;
				g.translate(sx, 0);
				g.scale(scale, scale);
				g.setFont(cjkFont);
				g.setColor(textColor);
				g.drawString(cjkLine.toString(), 0, cursorY + cjkAscent);
				g.scale(1.0 / scale, 1.0 / scale);
				g.translate(-sx, 0);
				cursorY += cjkAscent + cjkDescent + gap;
			}

			if (hasLatin) {
				final float sx = (textureWidth - latinTotalWidth * scale) / 2.0F;
				g.translate(sx, 0);
				g.scale(scale, scale);
				g.setFont(latinFont);
				g.setColor(textColor);
				g.drawString(latinLine.toString(), 0, cursorY + latinAscent);
				g.scale(1.0 / scale, 1.0 / scale);
				g.translate(-sx, 0);
			}

			g.dispose();

			return FontRenderUtils.toNativeImage(img);
		} catch (Exception e) {
			LOGGER.error("Failed to render text \"{}\" to texture", text, e);
			return null;
		}
	}
}