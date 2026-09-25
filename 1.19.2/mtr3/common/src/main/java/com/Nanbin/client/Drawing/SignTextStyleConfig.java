package com.Nanbin.client.Drawing;

import java.util.HashMap;
import java.util.Map;

/**
 * 从 nanbin:signs.json 中解析出来的、随告示牌标志一起注册的自定义文字样式。
 * <p>
 * MTR 原版 {@code SignResource} 只识别 id/textureResource/backgroundColor/customText/
 * flipTexture/flipCustomText/small，因此 textColor/textSize/textBold 由
 * {@link com.Nanbin.mixin.CustomResourceLoaderMixin} 单独读取并存放到这里，
 * 渲染器（RenderCRTRailwaySign）据此调整线路编号等纯文字格子的显示。
 */
public final class SignTextStyleConfig {

	private static final Map<String, SignTextStyle> STYLES = new HashMap<>();

	private SignTextStyleConfig() {
	}

	public static void clear() {
		STYLES.clear();
	}

	public static void put(String signId, SignTextStyle style) {
		STYLES.put(signId, style);
	}

	/** 返回指定标志的自定义文字样式；未配置时返回 null。 */
	public static SignTextStyle get(String signId) {
		return STYLES.get(signId);
	}

	/**
	 * 解析 "#RRGGBB" / "#AARRGGBB"（也允许不带 #）为 ARGB int。
	 * 6 位十六进制补全为不透明（0xFFRRGGBB）；解析失败返回 0（表示未设置）。
	 */
	public static int parseColor(String color) {
		if (color == null || color.isEmpty()) {
			return 0;
		}
		try {
			final String hex = color.startsWith("#") ? color.substring(1) : color;
			if (hex.length() == 6) {
				return (int) Long.parseLong(hex, 16) | 0xFF000000;
			} else if (hex.length() == 8) {
				return (int) Long.parseLong(hex, 16);
			}
		} catch (NumberFormatException ignored) {
		}
		return 0;
	}

	/** 单个标志的自定义文字样式（textColor 为 0 表示未设置，沿用自动黑/白）。 */
	public static final class SignTextStyle {

		public final int textColor;
		/** 字号倍率，1.0 表示默认（线路编号默认填满整格）。 */
		public final float textSize;
		/** 是否加粗。 */
		public final boolean textBold;

		public SignTextStyle(int textColor, float textSize, boolean textBold) {
			this.textColor = textColor;
			this.textSize = textSize;
			this.textBold = textBold;
		}

		public boolean hasColor() {
			return textColor != 0;
		}
	}
}
