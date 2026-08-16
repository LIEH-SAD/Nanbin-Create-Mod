package com.Nanbin.client.Drawing;

import org.mtr.mapping.holder.NativeImage;
import org.mtr.mod.config.Config;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Shared helpers for the font texture renderers: AWT-to-Minecraft image conversion,
 * CJK detection, and font metric measurement.
 */
public final class FontRenderUtils {

	/** Base texture height in pixels. Actual height scales linearly with resolution setting. */
	public static final int BASE_TEXTURE_HEIGHT = 128;

	private FontRenderUtils() {
	}

	/**
	 * Gets the current texture height based on MTR dynamic texture resolution setting.
	 * Resolution range: 0-7, linear growth: 128, 256, 384, 512, 640, 768, 896, 1024
	 */
	public static int getTextureHeight() {
		final int resolution = Config.getClient().getDynamicTextureResolution();
		return BASE_TEXTURE_HEIGHT * (resolution + 1);
	}

	/**
	 * Gets the current scale factor based on dynamic texture resolution.
	 * Calibrated so that scale = 1.0 at original 512px texture height.
	 * Resolution 0 (128px): 0.25x, Resolution 3 (512px): 1.0x, Resolution 7 (1024px): 2.0x
	 */
	public static float getResolutionScale() {
		return (float) getTextureHeight() / 512.0F;
	}

	/** Converts an AWT ARGB image into a Minecraft NativeImage (ARGB pixel data repacked as ABGR). */
	public static NativeImage toNativeImage(BufferedImage img) {
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
	 * MTR-style CJK detection: true if the text contains any CJK ideograph characters.
	 */
	public static boolean containsCjk(String text) {
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

	/** Measures string width for a font using a throwaway BufferedImage. */
	public static int getStringWidth(String text, Font font) {
		final BufferedImage temp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = temp.createGraphics();
		g.setFont(font);
		final int w = g.getFontMetrics(font).stringWidth(text);
		g.dispose();
		return w;
	}

	public static int getFontHeight(Font font) {
		final BufferedImage temp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = temp.createGraphics();
		g.setFont(font);
		final int h = g.getFontMetrics(font).getHeight();
		g.dispose();
		return h;
	}

	public static int getAscent(Font font) {
		final BufferedImage temp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = temp.createGraphics();
		g.setFont(font);
		final int a = g.getFontMetrics(font).getAscent();
		g.dispose();
		return a;
	}

	public static int getDescent(Font font) {
		final BufferedImage temp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = temp.createGraphics();
		g.setFont(font);
		final int d = g.getFontMetrics(font).getDescent();
		g.dispose();
		return d;
	}
}