package com.Nanbin.client.Drawing;

import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.NativeImage;
import org.mtr.mapping.holder.NativeImageBackedTexture;
import org.mtr.mod.client.DynamicTextureCache;

import java.awt.*;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.Nanbin.InitClient.LOGGER;

public class CustomFontTextureCache {

	public static final CustomFontTextureCache instance = new CustomFontTextureCache();

	public enum FontType {
		ALIBABA("Alibaba PuHuiTi", "/assets/nanbin/font/alibabapuhuiti_regular.ttf"),
		SOURCE_HAN("Source Han Sans SC Bold", "/assets/nanbin/font/sourcehansanssc_bold.otf"),
		SOURCE_SANS_3("Source Sans 3", "/assets/nanbin/font/sourcesans3_regular.otf");

		public final String displayName;
		public final String path;

		FontType(String displayName, String path) {
			this.displayName = displayName;
			this.path = path;
		}
	}

	private final Map<FontType, Font> rawFonts = new HashMap<>();
	private final Map<String, Identifier> textureCache = new HashMap<>();
	private final Map<String, FittedTextTexture> fittedTextureCache = new HashMap<>();
	private final String textureIdPrefix = "nanbin_font_" + UUID.randomUUID().toString().replace("-", "") + "_";
	private int textureCounter = 0;

	public Identifier getTextTexture(String text, float maxWidth, FontType fontType, int fontSize, Color textColor) {
		// Apply MTR pipe rules: "||" or more hides everything after it
		final String displayText = extractVisibleText(text);
		if (displayText.isEmpty()) {
			return DynamicTextureCache.instance.getStationName(text, maxWidth).identifier;
		}

		final int resolution = org.mtr.mod.config.Config.getClient().getDynamicTextureResolution();
		final String cacheKey = "res" + resolution + "|" + fontType.name() + "|" + displayText + "|" + maxWidth + "|" + fontSize + "|" + textColor.getRGB();
		final Identifier cached = textureCache.get(cacheKey);
		if (cached != null) {
			return cached;
		}

		final Font rawFont = getRawFont(fontType);

		final Identifier id;
		if (rawFont == null) {
			id = DynamicTextureCache.instance.getStationName(text, maxWidth).identifier;
		} else {
			final NativeImage nativeImage = TextRenderer.render(displayText, maxWidth, rawFont, fontSize, textColor);
			if (nativeImage == null) {
				id = DynamicTextureCache.instance.getStationName(text, maxWidth).identifier;
			} else {
				id = registerTexture(nativeImage, "");
			}
		}

		textureCache.put(cacheKey, id);
		return id;
	}

	/**
	 * A tightly-fitted text texture: the texture is exactly as large as the rendered text
	 * (plus a small padding), so it can be drawn in a GUI at a 1:1 pixel size and scaled
	 * down proportionally when it exceeds a display box.
	 */
	public static class FittedTextTexture {
		public final Identifier identifier;
		public final int width;
		public final int height;

		public FittedTextTexture(Identifier identifier, int width, int height) {
			this.identifier = identifier;
			this.width = width;
			this.height = height;
		}
	}

	public FittedTextTexture getFittedTextTexture(String text, FontType fontType, int fontSize, Color textColor) {
		return getFittedTextTexture(text, fontType, fontSize, textColor, false);
	}

	public FittedTextTexture getFittedTextTexture(String text, FontType fontType, int fontSize, Color textColor, boolean bold) {
		final String displayText = extractVisibleText(text);
		if (displayText.isEmpty()) {
			return new FittedTextTexture(DynamicTextureCache.instance.getStationName(text, 1.0F).identifier, 0, 0);
		}

		final int resolution = org.mtr.mod.config.Config.getClient().getDynamicTextureResolution();
		final String cacheKey = "res" + resolution + "|fit|" + fontType.name() + "|" + displayText + "|" + fontSize + "|" + textColor.getRGB() + "|" + bold;
		final FittedTextTexture cached = fittedTextureCache.get(cacheKey);
		if (cached != null) {
			return cached;
		}

		final Font rawFont = getRawFont(fontType);

		// 字体加载失败或渲染失败时不缓存，以免字体恢复后仍显示空/兜底结果
		if (rawFont == null) {
			return new FittedTextTexture(DynamicTextureCache.instance.getStationName(text, 1.0F).identifier, 0, 0);
		}

		final NativeImage nativeImage = FittedTextRenderer.render(displayText, rawFont, fontSize, textColor, bold);
		if (nativeImage == null) {
			return new FittedTextTexture(DynamicTextureCache.instance.getStationName(text, 1.0F).identifier, 0, 0);
		}

		final FittedTextTexture result = new FittedTextTexture(registerTexture(nativeImage, "f"), nativeImage.getWidth(), nativeImage.getHeight());

		fittedTextureCache.put(cacheKey, result);
		return result;
	}

	/**
	 * Bilingual (CJK + Latin) tightly-fitted text texture. CJK stays on one large line; Latin may wrap into
	 * up to 2 smaller lines. Returns a texture tightly bounded to the rendered text so the caller can scale it.
	 *
	 * @param text           already-extracted visible text (no pipe rules)
	 * @param maxWidth       max width in pixels used for wrapping the Latin line (already resolution-scaled)
	 */
	public FittedTextTexture getBilingualFittedTexture(String text, FontType fontType, int fontSize, float latinFontRatio, int maxWidth, Color textColor) {
		return getBilingualFittedTexture(text, fontType, fontSize, latinFontRatio, maxWidth, textColor, 0.04F);
	}

	/**
	 * Bilingual (CJK + Latin) tightly-fitted text texture. CJK stays on one large line; Latin may wrap into
	 * up to 2 smaller lines. Returns a texture tightly bounded to the rendered text so the caller can scale it.
	 *
	 * @param text           already-extracted visible text (no pipe rules)
	 * @param maxWidth       max width in pixels used for wrapping the Latin line (already resolution-scaled)
	 * @param gapRatio       gap between CJK and Latin lines as a ratio of fontSize (0 = tightly packed)
	 */
	public FittedTextTexture getBilingualFittedTexture(String text, FontType fontType, int fontSize, float latinFontRatio, int maxWidth, Color textColor, float gapRatio) {
		final String displayText = extractVisibleText(text);
		if (displayText.isEmpty()) {
			return new FittedTextTexture(DynamicTextureCache.instance.getStationName(text, 1.0F).identifier, 0, 0);
		}

		final int resolution = org.mtr.mod.config.Config.getClient().getDynamicTextureResolution();
		final String cacheKey = "res" + resolution + "|bil|" + fontType.name() + "|" + displayText + "|" + fontSize + "|" + latinFontRatio + "|" + maxWidth + "|" + textColor.getRGB() + "|" + gapRatio;
		final FittedTextTexture cached = fittedTextureCache.get(cacheKey);
		if (cached != null) {
			return cached;
		}

		final Font rawFont = getRawFont(fontType);

		// 字体加载失败或渲染失败时不缓存，以免字体恢复后仍显示空/兜底结果
		if (rawFont == null) {
			return new FittedTextTexture(DynamicTextureCache.instance.getStationName(text, 1.0F).identifier, 0, 0);
		}

		final NativeImage nativeImage = BilingualFittedRenderer.render(displayText, rawFont, fontSize, latinFontRatio, maxWidth, textColor, gapRatio);
		if (nativeImage == null) {
			return new FittedTextTexture(DynamicTextureCache.instance.getStationName(text, 1.0F).identifier, 0, 0);
		}

		final FittedTextTexture result = new FittedTextTexture(registerTexture(nativeImage, "bil"), nativeImage.getWidth(), nativeImage.getHeight());

		fittedTextureCache.put(cacheKey, result);
		return result;
	}

	/**
	 * 生成站名牌纹理（缓存 + 分发）。本类只负责字体加载与纹理缓存，不内置任何具体数值；
	 * 所有布局/字号/圆参数均由调用方（各 Render 类）通过参数传入。
	 */
	public Identifier getSignTexture(String stationName, int stationColor, int routeColor, String routeNumber, String platformNumber, float aspect, FontType fontType, int fontSize, FontType circleFontType, float topBarEnd, float textEnd, float middleBarStart, float middleBarEnd, float middleBarWidth, float bottomBarStart, float latinFontRatio, float gapRatio, float verticalBias, float extraOffsetRatio, float textMarginDefault, float textMarginMedium, float textMarginShort, float circleCenterYOffset, float circleRadiusHRatio, float circleRadiusWRatio, float strokeHRatio, float circleFontRatio, boolean invertedCircle, boolean whiteBackground) {
		final String displayText = extractVisibleText(stationName);
		if (displayText.isEmpty()) {
			return DynamicTextureCache.instance.getStationName(stationName, aspect).identifier;
		}

		final int resolution = org.mtr.mod.config.Config.getClient().getDynamicTextureResolution();
		final String cacheKey = "res" + resolution + "|sign|" + fontType.name() + "|" + fontSize + "|" + circleFontType.name() + "|" + topBarEnd + "|" + textEnd + "|" + middleBarStart + "|" + middleBarEnd + "|" + middleBarWidth + "|" + bottomBarStart + "|" + latinFontRatio + "|" + gapRatio + "|" + verticalBias + "|" + extraOffsetRatio + "|" + textMarginDefault + "|" + textMarginMedium + "|" + textMarginShort + "|" + circleCenterYOffset + "|" + circleRadiusHRatio + "|" + circleRadiusWRatio + "|" + strokeHRatio + "|" + circleFontRatio + "|" + invertedCircle + "|" + whiteBackground + "|" + displayText + "|" + stationColor + "|" + routeColor + "|" + routeNumber + "|" + platformNumber + "|" + aspect;
		final Identifier cached = textureCache.get(cacheKey);
		if (cached != null) {
			return cached;
		}

		final Font rawFont = getRawFont(fontType);
		final Font circleRawFont = getRawFont(circleFontType);

		final Identifier id;
		if (rawFont == null) {
			id = DynamicTextureCache.instance.getStationName(stationName, aspect).identifier;
		} else {
			final NativeImage nativeImage = SignRenderer.render(displayText, stationColor, routeColor, routeNumber, platformNumber, aspect, rawFont, fontSize, circleRawFont, topBarEnd, textEnd, middleBarStart, middleBarEnd, middleBarWidth, bottomBarStart, latinFontRatio, gapRatio, verticalBias, extraOffsetRatio, textMarginDefault, textMarginMedium, textMarginShort, circleCenterYOffset, circleRadiusHRatio, circleRadiusWRatio, strokeHRatio, circleFontRatio, invertedCircle, whiteBackground);
			if (nativeImage == null) {
				id = DynamicTextureCache.instance.getStationName(stationName, aspect).identifier;
			} else {
				id = registerTexture(nativeImage, "s");
			}
		}

		textureCache.put(cacheKey, id);
		return id;
	}

	/**
	 * StationInfo1 顶部 0.5F 条带纹理：灰色矩形 + 本站名 + 线路上一站名，合成一张纹理后一次绘制。
	 *
	 * @param stationName       本站名
	 * @param prevStationName   上一站名（可为空，则不画文字只画矩形）
	 * @param fontType          字体
	 * @param fontSize          本站名 CJK 字号
	 * @param latinFontRatio    拉丁字号 = CJK 字号 × ratio
	 * @param gapRatio          本站名中/拉两行间距 = fontSize × ratio
	 * @param textColor         本站名颜色
	 * @param rectColor         灰色矩形与上一站文字颜色
	 * @param bandWidth         条带宽度（块）
	 * @param bandHeight        条带高度（块）
	 * @param flip              true = 镜像：线路色组合图在左（三角顶点向左）、灰色矩形在右
	 * @param rectLeft          矩形左缘距条带左缘（块）
	 * @param rectLength        矩形长度（块）
	 * @param rectHeight        矩形高度（块）
	 * @param margin            本站名与条带边缘最小留边（块）
	 * @param prevFontSize      上一站 CJK 字号
	 * @param prevMaxWidth      上一站文字最大宽度（块）
	 * @param prevMaxHeight     上一站文字最大高度（块）
	 */
	public Identifier getStationInfoBandTexture(String stationName, String prevStationName, String nextStationName, int lineColor, boolean flip, FontType fontType, int fontSize, float latinFontRatio, float gapRatio, int textColor, int rectColor, float bandWidth, float bandHeight, float rectLeft, float rectLength, float rectHeight, float rightGap, float margin, int prevFontSize, float prevMaxWidth, float prevMaxHeight) {
		final String displayText = extractVisibleText(stationName);
		final String prevText = extractVisibleText(prevStationName);
		final String nextText = extractVisibleText(nextStationName);
		final int resolution = org.mtr.mod.config.Config.getClient().getDynamicTextureResolution();
		final String cacheKey = "res" + resolution + "|sib|" + fontType.name() + "|" + displayText + "|" + prevText + "|" + nextText + "|" + lineColor + "|" + flip + "|" + fontSize + "|" + latinFontRatio + "|" + gapRatio + "|" + textColor + "|" + rectColor + "|" + bandWidth + "|" + bandHeight + "|" + rectLeft + "|" + rectLength + "|" + rectHeight + "|" + rightGap + "|" + margin + "|" + prevFontSize + "|" + prevMaxWidth + "|" + prevMaxHeight;
		final Identifier cached = textureCache.get(cacheKey);
		if (cached != null) {
			return cached;
		}

		final Identifier id;
		final Font rawFont = getRawFont(fontType);
		if (rawFont == null) {
			id = DynamicTextureCache.instance.getStationName(stationName, bandWidth / bandHeight).identifier;
		} else {
			final NativeImage nativeImage = StationInfoBandRenderer.render(displayText, prevText, nextText, lineColor, flip, bandWidth, bandHeight, rawFont, fontSize, latinFontRatio, gapRatio, textColor, rectColor, rectLeft, rectLength, rectHeight, rightGap, margin, prevFontSize, prevMaxWidth, prevMaxHeight);
			if (nativeImage == null) {
				id = DynamicTextureCache.instance.getStationName(stationName, bandWidth / bandHeight).identifier;
			} else {
				id = registerTexture(nativeImage, "sib");
			}
		}

		textureCache.put(cacheKey, id);
		return id;
	}

	/**
	 * Extracts the visible portion using MTR pipe rules:
	 * "||" (or more) hides everything after; single "|" is an invisible segment separator (like MTR).
	 */
	private static String extractVisibleText(String text) {
		if (text == null || text.isEmpty()) {
			return "";
		}
		final int idx = text.indexOf("||");
		if (idx >= 0) {
			return text.substring(0, idx);
		}
		return text;
	}

	private Font getRawFont(FontType fontType) {
		Font rawFont = rawFonts.get(fontType);
		if (rawFont == null) {
			rawFont = loadFont(fontType);
			if (rawFont != null) {
				rawFonts.put(fontType, rawFont);
			}
		}
		return rawFont;
	}

	private Font loadFont(FontType fontType) {
		try (InputStream is = getClass().getResourceAsStream(fontType.path)) {
			if (is == null) {
				LOGGER.error("Font file not found: {} ({})", fontType.path, fontType.displayName);
				return null;
			}
			return Font.createFont(Font.TRUETYPE_FONT, is);
		} catch (Exception e) {
			LOGGER.error("Failed to load font [{}] from {}", fontType.name(), fontType.path, e);
			return null;
		}
	}

	private Identifier registerTexture(NativeImage nativeImage, String prefix) {
		final NativeImageBackedTexture texture = new NativeImageBackedTexture(nativeImage);
		final String name = textureIdPrefix + prefix + (textureCounter++);
		return MinecraftClient.getInstance().getTextureManager().registerDynamicTexture(name, texture);
	}

	/** 注册网络图片纹理（必须在渲染线程调用），供 WebImageCache 使用。 */
	public Identifier registerWebTexture(NativeImage nativeImage) {
		return registerTexture(nativeImage, "web");
	}

	/** 纯色实心矩形纹理缓存：color(ARGB) -> Identifier。 */
	private final Map<Integer, Identifier> solidColorCache = new HashMap<>();

	/**
	 * 获取一个纯色纹理（1×1 像素，拉伸绘制即实心矩形），按颜色缓存。
	 * 必须在渲染线程调用。
	 */
	public Identifier getSolidColorTexture(int color) {
		final Identifier cached = solidColorCache.get(color);
		if (cached != null) {
			return cached;
		}
		final NativeImage nativeImage = new NativeImage(1, 1, false);
		nativeImage.setPixelColor(0, 0, color);
		final Identifier id = registerTexture(nativeImage, "solid");
		solidColorCache.put(color, id);
		return id;
	}

	public void clearCache() {
		textureCache.clear();
		fittedTextureCache.clear();
		rawFonts.clear();
	}

	/**
	 * 仅清除文字纹理缓存（保留已加载的字体），供渲染器定时刷新使用。
	 * MTR 的 Render 加载早于 BlockEntity，且方块数据更新后纹理不会自动失效，
	 * 渲染器需每秒调用一次以确保显示最新的文字内容。
	 */
	public void clearFittedTextureCache() {
		fittedTextureCache.clear();
		textureCache.clear();
	}
}