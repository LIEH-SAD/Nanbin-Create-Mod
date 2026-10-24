package com.Nanbin.client.Drawing;

import net.minecraft.client.texture.NativeImage;

/**
 * {@code NativeImage} 的单像素写入（1.21.1 版），颜色按 <b>ABGR</b> 字节序传入。
 * <p>
 * 必须<b>直接调用</b>，不能按方法名反射：发布后的 Fabric 用 intermediary 名，
 * NeoForge 用 Mojang 官方名（{@code setPixelRGBA}），都不是 Yarn 的 {@code setColor}。
 * <p>
 * 1.21.4 把这个方法改成了 private，公开的那个是 ARGB 版，所以两边写法不同，
 * 见 {@code common/src/versioned/1.21.4}。
 */
public final class NativePixelWriter {

	private NativePixelWriter() {
	}

	/** 写入 ABGR 顺序的颜色（A 在最高字节，R 在最低字节）。 */
	public static void setPixelAbgr(NativeImage image, int x, int y, int abgr) {
		image.setColor(x, y, abgr);
	}
}
