package com.Nanbin.client.CRTRailwaySignEditor.render;

import com.Nanbin.client.JavaScriptSupport.JSSignTextureBaker;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.NativeImage;
import org.mtr.mod.resource.SignResource;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 图标贴图重着色：把标志贴图里的「矢量部分」（深色/彩色笔画）整体替换成用户选择的颜色，
 * 保留白色背景与透明边缘的过渡。
 *
 * <p>逐像素规则（MC NativeImage 颜色为 ABGR）：
 * <ul>
 *   <li>完全透明 → 保持透明；</li>
 *   <li>半透明（透明背景贴图）→ 用目标颜色，保留原 alpha（箭头/设施等线条图标）；</li>
 *   <li>不透明且接近白色 → 保留原白底；</li>
 *   <li>不透明且非白 → 按“离白色多暗/多灰”的程度从白色混合到目标颜色，保留抗锯齿边缘。</li>
 * </ul>
 *
 * <p>结果按「贴图 + 目标色 + 翻转 + 旋转 + 资源代际」缓存，使用 LRU 控制内存；
 * 旋转（-90/90/180）在重着色时一并烘焙，绘制端拿到的就是最终方向的图。</p>
 */
public final class CRTSignIconTint {

	private static final int MAX_CACHE = 32;
	private static final float WHITE_THRESHOLD = 245.0F;
	private static final Map<String, NativeImage> CACHE = new LinkedHashMap<String, NativeImage>(16, 0.75F, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, NativeImage> eldest) {
			if (size() > MAX_CACHE) {
				try {
					eldest.getValue().close();
				} catch (Throwable ignored) {
				}
				return true;
			}
			return false;
		}
	};

	private CRTSignIconTint() {
	}

	/**
	 * 取重着色后的图标贴图；失败/资源缺失时返回 null（调用方回退原贴图）。
	 *
	 * @param sign     标志资源（贴图来自 sign.getTexture()）
	 * @param argb     目标颜色（建议 ARGB，alpha 用于半透明贴图）
	 * @param flipped  是否水平镜像（与 sign.flipTexture 一致）
	 * @param rotation 图标旋转角度（-90 / 0 / 90 / 180，其它按 0 处理）
	 */
	@Nullable
	public static NativeImage tint(@Nullable SignResource sign, int argb, boolean flipped, int rotation) {
		if (sign == null) {
			return null;
		}
		final Identifier texture = sign.getTexture();
		if (texture == null) {
			return null;
		}
		final String key = texture + "|" + Integer.toHexString(argb) + "|" + (flipped ? "f" : "n") + "|"
				+ rotation + "|" + JSSignTextureBaker.getImageVersion();
		final NativeImage cached = CACHE.get(key);
		if (cached != null) {
			return cached;
		}
		final NativeImage source = JSSignTextureBaker.getTextureImage(texture);
		if (source == null) {
			return null;
		}
		NativeImage result = null;
		try {
			result = build(source, argb, flipped, rotation);
			CACHE.put(key, result);
		} catch (Throwable ignored) {
			if (result != null) {
				try {
					result.close();
				} catch (Throwable ignored2) {
				}
			}
			return null;
		}
		return result;
	}

	private static NativeImage build(NativeImage source, int argb, boolean flipped, int rotation) {
		final int sourceWidth = source.getWidth();
		final int sourceHeight = source.getHeight();
		final int normalized = ((rotation % 360) + 360) % 360;
		final int rotationIndex = normalized / 90;
		final int width = rotationIndex == 1 || rotationIndex == 3 ? sourceHeight : sourceWidth;
		final int height = rotationIndex == 1 || rotationIndex == 3 ? sourceWidth : sourceHeight;
		final NativeImage result = new NativeImage(width, height, false);
		final int targetA = (argb >>> 24) & 0xFF;
		final int targetR = (argb >> 16) & 0xFF;
		final int targetG = (argb >> 8) & 0xFF;
		final int targetB = argb & 0xFF;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				final int destX = flipped ? width - 1 - x : x;
				final int sourceX;
				final int sourceY;
				switch (rotationIndex) {
					case 1: // 90° 顺时针
						sourceX = y;
						sourceY = sourceHeight - 1 - destX;
						break;
					case 2: // 180°
						sourceX = sourceWidth - 1 - destX;
						sourceY = sourceHeight - 1 - y;
						break;
					case 3: // 270°（-90°）
						sourceX = sourceWidth - 1 - y;
						sourceY = destX;
						break;
					default:
						sourceX = destX;
						sourceY = y;
						break;
				}
				final int color = source.getColor(sourceX, sourceY);
				final int a = (color >>> 24) & 0xFF;
				if (a == 0) {
					result.setPixelColor(x, y, 0);
					continue;
				}
				final int r = (color >> 16) & 0xFF;
				final int g = (color >> 8) & 0xFF;
				final int b = color & 0xFF;
				if (a < 250) {
					// 透明背景贴图：直接换成目标色，保留 alpha（线条/箭头类图标）
					final int alpha = targetA <= 0 ? a : (a * targetA / 255);
					result.setPixelColor(x, y, pack(alpha, targetR, targetG, targetB));
				} else {
					final int minChannel = Math.min(r, Math.min(g, b));
					if (minChannel >= (int) WHITE_THRESHOLD) {
						// 白底 / 白字保留原样
						result.setPixelColor(x, y, color);
					} else {
						// 深色/彩色笔画：离白色越远越接近目标色（保留 AA 过渡）
						final float strength = Math.max(0.0F, Math.min(1.0F, (255.0F - minChannel) / 215.0F));
						final int outR = Math.round(255.0F - (255.0F - targetR) * strength);
						final int outG = Math.round(255.0F - (255.0F - targetG) * strength);
						final int outB = Math.round(255.0F - (255.0F - targetB) * strength);
						result.setPixelColor(x, y, pack(targetA <= 0 ? 255 : targetA, outR, outG, outB));
					}
				}
			}
		}
		return result;
	}

	private static int pack(int a, int r, int g, int b) {
		return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
	}
}
