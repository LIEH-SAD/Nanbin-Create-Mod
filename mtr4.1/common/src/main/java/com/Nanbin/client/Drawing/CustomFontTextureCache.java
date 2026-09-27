package com.Nanbin.client.Drawing;

import com.Nanbin.Init;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.mtr.client.DynamicTextureCache;
import org.mtr.config.Config;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.Nanbin.InitClient.LOGGER;

/**
 * 自定义字体的纹理缓存：内置三套字体（阿里普惠体 / 思源黑体 / Source Sans 3），
 * 按「分辨率 + 字体 + 文本 + 参数」缓存生成好的动态纹理，供各渲染器复用。
 * <p>
 * 4.1 适配：{@code org.mtr.mapping.holder.*} 包装类换回原版类型；
 * 动态纹理注册改用两个版本都有的 {@code TextureManager.registerTexture(Identifier, AbstractTexture)}
 * （1.21.4 已移除 {@code registerDynamicTexture}），Identifier 由我们自己生成。
 */
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
		// MTR 管道规则："||" 或更多会隐藏其后的全部内容
		final String displayText = extractVisibleText(text);
		if (displayText.isEmpty()) {
			return DynamicTextureCache.instance.getStationName(text, maxWidth).identifier;
		}

		final int resolution = Config.getClient().getDynamicTextureResolution();
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
	 * 紧贴文字的纹理：纹理尺寸刚好等于渲染出来的文字（外加少量内边距），
	 * 因此可以在 GUI 里按 1:1 像素绘制，超出显示框时再等比缩小。
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

		final int resolution = Config.getClient().getDynamicTextureResolution();
		final String cacheKey = "res" + resolution + "|fit|" + fontType.name() + "|" + displayText + "|" + fontSize + "|" + textColor.getRGB() + "|" + bold;
		final FittedTextTexture cached = fittedTextureCache.get(cacheKey);
		if (cached != null) {
			return cached;
		}

		final Font rawFont = getRawFont(fontType);

		// 字体加载失败或渲染失败时不缓存，以免字体恢复后仍显示空兜底结果
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

	/**
	 * 双语（CJK + 拉丁）紧贴纹理。CJK 占一行大字，拉丁最多折成 2 行小字。
	 * 返回的纹理紧密贴合文字，调用方可以按需缩放。
	 */
	public FittedTextTexture getBilingualFittedTexture(String text, FontType fontType, int fontSize, float latinFontRatio, int maxWidth, Color textColor, float gapRatio) {
		final String displayText = extractVisibleText(text);
		if (displayText.isEmpty()) {
			return new FittedTextTexture(DynamicTextureCache.instance.getStationName(text, 1.0F).identifier, 0, 0);
		}

		final int resolution = Config.getClient().getDynamicTextureResolution();
		final String cacheKey = "res" + resolution + "|bil|" + fontType.name() + "|" + displayText + "|" + fontSize + "|" + latinFontRatio + "|" + maxWidth + "|" + textColor.getRGB() + "|" + gapRatio;
		final FittedTextTexture cached = fittedTextureCache.get(cacheKey);
		if (cached != null) {
			return cached;
		}

		final Font rawFont = getRawFont(fontType);
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

		final int resolution = Config.getClient().getDynamicTextureResolution();
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

	public Identifier getStationInfoBandTexture(String stationName, String prevStationName, String nextStationName, int lineColor, boolean flip, FontType fontType, int fontSize, float latinFontRatio, float gapRatio, int textColor, int rectColor, float bandWidth, float bandHeight, float rectLeft, float rectLength, float rectHeight, float rightGap, float margin, int prevFontSize, float prevMaxWidth, float prevMaxHeight) {
		final String displayText = extractVisibleText(stationName);
		final String prevText = extractVisibleText(prevStationName);
		final String nextText = extractVisibleText(nextStationName);
		final int resolution = Config.getClient().getDynamicTextureResolution();
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

	/** 屏蔽门编号铭牌纹理：线路色正方形 + 白色编号，按编号与颜色缓存。 */
	public Identifier getDoorNumberPlateTexture(int number, int lineColor) {
		final int resolution = Config.getClient().getDynamicTextureResolution();
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

	/** 字体/绘制失败时的兜底：生成一块纯线路色的铭牌纹理，同样放进专用缓存。 */
	private Identifier createSolidPlateTexture(int lineColor, int sidePx) {
		final int side = Math.max(1, sidePx);
		final BufferedImage img = new BufferedImage(side, side, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = img.createGraphics();
		g.setColor(new Color(lineColor | 0xFF000000, true));
		g.fillRect(0, 0, side, side);
		g.dispose();
		return registerTexture(FontRenderUtils.toNativeImage(img), "dnp");
	}

	/**
	 * 按 MTR 管道规则取出可见部分：
	 * "||"（或更多）隐藏其后全部内容；单个 "|" 是不可见的段分隔符（与 MTR 一致）。
	 */
	private static String extractVisibleText(String text) {
		if (text == null || text.isEmpty()) {
			return "";
		}
		final int index = text.indexOf("||");
		if (index >= 0) {
			return text.substring(0, index);
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
		try (InputStream inputStream = getClass().getResourceAsStream(fontType.path)) {
			if (inputStream == null) {
				LOGGER.error("Font file not found: {} ({})", fontType.path, fontType.displayName);
				return null;
			}
			return Font.createFont(Font.TRUETYPE_FONT, inputStream);
		} catch (Exception e) {
			LOGGER.error("Failed to load font [{}] from {}", fontType.name(), fontType.path, e);
			return null;
		}
	}

	private Identifier registerTexture(NativeImage nativeImage, String prefix) {
		final Identifier identifier = Identifier.of(Init.MOD_ID, textureIdPrefix + prefix + (textureCounter++));
		MinecraftClient.getInstance().getTextureManager().registerTexture(identifier, new NativeImageBackedTexture(nativeImage));
		return identifier;
	}

	/** 注册网络图片纹理（必须在渲染线程调用），供 WebImageCache 使用。 */
	public Identifier registerWebTexture(NativeImage nativeImage) {
		return registerTexture(nativeImage, "web");
	}

	/** 纯色实心矩形纹理缓存：color(ARGB) -> Identifier。 */
	private final Map<Integer, Identifier> solidColorCache = new HashMap<>();

	/**
	 * 屏蔽门编号铭牌纹理缓存：key(res|number|color) -> Identifier。
	 * 与 textureCache / solidColorCache 不同，本缓存<b>不</b>参与 destroyAllTextures，
	 * 避免被其他渲染器每秒调用的 clearFittedTextureCache() 误销毁后，
	 * 门渲染器在当帧稍后用已销毁的 GL 句柄绘制导致闪出破损纹理。
	 */
	private final Map<String, Identifier> doorNumberPlateCache = new HashMap<>();

	/** 获取一张纯色纹理（1×1 像素，拉伸绘制即实心矩形），按颜色缓存。必须在渲染线程调用。 */
	public Identifier getSolidColorTexture(int color) {
		final Identifier cached = solidColorCache.get(color);
		if (cached != null) {
			return cached;
		}
		final NativeImage nativeImage = new NativeImage(1, 1, false);
		// NativeImage 的单像素写入期望 ABGR（见 FontRenderUtils.toNativeImage），而 color 是 ARGB，需重排字节
		final int abgr = ((color >>> 24) & 0xFF) << 24 | (color & 0xFF) << 16 | ((color >>> 8) & 0xFF) << 8 | ((color >>> 16) & 0xFF);
		FontRenderUtils.setPixel(nativeImage, 0, 0, abgr);
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
		textureCache.clear();
		fittedTextureCache.clear();
		solidColorCache.clear();
		doorNumberPlateCache.clear();
		rawFonts.clear();
		enqueueTexturesForDestroy(textures);
	}

	/**
	 * 仅清除文字纹理缓存（保留已加载的字体），供渲染器定时刷新使用。
	 * MTR 的 Render 加载早于 BlockEntity，且方块数据更新后纹理不会自动失效，
	 * 渲染器需每秒调用一次以确保显示最新的文字内容。
	 */
	public void clearFittedTextureCache() {
		final List<Identifier> textures = new ArrayList<>();
		textures.addAll(textureCache.values());
		fittedTextureCache.values().forEach(value -> textures.add(value.identifier));
		// 纯色纹理的 GPU 资源随纹理队列延迟销毁，缓存必须一并清掉，
		// 否则下次 getSolidColorTexture 会返回已销毁的 Identifier，导致线路色矩形显示成别的颜色。
		textures.addAll(solidColorCache.values());
		textureCache.clear();
		fittedTextureCache.clear();
		solidColorCache.clear();
		enqueueTexturesForDestroy(textures);
	}

	private static final long TEXTURE_DESTROY_DELAY_MS = 2000L;

	/** 一批待销毁纹理及其入队时间；每批只会被销毁一次。 */
	private static final class PendingTextures {
		private final List<Identifier> textures;
		private final long enqueuedAt;

		private PendingTextures(List<Identifier> textures) {
			this.textures = textures;
			this.enqueuedAt = System.currentTimeMillis();
		}
	}

	private final Deque<PendingTextures> pendingDestroy = new ArrayDeque<>();

	/**
	 * 把已从各缓存移除的纹理入队延迟销毁：先入队，再销毁入队时间足够久
	 * （超过 {@link #TEXTURE_DESTROY_DELAY_MS}）的批次。所有待销毁纹理都按此节奏分批释放，
	 * 防止每秒刷新缓存时纹理无限累积导致内存泄漏，同时避免销毁仍在渲染队列中的纹理。
	 */
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
