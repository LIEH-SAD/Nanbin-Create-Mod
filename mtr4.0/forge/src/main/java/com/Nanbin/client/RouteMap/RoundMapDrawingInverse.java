package com.Nanbin.client.RouteMap;

import java.awt.*;

/**
 * 圆形线路标志的反色版本：与 {@link RoundMapDrawing} 相反——
 * 原主题色的位置改为白色，原白色的位置改为主题色，透明区域保持透明。
 * 适用于白底、深色背景等需要反色配色的场景。
 */
public final class RoundMapDrawingInverse {

	private RoundMapDrawingInverse() {
	}

	/**
	 * 反色版圆形线路标志，参数与 {@link RoundMapDrawing#drawInverted} 一致。
	 *
	 * @param g              目标画布
	 * @param centerX        圆心 X
	 * @param centerY        圆心 Y
	 * @param radius         圆半径
	 * @param strokeWidth    描边与横线的宽度
	 * @param themeColor     站台主题色（反色后用作圆底填充）
	 * @param textColor      线路编号/站台号文字颜色（通常为线路色）
	 * @param numberFont     纯数字文字字体（大小由调用方决定）
	 * @param textFont       非数字文字字体（大小由调用方决定）
	 * @param routeNumber    线路编号，可为空
	 * @param platformNumber 站台号，可为空
	 */
	public static void draw(Graphics2D g, int centerX, int centerY, int radius, int strokeWidth, Color themeColor, Color textColor, Font numberFont, Font textFont, String routeNumber, String platformNumber) {
		RoundMapDrawing.drawInverted(g, centerX, centerY, radius, strokeWidth, themeColor, textColor, numberFont, textFont, routeNumber, platformNumber);
	}
}
