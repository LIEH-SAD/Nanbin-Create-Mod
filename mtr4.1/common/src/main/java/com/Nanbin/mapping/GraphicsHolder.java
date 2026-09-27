package com.Nanbin.mapping;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.RotationAxis;

/**
 * 取代 MTR 4.0 的 {@code org.mtr.mapping.mapper.GraphicsHolder}。
 * <p>
 * 4.1 删掉了这个包装层（绘制直接用原版 {@link DrawContext} + {@link MatrixStack}），
 * 这里按 4.0 的调用面重建一层薄适配，让移植过来的界面代码改动最小：
 * push/pop、translate、rotateXDegrees/rotateYDegrees/rotateZDegrees、drawText、drawCenteredText、
 * getTextWidth，以及静态的 {@link #getDefaultLight()}。
 * <p>
 * GUI 里的亮度是固定的（不走世界光照），所以 {@code light} 参数被忽略。
 */
public final class GraphicsHolder {

	/** 原版 GUI 默认亮度值（0xF000F0）。 */
	public static final int DEFAULT_LIGHT = 0xF000F0;

	private final DrawContext context;
	private final MatrixStack matrices;

	public GraphicsHolder(DrawContext context) {
		this.context = context;
		this.matrices = context.getMatrices();
	}

	public GraphicsHolder(DrawContext context, MatrixStack matrices) {
		this.context = context;
		this.matrices = matrices;
	}

	public static int getDefaultLight() {
		return DEFAULT_LIGHT;
	}

	/** 静态版字宽测量（对应 4.0 的 {@code GraphicsHolder.getTextWidth}）。 */
	public static int getTextWidthFor(String text) {
		return net.minecraft.client.MinecraftClient.getInstance().textRenderer.getWidth(text);
	}

	public DrawContext getContext() {
		return context;
	}

	public MatrixStack getMatrices() {
		return matrices;
	}

	public void push() {
		matrices.push();
	}

	public void pop() {
		matrices.pop();
	}

	public void translate(double x, double y, double z) {
		matrices.translate(x, y, z);
	}

	public void translate(float x, float y) {
		matrices.translate(x, y, 0);
	}

	public void scale(float scaleX, float scaleY, float scaleZ) {
		matrices.scale(scaleX, scaleY, scaleZ);
	}

	public void rotateXDegrees(float degrees) {
		matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(degrees));
	}

	public void rotateYDegrees(float degrees) {
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(degrees));
	}

	public void rotateZDegrees(float degrees) {
		matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(degrees));
	}

	public TextRenderer getTextRenderer() {
		return net.minecraft.client.MinecraftClient.getInstance().textRenderer;
	}

	/** 返回绘制后的结束 X 坐标。 */
	public int drawText(String text, int x, int y, int color, boolean shadow, int light) {
		return shadow
				? context.drawTextWithShadow(getTextRenderer(), text, x, y, color)
				: context.drawText(getTextRenderer(), text, x, y, color, false);
	}

	public int drawText(Text text, int x, int y, int color, boolean shadow, int light) {
		return shadow
				? context.drawTextWithShadow(getTextRenderer(), text, x, y, color)
				: context.drawText(getTextRenderer(), text, x, y, color, false);
	}

	public int drawCenteredText(String text, int centerX, int y, int color, boolean shadow, int light) {
		return drawText(text, centerX - getTextWidth(text) / 2, y, color, shadow, light);
	}

	public int drawCenteredText(Text text, int centerX, int y, int color, boolean shadow, int light) {
		return drawText(text, centerX - getTextWidth(text) / 2, y, color, shadow, light);
	}

	/*
	 * 字宽测量是静态的（对应 4.0 的 GraphicsHolder.getTextWidth 静态调用），
	 * 用游戏内的文本渲染器测量；写成静态后通过实例调用也能编译，两种调用风格都兼容。
	 */
	public static int getTextWidth(String text) {
		return net.minecraft.client.MinecraftClient.getInstance().textRenderer.getWidth(text);
	}

	public static int getTextWidth(Text text) {
		return net.minecraft.client.MinecraftClient.getInstance().textRenderer.getWidth(text);
	}
}
