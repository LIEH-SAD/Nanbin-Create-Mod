package com.Nanbin.client.Drawing;

import com.Nanbin.InitClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import java.awt.*;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.Nanbin.client.RouteMap.RouteMapGenerator.StationNameLayout;

import static com.Nanbin.Init.LOGGER;

public class CustomFontTextureCache {
	public static final CustomFontTextureCache instance = new CustomFontTextureCache();

	public enum FontType {
		ALIBABA("Alibaba PuHuiTi", "/assets/nanbin/font/alibabapuhuiti_regular.ttf"),
		SOURCE_HAN("Source Han Sans SC Bold", "/assets/nanbin/font/sourcehansanssc_bold.otf");

		public final String displayName;
		public final String path;

		FontType(String displayName, String path) {
			this.displayName = displayName;
			this.path = path;
		}
	}

	public FontType selectedFont = FontType.ALIBABA;
	public int fontSize = 48;
	private static final int TEXTURE_HEIGHT = 512;

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

		final String cacheKey = fontType.name() + "|" + displayText + "|" + maxWidth + "|" + fontSize + "|" + textColor.getRGB();
		final Identifier cached = textureCache.get(cacheKey);
		if (cached != null) {
			return cached;
		}

		final Font rawFont = getRawFont(fontType);
		Identifier id;
		if (rawFont == null) {
			id = fallbackEmptyTex;
		} else {
			final NativeImage nativeImage = renderText(displayText, maxWidth, rawFont, fontSize, textColor);
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
		final String displayText = extractVisibleText(text);
		if (displayText.isEmpty()) {
			return new FittedTextTexture(fallbackEmptyTex, 0, 0);
		}

		final String cacheKey = "fit|" + fontType.name() + "|" + displayText + "|" + fontSize + "|" + textColor.getRGB();
		final FittedTextTexture cached = fittedTextureCache.get(cacheKey);
		if (cached != null) {
			return cached;
		}

		final Font rawFont = getRawFont(fontType);
		if (rawFont == null) {
			return new FittedTextTexture(fallbackEmptyTex, 0, 0);
		}

		final NativeImage nativeImage = renderFittedText(displayText, rawFont, fontSize, textColor);
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
		final String displayText = extractVisibleText(stationName);
		if (displayText.isEmpty()) {
			return fallbackEmptyTex;
		}

		final String cacheKey = "sign|" + fontType.name() + "|" + displayText + "|" + stationColor + "|" + routeColor + "|" + routeNumber + "|" + platformNumber + "|" + aspect + "|" + fontSize;
		final Identifier cached = textureCache.get(cacheKey);
		if (cached != null) {
			return cached;
		}

		final Font rawFont = getRawFont(fontType);
		Identifier id;
		if (rawFont == null) {
			id = fallbackEmptyTex;
		} else {
			final NativeImage nativeImage = renderSign(displayText, stationColor, routeColor, routeNumber, platformNumber, aspect, rawFont);
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

	// 渲染核心
	private NativeImage renderFittedText(String text, Font rawFont, int fontSize, Color textColor) {
		try {
			final Font textFont = rawFont.deriveFont(Font.PLAIN, (float) fontSize);
			final BufferedImage temp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
			final Graphics2D tempG = temp.createGraphics();
			tempG.setFont(textFont);
			final FontMetrics metrics = tempG.getFontMetrics(textFont);
			final int textWidth = metrics.stringWidth(text);
			final int textAscent = metrics.getAscent();
			final int textDescent = metrics.getDescent();
			tempG.dispose();

			final int padding = Math.max(2, Math.round(fontSize * 0.08F));
			final int textureWidth = Math.max(1, textWidth + padding * 2);
			final int textureHeight = Math.max(1, textAscent + textDescent + padding * 2);

			final BufferedImage img = new BufferedImage(textureWidth, textureHeight, BufferedImage.TYPE_INT_ARGB);
			final Graphics2D g = img.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
			g.setFont(textFont);
			g.setColor(textColor);
			g.drawString(text, padding, padding + textAscent);
			g.dispose();

			return toNativeImage(img);
		} catch (Exception e) {
			LOGGER.error("Failed to render fitted text \"{}\" to texture", text, e);
			return null;
		}
	}

	private NativeImage renderText(String text, float maxWidth, Font rawFont, int fontSize, Color textColor) {
		try {
			final int textureHeight = TEXTURE_HEIGHT;
			final int textureWidth = Math.round(textureHeight * maxWidth);
			if (textureWidth <= 0) return null;

			final String[] segments = text.split("\\|", -1);
			final StringBuilder cjkLine = new StringBuilder();
			final StringBuilder latinLine = new StringBuilder();
			for (final String seg : segments) {
				if (seg.isEmpty()) continue;
				if (containsCjk(seg)) {
					cjkLine.append(seg);
				} else {
					latinLine.append(seg);
				}
			}
			final boolean hasCjk = cjkLine.length() > 0;
			final boolean hasLatin = latinLine.length() > 0;
			if (!hasCjk && !hasLatin) return null;

			final int cjkFontSize = fontSize;
			final int latinFontSize = Math.max(Math.round(fontSize * 0.6F), 8);
			final float gapRatio = 0.15F;

			final Font cjkFont = rawFont.deriveFont(Font.PLAIN, (float) cjkFontSize);
			final Font latinFont = rawFont.deriveFont(Font.PLAIN, (float) latinFontSize);

			final BufferedImage temp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
			final Graphics2D tempG = temp.createGraphics();
			int cjkTotalWidth = 0, cjkAscent = 0, cjkDescent = 0;
			if (hasCjk) {
				tempG.setFont(cjkFont);
				final FontMetrics m = tempG.getFontMetrics(cjkFont);
				cjkTotalWidth = m.stringWidth(cjkLine.toString());
				cjkAscent = m.getAscent();
				cjkDescent = m.getDescent();
			}
			int latinTotalWidth = 0, latinAscent = 0, latinDescent = 0;
			if (hasLatin) {
				tempG.setFont(latinFont);
				final FontMetrics m = tempG.getFontMetrics(latinFont);
				latinTotalWidth = m.stringWidth(latinLine.toString());
				latinAscent = m.getAscent();
				latinDescent = m.getDescent();
			}
			tempG.dispose();

			final int maxLineWidth = Math.max(cjkTotalWidth, latinTotalWidth);
			final int gap = (hasCjk && hasLatin) ? Math.round(cjkFontSize * gapRatio) : 0;
			final int totalContentHeight = (hasCjk ? (cjkAscent + cjkDescent) : 0)
					+ gap
					+ (hasLatin ? (latinAscent + latinDescent) : 0);

			final float scale = Math.min(1.0F, Math.min(
					(float) textureWidth / (float) maxLineWidth,
					(float) textureHeight / (float) totalContentHeight
			));

			final BufferedImage img = new BufferedImage(textureWidth, textureHeight, BufferedImage.TYPE_INT_ARGB);
			final Graphics2D g = img.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

			final float scaledContentHeight = totalContentHeight * scale;
			final float baseY = (textureHeight - scaledContentHeight) / 2.0F / scale;
			float cursorY = baseY;

			if (hasCjk) {
				final float sx = (textureWidth - cjkTotalWidth * scale) / 2.0F;
				g.translate(sx, 0);
				g.scale(scale, scale);
				g.setFont(cjkFont);
				g.setColor(textColor);
				g.drawString(cjkLine.toString(), 0, cursorY + cjkAscent);
				g.scale(1.0 / scale, 1.0 / scale);
				g.translate(-sx, 0);
				cursorY += cjkAscent + cjkDescent + gap;
			}
			if (hasLatin) {
				final float sx = (textureWidth - latinTotalWidth * scale) / 2.0F;
				g.translate(sx, 0);
				g.scale(scale, scale);
				g.setFont(latinFont);
				g.setColor(textColor);
				g.drawString(latinLine.toString(), 0, cursorY + latinAscent);
				g.scale(1.0 / scale, 1.0 / scale);
				g.translate(-sx, 0);
			}
			g.dispose();

			return toNativeImage(img);
		} catch (Exception e) {
			LOGGER.error("Failed to render text \"{}\" to texture", text, e);
			return null;
		}
	}

	private NativeImage renderSign(String stationName, int stationColor, int routeColor, String routeNumber, String platformNumber, float aspect, Font rawFont) {
		try {
			final int H = TEXTURE_HEIGHT;
			final int W = Math.round(H * aspect);
			if (W <= 0) return null;

			final BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
			final Graphics2D g = img.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

			final Color themeAwtColor = stationColor == -1 ? Color.WHITE : new Color(stationColor | 0xFF000000, true);
			final Color routeAwtColor = routeColor == -1 ? themeAwtColor : new Color(routeColor | 0xFF000000, true);
			final Color textColor = Color.BLACK;

			// 有线路时全局用线路颜色，无线路时仅用车站颜色（基础信息：色条 + 站名）
			final boolean hasRoute = !routeNumber.isEmpty();
			final Color barAwtColor = hasRoute ? routeAwtColor : themeAwtColor;

			final float TOP_BAR_END = StationNameLayout.TOP_BAR_END;
			final float TEXT_END = StationNameLayout.TEXT_END;
			final float CIRCLE_CENTER_Y = StationNameLayout.CIRCLE_CENTER_Y;
			final float BOTTOM_BAR_START = StationNameLayout.BOTTOM_BAR_START;

			// 顶部色条
			g.setColor(barAwtColor);
			g.fillRect(0, 0, W, Math.round(H * TOP_BAR_END));

			// 文字区域
			final String[] segments = stationName.split("\\|", -1);
			final StringBuilder cjkLine = new StringBuilder();
			final StringBuilder latinLine = new StringBuilder();
			for (final String seg : segments) {
				if (seg.isEmpty()) continue;
				if (containsCjk(seg)) {
					cjkLine.append(seg);
				} else {
					latinLine.append(seg);
				}
			}
			final boolean hasCjk = cjkLine.length() > 0;
			final boolean hasLatin = latinLine.length() > 0;
			if (hasCjk || hasLatin) {
				final Font cjkFont = rawFont.deriveFont(Font.PLAIN, (float) fontSize);
				final Font latinFont = rawFont.deriveFont(Font.PLAIN, (float) Math.max(Math.round(fontSize * 0.6F), 8));
				final int cjkW = hasCjk ? getStringWidth(cjkLine.toString(), cjkFont) : 0;
				final int latinW = hasLatin ? getStringWidth(latinLine.toString(), latinFont) : 0;
				final int cjkH = hasCjk ? getFontHeight(cjkFont) : 0;
				final int latinH = hasLatin ? getFontHeight(latinFont) : 0;
				final int gap = (hasCjk && hasLatin) ? Math.round(fontSize * 0.02F) : 0;
				final int maxLineW = Math.max(cjkW, latinW);
				final int totalTextH = (hasCjk ? cjkH : 0) + gap + (hasLatin ? latinH : 0);
				final int textZoneH = Math.round(H * (TEXT_END - TOP_BAR_END));

				final float horizontalMargin = StationNameLayout.computeHorizontalMargin(maxLineW, W);
				final int textZoneW = W - Math.round(W * horizontalMargin);
				final float scale = Math.min(1.0F, Math.min(
						(float) textZoneW / (float) maxLineW,
						(float) textZoneH / (float) totalTextH
				));

				final int textZoneTop = Math.round(H * TOP_BAR_END);
				final float scaledTextH = totalTextH * scale;
				final float baseY = textZoneTop + (textZoneH - scaledTextH) * StationNameLayout.TEXT_VERTICAL_ALIGN + H * StationNameLayout.TEXT_BASE_Y_OFFSET;
				float cursorY = baseY;

				if (hasCjk) {
					final float sx = (W - cjkW * scale) / 2.0F;
					g.translate(sx, 0);
					g.scale(scale, scale);
					g.setFont(cjkFont);
					g.setColor(textColor);
					g.drawString(cjkLine.toString(), 0, cursorY + getAscent(cjkFont));
					g.scale(1.0 / scale, 1.0 / scale);
					g.translate(-sx, 0);
					cursorY += (getAscent(cjkFont) + getDescent(cjkFont)) * scale + gap * scale;
				}
				if (hasLatin) {
					final float sx = (W - latinW * scale) / 2.0F;
					g.translate(sx, 0);
					g.scale(scale, scale);
					g.setFont(latinFont);
					g.setColor(textColor);
					g.drawString(latinLine.toString(), 0, cursorY / scale + getAscent(latinFont));
					g.scale(1.0 / scale, 1.0 / scale);
					g.translate(-sx, 0);
				}
			}

			// 底部色条
			g.setColor(barAwtColor);
			final int bottomStart = Math.round(H * BOTTOM_BAR_START);
			g.fillRect(0, bottomStart, W, H - bottomStart);

			// 圆形标识：仅当已选择线路（有线路编号）时才绘制圆形序号与线路颜色，
			// 未选择线路时只显示站色与站名
			if (!routeNumber.isEmpty()) {
				final int circleCenterY = Math.round(H * CIRCLE_CENTER_Y);
				final int circleRadius = Math.min(Math.round(H * StationNameLayout.CIRCLE_RADIUS_H), Math.round(W * StationNameLayout.CIRCLE_RADIUS_W));
				final int circleCenterX = W / 2;
				final int strokeWidth = Math.max(Math.round(H * StationNameLayout.CIRCLE_STROKE_H), 2);

				g.setColor(Color.WHITE);
				g.fillOval(circleCenterX - circleRadius, circleCenterY - circleRadius, circleRadius * 2, circleRadius * 2);
				g.setColor(barAwtColor);
				g.setStroke(new BasicStroke(strokeWidth));
				g.drawOval(circleCenterX - circleRadius, circleCenterY - circleRadius, circleRadius * 2, circleRadius * 2);

				final int lineHalfWidth = Math.round(circleRadius * 0.8F);
				g.drawLine(circleCenterX - lineHalfWidth, circleCenterY, circleCenterX + lineHalfWidth, circleCenterY);

				final int circleFontSize = Math.max(Math.round(fontSize * StationNameLayout.CIRCLE_FONT_RATIO), 12);
				final Font circleFont = rawFont.deriveFont(Font.PLAIN, (float) circleFontSize);
				g.setFont(circleFont);
				final FontMetrics circleMetrics = g.getFontMetrics(circleFont);
				final int ascent = circleMetrics.getAscent();
				final int descent = circleMetrics.getDescent();
				final int textLeading = Math.max(Math.round(circleFontSize * StationNameLayout.CIRCLE_TEXT_LEADING_RATIO), 2);

				final boolean hasPlat = !platformNumber.isEmpty();
				final int totalTextH = (hasRoute ? (ascent + descent) : 0)
						+ (hasRoute && hasPlat ? textLeading : 0)
						+ (hasPlat ? (ascent + descent) : 0);
				final int textBlockTop = circleCenterY - totalTextH / 2;
				int cursorY = textBlockTop;

				if (hasRoute) {
					final int rnW = circleMetrics.stringWidth(routeNumber);
					g.setColor(routeAwtColor);
					g.drawString(routeNumber, circleCenterX - rnW / 2, cursorY + ascent);
					cursorY += ascent + descent + textLeading;
				}
				if (hasPlat) {
					final int pnW = circleMetrics.stringWidth(platformNumber);
					g.setColor(routeAwtColor);
					g.drawString(platformNumber, circleCenterX - pnW / 2, cursorY + ascent);
				}
			}
			g.dispose();

			return toNativeImage(img);
		} catch (Exception e) {
			LOGGER.error("Failed to render sign for \"{}\"", stationName, e);
			return null;
		}
	}

	private static NativeImage toNativeImage(BufferedImage img) {
		final int w = img.getWidth();
		final int h = img.getHeight();
		final NativeImage nativeImage = new NativeImage(w, h, false);
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				final int argb = img.getRGB(x, y);
				final int a = (argb >> 24) & 0xFF;
				final int r = (argb >> 16) & 0xFF;
				final int g = (argb >> 8) & 0xFF;
				final int b = argb & 0xFF;
				int rgbaPacked = (a << 24) | (b << 16) | (g << 8) | r;
				nativeImage.setColor(x, y, rgbaPacked);
			}
		}
		return nativeImage;
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

	private static boolean containsCjk(String text) {
		return text.codePoints().anyMatch(cp -> {
			final Character.UnicodeBlock block = Character.UnicodeBlock.of(cp);
			return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
					|| block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
					|| block == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
					|| block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
					|| block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
					|| block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_C
					|| block == Character.UnicodeBlock.CJK_COMPATIBILITY_FORMS
					|| block == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
					|| Character.isIdeographic(cp);
		});
	}

	private static int getStringWidth(String text, Font font) {
		final BufferedImage temp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = temp.createGraphics();
		g.setFont(font);
		final int w = g.getFontMetrics(font).stringWidth(text);
		g.dispose();
		return w;
	}

	private static int getFontHeight(Font font) {
		final BufferedImage temp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = temp.createGraphics();
		g.setFont(font);
		final int h = g.getFontMetrics(font).getHeight();
		g.dispose();
		return h;
	}

	private static int getAscent(Font font) {
		final BufferedImage temp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = temp.createGraphics();
		g.setFont(font);
		final int a = g.getFontMetrics(font).getAscent();
		g.dispose();
		return a;
	}

	private static int getDescent(Font font) {
		final BufferedImage temp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = temp.createGraphics();
		g.setFont(font);
		final int d = g.getFontMetrics(font).getDescent();
		g.dispose();
		return d;
	}

	public void clearCache() {
		textureCache.clear();
		fittedTextureCache.clear();
		rawFonts.clear();
	}

	public void clearFittedTextureCache() {
		fittedTextureCache.clear();
		textureCache.clear();
	}
}