package com.Nanbin.client.Drawing;

import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.NativeImage;
import org.mtr.mapping.holder.NativeImageBackedTexture;
import org.mtr.mod.client.DynamicTextureCache;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.Nanbin.InitClient.LOGGER;

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
	/** Default render font size (used for CJK segments). Latin segments use fontSize/2. */
	public int fontSize = 48;
	/** Fixed texture height in pixels. Width = height * maxWidth. */
	private static final int TEXTURE_HEIGHT = 512;

	private final Map<FontType, Font> rawFonts = new HashMap<>();
	private final Map<String, Identifier> textureCache = new HashMap<>();
	private final Map<String, FittedTextTexture> fittedTextureCache = new HashMap<>();
	private final String textureIdPrefix = "nanbin_font_" + UUID.randomUUID().toString().replace("-", "") + "_";
	private int textureCounter = 0;

	public Identifier getTextTexture(String text, float maxWidth) {
		return getTextTexture(text, maxWidth, selectedFont);
	}

	public Identifier getTextTexture(String text, float maxWidth, FontType fontType) {
		return getTextTexture(text, maxWidth, fontType, fontSize, Color.WHITE);
	}

	public Identifier getTextTexture(String text, float maxWidth, FontType fontType, int fontSize, Color textColor) {
		// Apply MTR pipe rules: "||" or more hides everything after it
		final String displayText = extractVisibleText(text);
		if (displayText.isEmpty()) {
			return DynamicTextureCache.instance.getStationName(text, maxWidth).identifier;
		}

		final String cacheKey = fontType.name() + "|" + displayText + "|" + maxWidth + "|" + fontSize + "|" + textColor.getRGB();
		final Identifier cached = textureCache.get(cacheKey);
		if (cached != null) {
			return cached;
		}

		final Font rawFont = getRawFont(fontType);

		final Identifier id;
		if (rawFont == null) {
			id = DynamicTextureCache.instance.getStationName(text, maxWidth).identifier;
		} else {
			final NativeImage nativeImage = renderText(displayText, maxWidth, rawFont, fontSize, textColor);
			if (nativeImage == null) {
				id = DynamicTextureCache.instance.getStationName(text, maxWidth).identifier;
			} else {
				final NativeImageBackedTexture texture = new NativeImageBackedTexture(nativeImage);
				final String name = textureIdPrefix + (textureCounter++);
				id = MinecraftClient.getInstance().getTextureManager().registerDynamicTexture(name, texture);
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
		final String displayText = extractVisibleText(text);
		if (displayText.isEmpty()) {
			return new FittedTextTexture(DynamicTextureCache.instance.getStationName(text, 1.0F).identifier, 0, 0);
		}

		final String cacheKey = "fit|" + fontType.name() + "|" + displayText + "|" + fontSize + "|" + textColor.getRGB();
		final FittedTextTexture cached = fittedTextureCache.get(cacheKey);
		if (cached != null) {
			return cached;
		}

		final Font rawFont = getRawFont(fontType);

		// 字体加载失败或渲染失败时不缓存，以免字体恢复后仍显示空/兜底结果
		if (rawFont == null) {
			return new FittedTextTexture(DynamicTextureCache.instance.getStationName(text, 1.0F).identifier, 0, 0);
		}

		final NativeImage nativeImage = renderFittedText(displayText, rawFont, fontSize, textColor);
		if (nativeImage == null) {
			return new FittedTextTexture(DynamicTextureCache.instance.getStationName(text, 1.0F).identifier, 0, 0);
		}

		final NativeImageBackedTexture texture = new NativeImageBackedTexture(nativeImage);
		final String name = textureIdPrefix + "f" + (textureCounter++);
		final FittedTextTexture result = new FittedTextTexture(MinecraftClient.getInstance().getTextureManager().registerDynamicTexture(name, texture), nativeImage.getWidth(), nativeImage.getHeight());

		fittedTextureCache.put(cacheKey, result);
		return result;
	}

	/** Renders a single line of text tightly fitted to its content (with a small padding). */
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

	/** Converts an AWT ARGB image into a Minecraft NativeImage (ARGB pixel data repacked as ABGR). */
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
				final int abgr = (a << 24) | (b << 16) | (g << 8) | r;
				nativeImage.setPixelColor(x, y, abgr);
			}
		}
		return nativeImage;
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

	/**
	 * MTR-style CJK detection: true if the text contains any CJK ideograph characters.
	 */
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

	/**
	 * Renders text in two rows:
	 * - Top row: CJK segments concatenated (fontSize), centered
	 * - Bottom row: Latin segments concatenated (fontSize/2), centered
	 * - Small gap between rows
	 * - Uniformly scaled down if either row exceeds texture bounds
	 */
	private NativeImage renderText(String text, float maxWidth, Font rawFont, int fontSize, Color textColor) {
		try {
			final int textureHeight = TEXTURE_HEIGHT;
			final int textureWidth = Math.round(textureHeight * maxWidth);
			if (textureWidth <= 0) {
				return null;
			}

			// Split by "|" and classify segments into CJK and Latin groups
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
			final float gapRatio = 0.15F; // small vertical gap relative to cjk font height
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
			final int gap;
			if (hasCjk && hasLatin) {
				gap = Math.round(cjkFontSize * gapRatio);
			} else {
				gap = 0;
			}
			final int totalContentHeight = (hasCjk ? (cjkAscent + cjkDescent) : 0)
					+ gap
					+ (hasLatin ? (latinAscent + latinDescent) : 0);

			// Uniform scale: shrink to fit, never upscale
			final float scale = Math.min(1.0F, Math.min(
					(float) textureWidth / (float) maxLineWidth,
					(float) textureHeight / (float) totalContentHeight
			));

			// Render both rows centered into one texture
			final BufferedImage img = new BufferedImage(textureWidth, textureHeight, BufferedImage.TYPE_INT_ARGB);
			final Graphics2D g = img.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

			// Vertically center the two-row block
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

			// Convert ARGB to ABGR
			final NativeImage nativeImage = new NativeImage(textureWidth, textureHeight, false);
			for (int y = 0; y < textureHeight; y++) {
				for (int xi = 0; xi < textureWidth; xi++) {
					final int argb = img.getRGB(xi, y);
					final int a = (argb >> 24) & 0xFF;
					final int r = (argb >> 16) & 0xFF;
					final int g2 = (argb >> 8) & 0xFF;
					final int b = argb & 0xFF;
					final int abgr = (a << 24) | (b << 16) | (g2 << 8) | r;
					nativeImage.setPixelColor(xi, y, abgr);
				}
			}

			return nativeImage;
		} catch (Exception e) {
			LOGGER.error("Failed to render text \"{}\" to texture", text, e);
			return null;
		}
	}

	/**
	 * Generates a full sign texture with route-color bars, station name, and circle icon.
	 * Layout (top-to-bottom):
	 *   0%-10%  : routeColor bar
	 *   10%-55% : station name (centered, CJK large + Latin small)
	 *   55%-72% : circle icon (route number / platform number)
	 *   70%-100%: routeColor bar
	 */
	public Identifier getSignTexture(String stationName, int stationColor, int routeColor, String routeNumber, String platformNumber, float aspect, FontType fontType) {
		final String displayText = extractVisibleText(stationName);
		if (displayText.isEmpty()) {
			return DynamicTextureCache.instance.getStationName(stationName, aspect).identifier;
		}

		final String cacheKey = "sign|" + fontType.name() + "|" + displayText + "|" + stationColor + "|" + routeColor + "|" + routeNumber + "|" + platformNumber + "|" + aspect + "|" + fontSize;
		final Identifier cached = textureCache.get(cacheKey);
		if (cached != null) {
			return cached;
		}

		final Font rawFont = getRawFont(fontType);

		final Identifier id;
		if (rawFont == null) {
			id = DynamicTextureCache.instance.getStationName(stationName, aspect).identifier;
		} else {
			final NativeImage nativeImage = renderSign(displayText, stationColor, routeColor, routeNumber, platformNumber, aspect, rawFont);
			if (nativeImage == null) {
				id = DynamicTextureCache.instance.getStationName(stationName, aspect).identifier;
			} else {
				final NativeImageBackedTexture texture = new NativeImageBackedTexture(nativeImage);
				final String name = textureIdPrefix + "s" + (textureCounter++);
				id = MinecraftClient.getInstance().getTextureManager().registerDynamicTexture(name, texture);
			}
		}

		textureCache.put(cacheKey, id);
		return id;
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

			// Theme color: use station color for bars and circle border
			final Color themeAwtColor = stationColor == -1 ? Color.WHITE : new Color(stationColor | 0xFF000000, true);
			final Color routeAwtColor = routeColor == -1 ? themeAwtColor : new Color(routeColor | 0xFF000000, true);
			final Color textColor = Color.BLACK;

			// ---- Layout fractions (relative to H) ----
			final float TOP_BAR_END = 0.10F;           // top bar = 10% of H
			final float TEXT_END = 0.55F;               // station name text zone
			final float CIRCLE_CENTER_Y = 0.70F;         // circle center at top of bottom bar
			final float BOTTOM_BAR_START = 0.70F;        // bottom bar = 30% of H

			// ---- 1. Top bar (station theme color) ----
			g.setColor(themeAwtColor);
			g.fillRect(0, 0, W, Math.round(H * TOP_BAR_END));

			// ---- 2. Station name ----
			// Split into CJK / Latin lines
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

				// Uniform scale if exceeds text zone
				final int textZoneH = Math.round(H * (TEXT_END - TOP_BAR_END));
				// When text is short, add more horizontal padding so it doesn't stretch
				float horizontalMargin = 0.04F;
				if (maxLineW > 0 && maxLineW < W * 0.3F) {
					horizontalMargin = 0.20F; // very short text → large padding
				} else if (maxLineW > 0 && maxLineW < W * 0.5F) {
					horizontalMargin = 0.12F;
				}
				final int textZoneW = W - Math.round(W * horizontalMargin);
				final float scale = Math.min(1.0F, Math.min(
						(float) textZoneW / (float) maxLineW,
						(float) textZoneH / (float) totalTextH
				));

				// Center text block vertically in the text zone, biased slightly downward
				final int textZoneTop = Math.round(H * TOP_BAR_END);
				final float scaledTextH = totalTextH * scale;
				final float baseY = textZoneTop + (textZoneH - scaledTextH) * 0.65F + H * 0.05F;

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

			// ---- 3. Bottom bar (route color) ----
			g.setColor(themeAwtColor);
			final int bottomStart = Math.round(H * BOTTOM_BAR_START);
			g.fillRect(0, bottomStart, W, H - bottomStart);

			// ---- 4. Circle icon with route/platform numbers ----
			if (!routeNumber.isEmpty() || !platformNumber.isEmpty()) {
				final int circleCenterY = Math.round(H * CIRCLE_CENTER_Y);
				final int circleRadius = Math.min(Math.round(H * 0.10F), Math.round(W * 0.14F));
				final int circleCenterX = W / 2;
				final int strokeWidth = Math.max(Math.round(H * 0.008F), 2);

				// White fill
				g.setColor(Color.WHITE);
				g.fillOval(circleCenterX - circleRadius, circleCenterY - circleRadius, circleRadius * 2, circleRadius * 2);

				// Station theme color stroke
				g.setColor(themeAwtColor);
				g.setStroke(new BasicStroke(strokeWidth));
				g.drawOval(circleCenterX - circleRadius, circleCenterY - circleRadius, circleRadius * 2, circleRadius * 2);

				// Horizontal line through center (80% of diameter, same thickness as border)
				final int lineHalfWidth = Math.round(circleRadius * 0.8F);
				g.setStroke(new BasicStroke(strokeWidth));
				g.drawLine(circleCenterX - lineHalfWidth, circleCenterY, circleCenterX + lineHalfWidth, circleCenterY);

				// Route number above line, platform number below
				if (!routeNumber.isEmpty() || !platformNumber.isEmpty()) {
					final int circleFontSize = Math.max(Math.round(fontSize * 0.35F), 12);
					final Font circleFont = rawFont.deriveFont(Font.PLAIN, (float) circleFontSize);
					g.setFont(circleFont);
					final FontMetrics circleMetrics = g.getFontMetrics(circleFont);
					final int ascent = circleMetrics.getAscent();
					final int descent = circleMetrics.getDescent();
					final int textLeading = Math.max(Math.round(circleFontSize * 0.15F), 2);

					// Calculate total height of both text blocks + gap
					final boolean hasRoute = !routeNumber.isEmpty();
					final boolean hasPlat = !platformNumber.isEmpty();
					final int totalTextH = (hasRoute ? (ascent + descent) : 0)
							+ (hasRoute && hasPlat ? textLeading : 0)
							+ (hasPlat ? (ascent + descent) : 0);

					// Vertical center offset within the circle
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
			}

			g.dispose();

			// Convert ARGB to ABGR
			final NativeImage nativeImage = new NativeImage(W, H, false);
			for (int y = 0; y < H; y++) {
				for (int x = 0; x < W; x++) {
					final int argb = img.getRGB(x, y);
					final int a = (argb >> 24) & 0xFF;
					final int r = (argb >> 16) & 0xFF;
					final int g2 = (argb >> 8) & 0xFF;
					final int b = argb & 0xFF;
					final int abgr = (a << 24) | (b << 16) | (g2 << 8) | r;
					nativeImage.setPixelColor(x, y, abgr);
				}
			}

			return nativeImage;
		} catch (Exception e) {
			LOGGER.error("Failed to render sign for \"{}\"", stationName, e);
			return null;
		}
	}

	/** Measures string width for a font using a throwaway BufferedImage. */
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
