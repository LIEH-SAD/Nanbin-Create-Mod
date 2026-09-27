package com.Nanbin.client.Drawing;

import net.minecraft.client.texture.NativeImage;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import static com.Nanbin.InitClient.LOGGER;

/**
 * 双行文字纹理：
 * <ul>
 *     <li>上行：CJK 片段拼接（fontSize），居中</li>
 *     <li>下行：拉丁片段拼接（fontSize/2），居中</li>
 *     <li>两行之间留一小段间隙</li>
 *     <li>任一行超出纹理范围时整体等比缩小</li>
 * </ul>
 */
public final class TextRenderer {

	private TextRenderer() {
	}

	/**
	 * @param text     已按管道规则提取过的可见文字（无 "|" 规则），"|" 用来分隔 CJK / 拉丁片段
	 * @param maxWidth 纹理宽度，作为 {@link FontRenderUtils#BASE_TEXTURE_HEIGHT} 的比例
	 */
	public static NativeImage render(String text, float maxWidth, Font rawFont, int fontSize, Color textColor) {
		try {
			final int textureHeight = FontRenderUtils.getTextureHeight();
			final int textureWidth = Math.round(textureHeight * maxWidth);
			if (textureWidth <= 0) {
				return null;
			}

			final String[] segments = text.split("\\|", -1);
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
			if (!hasCjk && !hasLatin) {
				return null;
			}

			final float resolutionScale = FontRenderUtils.getResolutionScale();
			final int scaledCjkFontSize = Math.round(fontSize * resolutionScale);
			final int scaledLatinFontSize = Math.max(Math.round(fontSize * 0.6F * resolutionScale), Math.round(8 * resolutionScale));
			final Font cjkFont = rawFont.deriveFont(Font.PLAIN, (float) scaledCjkFontSize);
			final Font latinFont = rawFont.deriveFont(Font.PLAIN, (float) scaledLatinFontSize);

			final BufferedImage temp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
			final Graphics2D tempG = temp.createGraphics();

			int cjkTotalWidth = 0;
			int cjkAscent = 0;
			int cjkDescent = 0;
			if (hasCjk) {
				tempG.setFont(cjkFont);
				final FontMetrics metrics = tempG.getFontMetrics(cjkFont);
				cjkTotalWidth = metrics.stringWidth(cjkLine.toString());
				cjkAscent = metrics.getAscent();
				cjkDescent = metrics.getDescent();
			}

			int latinTotalWidth = 0;
			int latinAscent = 0;
			int latinDescent = 0;
			if (hasLatin) {
				tempG.setFont(latinFont);
				final FontMetrics metrics = tempG.getFontMetrics(latinFont);
				latinTotalWidth = metrics.stringWidth(latinLine.toString());
				latinAscent = metrics.getAscent();
				latinDescent = metrics.getDescent();
			}
			tempG.dispose();

			final int maxLineWidth = Math.max(cjkTotalWidth, latinTotalWidth);
			final int gap = hasCjk && hasLatin ? Math.round(fontSize * 0.15F * resolutionScale) : 0;
			final int totalContentHeight = (hasCjk ? cjkAscent + cjkDescent : 0)
					+ gap
					+ (hasLatin ? latinAscent + latinDescent : 0);

			// 只缩小、不放大
			final float scale = Math.min(1.0F, Math.min(
					(float) textureWidth / (float) maxLineWidth,
					(float) textureHeight / (float) totalContentHeight));

			final BufferedImage img = new BufferedImage(textureWidth, textureHeight, BufferedImage.TYPE_INT_ARGB);
			final Graphics2D g = img.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

			final float scaledContentHeight = totalContentHeight * scale;
			float cursorY = (textureHeight - scaledContentHeight) / 2.0F / scale;

			if (hasCjk) {
				final float offsetX = (textureWidth - cjkTotalWidth * scale) / 2.0F;
				g.translate(offsetX, 0);
				g.scale(scale, scale);
				g.setFont(cjkFont);
				g.setColor(textColor);
				g.drawString(cjkLine.toString(), 0, cursorY + cjkAscent);
				g.scale(1.0 / scale, 1.0 / scale);
				g.translate(-offsetX, 0);
				cursorY += cjkAscent + cjkDescent + gap;
			}

			if (hasLatin) {
				final float offsetX = (textureWidth - latinTotalWidth * scale) / 2.0F;
				g.translate(offsetX, 0);
				g.scale(scale, scale);
				g.setFont(latinFont);
				g.setColor(textColor);
				g.drawString(latinLine.toString(), 0, cursorY + latinAscent);
				g.scale(1.0 / scale, 1.0 / scale);
				g.translate(-offsetX, 0);
			}

			g.dispose();
			return FontRenderUtils.toNativeImage(img);
		} catch (Exception e) {
			LOGGER.error("Failed to render text \"{}\" to texture", text, e);
			return null;
		}
	}
}
