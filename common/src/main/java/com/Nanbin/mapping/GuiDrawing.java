package com.Nanbin.mapping;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/**
 * MTR 4.0 {@code org.mtr.mapping.mapper.GuiDrawing} 的等价物：GUI 空间矩形/贴图绘制。
 *
 * <p>MTR 4.0 里 {@code GuiDrawing} 是 {@link GraphicsHolder} 的配套对象，屏幕每帧
 * {@code new GuiDrawing(graphicsHolder)} 后连续画矩形与贴图。MTR 3.6.3 的界面绘制入口是
 * {@link DrawContext}，这里只做参数换算（MTR 4 用归一化 UV，原版用贴图像素 UV）。</p>
 */
public final class GuiDrawing {

	/** 归一化 UV 换算用的虚拟贴图尺寸（只要分子分母一致，缩放不失真）。 */
	private static final int UV_BASE = 256;

	private final DrawContext drawContext;
	private Identifier currentTexture;

	public GuiDrawing(GraphicsHolder graphicsHolder) {
		this.drawContext = graphicsHolder == null ? null : graphicsHolder.getDrawContext();
	}

	public GuiDrawing(DrawContext drawContext) {
		this.drawContext = drawContext;
	}

	// ---------------------------------------------------------------- rectangles

	public void beginDrawingRectangle() {
		// 原版 fill 自带 begin/end，无需预声明
	}

	public void finishDrawingRectangle() {
		// 同上
	}

	public void drawRectangle(double x1, double y1, double x2, double y2, int color) {
		if (drawContext == null) {
			return;
		}
		drawContext.fill(round(x1), round(y1), round(x2), round(y2), color);
	}

	/** MTR 4 的渐变矩形重载；移植代码只把它当描边色块用，这里退化为纯色填充。 */
	public void drawRectangle(double x1, double y1, double x2, double y2, double u1, double v1, double u2, double v2, int color) {
		drawRectangle(x1, y1, x2, y2, color);
	}

	// ---------------------------------------------------------------- textures

	public void beginDrawingTexture(Identifier texture) {
		currentTexture = texture;
	}

	public void finishDrawingTexture() {
		currentTexture = null;
	}

	public void drawTexture(double x1, double y1, double x2, double y2, float u1, float v1, float u2, float v2) {
		if (drawContext == null || currentTexture == null) {
			return;
		}
		final int x = round(x1);
		final int y = round(y1);
		final int width = Math.max(1, round(x2) - x);
		final int height = Math.max(1, round(y2) - y);
		final float u = u1 * UV_BASE;
		final float v = v1 * UV_BASE;
		final int uWidth = Math.max(1, Math.round((u2 - u1) * UV_BASE));
		final int vHeight = Math.max(1, Math.round((v2 - v1) * UV_BASE));
		drawContext.drawTexture(currentTexture, x, y, width, height, u, v, uWidth, vHeight, UV_BASE, UV_BASE);
	}

	private static int round(double value) {
		return (int) Math.round(value);
	}
}
