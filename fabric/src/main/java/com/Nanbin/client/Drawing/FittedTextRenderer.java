package com.Nanbin.client.Drawing;

import org.mtr.mapping.holder.NativeImage;

import java.awt.*;
import java.awt.image.BufferedImage;

import static com.Nanbin.InitClient.LOGGER;

/** Renders a single line of text tightly fitted to its content (with a small padding). */
public final class FittedTextRenderer {

	private FittedTextRenderer() {
	}

	/**
	 * Renders {@code text} into a NativeImage whose size exactly matches the drawn glyphs
	 * (plus a small padding), so it can be displayed 1:1 and scaled down when overflowing.
	 *
	 * @param text already-extracted visible text (no pipe rules)
	 */
	public static NativeImage render(String text, Font rawFont, int fontSize, Color textColor) {
		return render(text, rawFont, fontSize, textColor, false);
	}

	/**
	 * Renders {@code text} into a NativeImage whose size exactly matches the drawn glyphs
	 * (plus a small padding), so it can be displayed 1:1 and scaled down when overflowing.
	 *
	 * @param text     already-extracted visible text (no pipe rules)
	 * @param bold     whether to apply bold style to the font
	 */
	public static NativeImage render(String text, Font rawFont, int fontSize, Color textColor, boolean bold) {
		try {
			// Scale font size proportionally with texture resolution
			final float resolutionScale = FontRenderUtils.getResolutionScale();
			final int scaledFontSize = Math.round(fontSize * resolutionScale);
			final Font textFont = rawFont.deriveFont(bold ? Font.BOLD : Font.PLAIN, (float) scaledFontSize);

			final BufferedImage temp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
			final Graphics2D tempG = temp.createGraphics();
			tempG.setFont(textFont);
			final FontMetrics metrics = tempG.getFontMetrics(textFont);
			final int textWidth = metrics.stringWidth(text);
			final int textAscent = metrics.getAscent();
			final int textDescent = metrics.getDescent();
			tempG.dispose();

			final int padding = Math.max(2, Math.round(fontSize * 0.08F * resolutionScale));
			final int textureWidth = Math.max(1, textWidth + padding * 2);
			final int textureHeight = Math.max(1, textAscent + textDescent + padding * 2);

			final BufferedImage img = new BufferedImage(textureWidth, textureHeight, BufferedImage.TYPE_INT_ARGB);
			final Graphics2D g = img.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
			g.setFont(textFont);
			g.setColor(textColor);
			g.drawString(text, padding, padding + textAscent);
			g.dispose();

			return FontRenderUtils.toNativeImage(img);
		} catch (Exception e) {
			LOGGER.error("Failed to render fitted text \"{}\" to texture", text, e);
			return null;
		}
	}
}