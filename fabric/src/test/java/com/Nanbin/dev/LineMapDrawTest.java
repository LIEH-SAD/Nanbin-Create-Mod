package com.Nanbin.dev;

import com.Nanbin.client.Drawing.FasciaCRTRouteMapRenderer;
import com.Nanbin.client.Drawing.LineMapData;

import javax.imageio.ImageIO;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class LineMapDrawTest {

	private LineMapDrawTest() {
	}

	private static final int LINE_COLOR = 0xFFFF0000;
	private static final int TEXT_COLOR = 0xFF111111;
	private static final float BAND_HEIGHT = 0.6F;
	private static final float GROUP_BLOCKS = 3F;
	private static final int NAME_FONT_SIZE = 114;
	private static final int NUMBER_FONT_SIZE = 60;
	private static final float LATIN_FONT_RATIO = 13F / 24F;
	private static final float GAP_RATIO = 0.45F;

	public static void main(String[] args) throws Exception {
		final File outDir = new File(args.length > 0 ? args[0] : "build/draw-test");
		//noinspection ResultOfMethodCallIgnored
		outDir.mkdirs();
		final int height = args.length > 1 ? Integer.parseInt(args[1]) : 768;

		final Font nameFont = loadFont("src/main/resources/assets/nanbin/font/sourcehansanssc_bold.otf");
		final Font numberFont = loadFont("src/main/resources/assets/nanbin/font/sourcesans3_bold.otf");

		// 无支线（对照组）
		draw(outDir, "01-no-branch", height, nameFont, numberFont, new LineMapData(LINE_COLOR, "1", 4, mainStations(), List.of()));
		// 只有一条支线（当前实机那种：分叉站就是本站）
		draw(outDir, "02-branch-1", height, nameFont, numberFont,
				new LineMapData(LINE_COLOR, "1", 4, mainStations(), List.of(branch(4, 1))));
		// 支线多站
		draw(outDir, "03-branch-3", height, nameFont, numberFont,
				new LineMapData(LINE_COLOR, "1", 4, mainStations(), List.of(branch(4, 3))));
		// 两条支线
		draw(outDir, "04-branch-1x2", height, nameFont, numberFont,
				new LineMapData(LINE_COLOR, "1", 4, mainStations(), List.of(branch(4, 1), branch(4, 2))));
		// 分叉站在中间（支线带更长的一段）
		draw(outDir, "05-branch-fork-mid", height, nameFont, numberFont,
				new LineMapData(LINE_COLOR, "1", 4, mainStations(), List.of(branch(2, 2))));
		// 本站就是首站（分叉站贴左边缘的兜底）
		draw(outDir, "06-current-first", height, nameFont, numberFont,
				new LineMapData(LINE_COLOR, "1", 0, mainStations(), List.of(branch(0, 2))));
		// 反向（镜像）
		draw(outDir, "07-branch-reversed", height, nameFont, numberFont, true,
				new LineMapData(LINE_COLOR, "1", 4, mainStations(), List.of(branch(4, 1))));
		// 最长的站名 + 长支线，看站名缩放兜底
		draw(outDir, "08-long-names", height, nameFont, numberFont,
				new LineMapData(LINE_COLOR, "1", 3, longNameStations(), List.of(branch(2, 2))));
		// 环线（胶囊轮廓）：上下两行车站，两端半圆接上
		draw(outDir, "09-loop", height, nameFont, numberFont,
				new LineMapData(LINE_COLOR, "环线", 5, loopStations(16), List.of(), true));
		// 环线反向（镜像）
		draw(outDir, "10-loop-reversed", height, nameFont, numberFont, true,
				new LineMapData(LINE_COLOR, "环线", 9, loopStations(12), List.of(), true));

		System.out.println("draw test done -> " + outDir.getAbsolutePath());
	}

	/** 与实机一致的一条 6 站线路（本站 = 第 5 站，带两个换乘）。 */
	private static List<LineMapData.Station> mainStations() {
		return List.of(
				station("出生点|Spawn", "01"),
				station("村庄1|Village 1", "02"),
				station("村庄3|Village 3", "03", interchange(0xFFFFDA00, "2")),
				station("田所驴の粪坑|Tiansuo Pit", "04"),
				station("村庄管理部(村庄2)|Village Management Office", "1"),
				station("左卿|Zuoqing", "06", interchange(0xFF1E6ECE, "T1")));
	}

	/** 长站名 + 多个换乘，用来压测站名缩放与换乘圆徽排布。 */
	private static List<LineMapData.Station> longNameStations() {
		return List.of(
				station("重庆北站南广场|Chongqingbei Railway Station South Square", "01",
						interchange(0xFF73BF70, "8"), interchange(0xFF00A9A9, "24"), interchange(0xFF8E44AD, "27")),
				station("长生桥|Changshengqiao", "02", interchange(0xFFE5007F, "20"), interchange(0xFFB49A6B, "环线")),
				station("刘家坪|Liujiaping", "03", interchange(0xFF1E6ECE, "1"), interchange(0xFF00A9A9, "18")),
				station("上新街|Shangxinjie", "04", interchange(0xFF73BF70, "26")),
				station("小什字|Xiaoshizi", "05"));
	}

	private static LineMapData.Branch branch(int forkIndex, int stationCount) {
		final List<LineMapData.Station> afterStations = new ArrayList<>();
		for (int i = 0; i < stationCount; i++) {
			afterStations.add(station("支线" + (i + 1) + "站|Branch " + (i + 1), String.format("%02d", i + 1)));
		}
		return new LineMapData.Branch(forkIndex, LINE_COLOR, "1", afterStations, List.of());
	}

	/** 环线车站：每隔几站挂一个换乘，看胶囊内部圆徽的排布。 */
	private static List<LineMapData.Station> loopStations(int count) {
		final List<LineMapData.Station> stations = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			final String number = String.format("%02d", i + 1);
			final String name = "环线" + (i + 1) + "站|Loop Station " + (i + 1);
			if (i % 5 == 3) {
				stations.add(station(name, number, interchange(0xFF73BF70, "8"), interchange(0xFFE5007F, "20")));
			} else if (i % 4 == 1) {
				stations.add(station(name, number, interchange(0xFF1E6ECE, "2")));
			} else {
				stations.add(station(name, number));
			}
		}
		return stations;
	}

	private static LineMapData.Station station(String name, String number, LineMapData.Interchange... interchanges) {
		return new LineMapData.Station(name, number, List.of(interchanges));
	}

	private static LineMapData.Interchange interchange(int color, String number) {
		return new LineMapData.Interchange(color, number);
	}

	private static void draw(File outDir, String name, int height, Font nameFont, Font numberFont, LineMapData data) throws Exception {
		draw(outDir, name, height, nameFont, numberFont, false, data);
	}

	private static void draw(File outDir, String name, int height, Font nameFont, Font numberFont, boolean reverse, LineMapData data)
			throws Exception {
		final BufferedImage image = FasciaCRTRouteMapRenderer.renderImage(data, GROUP_BLOCKS, BAND_HEIGHT, height,
				nameFont, NAME_FONT_SIZE, numberFont, NUMBER_FONT_SIZE, LATIN_FONT_RATIO, GAP_RATIO, TEXT_COLOR, reverse);
		if (image == null) {
			System.out.println("!! " + name + " 渲染失败");
			return;
		}
		final File file = new File(outDir, name + ".png");
		ImageIO.write(image, "png", file);
		System.out.printf("%-20s %4dx%-4d  %s%n", name, image.getWidth(), image.getHeight(), file.getAbsolutePath());
	}

	private static Font loadFont(String path) throws Exception {
		final File file = new File(path);
		if (!file.isFile()) {
			throw new IllegalStateException("找不到字体：" + file.getAbsolutePath() + "（请在 fabric 工程目录下运行）");
		}
		return Font.createFont(Font.TRUETYPE_FONT, file);
	}
}
