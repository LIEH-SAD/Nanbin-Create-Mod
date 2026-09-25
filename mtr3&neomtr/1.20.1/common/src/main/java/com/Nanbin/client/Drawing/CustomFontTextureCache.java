package com.Nanbin.client.Drawing;

import com.Nanbin.client.RouteMap.RouteMapGenerator.StationNameLayout;
import mtr.client.Config;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.awt.*;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.Nanbin.Init.LOGGER;

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

	public FontType selectedFont = FontType.ALIBABA;
	public int fontSize = 48;

	private final Map<FontType, Font> rawFonts = new HashMap<>();
	private final Map<String, Identifier> textureCache = new HashMap<>();
	private final Map<String, FittedTextTexture> fittedTextureCache = new HashMap<>();
	private final String textureIdPrefix = "nanbin_font_" + UUID.randomUUID().toString().replace("-", "") + "_";
	private int textureCounter = 0;

	// 无MTR兜底，缺失字体返回空占位纹理（你可自行替换默认纯色图）
	private Identifier fallbackEmptyTex;

	private CustomFontTextureCache() {
		// 初始化兜底空白纹理
		NativeImage blankImg = new NativeImage(2, 2, false);
		blankImg.fillRect(0, 0, 2, 2, 0x00000000);
		NativeImageBackedTexture blankTex = new NativeImageBackedTexture(blankImg);
		fallbackEmptyTex = MinecraftClient.getInstance().getTextureManager()
				.registerDynamicTexture("nanbin_empty_fallback", blankTex);
	}

	// region 对外API
	public Identifier getTextTexture(String text, float maxWidth) {
		return getTextTexture(text, maxWidth, selectedFont);
	}

	public Identifier getTextTexture(String text, float maxWidth, FontType fontType) {
		return getTextTexture(text, maxWidth, fontType, fontSize, Color.WHITE);
	}

	public Identifier getTextTexture(String text, float maxWidth, FontType fontType, int fontSize, Color textColor) {
		final String displayText = extractVisibleText(text);
		if (displayText.isEmpty()) {
			return fallbackEmptyTex;
		}

		final int resolution = Config.dynamicTextureResolution();
		final String cacheKey = "res" + resolution + "|" + fontType.name() + "|" + displayText + "|" + maxWidth + "|" + fontSize + "|" + textColor.getRGB();
		final Identifier cached = textureCache.get(cacheKey);
		if (cached != null) {
			return cached;
		}

		final Font rawFont = getRawFont(fontType);
		Identifier id;
		if (rawFont == null) {
			id = fallbackEmptyTex;
		} else {
			final NativeImage nativeImage = TextRenderer.render(displayText, maxWidth, rawFont, fontSize, textColor);
			if (nativeImage == null) {
				id = fallbackEmptyTex;
			} else {
				final NativeImageBackedTexture texture = new NativeImageBackedTexture(nativeImage);
				final String name = textureIdPrefix + (textureCounter++);
				id = MinecraftClient.getInstance().getTextureManager().registerDynamicTexture(name, texture);
			}
		}
		textureCache.put(cacheKey, id);
		return id;
	}

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
			return new FittedTextTexture(fallbackEmptyTex, 0, 0);
		}

		final int resolution = Config.dynamicTextureResolution();
		final String cacheKey = "res" + resolution + "|fit|" + fontType.name() + "|" + displayText + "|" + fontSize + "|" + textColor.getRGB() + "|" + bold;
		final FittedTextTexture cached = fittedTextureCache.get(cacheKey);
		if (cached != null) {
			return cached;
		}

		final Font rawFont = getRawFont(fontType);
		if (rawFont == null) {
			return new FittedTextTexture(fallbackEmptyTex, 0, 0);
		}

		final NativeImage nativeImage = FittedTextRenderer.render(displayText, rawFont, fontSize, textColor, bold);
		if (nativeImage == null) {
			return new FittedTextTexture(fallbackEmptyTex, 0, 0);
		}

		final NativeImageBackedTexture texture = new NativeImageBackedTexture(nativeImage);
		final String name = textureIdPrefix + "f" + (textureCounter++);
		final FittedTextTexture result = new FittedTextTexture(
				MinecraftClient.getInstance().getTextureManager().registerDynamicTexture(name, texture),
				nativeImage.getWidth(),
				nativeImage.getHeight()
		);
		fittedTextureCache.put(cacheKey, result);
		return result;
	}

	public Identifier getSignTexture(String stationName, int stationColor, int routeColor, String routeNumber, String platformNumber, float aspect, FontType fontType) {
		return getSignTexture(stationName, stationColor, routeColor, routeNumber, platformNumber, aspect, fontType, fontSize, fontType,
				StationNameLayout.TOP_BAR_END, StationNameLayout.TEXT_END, 0F, 0F, 0F, StationNameLayout.BOTTOM_BAR_START,
				0.5F, 0.02F, StationNameLayout.TEXT_VERTICAL_ALIGN, StationNameLayout.TEXT_BASE_Y_OFFSET,
				0.04F, 0.12F, 0.20F,
				0F, StationNameLayout.CIRCLE_RADIUS_H, StationNameLayout.CIRCLE_RADIUS_W, StationNameLayout.CIRCLE_STROKE_H,
				StationNameLayout.CIRCLE_FONT_RATIO, false, false);
	}

	/**
	 * 全参数版站名牌纹理：布局/字号/圆参数全部由调用方传入（见 {@link SignRenderer}）。
	 * 数据暂缺时回退到空白纹理，不再跳过渲染。
	 */
	public Identifier getSignTexture(String stationName, int stationColor, int routeColor, String routeNumber, String platformNumber, float aspect, FontType fontType, int fontSize, FontType circleFontType, float topBarEnd, float textEnd, float middleBarStart, float middleBarEnd, float middleBarWidth, float bottomBarStart, float latinFontRatio, float gapRatio, float verticalBias, float extraOffsetRatio, float textMarginDefault, float textMarginMedium, float textMarginShort, float circleCenterYOffset, float circleRadiusHRatio, float circleRadiusWRatio, float strokeHRatio, float circleFontRatio, boolean invertedCircle, boolean whiteBackground) {
		final String displayText = extractVisibleText(stationName);
		if (displayText.isEmpty()) {
			return fallbackEmptyTex;
		}

		final int resolution = Config.dynamicTextureResolution();
		final String cacheKey = "res" + resolution + "|sign|" + fontType.name() + "|" + fontSize + "|" + circleFontType.name() + "|" + topBarEnd + "|" + textEnd + "|" + middleBarStart + "|" + middleBarEnd + "|" + middleBarWidth + "|" + bottomBarStart + "|" + latinFontRatio + "|" + gapRatio + "|" + verticalBias + "|" + extraOffsetRatio + "|" + textMarginDefault + "|" + textMarginMedium + "|" + textMarginShort + "|" + circleCenterYOffset + "|" + circleRadiusHRatio + "|" + circleRadiusWRatio + "|" + strokeHRatio + "|" + circleFontRatio + "|" + invertedCircle + "|" + whiteBackground + "|" + displayText + "|" + stationColor + "|" + routeColor + "|" + routeNumber + "|" + platformNumber + "|" + aspect;
		final Identifier cached = textureCache.get(cacheKey);
		if (cached != null) {
			return cached;
		}

		final Font rawFont = getRawFont(fontType);
		final Font circleRawFont = getRawFont(circleFontType);

		final Identifier id;
		if (rawFont == null) {
			id = fallbackEmptyTex;
		} else {
			final NativeImage nativeImage = SignRenderer.render(displayText, stationColor, routeColor, routeNumber, platformNumber, aspect, rawFont, fontSize, circleRawFont, topBarEnd, textEnd, middleBarStart, middleBarEnd, middleBarWidth, bottomBarStart, latinFontRatio, gapRatio, verticalBias, extraOffsetRatio, textMarginDefault, textMarginMedium, textMarginShort, circleCenterYOffset, circleRadiusHRatio, circleRadiusWRatio, strokeHRatio, circleFontRatio, invertedCircle, whiteBackground);
			if (nativeImage == null) {
				id = fallbackEmptyTex;
			} else {
				final NativeImageBackedTexture texture = new NativeImageBackedTexture(nativeImage);
				final String name = textureIdPrefix + "s" + (textureCounter++);
				id = MinecraftClient.getInstance().getTextureManager().registerDynamicTexture(name, texture);
			}
		}
		textureCache.put(cacheKey, id);
		return id;
	}

	private static String extractVisibleText(String text) {
		if (text == null || text.isEmpty()) return "";
		final int idx = text.indexOf("||");
		return idx >= 0 ? text.substring(0, idx) : text;
	}

	private Font getRawFont(FontType fontType) {
		Font rawFont = rawFonts.get(fontType);
		if (rawFont == null) {
			rawFont = loadFont(fontType);
			if (rawFont != null) rawFonts.put(fontType, rawFont);
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
		// NativeImage 期望 ABGR（见 FontRenderUtils.toNativeImage），而 color 为 ARGB，需重排字节
		final int abgr = ((color >>> 24) & 0xFF) << 24 | (color & 0xFF) << 16 | ((color >>> 8) & 0xFF) << 8 | ((color >>> 16) & 0xFF);
		nativeImage.setColor(0, 0, abgr);
		final NativeImageBackedTexture texture = new NativeImageBackedTexture(nativeImage);
		final Identifier id = MinecraftClient.getInstance().getTextureManager().registerDynamicTexture(textureIdPrefix + "solid" + (textureCounter++), texture);
		solidColorCache.put(color, id);
		return id;
	}

	/** 注册一张外部（网络）加载的 NativeImage 为动态纹理，返回其 Identifier。必须在渲染线程调用。 */
	public Identifier registerWebTexture(NativeImage nativeImage) {
		final NativeImageBackedTexture texture = new NativeImageBackedTexture(nativeImage);
		return MinecraftClient.getInstance().getTextureManager().registerDynamicTexture(textureIdPrefix + "web" + (textureCounter++), texture);
	}

	/**
	 * 站台信息屏顶部条带贴图：灰色矩形 + 本站名 + 线路上一站名合成一张纹理，按参数缓存。
	 * 必须在渲染线程调用。
	 */
	public Identifier getStationInfoBandTexture(String stationName, String prevStationName, String nextStationName, int lineColor, boolean flip, FontType fontType, int fontSize, float latinFontRatio, float gapRatio, int textColor, int rectColor, float bandWidth, float bandHeight, float rectLeft, float rectLength, float rectHeight, float rightGap, float margin, int prevFontSize, float prevMaxWidth, float prevMaxHeight) {
		final String displayText = extractVisibleText(stationName);
		final String prevText = extractVisibleText(prevStationName);
		final String nextText = extractVisibleText(nextStationName);
		final int resolution = Config.dynamicTextureResolution();
		final String cacheKey = "res" + resolution + "|sib|" + fontType.name() + "|" + displayText + "|" + prevText + "|" + nextText + "|" + lineColor + "|" + flip + "|" + fontSize + "|" + latinFontRatio + "|" + gapRatio + "|" + textColor + "|" + rectColor + "|" + bandWidth + "|" + bandHeight + "|" + rectLeft + "|" + rectLength + "|" + rectHeight + "|" + rightGap + "|" + margin + "|" + prevFontSize + "|" + prevMaxWidth + "|" + prevMaxHeight;
		final Identifier cached = textureCache.get(cacheKey);
		if (cached != null) {
			return cached;
		}

		final Identifier id;
		final Font rawFont = getRawFont(fontType);
		if (rawFont == null) {
			id = fallbackEmptyTex;
		} else {
			final NativeImage nativeImage = StationInfoBandRenderer.render(displayText, prevText, nextText, lineColor, flip, bandWidth, bandHeight, rawFont, fontSize, latinFontRatio, gapRatio, textColor, rectColor, rectLeft, rectLength, rectHeight, rightGap, margin, prevFontSize, prevMaxWidth, prevMaxHeight);
			id = nativeImage == null ? fallbackEmptyTex : registerWebTexture(nativeImage);
		}

		textureCache.put(cacheKey, id);
		return id;
	}

	public void clearCache() {
		textureCache.clear();
		fittedTextureCache.clear();
		solidColorCache.clear();
		rawFonts.clear();
	}

	public void clearFittedTextureCache() {
		fittedTextureCache.clear();
		textureCache.clear();
		solidColorCache.clear();
	}
}
