package com.Nanbin.client.RouteMap;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

import static com.Nanbin.client.Drawing.FontRenderUtils.isNumeric;

/**
 * 绘制地铁风格的圆形线路标志（站名牌、线路图等共用）：
 * 白色圆底 + 主题色描边 + 圆心横线，横线上方为线路编号、下方为站台号。
 */
public final class RoundMapDrawing {

	private RoundMapDrawing() {
	}

	/**
	 * 在给定的 Graphics2D 上绘制一个圆形线路标志。
	 * 编号/站台号任一为空时不绘制对应文字；两者都为空时只画圆与横线。
	 *
	 * @param themeColor  描边颜色（通常为站台主题色）
	 * @param textColor   线路编号/站台号文字颜色（通常为线路色）
	 * @param numberFont  纯数字文字字体（大小由调用方决定）
	 * @param textFont    非数字文字字体（大小由调用方决定）
	 */
	public static void draw(Graphics2D g, int centerX, int centerY, int radius, int strokeWidth, Color themeColor, Color textColor, Font numberFont, Font textFont, String routeNumber, String platformNumber) {
		draw(g, centerX, centerY, radius, strokeWidth, themeColor, textColor, numberFont, textFont, routeNumber, platformNumber, false);
	}

	/**
	 * 反色版绘制：主题色与白色互换，文字颜色也改为白色，透明区域保持透明。
	 */
	public static void drawInverted(Graphics2D g, int centerX, int centerY, int radius, int strokeWidth, Color themeColor, Color textColor, Font numberFont, Font textFont, String routeNumber, String platformNumber) {
		draw(g, centerX, centerY, radius, strokeWidth, themeColor, textColor, numberFont, textFont, routeNumber, platformNumber, true);
	}

	private static void draw(Graphics2D g, int centerX, int centerY, int radius, int strokeWidth, Color themeColor, Color textColor, Font numberFont, Font textFont, String routeNumber, String platformNumber, boolean inverted) {
		final boolean hasRoute = routeNumber != null && !routeNumber.isEmpty();
		final boolean hasPlatform = platformNumber != null && !platformNumber.isEmpty();

		// 反色时主题色与白色互换，文字也改为白色（落在主题色圆底上更清晰）
		final Color backColor = inverted ? Color.WHITE : themeColor;
		final Color fillColor = inverted ? themeColor : Color.WHITE;
		final Color strokeColor = inverted ? Color.WHITE : themeColor;
		final Color drawTextColor = inverted ? Color.WHITE : textColor;

		// 底层：垫底圆（同圆心，直径为标准圆的 1.05 倍），构成最外层边框
		final int backRadius = Math.round(radius * 1.05F);
		g.setColor(backColor);
		g.fillOval(centerX - backRadius, centerY - backRadius, backRadius * 2, backRadius * 2);

		// 圆底
		g.setColor(fillColor);
		g.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);

		// 描边
		g.setColor(strokeColor);
		g.setStroke(new BasicStroke(strokeWidth));
		g.drawOval(centerX - radius, centerY - radius, radius * 2, radius * 2);

		// 圆心横线（直径的 80%，与描边同宽）
		final int lineHalfWidth = Math.round(radius * 0.8F);
		g.setStroke(new BasicStroke(strokeWidth));
		g.drawLine(centerX - lineHalfWidth, centerY, centerX + lineHalfWidth, centerY);

		if (!hasRoute && !hasPlatform) {
			return;
		}

		// 每条文字独立选择字体：纯数字用数字字体（SOURCE_SANS_3），其余用主字体
		final Font routeFont = hasRoute && isNumeric(routeNumber) ? numberFont : textFont;
		final Font platformFont = hasPlatform && isNumeric(platformNumber) ? numberFont : textFont;

		g.setFont(routeFont);
		final FontMetrics routeMetrics = g.getFontMetrics(routeFont);
		final FontMetrics platformMetrics = g.getFontMetrics(platformFont);
		final int routeAscent = hasRoute ? routeMetrics.getAscent() : 0;
		final int routeDescent = hasRoute ? routeMetrics.getDescent() : 0;
		final int platformAscent = hasPlatform ? platformMetrics.getAscent() : 0;
		final int platformDescent = hasPlatform ? platformMetrics.getDescent() : 0;
		final int textLeading = Math.max(Math.round(platformFont.getSize() * 0.15F), 2);

		final int totalTextHeight = (hasRoute ? routeAscent + routeDescent : 0)
				+ (hasRoute && hasPlatform ? textLeading : 0)
				+ (hasPlatform ? platformAscent + platformDescent : 0);

		int cursorY = centerY - totalTextHeight / 2;
		if (hasRoute) {
			final int routeWidth = routeMetrics.stringWidth(routeNumber);
			g.setFont(routeFont);
			g.setColor(drawTextColor);
			g.drawString(routeNumber, centerX - routeWidth / 2, cursorY + routeAscent);
			cursorY += routeAscent + routeDescent + textLeading;
		}
		if (hasPlatform) {
			final int platformWidth = platformMetrics.stringWidth(platformNumber);
			g.setFont(platformFont);
			g.setColor(drawTextColor);
			g.drawString(platformNumber, centerX - platformWidth / 2, cursorY + platformAscent);
		}
	}
}
