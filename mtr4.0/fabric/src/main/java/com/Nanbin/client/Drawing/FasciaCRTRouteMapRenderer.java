package com.Nanbin.client.Drawing;

import com.Nanbin.client.RouteMap.LineMapData;
import com.Nanbin.client.RouteMap.RouteMapGenerator;
import com.Nanbin.client.RouteMap.RoundMapDrawingInverse;
import org.mtr.mapping.holder.NativeImage;

import java.awt.*;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static com.Nanbin.InitClient.LOGGER;

public final class FasciaCRTRouteMapRenderer {

	private FasciaCRTRouteMapRenderer() {
	}

	private static final int MAX_TEXTURE_WIDTH = 8192;
	private static final int MAX_TEXTURE_HEIGHT = 768;
	private static final float BAND_HEIGHT_RATIO = 0.145F;
	private static final float BAND_CENTER_Y_RATIO = 0.5F;
	private static final float EDGE_MARGIN_RATIO = 0.5F;
	private static final float CONTENT_WIDTH_RATIO = 0.9F;
	private static final float MARGIN_RATIO = 0.78F;
	private static final float END_BEVEL_RATIO = 1F;
	private static final float PLAIN_DIAMETER_PITCH_RATIO = 0.5F;
	private static final float PLAIN_DIAMETER_BAND_RATIO = 0.61F;
	private static final float CURRENT_DIAMETER_RATIO = 1.2F;
	private static final float CIRCLE_ABOVE_RATIO = 0.38F;
	private static final float RING_RATIO = 0.035F;
	private static final float NUMBER_BAND_RATIO = 0.29F;
	private static final float NODE_FONT_RADIUS_RATIO = 0.6F;
	private static final float NOTCH_WIDTH_RATIO = 0.34F;
	private static final float NOTCH_DEPTH_RATIO = 0.58F;
	private static final float BRANCH_LINE_RATIO = 1.06F;
	private static final float BRANCH_FILLET_RATIO = 0.25F;
	private static final float NAME_CJK_BAND_RATIO = 0.68F;
	private static final float NAME_ANCHOR_GAP_RATIO = 0.1F;
	private static final float EDGE_SAFE_RATIO = 0.55F;
	private static final float BRANCH_FIRST_AFTER_FOOT_BH = 2.1F;
	private static final float MIN_NAME_SCALE = 0.3F;
	private static final float NAME_PITCH_FILL_RATIO = 0.92F;
	private static final float STUB_SLOPE = 1F;
	private static final float STUB_START_RATIO = 0F;
	private static final float STUB_DEPTH_RATIO = 1F;
	private static final float STUB_LINE_RATIO = 0.25F;
	private static final float STUB_TOP_OFFSET_RATIO = CIRCLE_ABOVE_RATIO;
	private static final float STUB_OUTLINE_RATIO = 0.03F;
	private static final float BADGE_RADIUS_RATIO = 0.33F;
	private static final float BADGE_FONT_RATIO = 0.42F;
	private static final int MAX_INTERCHANGE_STUBS = 3;
	private static final float TRANSFER_FONT_RATIO = 0.24F;
	private static final float TRANSFER_ANCHOR_RATIO = 2.35F;
	private static final int GREY_COLOR = 0xFF68645E;
	private static final String TRANSFER_LABEL = "换乘";
	private static final String TRANSFER_LATIN = "Transfer";
	private static final float HALF_SQRT_2 = 0.7071068F;
	private static final float NAME_STRIP_BH = 1.4F;
	private static final float BRANCH_PITCH_BH = 4.4F;
	private static final float BRANCH_STUB_DEPTH_RATIO = 0.3F;
	private static final int MAX_BRANCHES = 3;
	private static final float BAND_TO_NAME_BH = 0.5F + CIRCLE_ABOVE_RATIO + PLAIN_DIAMETER_BAND_RATIO / 2F + NAME_ANCHOR_GAP_RATIO;
	private static final float BRANCH_TOTAL_BH = 2F * EDGE_MARGIN_RATIO + NAME_STRIP_BH + BAND_TO_NAME_BH + BRANCH_PITCH_BH + 0.5F;

	public static NativeImage render(LineMapData data, float groupBlocks, float bandHeight,
			Font nameFont, int nameFontSize, Font numberFont, int numberFontSize,
			float latinFontRatio, float gapRatio, int textColor, boolean reverse) {
		final BufferedImage image = renderImage(data, groupBlocks, bandHeight, FontRenderUtils.getTextureHeight(),
				nameFont, nameFontSize, numberFont, numberFontSize, latinFontRatio, gapRatio, textColor, reverse);
		return image == null ? null : FontRenderUtils.toNativeImage(image);
	}

	public static BufferedImage renderImage(LineMapData data, float groupBlocks, float bandHeight, int baseHeight,
			Font nameFont, int nameFontSize, Font numberFont, int numberFontSize,
			float latinFontRatio, float gapRatio, int textColor, boolean reverse) {
		try {
			final int stationCount = data.stations().size();
			if (stationCount == 0) {
				return null;
			}
			// 本站下标
			final int currentIndex = Math.max(0, Math.min(stationCount - 1, data.currentIndex()));
			final List<LineMapData.Branch> branches = data.branches().size() > MAX_BRANCHES
					? data.branches().subList(0, MAX_BRANCHES) : data.branches();
			final boolean hasBranch = !branches.isEmpty();

			// 画布宽高比
			int h = Math.min(baseHeight, MAX_TEXTURE_HEIGHT);
			int w = Math.max(1, Math.round(h * groupBlocks / bandHeight));
			if (w > MAX_TEXTURE_WIDTH) {
				h = Math.max(64, Math.round(MAX_TEXTURE_WIDTH * bandHeight / groupBlocks));
				w = Math.max(1, Math.round(h * groupBlocks / bandHeight));
			}
			final float resolutionScale = h / 512.0F;

			final BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
			final Graphics2D g = img.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			final Color lineAwt = new Color(data.lineColor() | 0xFF000000, true);
			final Color textAwt = new Color(textColor, true);
			final Color greyAwt = new Color(GREY_COLOR, true);

			g.setColor(Color.WHITE);
			g.fillRect(0, 0, w, h);

			// 端头预留
			final float hMargin = w * (1F - CONTENT_WIDTH_RATIO) / 2F; // 内容整体居中
			// 有支线时整张图按比例缩小
			final float bandRatio = hasBranch ? Math.min(BAND_HEIGHT_RATIO, 1F / BRANCH_TOTAL_BH) : BAND_HEIGHT_RATIO;
			final float stubDepthRatio = hasBranch ? BRANCH_STUB_DEPTH_RATIO : STUB_DEPTH_RATIO;
			float bh = h * bandRatio;
			float margin = bh * MARGIN_RATIO;
			float inset = 0F;
			float insetRight = 0F;
			float lineEnd = lineEndOf(w, hMargin, 0F);
			// 末站离线带端头的安全距离
			float safeRight = margin + bh * (END_BEVEL_RATIO + EDGE_SAFE_RATIO);
			float pitch = pitchOf(lineEnd, hMargin, margin + inset, safeRight, stationCount);
			float rowPitch = 0F;
			final boolean forkAtFirst = forkAtFirst(branches);
			for (int pass = 0; pass < 4; pass++) {
				bh = Math.min(h * bandRatio, pitch * PLAIN_DIAMETER_PITCH_RATIO / PLAIN_DIAMETER_BAND_RATIO);
				margin = Math.max(bh * MARGIN_RATIO, bh * EDGE_SAFE_RATIO);
				// 折行行距
				rowPitch = Math.min(bh * BRANCH_PITCH_BH, rowSpaceOf(h, bh) / Math.max(1, branches.size()));
				inset = leftNotchInset(bh, margin, data, currentIndex, forkAtFirst);
				// 换乘斜带现在朝行进方向斜下，圆徽落在线带右前方，末站要向右多留一段
				insetRight = rightStubInset(bh, margin, stubDepthRatio, data);
				lineEnd = lineEndOf(w, hMargin, 0F);
				safeRight = margin + bh * (END_BEVEL_RATIO + EDGE_SAFE_RATIO);
				pitch = pitchOf(lineEnd, hMargin, margin + inset, safeRight + insetRight, stationCount);
				// 支线折行朝右下走，会占掉第二行右侧一段；如果右侧放不下支线车站，
				// 就把主线站距整体缩小（站距仍然一致），把分叉站连同折行落脚点一起往左挪，
				// 而不是让支线车站挤成一团。
				for (int k = 0; k < branches.size(); k++) {
					final LineMapData.Branch br = branches.get(k);
					final int cnt = br.afterStations().isEmpty() ? br.beforeStations().size() : br.afterStations().size();
					final int fi = Math.max(0, Math.min(stationCount - 1, br.forkIndex()));
					if (cnt <= 0 || fi <= 0) {
						continue;
					}
					final float foldFoot = branchHalfOf(bh) + Math.max(bh, rowPitch) + bh;
					final float need = bh * BRANCH_FIRST_AFTER_FOOT_BH
							+ (cnt - 1) * bh * PLAIN_DIAMETER_PITCH_RATIO / PLAIN_DIAMETER_BAND_RATIO;
					final float cap = (lineEnd - hMargin - margin - inset - insetRight - safeRight - foldFoot - need) / fi;
					pitch = Math.min(pitch, Math.max(bh * PLAIN_DIAMETER_PITCH_RATIO / PLAIN_DIAMETER_BAND_RATIO, cap));
				}
			}

			// 主线带：没有支线时居中；有支线时自上而下按「留白 + 站名条 + 站圆 + 线带」排，把下方留给支线
			final float bandCy = hasBranch ? bh * (EDGE_MARGIN_RATIO + NAME_STRIP_BH + BAND_TO_NAME_BH) : h * BAND_CENTER_Y_RATIO;
			final float bandTop = bandCy - bh / 2F;
			final float bandBottom = bandCy + bh / 2F;
			final float bevel = bh * END_BEVEL_RATIO;
			final float circleR = bh * PLAIN_DIAMETER_BAND_RATIO / 2F;
			final float ringW = Math.max(1F, bh * RING_RATIO);
			final float circleCy = bandTop - bh * CIRCLE_ABOVE_RATIO;
			// 本站[圆形组合图形]比普通站圆大，而且要比它更靠左上：
			// 圆心沿本站那条 45° 线往左上退，退到圆不再压住线带为止（与带上缘相切）。
			// 退的方向就是 45° 轴本身，所以轴不动 —— 站名、站序号、站斜槽仍然共线。
			final float currentR = circleR * CURRENT_DIAMETER_RATIO;
			final float currentAxisCy = circleCy + (currentR - circleR);      // 还没退时圆心在轴上的位置
			final float currentShift = Math.max(0F, currentAxisCy + currentR - bandTop);
			final float currentCy = currentAxisCy - currentShift;

			final float[] cx = new float[stationCount];
			for (int i = 0; i < stationCount; i++) {
				cx[i] = stationCount > 1 ? hMargin + margin + inset + i * pitch : (hMargin + lineEnd) / 2F;
			}

			// 每一站那条 45° 线：过站圆圆心、方向与斜槽／换乘斜带／端头斜切一致。
			// 线在带上缘的交点就是本站斜槽的中心，也是换乘斜带的起点，
			// 「站名 → 站序号 → 站斜槽 → 换乘斜带」全在同一条直线上。
			final float[] slotCx = new float[stationCount];
			for (int i = 0; i < stationCount; i++) {
				final float centreY = i == currentIndex ? currentAxisCy : circleCy;
				slotCx[i] = cx[i] + (bandTop - centreY);
			}

			// 支线
			final List<BranchLayout> layouts = new ArrayList<>();
			final float[] forkTopX = new float[stationCount];
			for (int k = 0; k < branches.size(); k++) {
				final LineMapData.Branch branch = branches.get(k);
				// 支线专属车站在分叉站前方就用正向序，只在反方向就用反序
				final List<LineMapData.Station> branchStations = branch.afterStations().isEmpty()
						? reversed(branch.beforeStations()) : branch.afterStations();
				if (branchStations.isEmpty()) {
					continue;
				}
				final int count = branchStations.size();
				final int forkIndex = Math.max(0, Math.min(stationCount - 1, branch.forkIndex()));
				final float rowCy = bandCy + (k + 1F) * rowPitch;
				final float rowTop = rowCy - bh / 2F;
				final float rowBottom = rowCy + bh / 2F;
				// 折行斜带正对分叉站中心、以 45° 穿过主线带，再折到第二行，
				// 方向和斜槽／换乘斜带／端头斜切完全一致（左上 → 右下），整张图只有一个斜向。
				final float forkX = cx[forkIndex];
				final float branchHalf = Math.max(1F, bh * BRANCH_LINE_RATIO) * HALF_SQRT_2;
				final float run = Math.max(bh, rowTop - bandTop);
				// 折行段落到第二行时，斜带左缘／右缘的横坐标
				final float footLeft = forkX - branchHalf + run;
				final float footRight = forkX + branchHalf + run;
				// 折行一路斜到第二行下缘，左缘正好接上第二行带的下缘；并集就是一个简单多边形：
				//   A 带上缘左 → B 带上缘右 → 折行右缘 → 内角圆角 → 第二行带上缘 → 右端斜切
				//   → 第二行带下缘往回 → 外角圆角 → 折行左缘回到 A
				// 两个角用二次贝塞尔（控制点在角点上）削圆，曲线与两条边相切。
				final float fr = Math.max(1F, bh * BRANCH_FILLET_RATIO);
				final Path2D ribbon = new Path2D.Float();
				ribbon.moveTo(forkX - branchHalf, bandTop);
				ribbon.lineTo(forkX + branchHalf, bandTop);
				// 折行右缘 → 内角（与第二行带上缘相交）
				ribbon.lineTo(footRight - fr * HALF_SQRT_2, rowTop - fr * HALF_SQRT_2);
				ribbon.quadTo(footRight, rowTop, footRight + fr, rowTop);
				ribbon.lineTo(lineEnd - bevel, rowTop);
				ribbon.lineTo(lineEnd, rowBottom);
				// 第二行带下缘往回 → 外角（与折行左缘相交）
				ribbon.lineTo(footLeft + bh + fr, rowBottom);
				ribbon.quadTo(footLeft + bh, rowBottom, footLeft + bh - fr * HALF_SQRT_2, rowBottom - fr * HALF_SQRT_2);
				ribbon.closePath();
				forkTopX[forkIndex] = forkX;
				// 支线行的站圆与主线一样大，斜槽也就一样大
				final float branchRadius = bh * PLAIN_DIAMETER_BAND_RATIO / 2F;
				// 支线车站排在第二行带上：左端躲开折行斜带、右端留出缺口与端头斜切的安全距离
				final float firstX = Math.max(hMargin + margin, footLeft + bh * BRANCH_FIRST_AFTER_FOOT_BH);
				final float lastX = Math.max(firstX, lineEnd - safeRight);
				final float[] x = new float[count];
				for (int j = 0; j < count; j++) {
					x[j] = count > 1 ? firstX + (lastX - firstX) * j / (count - 1F) : (firstX + lastX) / 2F;
				}
				layouts.add(new BranchLayout(branchStations, rowCy, ribbon, x,
						branch.forkIndex() < currentIndex ? greyAwt : new Color(branch.lineColor() | 0xFF000000, true),
						forkX, branchHalf, branchRadius));
			}

			final Path2D band = bandPath(hMargin, lineEnd, bandCy, bh, 0F, bevel);
			fillMirrored(g, band, lineAwt, reverse, w);

			final float notchW = bh * NOTCH_WIDTH_RATIO;
			final float notchDepth = bh * NOTCH_DEPTH_RATIO;

			if (currentIndex > 0) {
				fillMirrored(g, bandPath(hMargin, cx[currentIndex], bandCy, bh, 0F, bh), greyAwt, reverse, w);
			}

			final Shape bandClip = g.getClip();
			final float outlineW = Math.max(1.5F, bh * STUB_OUTLINE_RATIO) * 2F;
			g.clip(reverse ? mirror(band, w) : band);
			g.setColor(Color.WHITE);
			g.setStroke(new BasicStroke(outlineW, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
			for (final BranchLayout layout : layouts) {
				for (final float edgeX : new float[]{layout.forkTopX() - layout.halfW(), layout.forkTopX() + layout.halfW()}) {
					// 白缝跟折行斜带同向（左上 → 右下）：横向每一行右移 1
					g.draw(new Line2D.Float(fx(edgeX, reverse, w), bandTop, fx(edgeX + bh, reverse, w), bandBottom));
				}
			}
			g.setClip(bandClip);
			for (final BranchLayout layout : layouts) {
				fillMirrored(g, layout.ribbon(), layout.colour(), reverse, w);
			}

			final Shape oldClip = g.getClip();
			g.setColor(Color.WHITE);
			g.clip(reverse ? mirror(band, w) : band);
			for (int i = 0; i < stationCount; i++) {
				drawNotch(g, slotCx[i] - notchW / 2F, bandTop, notchDepth, notchW, reverse, w);
			}
			g.setClip(oldClip);
			for (final BranchLayout layout : layouts) {
				g.clip(reverse ? mirror(layout.ribbon(), w) : layout.ribbon());
				for (int j = 0; j < layout.x().length; j++) {
					drawNotch(g, layout.x()[j] + bh * CIRCLE_ABOVE_RATIO - notchW / 2F, layout.cy() - bh / 2F,
							notchDepth, notchW, reverse, w);
				}
				g.setClip(oldClip);
			}

			// 换乘斜带
			final Font transferFont = nameFont.deriveFont(Font.PLAIN, Math.max(4F, bh * TRANSFER_FONT_RATIO));
			final Font transferCjkFont = transferFont;
			final Font transferLatinFont = numberFont.deriveFont(Font.PLAIN, Math.max(4F, bh * TRANSFER_FONT_RATIO * 0.8F));
			final float badgeR = bh * BADGE_RADIUS_RATIO;
			final float gapW = bh * 0.06F;
			final float outline = Math.max(0.75F, bh * STUB_OUTLINE_RATIO);
			final float spread = stubHalfOf(bh);
			final float badgeFontSize = Math.max(4F, Math.min(bh * BADGE_FONT_RATIO, badgeR * 1.15F));
			for (int i = 0; i < stationCount; i++) {
				final List<LineMapData.Interchange> interchanges = data.stations().get(i).interchanges();
				if (interchanges.isEmpty()) {
					continue;
				}
				final int count = Math.min(MAX_INTERCHANGE_STUBS, interchanges.size());
				// 圆徽之间的中心间距
				final float step = (badgeR * 2F + gapW) * HALF_SQRT_2;
				final float gapD = (badgeR + gapW) * HALF_SQRT_2;
				final float firstD = bh * (1F + stubDepthRatio) + badgeR;
				final float lastD = firstD + (count - 1) * step;
				final float stationTopX = slotCx[i];
				// 只画一条斜带
				final float bandEndX = stationTopX + firstD;
				final float bandEndY = bandTop + firstD;

				// 白描边只在主线带内部看得见
				g.clip(reverse ? mirror(band, w) : band);
				g.setColor(Color.WHITE);
				fillStub(g, stationTopX, bandTop, bandEndX, bandEndY, spread + outline, reverse, w);
				g.setClip(oldClip);
				// 斜带用第一位换乘线路的颜色
				g.setColor(new Color(interchanges.get(0).color() | 0xFF000000, true));
				fillStub(g, stationTopX, bandTop, bandEndX, bandEndY, spread, reverse, w);
				// 各线路的圆徽沿线依次往下排（圆之间没有带子）
				for (int k = 0; k < count; k++) {
					final LineMapData.Interchange interchange = interchanges.get(k);
					final float d = firstD + k * step;
					final float badgeX = fx(stationTopX + d, reverse, w);
					final float badgeY = bandTop + d;
					g.setColor(new Color(interchange.color() | 0xFF000000, true));
					g.fill(new Ellipse2D.Float(badgeX - badgeR, badgeY - badgeR, badgeR * 2F, badgeR * 2F));
					drawCentredText(g, interchange.number(), badgeX, badgeY,
							pickFont(interchange.number(), numberFont, nameFont).deriveFont(Font.BOLD, badgeFontSize), Color.WHITE);
				}
				// 末尾
				drawTransferLabel(g, transferCjkFont, transferLatinFont, textAwt,
						fx(stationTopX + lastD + gapD, reverse, w), bandTop + lastD + gapD,
						reverse ? -Math.PI * 0.25 : Math.PI * 0.25, !reverse);
			}

			// 车站圆
			final Font nodeFont = numberFont.deriveFont(Font.BOLD, Math.max(4F, Math.min(bh * NUMBER_BAND_RATIO, numberFontSize * resolutionScale)));
			final Font currentNodeFont = numberFont.deriveFont(Font.PLAIN, Math.max(4F, currentR * NODE_FONT_RADIUS_RATIO));
			final Font currentNodeTextFont = nameFont.deriveFont(Font.PLAIN, Math.max(4F, currentR * NODE_FONT_RADIUS_RATIO));
			for (int i = 0; i < stationCount; i++) {
				// 本站圆心已经沿 45° 轴往左上退过，横向也跟着退同样多（45°）
				final float x = fx(i == currentIndex ? cx[i] - currentShift : cx[i], reverse, w);
				if (i == currentIndex) {
					// 本站
					RoundMapDrawingInverse.draw(g, Math.round(x), Math.round(currentCy), Math.round(currentR), Math.max(1, Math.round(ringW)), lineAwt, lineAwt,
							currentNodeFont, currentNodeTextFont,
							RouteMapGenerator.cleanRouteNumber(data.lineNumber()), data.stations().get(i).number());
				} else {
					// 已经过的车站圆环与站号同样用中性灰
					drawNode(g, x, circleCy, circleR, ringW, i < currentIndex ? greyAwt : lineAwt,
							data.stations().get(i).number(), pickFont(data.stations().get(i).number(), nodeFont, nameFont));
				}
			}
			// 支线车站圆与主线同款：白底圆 + 支线色环 + 支线站号，压在第二行线带的上缘
			for (final BranchLayout layout : layouts) {
				final float rowCircleCy = layout.cy() - bh / 2F - bh * CIRCLE_ABOVE_RATIO;
				for (int j = 0; j < layout.x().length; j++) {
					final LineMapData.Station station = layout.stations().get(j);
					drawNode(g, fx(layout.x()[j], reverse, w), rowCircleCy, circleR, ringW, layout.colour(),
							station.number(), pickFont(station.number(), nodeFont, nameFont));
				}
			}

			// 站名
			final float[] bandPitch = new float[1 + layouts.size()];
			bandPitch[0] = pitch;
			final List<NameSlot> slots = new ArrayList<>();
			final float topLimit = bh * EDGE_MARGIN_RATIO;
			for (int i = 0; i < stationCount; i++) {
				// 锚点 = 本站圆沿那条 45° 线往「远离线带」一侧退到圆外一点
				final float r = i == currentIndex ? currentR : circleR;
				// 本站圆心已在轴上往左上退过，锚点按退过之后的圆心算，站名才不会压到圆上
				final float shift = i == currentIndex ? currentShift : 0F;
				final float cy = i == currentIndex ? currentCy : circleCy;
				final float off = (r + bh * NAME_ANCHOR_GAP_RATIO) * HALF_SQRT_2;
				slots.add(new NameSlot(data.stations().get(i), cx[i] - shift - off, cy - off, false,
						i == currentIndex, 0, i == currentIndex ? lineAwt : textAwt, topLimit));
			}
			// 支线站名挤在两行线带之间：下界是主线带的换乘圆徽区，不许压上去
			final float branchLimit = bandBottom + bh * (stubDepthRatio + BADGE_RADIUS_RATIO + 0.25F);
			for (int k = 0; k < layouts.size(); k++) {
				final BranchLayout layout = layouts.get(k);
				final float rowCircleCy = layout.cy() - bh / 2F - bh * CIRCLE_ABOVE_RATIO;
				final float off = (circleR + bh * NAME_ANCHOR_GAP_RATIO) * HALF_SQRT_2;
				bandPitch[k + 1] = layout.x().length > 1
						? (layout.x()[layout.x().length - 1] - layout.x()[0]) / (layout.x().length - 1F) : Float.MAX_VALUE;
				for (int j = 0; j < layout.x().length; j++) {
					slots.add(new NameSlot(layout.stations().get(j), layout.x()[j] - off, rowCircleCy - off, false, false,
							k + 1, textAwt, branchLimit));
				}
			}
			drawStationNames(g, slots, h, w, hMargin, bandPitch, bh, nameFont, nameFontSize, latinFontRatio, gapRatio, resolutionScale, reverse);

			g.dispose();
			return img;
		} catch (Exception e) {
			LOGGER.error("Failed to render fascia line map ({} stations)", data == null ? 0 : data.stations().size(), e);
			return null;
		}
	}

	private record BranchLayout(List<LineMapData.Station> stations, float cy, Shape ribbon, float[] x, Color colour,
			float forkTopX, float halfW, float circleR) {
	}

	private record NameSlot(LineMapData.Station station, float x, float anchorY, boolean below, boolean current, int band,
			Color colour, float limitY) {
	}

	private static float lineEndOf(int w, float hMargin, float greyW) {
		return Math.max(1F, w - hMargin - greyW);
	}

	private static float pitchOf(float lineEnd, float hMargin, float leftMargin, float rightMargin, int stationCount) {
		// 首末站都内缩同样的留白：末站的斜切槽才不会被线带端头切掉
		final float usable = Math.max(1F, lineEnd - hMargin - leftMargin - rightMargin);
		return stationCount > 1 ? usable / (stationCount - 1) : usable;
	}

	private static float rightStubInset(float bh, float margin, float stubDepthRatio, LineMapData data) {
		if (data.stations().isEmpty()) {
			return 0F;
		}
		final int last = data.stations().size() - 1;
		if (data.stations().get(last).interchanges().isEmpty()) {
			return 0F;
		}
		final float drift = bh * (1F + stubDepthRatio - STUB_START_RATIO) * STUB_SLOPE;
		final float groupHalf = stubGroupHalfOf(bh, Float.MAX_VALUE, data.stations().get(last).interchanges().size());
		return Math.max(0F, bh * STUB_TOP_OFFSET_RATIO + groupHalf + drift
				+ bh * BADGE_RADIUS_RATIO - margin);
	}


	private static float leftNotchInset(float bh, float margin, LineMapData data, int currentIndex, boolean forkAtFirst) {
		if (data.stations().isEmpty()) {
			return 0F;
		}
		final int stubCount = Math.min(MAX_INTERCHANGE_STUBS, data.stations().get(0).interchanges().size());
		final float notchW = bh * NOTCH_WIDTH_RATIO;
		// 槽左缘相对首站中心的距离（带正负号），整条槽都要离线带左端头留出安全距离
		final float notchLeft;
		if (forkAtFirst) {
			notchLeft = -branchHalfOf(bh) - notchW;
		} else if (stubCount > 0) {
			// 换乘斜带的整组左缘 = 槽心 - 组半宽 - 斜带半宽（比槽本身还靠左）
			notchLeft = bh * STUB_TOP_OFFSET_RATIO - stubGroupHalfOf(bh, Float.MAX_VALUE, stubCount)
					- stubHalfOf(bh);
		} else if (currentIndex == 0) {
			// 本站圆更大、圆心更低，那条 45° 线与带上缘的交点也更靠左
			notchLeft = bh * (CIRCLE_ABOVE_RATIO - PLAIN_DIAMETER_BAND_RATIO / 2F * (CURRENT_DIAMETER_RATIO - 1F))
					- notchW / 2F;
		} else {
			notchLeft = bh * (CIRCLE_ABOVE_RATIO - NOTCH_WIDTH_RATIO / 2F);
		}
		// 尾端是竖直矩形收尾，最左的点就是 hMargin，不需要再额外让出端头斜切的投影
		return Math.max(0F, -notchLeft + bh * EDGE_SAFE_RATIO - margin);
	}

	// 是否有支线从首站分叉出去
	private static boolean forkAtFirst(List<LineMapData.Branch> branches) {
		for (final LineMapData.Branch branch : branches) {
			if (branch.forkIndex() <= 0 && (!branch.afterStations().isEmpty() || !branch.beforeStations().isEmpty())) {
				return true;
			}
		}
		return false;
	}

	private static float stubHalfOf(float bh) {
		return Math.max(1F, bh * STUB_LINE_RATIO) * HALF_SQRT_2;
	}

	private static float branchHalfOf(float bh) {
		return Math.max(1F, bh * BRANCH_LINE_RATIO) * HALF_SQRT_2;
	}

	private static void roundCorner(Area area, float px, float py, float bx, float by, float r) {
		final float d = (float) (r / Math.sin(Math.toRadians(67.5)));
		area.subtract(new Area(new Ellipse2D.Float(px + bx * d - r, py + by * d - r, r * 2F, r * 2F)));
	}

	private static float stubGroupHalfOf(float bh, float pitch, int interchangeCount) {
		final int count = Math.min(MAX_INTERCHANGE_STUBS, interchangeCount);
		if (count <= 1) {
			return 0F;
		}
		final float gapW = bh * 0.06F;
		final float groupR = Math.min(bh * BADGE_RADIUS_RATIO, Math.max(bh * 0.12F, (pitch - gapW) / (count * 2F)));
		return (count - 1) / 2F * (groupR * 2F + gapW);
	}

	private static void fillStub(Graphics2D g, float startX, float startY, float endX, float endY, float halfW,
			boolean reverse, int w) {
		final Path2D stub = new Path2D.Float();
		stub.moveTo(startX - halfW, startY);
		stub.lineTo(startX + halfW, startY);
		stub.lineTo(endX + halfW, endY);
		stub.lineTo(endX - halfW, endY);
		stub.closePath();
		fillMirrored(g, stub, null, reverse, w);
	}

	private static float rowSpaceOf(int h, float bh) {
		return Math.max(bh, h - bh * (2F * EDGE_MARGIN_RATIO + NAME_STRIP_BH + BAND_TO_NAME_BH + 0.5F));
	}

	private static float leftBranchInset(float bh, float margin, float rowPitch, float pitch, List<LineMapData.Branch> branches) {
		if (branches.isEmpty()) {
			return 0F;
		}
		int deepestRow = 1;
		int leftmostFork = Integer.MAX_VALUE;
		for (int k = 0; k < branches.size(); k++) {
			deepestRow = Math.max(deepestRow, k + 1);
			leftmostFork = Math.min(leftmostFork, Math.max(0, branches.get(k).forkIndex()));
		}
		// 折行斜带从分叉站中心穿过主线带
		final float run = deepestRow * rowPitch + bh * (EDGE_SAFE_RATIO + BRANCH_LINE_RATIO) * HALF_SQRT_2;
		return Math.max(0F, run - margin - leftmostFork * pitch);
	}

	private static float clamp(float value, float min, float max) {
		return value < min ? min : Math.min(value, max);
	}

	private static Path2D bandPath(float x1, float x2, float cy, float height, float leftBevel, float rightBevel) {
		final float top = cy - height / 2F;
		final float bottom = cy + height / 2F;
		final Path2D path = new Path2D.Float();
		path.moveTo(x1, top);
		path.lineTo(x2 - rightBevel, top);
		path.lineTo(x2, bottom);
		path.lineTo(x1 + leftBevel, bottom);
		path.closePath();
		return path;
	}

	private static List<LineMapData.Station> reversed(List<LineMapData.Station> stations) {
		final List<LineMapData.Station> reversed = new ArrayList<>(stations);
		java.util.Collections.reverse(reversed);
		return reversed;
	}

	private static float fx(float x, boolean reverse, int w) {
		return reverse ? w - x : x;
	}

	private static Shape mirror(Shape shape, int w) {
		return AffineTransform.getTranslateInstance(w, 0).createTransformedShape(
				AffineTransform.getScaleInstance(-1, 1).createTransformedShape(shape));
	}

	private static void fillMirrored(Graphics2D g, Shape shape, Color color, boolean reverse, int w) {
		if (color != null) {
			g.setColor(color);
		}
		g.fill(reverse ? mirror(shape, w) : shape);
	}

	private static void drawNotch(Graphics2D g, float leftX, float edgeY, float height, float width, boolean reverse, int w) {
		final Path2D notch = new Path2D.Float();
		notch.moveTo(leftX, edgeY - 1F);
		notch.lineTo(leftX + width, edgeY - 1F);
		notch.lineTo(leftX + width + height, edgeY + height);
		notch.lineTo(leftX + height, edgeY + height);
		notch.closePath();
		fillMirrored(g, notch, null, reverse, w);
	}

	private static void drawNode(Graphics2D g, float x, float y, float r, float ringW, Color lineAwt,
			String number, Font font) {
		final float fillR = Math.max(1F, r - ringW);
		g.setColor(Color.WHITE);
		g.fill(new Ellipse2D.Float(x - fillR, y - fillR, fillR * 2F, fillR * 2F));
		g.setColor(lineAwt);
		g.setStroke(new BasicStroke(ringW));
		final float strokeR = r - ringW / 2F;
		g.draw(new Ellipse2D.Float(x - strokeR, y - strokeR, strokeR * 2F, strokeR * 2F));
		drawCentredText(g, number, x, y, font, lineAwt);
	}

	private static Font pickFont(String text, Font numberFont, Font textFont) {
		return text != null && FontRenderUtils.isNumeric(text) ? numberFont : textFont;
	}

	private static void drawStationNames(Graphics2D g, List<NameSlot> slots, int h, int w, float hMargin,
			float[] bandPitch, float bh, Font nameFont, int nameFontSize, float latinFontRatio, float gapRatio,
			float resolutionScale, boolean reverse) {
		final float cjkPx = Math.min(nameFontSize * resolutionScale, bh * NAME_CJK_BAND_RATIO);
		final int cjkRef = Math.max(1, Math.round(cjkPx / resolutionScale));
		final CRTStationInfoBandRenderer.TextBlock[] blocks = new CRTStationInfoBandRenderer.TextBlock[slots.size()];

		// 先按最长/最高的站名求一个统一缩放，保证一排站名同样大小
		float nameScale = 1F;
		for (int i = 0; i < slots.size(); i++) {
			final NameSlot slot = slots.get(i);
			// 英文按单词换行，行宽不超过中文那一行 —— 英文永远不会比中文更长，
			// 也就不会斜着伸出去压到别的站名／圆徽上
			final CRTStationInfoBandRenderer.TextBlock raw = CRTStationInfoBandRenderer.buildTextBlock(
					slot.station().name(), nameFont, cjkRef, latinFontRatio, gapRatio, slot.colour(), resolutionScale,
					Integer.MAX_VALUE, true, false);
			final CRTStationInfoBandRenderer.TextBlock block = raw.latinLines.isEmpty() || raw.cjkW <= 0 ? raw
					: CRTStationInfoBandRenderer.buildTextBlock(slot.station().name(), nameFont, cjkRef, latinFontRatio,
							gapRatio, slot.colour(), resolutionScale, raw.cjkW, true, false);
			blocks[i] = block;
			if (block.width > 0 && block.height > 0) {
				// 站名沿着这条 45° 线往「远离线带」一侧铺开：沿线方向占的竖向高度 = width/√2，
				// 横向占的宽度 = (width + height)/√2（法向那几行也要算进去）
				final float alongExtent = block.width * HALF_SQRT_2;
				final float acrossExtent = (block.width + block.height) * HALF_SQRT_2;
				// 站名条带也要留出画布留白，不然斜排的最后一截会被纹理边缘切掉；
				// 支线站的站名还要受「不许压到主线带那排换乘圆徽」那条界限制
				final float available = slot.below() ? h - bh * EDGE_MARGIN_RATIO - slot.anchorY() : slot.anchorY() - slot.limitY();
				nameScale = Math.min(nameScale, available / alongExtent);
				// 站名往画布边上斜铺，铺到画布边为止（实拍里端头的站名也是贴着边走的）
				nameScale = Math.min(nameScale, (w - slot.x()) / acrossExtent);
				nameScale = Math.min(nameScale, slot.x() / acrossExtent);
				// 同一条带上相邻站名平行排布，两行之间的法向距离 = 站距/√2，站名厚度不能超过它
				nameScale = Math.min(nameScale, bandPitch[slot.band()] * NAME_PITCH_FILL_RATIO / (block.height * HALF_SQRT_2));
			}
		}
		nameScale = Math.max(MIN_NAME_SCALE, Math.min(1F, nameScale));

		for (int i = 0; i < slots.size(); i++) {
			final NameSlot slot = slots.get(i);
			final CRTStationInfoBandRenderer.TextBlock block = blocks[i];
			if (block == null || block.height <= 0) {
				continue;
			}
			// 站名和斜槽、换乘斜带、端头斜切同一个方向：向右行进时是「左上 → 右下」
			final double angle = reverse ? -Math.PI * 0.25 : Math.PI * 0.25;
			// 锚点已经退到圆外，整块沿线的「远离线带」方向铺开
			final boolean awayPositive = slot.below() != reverse;
			final AffineTransform old = g.getTransform();
			g.translate(fx(slot.x(), reverse, w), slot.anchorY());
			g.rotate(angle);
			drawInlineLabel(g, block, nameScale, awayPositive);
			g.setTransform(old);
		}
	}

	private static void drawInlineLabel(Graphics2D g, CRTStationInfoBandRenderer.TextBlock block, float scale,
			boolean awayPositive) {
		g.scale(scale, scale);
		g.setColor(block.color);
		final float x = awayPositive ? 0F : -block.width;
		float y = 0F;
		if (block.hasCjk) {
			g.setFont(block.cjkFont);
			g.drawString(block.cjk, x, 0F);
			y = inkBelow(block.cjk, block.cjkFont, g);
		}
		for (final String line : block.latinLines) {
			g.setFont(block.latinFont);
			y += inkAbove(line, block.latinFont, g);
			g.drawString(line, x, y);
			y += inkBelow(line, block.latinFont, g);
		}
		g.scale(1F / scale, 1F / scale);
	}

	private static float inkAbove(String text, Font font, Graphics2D g) {
		return Math.max(0F, -inkExtent(text, font)[0]);
	}

	private static float inkBelow(String text, Font font, Graphics2D g) {
		return Math.max(0F, inkExtent(text, font)[1]);
	}

	private static float[] inkExtent(String text, Font font) {
		final BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D metricsG = probe.createGraphics();
		metricsG.setFont(font);
		final FontMetrics fm = metricsG.getFontMetrics(font);
		metricsG.dispose();
		final int w = Math.max(1, fm.stringWidth(text) + 4);
		final int h = Math.max(1, fm.getHeight() + 4);
		final int baseline = fm.getAscent() + 2;
		final BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D probeG = img.createGraphics();
		probeG.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		probeG.setFont(font);
		probeG.setColor(Color.BLACK);
		probeG.drawString(text, 2, baseline);
		probeG.dispose();
		int top = Integer.MAX_VALUE;
		int bottom = Integer.MIN_VALUE;
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				if ((img.getRGB(x, y) >>> 24) > 8) {
					top = Math.min(top, y);
					bottom = Math.max(bottom, y);
					break;
				}
			}
		}
		if (top == Integer.MAX_VALUE) {
			return new float[]{0F, 0F};
		}
		return new float[]{top - baseline, bottom - baseline};
	}

	private static void drawCentredText(Graphics2D g, String text, float cx, float cy, Font font, Color color) {
		if (text == null || text.isEmpty()) {
			return;
		}
		g.setFont(font);
		g.setColor(color);
		final FontMetrics metrics = g.getFontMetrics(font);
		g.drawString(text, cx - metrics.stringWidth(text) / 2F, cy + (metrics.getAscent() - metrics.getDescent()) / 2F);
	}

	private static void drawRotatedText(Graphics2D g, String text, Font font, Color color, float x, float y, double angle) {
		if (text == null || text.isEmpty()) {
			return;
		}
		final AffineTransform old = g.getTransform();
		g.translate(x, y);
		g.rotate(angle);
		g.setFont(font);
		g.setColor(color);
		g.drawString(text, 0F, FontRenderUtils.getAscent(font));
		g.setTransform(old);
	}

	private static void drawTransferLabel(Graphics2D g, Font cjkFont, Font latinFont, Color color,
			float x, float y, double angle, boolean awayPositive) {
		final AffineTransform old = g.getTransform();
		g.translate(x, y);
		g.rotate(angle);
		g.setColor(color);
		// 镜像布局里文字要往「远离圆徽」的那一侧铺（-x），否则会倒着压回圆徽上
		final float left = awayPositive ? 0F : -Math.max(FontRenderUtils.getStringWidth(TRANSFER_LABEL, cjkFont),
				FontRenderUtils.getStringWidth(TRANSFER_LATIN, latinFont));
		g.setFont(cjkFont);
		g.drawString(TRANSFER_LABEL, left, 0F);
		g.setFont(latinFont);
		g.drawString(TRANSFER_LATIN, left, inkBelow(TRANSFER_LABEL, cjkFont, g) + inkAbove(TRANSFER_LATIN, latinFont, g));
		g.setTransform(old);
	}
}
