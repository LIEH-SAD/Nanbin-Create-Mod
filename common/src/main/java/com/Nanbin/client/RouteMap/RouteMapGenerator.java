package com.Nanbin.client.RouteMap;

import mtr.client.ClientData;
import mtr.client.Config;
import mtr.data.IGui;
import mtr.data.Route;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * 站台线路数据解析器（MTR 3.2.2 版本 API）。
 * 供站名牌/线路图渲染使用：根据站台 ID 解析线路颜色、线路编号等数据。
 * 数据源为 {@link ClientData#ROUTES} / {@link ClientData#PLATFORMS} / {@link ClientData#STATIONS}（客户端缓存集合）。
 */
public class RouteMapGenerator implements IGui {
	protected static int scale;
	protected static int lineSize;
	protected static int lineSpacing;
	protected static int fontSizeBig;
	protected static int fontSizeSmall;

	public static final int MIN_VERTICAL_SIZE = 5;

	public static void setConstants() {
		scale = (int) Math.pow(2, Config.dynamicTextureResolution() + 5);
		lineSize = scale / 8;
		lineSpacing = lineSize * 3 / 2;
		fontSizeBig = lineSize * 2;
		fontSizeSmall = fontSizeBig / 2;
	}

	/** 根据站台 ID 获取途经线路的颜色列表（途经线路优先，全部为终点时退化为终点线路颜色）。 */
	protected static List<Integer> getRouteColors(long platformId, BiConsumer<Route, Integer> nonTerminatingCallback) {
		final List<Integer> colors = new ArrayList<>();
		final List<Integer> terminatingColors = new ArrayList<>();
		ClientData.ROUTES.stream().filter(route -> route.getPlatformIdIndex(platformId) >= 0).sorted().forEach(route -> {
			final int currentStationIndex = route.getPlatformIdIndex(platformId);
			if (currentStationIndex < route.platformIds.size() - 1) {
				nonTerminatingCallback.accept(route, currentStationIndex);
				if (!colors.contains(route.color)) {
					colors.add(route.color);
				}
			} else {
				if (!terminatingColors.contains(route.color)) {
					terminatingColors.add(route.color);
				}
			}
		});
		if (colors.isEmpty()) {
			colors.addAll(terminatingColors);
		}
		return colors;
	}

	protected record StationPosition(float x, float y, boolean isCommon) { }

	protected record StationPositionGrouped(StationPosition stationPosition, int stationOffset, List<Integer> interchangeColors, List<String> interchangeNames) { }

	/**
	 * CRT 站名牌（样式 1）的布局规范。
	 * 布局规范属于具体方块，因此集中定义在此处；{@code CustomFontTextureCache}
	 * 只负责按本规范通用绘制贴图，不内嵌任何方块专属尺寸。
	 */
	public static final class StationNameLayout {
		/** 贴图纹理内部布局比例（相对纹理高度 H 的比例） */
		public static final float TOP_BAR_END = 0.10F;
		public static final float TEXT_END = 0.55F;
		public static final float CIRCLE_CENTER_Y = 0.70F;
		public static final float BOTTOM_BAR_START = 0.70F;
		public static final float CIRCLE_RADIUS_H = 0.10F;
		public static final float CIRCLE_RADIUS_W = 0.14F;
		public static final float CIRCLE_STROKE_H = 0.008F;
		public static final float CIRCLE_FONT_RATIO = 0.35F;
		public static final float CIRCLE_TEXT_LEADING_RATIO = 0.15F;
		/** 文字垂直定位微调（相对文字区域高度的偏移系数 / 相对纹理高度的附加偏移） */
		public static final float TEXT_VERTICAL_ALIGN = 0.65F;
		public static final float TEXT_BASE_Y_OFFSET = 0.05F;

		/** 世界绘制尺寸（方块正面文字贴图区域） */
		public static final float BG_WIDTH = 1.4F;
		public static final float BG_HEIGHT = 1.6F;
		public static final float TEXT_MARGIN = 0.05F;
		public static final float TEXT_SCALE = 0.6F;
		public static final float Z_FROM_CENTER = 0.459375F;

		private StationNameLayout() {
		}

		/** 文字贴图的宽高比（由方块正面的绘制尺寸推导） */
		public static float computeAspect() {
			return (BG_WIDTH - TEXT_MARGIN * 2) / (BG_HEIGHT - TEXT_MARGIN * 2);
		}

		/**
		 * 根据文字宽度动态调整水平边距比例（相对纹理宽度 W 的比例）。
		 * 文字越窄，左右留白越多，避免大字居中时贴边。
		 */
		public static float computeHorizontalMargin(float maxLineWidth, float textureWidth) {
			if (maxLineWidth > 0 && maxLineWidth < textureWidth * 0.3F) {
				return 0.20F;
			}
			if (maxLineWidth > 0 && maxLineWidth < textureWidth * 0.5F) {
				return 0.12F;
			}
			return 0.04F;
		}
	}
}
