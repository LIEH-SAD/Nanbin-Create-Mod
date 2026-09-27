package com.Nanbin.client.Drawing;

import net.minecraft.client.texture.NativeImage;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import static com.Nanbin.InitClient.LOGGER;

/** 把单行文字渲染成紧贴内容（只有少量内边距）的纹理。 */
public final class FittedTextRenderer {

	private FittedTextRenderer() {
	}

	public static NativeImage render(String text, Font rawFont, int fontSize, Color textColor) {
		return render(text, rawFont, fontSize, textColor, false);
	}

	/**
	 * 把 {@code text} 渲染进一张尺寸刚好贴合字形（外加少量内边距）的纹理，
	 * 这样就能 1:1 显示，溢出时再等比缩小。
	 *
	 * @param text 已经按管道规则提取过的可见文字
	 * @param bold 是否对字体加粗
	 */
	public static NativeImage render(String text, Font rawFont, int fontSize, Color textColor, boolean bold) {
		try {
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
