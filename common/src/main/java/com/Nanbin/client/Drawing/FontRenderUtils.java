package com.Nanbin.client.Drawing;

import net.minecraft.client.texture.NativeImage;
import org.mtr.config.Config;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * 字体纹理渲染器共用的工具：AWT 图像转 Minecraft 纹理、CJK 判定、字体度量。
 * <p>
 * 4.1 相比 4.0 的变化：{@code org.mtr.mapping.holder.NativeImage} 换回原版
 * {@link NativeImage}，写入像素走按版本编译的 {@link NativePixelWriter}；
 * 配置入口从 {@code org.mtr.mod.config.Config} 挪到 {@link Config}。
 */
public final class FontRenderUtils {

	/** 基础纹理高度（像素），实际高度按分辨率设置线性放大。 */
	public static final int BASE_TEXTURE_HEIGHT = 128;

	private FontRenderUtils() {
	}

	/**
	 * 按 MTR 的动态纹理分辨率设置取当前纹理高度。
	 * 分辨率 0-7 线性增长：128, 256, 384, 512, 640, 768, 896, 1024。
	 */
	public static int getTextureHeight() {
		return BASE_TEXTURE_HEIGHT * (Config.getClient().getDynamicTextureResolution() + 1);
	}

	/**
	 * 当前缩放系数。以原来的 512px 纹理高度为 1.0 校准：
	 * 分辨率 0（128px）= 0.25 倍，3（512px）= 1.0 倍，7（1024px）= 2.0 倍。
	 */
	public static float getResolutionScale() {
		return (float) getTextureHeight() / 512.0F;
	}

	/** 把 AWT 的 ARGB 图转成 Minecraft 的 {@link NativeImage}（ARGB 重排为 ABGR）。 */
	public static NativeImage toNativeImage(BufferedImage img) {
		final int width = img.getWidth();
		final int height = img.getHeight();
		final NativeImage nativeImage = new NativeImage(width, height, false);
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				final int argb = img.getRGB(x, y);
				final int a = (argb >> 24) & 0xFF;
				final int r = (argb >> 16) & 0xFF;
				final int g = (argb >> 8) & 0xFF;
				final int b = argb & 0xFF;
				NativePixelWriter.setPixelAbgr(nativeImage, x, y, (a << 24) | (b << 16) | (g << 8) | r);
			}
		}
		return nativeImage;
	}

	/**
	 * 写单个像素（ABGR 顺序，与 {@link #toNativeImage} 一致）。
	 * 供需要逐像素构造纹理的调用方使用，两个版本的差异由 {@link NativePixelWriter} 抹平。
	 */
	public static void setPixel(NativeImage image, int x, int y, int abgr) {
		NativePixelWriter.setPixelAbgr(image, x, y, abgr);
	}

	/** MTR 风格的 CJK 判定：文本里是否含中日韩表意字符。 */
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

	/**
	 * 纯数字判定：只含 ASCII 数字，允许用 {@code /} 分隔（多个线路号拼成一行时用）。
	 * 用来决定能不能用没有 CJK 字形的 Source Sans 3 渲染。
	 */
	public static boolean isNumeric(String text) {
		if (text == null || text.isEmpty()) {
			return false;
		}
		boolean hasDigit = false;
		for (int i = 0; i < text.length(); i++) {
			final char c = text.charAt(i);
			if (c >= '0' && c <= '9') {
				hasDigit = true;
			} else if (c != '/') {
				return false;
			}
		}
		return hasDigit;
	}

	/** 用一次性的临时图量字宽。 */
	public static int getStringWidth(String text, Font font) {
		final BufferedImage temp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = temp.createGraphics();
		g.setFont(font);
		final int width = g.getFontMetrics(font).stringWidth(text);
		g.dispose();
		return width;
	}

	public static int getFontHeight(Font font) {
		final BufferedImage temp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = temp.createGraphics();
		g.setFont(font);
		final int height = g.getFontMetrics(font).getHeight();
		g.dispose();
		return height;
	}

	public static int getAscent(Font font) {
		final BufferedImage temp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = temp.createGraphics();
		g.setFont(font);
		final int ascent = g.getFontMetrics(font).getAscent();
		g.dispose();
		return ascent;
	}

	public static int getDescent(Font font) {
		final BufferedImage temp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = temp.createGraphics();
		g.setFont(font);
		final int descent = g.getFontMetrics(font).getDescent();
		g.dispose();
		return descent;
	}
}
