package com.Nanbin.client.Drawing;

import org.mtr.mapping.holder.NativeImage;

import java.awt.*;
import java.awt.image.BufferedImage;

import static com.Nanbin.InitClient.LOGGER;

public final class PSDCRTStationSignRenderer {

	private static final float BADGE_CENTER_Y_RATIO = 0.53F;
	private static final float BADGE_RADIUS_RATIO = 0.135F;
	private static final float RING_RADIUS_RATIO = 0.10F;
	private static final float BADGE_TEXT_RADIUS_RATIO = 0.66F;
	private static final float BAR_HEIGHT_RATIO = 0.125F;
	private static final float BAR_EDGE_RATIO = 0.04F;
	private static final float ARROW_SLANT_BAR_RATIO = 1.2F;
	private static final int NON_TRAVEL_COLOR = 0xFF68645E;
	private static final float NAME_ZONE_WIDTH_RATIO = 0.94F;
	private static final float NAME_TOP_RATIO = 0.03F;
	private static final float NAME_BADGE_GAP_RATIO = 0.005F;
	private static final float SIDE_NAME_GAP_RATIO = 0.02F;
	private static final float NAME_BOTTOM_MARGIN_RATIO = 0.02F;

	private PSDCRTStationSignRenderer() {
	}

	public static NativeImage render(String stationName, String prevStationName, String nextStationName, String routeNumber, String stationNumber,
			int lineColor, int textColor, float groupWidth, float bandHeight,
			Font rawFont, Font circleNumberFont, int fontSize, int prevFontSize, int badgeFontSize,
			float latinFontRatio, float gapRatio, boolean reverse) {
		try {
			// 纹理高按 MTR 精细度缩放，封顶 768px：站牌在 res7 下也不会生成巨型纹理。
			final int H = Math.min(FontRenderUtils.getTextureHeight(), 768);
			final float resolutionScale = H / 512.0F;
			final int W = Math.max(1, Math.round(H * groupWidth / bandHeight));

			final BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
			final Graphics2D g = img.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			final Color lineAwt = new Color(lineColor | 0xFF000000, true);
			final Color textAwt = new Color(textColor, true);
			final Color nonTravelAwt = new Color(NON_TRAVEL_COLOR, true);

			// 底色：白色
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, W, H);

			final int badgeCx = W / 2;
			final int badgeCy = Math.round(H * BADGE_CENTER_Y_RATIO);
			final int badgeR = Math.max(2, Math.round(H * BADGE_RADIUS_RATIO));
			final int barH = Math.max(1, Math.round(H * BAR_HEIGHT_RATIO));
			final int barTop = badgeCy - barH / 2;
			// 色带两端一直画到圆心 X：两段在圆正中相接，接缝由后画的圆（含最外层白线）盖住，
			// 于是色带看起来是「钻进圆下面」，圆最外面那圈白线也完整露出来
			final int triW = Math.max(2, Math.round(barH * ARROW_SLANT_BAR_RATIO));
			final int leftBarX0 = Math.round(W * BAR_EDGE_RATIO);
			final int leftBarX1 = badgeCx;
			final int rightBarX0 = badgeCx;
			final int tipX = W - Math.round(W * BAR_EDGE_RATIO);
			final boolean hasNext = nextStationName != null && !nextStationName.isEmpty();

			final int rightBarX1 = hasNext ? tipX - triW : tipX;

			// 站名：底边贴住圆徽（含白色外圈）顶部，只留一道窄缝 —— 实拍里站名/拼音几乎压在圆徽上
			final int badgeTop = badgeCy - Math.round(badgeR * 1.05F);
			final int nameZoneTop = Math.round(H * NAME_TOP_RATIO);
			final int nameZoneBottom = badgeTop - Math.round(H * NAME_BADGE_GAP_RATIO);
			final int nameZoneH = Math.max(1, nameZoneBottom - nameZoneTop);
			final int nameZoneW = Math.round(W * NAME_ZONE_WIDTH_RATIO);
			final CRTStationInfoBandRenderer.TextBlock mainText = CRTStationInfoBandRenderer.buildTextBlock(stationName, rawFont, fontSize, latinFontRatio, gapRatio, textAwt, resolutionScale, nameZoneW, false, false);
			if (mainText.height > 0) {
				final float scale = Math.min(1.0F, Math.min((float) nameZoneW / mainText.width, (float) nameZoneH / mainText.height));
				final int drawW = Math.max(1, Math.round(mainText.width * scale));
				final int drawH = Math.max(1, Math.round(mainText.height * scale));
				CRTStationInfoBandRenderer.drawTextBlock(g, mainText, Math.round((W - drawW) / 2.0F), nameZoneBottom - drawH, scale, drawW);
			}

			// 线路色带：行进方向（下一站一侧）用线路色；非行进方向（上一站一侧）用 #68645E，且不画箭头
			g.setColor(nonTravelAwt);
			fillRectMirrored(g, leftBarX0, leftBarX1, barTop, barH, reverse, W);
			g.setColor(lineAwt);
			fillRectMirrored(g, rightBarX0, rightBarX1, barTop, barH, reverse, W);
			if (hasNext) {
				// 箭头：色带末端斜切收尖，下缘最长、斜边落到色带上缘，与实拍一致
				final Polygon tip = new Polygon();
				tip.addPoint(mx(rightBarX1, reverse, W), barTop);
				tip.addPoint(mx(rightBarX1, reverse, W), barTop + barH);
				tip.addPoint(mx(tipX, reverse, W), barTop + barH);
				g.fillPolygon(tip);
			}

			// 圆徽：线色圆底 + 白色描边/分线/数字（实拍：上半为线路号、下半为车站号），故用反色版
			final int fittedBadgeFontSize = Math.max(4, Math.min(Math.round(badgeFontSize * resolutionScale), Math.round(badgeR * BADGE_TEXT_RADIUS_RATIO)));
			RoundMapDrawingInverse.draw(g, badgeCx, badgeCy, badgeR, Math.max(1, Math.round(badgeR * RING_RADIUS_RATIO)), lineAwt, lineAwt,
					circleNumberFont.deriveFont(Font.PLAIN, (float) fittedBadgeFontSize), rawFont.deriveFont(Font.PLAIN, (float) fittedBadgeFontSize),
					RouteMapGenerator.cleanRouteNumber(routeNumber), stationNumber);

			// 上一站名
			final int sideNameTop = barTop + barH + Math.round(H * SIDE_NAME_GAP_RATIO);
			final int sideNameH = Math.max(1, H - Math.round(H * NAME_BOTTOM_MARGIN_RATIO) - sideNameTop);
			final int prevAnchorX = mx(leftBarX0, reverse, W);
			final int nextAnchorX = mx(tipX, reverse, W);
			drawSideName(g, prevStationName, rawFont, prevFontSize, latinFontRatio, nonTravelAwt, resolutionScale,
					sideNameTop, sideNameH, leftBarX1 - leftBarX0, prevAnchorX, !reverse);
			drawSideName(g, nextStationName, rawFont, prevFontSize, latinFontRatio, lineAwt, resolutionScale,
					sideNameTop, sideNameH, tipX - rightBarX0, nextAnchorX, reverse);

			g.dispose();
			return FontRenderUtils.toNativeImage(img);
		} catch (Exception e) {
			LOGGER.error("Failed to render PSD station sign for \"{}\" / \"{}\"", stationName, prevStationName, e);
			return null;
		}
	}

	private static void drawSideName(Graphics2D g, String text, Font rawFont, int fontSize, float latinFontRatio, Color color,
			float resolutionScale, int zoneTop, int zoneH, int zoneW, int anchorX, boolean leftAlign) {
		if (text == null || text.isEmpty()) {
			return;
		}
		final int width = Math.max(1, zoneW);
		final CRTStationInfoBandRenderer.TextBlock block = CRTStationInfoBandRenderer.buildTextBlock(text, rawFont, fontSize, latinFontRatio, 0F, color, resolutionScale, width, leftAlign, !leftAlign);
		if (block.height <= 0) {
			return;
		}
		final float scale = Math.min(1.0F, Math.min((float) width / block.width, (float) zoneH / block.height));
		final int drawH = Math.max(1, Math.round(block.height * scale));
		CRTStationInfoBandRenderer.drawTextBlock(g, block, anchorX, zoneTop + Math.round((zoneH - drawH) / 2.0F), scale, width);
	}

	private static void fillRect(Graphics2D g, int x, int y, int w, int h) {
		if (w > 0 && h > 0) {
			g.fillRect(x, y, w, h);
		}
	}

	private static int mx(int x, boolean reverse, int w) {
		return reverse ? w - x : x;
	}

	private static void fillRectMirrored(Graphics2D g, int x0, int x1, int y, int h, boolean reverse, int w) {
		final int a = mx(x0, reverse, w);
		final int b = mx(x1, reverse, w);
		fillRect(g, Math.min(a, b), y, Math.abs(b - a), h);
	}
}
