package com.Nanbin.client.RouteMap;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

/**
 * 圆形线路标志的反色版本：与 {@link RoundMapDrawing} 相反 ——
 * 原主题色的位置改为白色，原白色的位置改为主题色，透明区域保持透明。
 * 适用于白底、深色背景等需要反色配色的场景。
 */
public final class RoundMapDrawingInverse {

	private RoundMapDrawingInverse() {
	}

	/**
	 * 反色版圆形线路标志，参数与 {@link RoundMapDrawing#drawInverted} 一致。
	 */
	public static void draw(Graphics2D g, int centerX, int centerY, int radius, int strokeWidth, Color themeColor, Color textColor, Font numberFont, Font textFont, String routeNumber, String platformNumber) {
		RoundMapDrawing.drawInverted(g, centerX, centerY, radius, strokeWidth, themeColor, textColor, numberFont, textFont, routeNumber, platformNumber);
	}
}
