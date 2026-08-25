package com.Nanbin.client.Drawing;

import org.mtr.mapping.holder.NativeImage;

import java.awt.*;
import java.awt.image.BufferedImage;

import static com.Nanbin.InitClient.LOGGER;

/**
 * 屏蔽门编号铭牌纹理绘制：线路色正方形 + 居中白色编号。
 * 仅包含绘制算法，不内置数值；尺寸与字体由调用方传入。
 */
public final class DoorNumberPlateRenderer {

	private DoorNumberPlateRenderer() {
	}

	/**
	 * @param number     门编号（≥1）
	 * @param lineColor  线路颜色（ARGB）
	 * @param rawFont    编号字体（建议 SOURCE_SANS_3，纯数字）
	 * @param sidePx     正方形边长（像素）
	 * @param fontScale  字体大小 = 边长 × scale
	 * @param marginRatio 文字与正方形边缘的留边比例
	 */
	public static NativeImage render(int number, int lineColor, Font rawFont, int sidePx, float fontScale, float marginRatio) {
		try {
			final int S = Math.max(1, sidePx);
			final BufferedImage img = new BufferedImage(S, S, BufferedImage.TYPE_INT_ARGB);
			final Graphics2D g = img.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

			// 线路色正方形
			g.setColor(new Color(lineColor | 0xFF000000, true));
			g.fillRect(0, 0, S, S);

			// 白色编号，水平/垂直居中并缩放到留边内
			final String text = Integer.toString(number);
			Font font = rawFont.deriveFont(Font.PLAIN, Math.max(1, Math.round(S * fontScale)));
			int w = FontRenderUtils.getStringWidth(text, font);
			final int margin = Math.max(1, Math.round(S * marginRatio));
			final int maxW = S - margin * 2;
			if (w > maxW && w > 0) {
				font = font.deriveFont(font.getSize2D() * (maxW / (float) w));
				w = FontRenderUtils.getStringWidth(text, font);
			}

			final int ascent = FontRenderUtils.getAscent(font);
			final int descent = FontRenderUtils.getDescent(font);
			final int x = Math.max(0, (S - w) / 2);
			final int baseline = (S + ascent - descent) / 2;

			g.setFont(font);
			g.setColor(Color.WHITE);
			g.drawString(text, x, baseline);
			g.dispose();

			return FontRenderUtils.toNativeImage(img);
		} catch (Exception e) {
			LOGGER.error("Failed to render door number plate for {}", number, e);
			return null;
		}
	}
}
