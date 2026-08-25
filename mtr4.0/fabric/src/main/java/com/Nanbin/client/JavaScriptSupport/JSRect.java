package com.Nanbin.client.JavaScriptSupport;

/**
 * JS 矩形绘制入口：{@code Rect.create().pos(x, y).size(w, h).color(0xFFFFFF).draw(ctx)}
 */
public final class JSRect {

	public JSRectBuilder create() {
		return new JSRectBuilder();
	}

	public static final class JSRectBuilder {

		private double x;
		private double y;
		private double w;
		private double h;
		private double color = 0xFFFFFFFF;
		private boolean hasSize;

		public JSRectBuilder pos(double x, double y) {
			this.x = x;
			this.y = y;
			return this;
		}

		public JSRectBuilder size(double w, double h) {
			this.w = w;
			this.h = h;
			hasSize = true;
			return this;
		}

		public JSRectBuilder color(double color) {
			this.color = color;
			return this;
		}

		public void draw(JSDrawContext ctx) {
			final double drawW = hasSize ? w : ctx.getWidth() - x;
			final double drawH = hasSize ? h : ctx.getHeight() - y;
			ctx.drawRect(x, y, drawW, drawH, color);
		}
	}
}
