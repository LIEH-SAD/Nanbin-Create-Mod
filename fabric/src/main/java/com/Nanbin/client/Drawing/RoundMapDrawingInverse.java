package com.Nanbin.client.Drawing;

import java.awt.*;

public final class RoundMapDrawingInverse {

	private RoundMapDrawingInverse() {
	}

	public static void draw(Graphics2D g, int centerX, int centerY, int radius, int strokeWidth, Color themeColor, Color textColor, Font numberFont, Font textFont, String routeNumber, String platformNumber) {
		RoundMapDrawing.drawInverted(g, centerX, centerY, radius, strokeWidth, themeColor, textColor, numberFont, textFont, routeNumber, platformNumber);
	}
}
