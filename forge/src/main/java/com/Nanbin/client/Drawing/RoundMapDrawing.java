package com.Nanbin.client.Drawing;

import java.awt.*;

import static com.Nanbin.client.Drawing.FontRenderUtils.isNumeric;

public final class RoundMapDrawing {

	private RoundMapDrawing() {
	}

	public static void draw(Graphics2D g, int centerX, int centerY, int radius, int strokeWidth, Color themeColor, Color textColor, Font numberFont, Font textFont, String routeNumber, String platformNumber) {
		draw(g, centerX, centerY, radius, strokeWidth, themeColor, textColor, numberFont, textFont, routeNumber, platformNumber, false);
	}

	public static void drawInverted(Graphics2D g, int centerX, int centerY, int radius, int strokeWidth, Color themeColor, Color textColor, Font numberFont, Font textFont, String routeNumber, String platformNumber) {
		draw(g, centerX, centerY, radius, strokeWidth, themeColor, textColor, numberFont, textFont, routeNumber, platformNumber, true);
	}

	private static void draw(Graphics2D g, int centerX, int centerY, int radius, int strokeWidth, Color themeColor, Color textColor, Font numberFont, Font textFont, String routeNumber, String platformNumber, boolean inverted) {
		final boolean hasRoute = routeNumber != null && !routeNumber.isEmpty();
		final boolean hasPlat = platformNumber != null && !platformNumber.isEmpty();

		// 反色时主题色
		final Color backColor = inverted ? Color.WHITE : themeColor;
		final Color fillColor = inverted ? themeColor : Color.WHITE;
		final Color strokeColor = inverted ? Color.WHITE : themeColor;
		final Color drawTextColor = inverted ? Color.WHITE : textColor;

		// 底层
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

		// 圆心横线
		final int lineHalfWidth = Math.round(radius * 0.8F);
		g.setStroke(new BasicStroke(strokeWidth));
		g.drawLine(centerX - lineHalfWidth, centerY, centerX + lineHalfWidth, centerY);

		if (!hasRoute && !hasPlat) {
			return;
		}

		// 独立选择字体
		final Font routeFont = hasRoute && isNumeric(routeNumber) ? numberFont : textFont;
		final Font platFont = hasPlat && isNumeric(platformNumber) ? numberFont : textFont;

		g.setFont(routeFont);
		final FontMetrics routeMetrics = g.getFontMetrics(routeFont);
		final FontMetrics platMetrics = g.getFontMetrics(platFont);
		final int routeAscent = hasRoute ? routeMetrics.getAscent() : 0;
		final int routeDescent = hasRoute ? routeMetrics.getDescent() : 0;
		final int platAscent = hasPlat ? platMetrics.getAscent() : 0;
		final int platDescent = hasPlat ? platMetrics.getDescent() : 0;
		final int textLeading = Math.max(Math.round(platFont.getSize() * 0.15F), 2);

		// 文字的总高度
		final int totalTextH = (hasRoute ? (routeAscent + routeDescent) : 0)
				+ (hasRoute && hasPlat ? textLeading : 0)
				+ (hasPlat ? (platAscent + platDescent) : 0);

		// 文字块垂直居中
		final int textBlockTop = centerY - totalTextH / 2;

		int cursorY = textBlockTop;
		if (hasRoute) {
			final int rnW = routeMetrics.stringWidth(routeNumber);
			g.setFont(routeFont);
			g.setColor(drawTextColor);
			g.drawString(routeNumber, centerX - rnW / 2, cursorY + routeAscent);
			cursorY += routeAscent + routeDescent + textLeading;
		}
		if (hasPlat) {
			final int pnW = platMetrics.stringWidth(platformNumber);
			g.setFont(platFont);
			g.setColor(drawTextColor);
			g.drawString(platformNumber, centerX - pnW / 2, cursorY + platAscent);
		}
	}
}
