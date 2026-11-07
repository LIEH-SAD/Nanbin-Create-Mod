package com.Nanbin.mapping;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.joml.Quaternionf;

/**
 * MTR 4.0 {@code org.mtr.mapping.mapper.GraphicsHolder} 在本工程的等价物。
 *
 * <p>MTR 4.0 把「矩阵 + 顶点消费者（世界里）」和「DrawContext（界面里）」统一成一个
 * 图形上下文对象传给渲染器与屏幕；MTR 3.6.3 没有这层抽象，渲染器直接拿
 * {@link MatrixStack}+{@link VertexConsumerProvider}，屏幕直接拿 {@link DrawContext}。
 * 移植过来的渲染/绘制代码全部按 MTR 4.0 的形状书写，这里用最小实现把它接回 MTR 3 的调用方式，
 * 从而让移植文件只改 import 就能保持原貌。</p>
 *
 * <p>两种构造方式对应两个渲染场景：</p>
 * <ul>
 *   <li>{@code new GraphicsHolder(matrices, vertexConsumers)}：方块实体渲染器（世界空间）；</li>
 *   <li>{@code new GraphicsHolder(drawContext)}：Screen（GUI 空间）。</li>
 * </ul>
 */
public final class GraphicsHolder {

	/** MTR 4.0 的默认亮度（等价于 {@code 0xF000F0}）。 */
	public static int getDefaultLight() {
		return LightmapTextureManager.MAX_LIGHT_COORDINATE;
	}

	private final MatrixStack matrices;
	private final VertexConsumerProvider vertexConsumers;
	private final DrawContext drawContext;

	public GraphicsHolder(MatrixStack matrices, VertexConsumerProvider vertexConsumers) {
		this.matrices = matrices;
		this.vertexConsumers = vertexConsumers;
		this.drawContext = null;
	}

	public GraphicsHolder(DrawContext drawContext) {
		this.matrices = new MatrixStack();
		this.vertexConsumers = null;
		this.drawContext = drawContext;
	}

	public MatrixStack getMatrices() {
		return matrices;
	}

	public VertexConsumerProvider getVertexConsumers() {
		return vertexConsumers;
	}

	public DrawContext getDrawContext() {
		return drawContext;
	}

	public boolean isGui() {
		return drawContext != null;
	}

	// ---------------------------------------------------------------- transforms

	public void push() {
		matrices.push();
	}

	public void pop() {
		matrices.pop();
	}

	public void translate(double x, double y, double z) {
		matrices.translate(x, y, z);
	}

	public void scale(float x, float y, float z) {
		matrices.scale(x, y, z);
	}

	public void rotateXRadians(float radians) {
		matrices.multiply(new Quaternionf().rotateX(radians));
	}

	public void rotateYRadians(float radians) {
		matrices.multiply(new Quaternionf().rotateY(radians));
	}

	public void rotateZRadians(float radians) {
		matrices.multiply(new Quaternionf().rotateZ(radians));
	}

	public void rotateXDegrees(float degrees) {
		rotateXRadians(degrees * MathHelper.RADIANS_PER_DEGREE);
	}

	public void rotateYDegrees(float degrees) {
		rotateYRadians(degrees * MathHelper.RADIANS_PER_DEGREE);
	}

	public void rotateZDegrees(float degrees) {
		rotateZRadians(degrees * MathHelper.RADIANS_PER_DEGREE);
	}

	// ---------------------------------------------------------------- text

	private static TextRenderer textRenderer() {
		return MinecraftClient.getInstance().textRenderer;
	}

	public void drawText(String text, int x, int y, int color, boolean shadow, int light) {
		if (drawContext != null) {
			// GUI 空间不需要亮度参数
			drawContext.drawText(textRenderer(), text, x, y, color, shadow);
			return;
		}
		textRenderer().draw(text, (float) x, (float) y, color, shadow, matrices.peek().getPositionMatrix(), vertexConsumers, TextRenderer.TextLayerType.NORMAL, 0, light);
	}

	public void drawText(Text text, int x, int y, int color, boolean shadow, int light) {
		drawText(text.getString(), x, y, color, shadow, light);
	}

	public void drawText(MutableText text, int x, int y, int color, boolean shadow, int light) {
		drawText(text.getString(), x, y, color, shadow, light);
	}

	public void drawCenteredText(String text, int x, int y, int color) {
		drawText(text, x - getTextWidth(text) / 2, y, color, false, getDefaultLight());
	}

	public void drawCenteredText(Text text, int x, int y, int color) {
		drawCenteredText(text.getString(), x, y, color);
	}

	public void drawCenteredText(MutableText text, int x, int y, int color) {
		drawCenteredText(text.getString(), x, y, color);
	}

	public static int getTextWidth(String text) {
		return textRenderer().getWidth(text);
	}

	public static int getTextWidth(Text text) {
		return textRenderer().getWidth(text);
	}

	public static int getTextWidth(MutableText text) {
		return textRenderer().getWidth(text);
	}

	public static java.util.List<OrderedText> wrapLines(MutableText text, int width) {
		return textRenderer().wrapLines(text, width);
	}
}
