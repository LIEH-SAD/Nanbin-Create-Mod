package com.Nanbin.mapping;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.function.Function;

/**
 * 取代 MTR 4.0 的 {@code org.mtr.mapping.mapper.GuiDrawing}。
 * <p>
 * 4.0 的这层包装是为了自己管理顶点缓冲；4.1 直接用原版 {@link DrawContext}，
 * 所以这里只需要把 4.0 的调用面映射到 {@code DrawContext}：
 * <ul>
 *     <li>{@code begin/finishDrawingTexture}、{@code begin/finishDrawingRectangle} → 空操作（DrawContext 自行批处理）</li>
 *     <li>{@code drawRectangle(x1, y1, x2, y2, color)} → {@code DrawContext#fill}</li>
 *     <li>{@code drawTexture(id, x, y, w, h, u, v, texW, texH)} → 见下面的跨版本垫片</li>
 * </ul>
 * <b>跨版本差异</b>：1.21.4 的 {@code DrawContext#drawTexture} 第一个参数变成了
 * {@code Function<Identifier, RenderLayer>}（1.21.1 没有），所以这里在类初始化时解析出可用的那一版。
 */
public final class GuiDrawing {

	private final GraphicsHolder graphicsHolder;
	private final DrawContext context;
	/** 4.0 的 {@code beginDrawingTexture(Identifier)} 绑定的当前纹理。 */
	private Identifier currentTexture;

	public GuiDrawing(GraphicsHolder graphicsHolder) {
		this.graphicsHolder = graphicsHolder;
		this.context = graphicsHolder.getContext();
	}

	public GraphicsHolder getGraphicsHolder() {
		return graphicsHolder;
	}

	/** 绑定接下来要绘制的纹理（对应 4.0 的同名方法）。 */
	public void beginDrawingTexture(Identifier texture) {
		this.currentTexture = texture;
	}

	public Identifier getCurrentTexture() {
		return currentTexture;
	}

	/*
	 * TODO(界面批次)：4.0 的 GuiDrawing 还有一组「归一化 UV」重载：
	 *     drawTexture(float x1, float y1, float x2, float y2, float u1, float v1, float u2, float v2)
	 * 它按 0~1 的 UV 绘制并允许拉伸；而原版 DrawContext#drawTexture 的 UV 是像素单位、且按 1:1 铺贴。
	 * 两者语义不同（缩放时结果会不一样），需要逐个调用点确认期望的映射方式（是否按纹理真实尺寸换算 UV 跨度），
	 * 因此暂不提供这组重载，避免给出行为错误的实现。
	 */

	public void beginDrawingTexture() {
		// DrawContext 自行批处理，无需手动开关
	}
	public void finishDrawingTexture() {
	}

	public void beginDrawingRectangle() {
	}

	public void finishDrawingRectangle() {
	}

	public void drawRectangle(int x1, int y1, int x2, int y2, int color) {
		context.fill(x1, y1, x2, y2, color);
	}

	/** 按「像素坐标 + UV + 纹理尺寸」绘制一张纹理。 */
	public void drawTexture(Identifier texture, int x, int y, int width, int height, float u, float v, int textureWidth, int textureHeight) {
		TEXTURE_DRAWER.draw(context, texture, x, y, width, height, u, v, textureWidth, textureHeight);
	}

	/**
	 * 4.0 风格的归一化 UV 重载：{@code (x1, y1, x2, y2, u1, v1, u2, v2)}，UV 为 0~1 且允许拉伸。
	 * <p>
	 * 原版 {@code DrawContext#drawTexture} 的 UV 是像素单位、并且用同一个尺寸同时当作「四边形大小」和
	 * 「UV 跨度」，所以没法直接表达「缩放绘制子矩形」。这里做一次等价换算：
	 * 取 {@code texW = 目标宽度 / UV 宽度跨度}，于是原版算出来的 UV 跨度恰好等于目标宽度，
	 * 既精确取到子矩形、又缩放到目标尺寸。
	 */
	public void drawTexture(float x1, float y1, float x2, float y2, float u1, float v1, float u2, float v2) {
		if (currentTexture == null) {
			return;
		}
		final float width = x2 - x1;
		final float height = y2 - y1;
		final float uSpan = u2 - u1;
		final float vSpan = v2 - v1;
		if (width <= 0 || height <= 0) {
			return;
		}
		if (uSpan <= 0 || vSpan <= 0) {
			// 退化情形：整张纹理铺满目标矩形
			drawTexture(currentTexture, Math.round(x1), Math.round(y1), Math.round(width), Math.round(height), 0, 0, 1, 1);
			return;
		}
		final float textureWidth = width / uSpan;
		final float textureHeight = height / vSpan;
		drawTexture(currentTexture, Math.round(x1), Math.round(y1), Math.round(width), Math.round(height),
				u1 * textureWidth, v1 * textureHeight, Math.max(1, Math.round(textureWidth)), Math.max(1, Math.round(textureHeight)));
	}

	@FunctionalInterface
	private interface TextureDrawer {
		void draw(DrawContext context, Identifier texture, int x, int y, int width, int height, float u, float v, int textureWidth, int textureHeight);
	}

	private static final TextureDrawer TEXTURE_DRAWER = resolveTextureDrawer();

	private static TextureDrawer resolveTextureDrawer() {
		// 1.21.4：(Function<Identifier, RenderLayer>, Identifier, int, int, float, float, int, int, int, int)
		for (final Method method : DrawContext.class.getMethods()) {
			if (!method.getName().equals("drawTexture") || method.getParameterCount() != 10 || !Function.class.isAssignableFrom(method.getParameterTypes()[0])) {
				continue;
			}
			try {
				method.setAccessible(true);
				final MethodHandle handle = MethodHandles.publicLookup().unreflect(method);
				final Function<Identifier, net.minecraft.client.render.RenderLayer> layerGetter = resolveGuiTexturedRenderLayer();
				if (layerGetter != null) {
					return (context, texture, x, y, width, height, u, v, textureWidth, textureHeight) -> {
						try {
							handle.invoke(context, layerGetter, texture, x, y, u, v, width, height, textureWidth, textureHeight);
						} catch (Throwable e) {
							throw new IllegalStateException("绘制纹理失败", e);
						}
					};
				}
			} catch (IllegalAccessException ignored) {
				// 试下一个重载
			}
		}

		// 1.21.1：(Identifier, int, int, float, float, int, int, int, int)
		for (final Method method : DrawContext.class.getMethods()) {
			if (!method.getName().equals("drawTexture") || method.getParameterCount() != 9 || !Identifier.class.isAssignableFrom(method.getParameterTypes()[0])) {
				continue;
			}
			try {
				method.setAccessible(true);
				final MethodHandle handle = MethodHandles.publicLookup().unreflect(method);
				return (context, texture, x, y, width, height, u, v, textureWidth, textureHeight) -> {
					try {
						handle.invoke(context, texture, x, y, u, v, width, height, textureWidth, textureHeight);
					} catch (Throwable e) {
						throw new IllegalStateException("绘制纹理失败", e);
					}
				};
			} catch (IllegalAccessException ignored) {
				// 试下一个重载
			}
		}

		throw new IllegalStateException("当前版本的 DrawContext 没有可用的 drawTexture 重载");
	}

	/** 1.21.4 的 {@code RenderLayer.getGuiTextured(Identifier)}（1.21.1 没有，返回 null）。 */
	private static Function<Identifier, net.minecraft.client.render.RenderLayer> resolveGuiTexturedRenderLayer() {
		try {
			final MethodHandle handle = MethodHandles.publicLookup().findStatic(
					net.minecraft.client.render.RenderLayer.class, "getGuiTextured",
					MethodType.methodType(net.minecraft.client.render.RenderLayer.class, Identifier.class));
			return identifier -> {
				try {
					return (net.minecraft.client.render.RenderLayer) handle.invoke(identifier);
				} catch (Throwable e) {
					throw new IllegalStateException("获取 GUI 渲染层失败", e);
				}
			};
		} catch (NoSuchMethodException | IllegalAccessException e) {
			return null;
		}
	}
}
