package com.Nanbin.client.Drawing;

import net.minecraft.client.texture.NativeImage;

/**
 * {@code NativeImage} 的单像素写入（1.21.4 版），颜色按 <b>ABGR</b> 字节序传入。
 * <p>
 * 1.21.4 把原来那个「按 ABGR 写入」的方法改成了 private（{@code setColor} = 官方的
 * {@code setPixelABGR}），公开出来的是 ARGB 版的 {@code setColorArgb}（官方的
 * {@code setPixel}）。所以这里手动把 ABGR 重排成 ARGB 再写，
 * 保证调色板上传上去的颜色通道不会 R/B 互换。
 * <p>
 * 必须<b>直接调用</b>，不能按方法名反射：发布后的 Fabric 用 intermediary 名，
 * NeoForge 用 Mojang 官方名，都不是 Yarn 的名字；
 * 而且反射用 publicLookup 也够不到 private 的 {@code setColor}，只会落到 ARGB 版上。
 * <p>
 * 1.21.1 的写法见 {@code common/src/versioned/1.21.1}。
 */
public final class NativePixelWriter {

	private NativePixelWriter() {
	}

	/** 写入 ABGR 顺序的颜色（A 在最高字节，R 在最低字节）。 */
	public static void setPixelAbgr(NativeImage image, int x, int y, int abgr) {
		image.setColorArgb(x, y, abgrToArgb(abgr));
	}

	/** ABGR → ARGB：A、G 位置不变，R 与 B 互换。 */
	private static int abgrToArgb(int abgr) {
		return (abgr & 0xFF00FF00) | ((abgr & 0x000000FF) << 16) | ((abgr >>> 16) & 0x000000FF);
	}
}
