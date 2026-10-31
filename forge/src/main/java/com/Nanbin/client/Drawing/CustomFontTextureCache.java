package com.Nanbin.client.Drawing;

import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.NativeImage;
import org.mtr.mapping.holder.NativeImageBackedTexture;
import org.mtr.mod.client.DynamicTextureCache;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.*;
import java.util.List;

import static com.Nanbin.InitClient.LOGGER;

public class CustomFontTextureCache {

	public static final CustomFontTextureCache instance = new CustomFontTextureCache();

	public enum FontType {
		ALIBABA("Alibaba PuHuiTi", "/assets/nanbin/font/alibabapuhuiti_regular.ttf"),
		SOURCE_HAN("Source Han Sans SC Bold", "/assets/nanbin/font/sourcehansanssc_bold.otf"),
		SOURCE_SANS_3("Source Sans 3", "/assets/nanbin/font/sourcesans3_bold.otf");

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

		// 字体加载失败或渲染失败时不缓存
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

	public FittedTextTexture getBilingualFittedTexture(String text, FontType fontType, int fontSize, float latinFontRatio, int maxWidth, Color textColor) {
		return getBilingualFittedTexture(text, fontType, fontSize, latinFontRatio, maxWidth, textColor, 0.04F);
	}

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

		// 字体加载失败或渲染失败时不缓存
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
			final NativeImage nativeImage = CRTSignRenderer.render(displayText, stationColor, routeColor, routeNumber, platformNumber, aspect, rawFont, fontSize, circleRawFont, topBarEnd, textEnd, middleBarStart, middleBarEnd, middleBarWidth, bottomBarStart, latinFontRatio, gapRatio, verticalBias, extraOffsetRatio, textMarginDefault, textMarginMedium, textMarginShort, circleCenterYOffset, circleRadiusHRatio, circleRadiusWRatio, strokeHRatio, circleFontRatio, invertedCircle, whiteBackground);
			if (nativeImage == null) {
				id = DynamicTextureCache.instance.getStationName(stationName, aspect).identifier;
			} else {
				id = registerTexture(nativeImage, "s");
			}
		}

		textureCache.put(cacheKey, id);
		return id;
	}

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
			final NativeImage nativeImage = CRTStationInfoBandRenderer.render(displayText, prevText, nextText, lineColor, flip, bandWidth, bandHeight, rawFont, fontSize, latinFontRatio, gapRatio, textColor, rectColor, rectLeft, rectLength, rectHeight, rightGap, margin, prevFontSize, prevMaxWidth, prevMaxHeight);
			if (nativeImage == null) {
				id = DynamicTextureCache.instance.getStationName(stationName, bandWidth / bandHeight).identifier;
			} else {
				id = registerTexture(nativeImage, "sib");
			}
		}

		textureCache.put(cacheKey, id);
		return id;
	}

	public Identifier getPSDStationSignTexture(String stationName, String prevStationName, String nextStationName, String routeNumber, String stationNumber,
			int lineColor, int textColor, float groupWidth, float bandHeight,
			FontType fontType, FontType circleFontType, int fontSize, int prevFontSize, int badgeFontSize,
			float latinFontRatio, float gapRatio, boolean reverse) {
		final String displayText = extractVisibleText(stationName);
		if (displayText.isEmpty()) {
			return DynamicTextureCache.instance.getStationName(stationName, groupWidth / bandHeight).identifier;
		}

		final int resolution = org.mtr.mod.config.Config.getClient().getDynamicTextureResolution();
		final String cacheKey = "res" + resolution + "|psd|" + fontType.name() + "|" + circleFontType.name() + "|" + displayText + "|" + prevStationName + "|" + nextStationName + "|" + routeNumber + "|" + stationNumber + "|" + lineColor + "|" + textColor + "|" + groupWidth + "|" + bandHeight + "|" + fontSize + "|" + prevFontSize + "|" + badgeFontSize + "|" + latinFontRatio + "|" + gapRatio + "|" + reverse;
		final Identifier cached = psdStationSignCache.get(cacheKey);
		if (cached != null) {
			return cached;
		}

		final Identifier id;
		final Font rawFont = getRawFont(fontType);
		final Font circleRawFont = getRawFont(circleFontType);
		if (rawFont == null || circleRawFont == null) {
			id = DynamicTextureCache.instance.getStationName(stationName, groupWidth / bandHeight).identifier;
		} else {
			final NativeImage nativeImage = PSDCRTStationSignRenderer.render(displayText, extractVisibleText(prevStationName), extractVisibleText(nextStationName), routeNumber, stationNumber,
					lineColor, textColor, groupWidth, bandHeight, rawFont, circleRawFont, fontSize, prevFontSize, badgeFontSize, latinFontRatio, gapRatio, reverse);
			if (nativeImage == null) {
				id = DynamicTextureCache.instance.getStationName(stationName, groupWidth / bandHeight).identifier;
			} else {
				id = registerTexture(nativeImage, "psd");
			}
		}

		psdStationSignCache.put(cacheKey, id);
		return id;
	}

	public Identifier getFasciaLineMapTexture(LineMapData data, float groupBlocks, float bandHeight,
			FontType nameFontType, FontType numberFontType, int nameFontSize, int numberFontSize,
			float latinFontRatio, float gapRatio, int textColor, boolean reverse) {
		final StringBuilder keyBuilder = new StringBuilder();
		keyBuilder.append("res").append(org.mtr.mod.config.Config.getClient().getDynamicTextureResolution())
				.append("|map|").append(nameFontType.name()).append('|').append(numberFontType.name())
				.append('|').append(groupBlocks).append('|').append(bandHeight)
				.append('|').append(nameFontSize).append('|').append(numberFontSize)
				.append('|').append(latinFontRatio).append('|').append(gapRatio).append('|').append(textColor)
				.append('|').append(reverse)
				.append("|loop").append(data.loop())
				.append('|').append(data.lineColor()).append('|').append(data.lineNumber()).append('|').append(data.currentIndex());
		for (final LineMapData.Station station : data.stations()) {
			keyBuilder.append('|').append(station.name()).append('~').append(station.number());
			for (final LineMapData.Interchange interchange : station.interchanges()) {
				keyBuilder.append('~').append(interchange.color()).append(':').append(interchange.number());
			}
		}
		for (final LineMapData.Branch branch : data.branches()) {
			keyBuilder.append("|B").append(branch.forkIndex()).append('@').append(branch.lineColor()).append('/').append(branch.lineNumber());
			for (final List<LineMapData.Station> branchStations : List.of(branch.afterStations(), branch.beforeStations())) {
				keyBuilder.append('#');
				for (final LineMapData.Station station : branchStations) {
					keyBuilder.append('|').append(station.name()).append('~').append(station.number());
					for (final LineMapData.Interchange interchange : station.interchanges()) {
						keyBuilder.append('~').append(interchange.color()).append(':').append(interchange.number());
					}
				}
			}
		}
		final String cacheKey = keyBuilder.toString();

		final Identifier cached = fasciaLineMapCache.get(cacheKey);
		if (cached != null) {
			return cached;
		}

		final Identifier id;
		final Font nameFont = getRawFont(nameFontType);
		final Font numberFont = getRawFont(numberFontType);
		if (nameFont == null || numberFont == null) {
			id = DynamicTextureCache.instance.getStationName(data.stations().get(0).name(), groupBlocks / bandHeight).identifier;
		} else {
			final NativeImage nativeImage = FasciaCRTRouteMapRenderer.render(data, groupBlocks, bandHeight, nameFont, nameFontSize, numberFont, numberFontSize, latinFontRatio, gapRatio, textColor, reverse);
			if (nativeImage == null) {
				id = DynamicTextureCache.instance.getStationName(data.stations().get(0).name(), groupBlocks / bandHeight).identifier;
			} else {
				id = registerTexture(nativeImage, "map");
			}
		}

		fasciaLineMapCache.put(cacheKey, id);
		return id;
	}

	public Identifier getDoorNumberPlateTexture(int number, int lineColor) {
		final int resolution = org.mtr.mod.config.Config.getClient().getDynamicTextureResolution();
		final String cacheKey = "res" + resolution + "|dnp|" + number + "|" + lineColor;
		final Identifier cached = doorNumberPlateCache.get(cacheKey);
		if (cached != null) {
			return cached;
		}

		final float resolutionScale = Math.min((float) Math.pow(2, resolution + 5), 1024.0F) / 60.0F;
		final int sidePx = Math.max(8, Math.round(0.3F * 120.0F * resolutionScale));

		final Identifier id;
		final Font rawFont = getRawFont(FontType.SOURCE_SANS_3);
		if (rawFont == null) {
			id = createSolidPlateTexture(lineColor, sidePx);
		} else {
			final NativeImage nativeImage = DoorNumberPlateRenderer.render(number, lineColor, rawFont, sidePx, 0.45F, 0.12F);
			if (nativeImage == null) {
				id = createSolidPlateTexture(lineColor, sidePx);
			} else {
				id = registerTexture(nativeImage, "dnp");
			}
		}

		doorNumberPlateCache.put(cacheKey, id);
		return id;
	}

	private Identifier createSolidPlateTexture(int lineColor, int sidePx) {
		final int S = Math.max(1, sidePx);
		final BufferedImage img = new BufferedImage(S, S, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = img.createGraphics();
		g.setColor(new Color(lineColor | 0xFF000000, true));
		g.fillRect(0, 0, S, S);
		g.dispose();
		return registerTexture(FontRenderUtils.toNativeImage(img), "dnp");
	}

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

	public Identifier registerWebTexture(NativeImage nativeImage) {
		return registerTexture(nativeImage, "web");
	}
	private final Map<Integer, Identifier> solidColorCache = new HashMap<>();
	private final Map<String, Identifier> doorNumberPlateCache = new HashMap<>();
	private final Map<String, Identifier> psdStationSignCache = new HashMap<>();
	private final Map<String, Identifier> fasciaLineMapCache = new HashMap<>();


	public Identifier getSolidColorTexture(int color) {
		final Identifier cached = solidColorCache.get(color);
		if (cached != null) {
			return cached;
		}
		final NativeImage nativeImage = new NativeImage(1, 1, false);
		// NativeImage.setPixelColor 期望 ABGR（见 FontRenderUtils.toNativeImage），而 color 为 ARGB，需重排字节
		final int abgr = ((color >>> 24) & 0xFF) << 24 | (color & 0xFF) << 16 | ((color >>> 8) & 0xFF) << 8 | ((color >>> 16) & 0xFF);
		nativeImage.setPixelColor(0, 0, abgr);
		final Identifier id = registerTexture(nativeImage, "solid");
		solidColorCache.put(color, id);
		return id;
	}

	public void clearCache() {
		final List<Identifier> textures = new ArrayList<>();
		textures.addAll(textureCache.values());
		fittedTextureCache.values().forEach(value -> textures.add(value.identifier));
		textures.addAll(solidColorCache.values());
		textures.addAll(doorNumberPlateCache.values());
		textures.addAll(psdStationSignCache.values());
		textures.addAll(fasciaLineMapCache.values());
		textureCache.clear();
		fittedTextureCache.clear();
		solidColorCache.clear();
		doorNumberPlateCache.clear();
		psdStationSignCache.clear();
		fasciaLineMapCache.clear();
		rawFonts.clear();
		enqueueTexturesForDestroy(textures);
	}

	public void clearFittedTextureCache() {
		final List<Identifier> textures = new ArrayList<>();
		textures.addAll(textureCache.values());
		fittedTextureCache.values().forEach(value -> textures.add(value.identifier));
		// 纯色纹理的 GPU 资源随纹理队列延迟销毁，缓存必须一并清掉，
		// 否则下次 getSolidColorTexture 会返回已销毁的 Identifier（GL 句柄可能已被
		// 其他纹理复用），导致线路色矩形显示成别的颜色。
		textures.addAll(solidColorCache.values());
		textureCache.clear();
		fittedTextureCache.clear();
		solidColorCache.clear();
		enqueueTexturesForDestroy(textures);
	}

	private static final long TEXTURE_DESTROY_DELAY_MS = 2000L;

	private static final class PendingTextures {
		private final List<Identifier> textures;
		private final long enqueuedAt;

		private PendingTextures(List<Identifier> textures) {
			this.textures = textures;
			this.enqueuedAt = System.currentTimeMillis();
		}
	}

	private final Deque<PendingTextures> pendingDestroy = new ArrayDeque<>();

	private void enqueueTexturesForDestroy(List<Identifier> textures) {
		if (!textures.isEmpty()) {
			pendingDestroy.addLast(new PendingTextures(textures));
		}
		final long now = System.currentTimeMillis();
		final var textureManager = MinecraftClient.getInstance().getTextureManager();
		while (!pendingDestroy.isEmpty() && now - pendingDestroy.peekFirst().enqueuedAt >= TEXTURE_DESTROY_DELAY_MS) {
			final PendingTextures oldest = pendingDestroy.removeFirst();
			oldest.textures.forEach(textureManager::destroyTexture);
		}
	}
}