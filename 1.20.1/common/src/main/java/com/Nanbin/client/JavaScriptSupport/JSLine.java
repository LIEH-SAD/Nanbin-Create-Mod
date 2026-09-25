package com.Nanbin.client.JavaScriptSupport;

/**
 * JS 线段绘制入口：{@code Line.create().from(x1, y1).to(x2, y2).width(0.05).color(0xFFFFFF).draw(ctx)}
 */
public final class JSLine {

	public JSLineBuilder create() {
		return new JSLineBuilder();
	}

	public static final class JSLineBuilder {

		private double x1;
		private double y1;
		private double x2;
		private double y2;
		private double width = 0.05;
		private double color = 0xFF000000;

		public JSLineBuilder from(double x1, double y1) {
			this.x1 = x1;
			this.y1 = y1;
			return this;
		}

		public JSLineBuilder to(double x2, double y2) {
			this.x2 = x2;
			this.y2 = y2;
			return this;
		}

		public JSLineBuilder width(double width) {
			this.width = width;
			return this;
		}

		public JSLineBuilder color(double color) {
			this.color = color;
			return this;
		}

		public void draw(JSDrawContext ctx) {
			ctx.drawLine(x1, y1, x2, y2, width, color);
		}
	}
}
