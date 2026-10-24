package com.Nanbin.client.JavaScriptSupport;

import com.Nanbin.client.Render.RenderCRTRailwaySign;

/**
 * JS 文本绘制入口：{@code Text.create().pos(x, y).size(w, h).text("...").color(0xFFFFFF).scale(1.0).draw(ctx)}
 * 可用 {@code Text.measure("...", height)} 测量文本渲染宽度（世界单位）。
 */
public final class JSText {

	public JSTextBuilder create() {
		return new JSTextBuilder();
	}

	/**
	 * 测量文本按当前渲染规则渲染后的自然宽度（世界单位）。
	 * height 为文字框高度（如 sign.getHeight()），多行文本返回最宽行宽度。
	 */
	public double measure(String text, double height) {
		return RenderCRTRailwaySign.measureTextWidth(text, (float) height);
	}

	public static final class JSTextBuilder {

		private double x;
		private double y;
		private double w;
		private double h;
		private String text = "";
		private double color = 0xFF000000;
		private double scale = 1.0;
		private boolean bold;
		private boolean centered = true;
		private boolean hasSize;

		public JSTextBuilder pos(double x, double y) {
			this.x = x;
			this.y = y;
			return this;
		}

		public JSTextBuilder size(double w, double h) {
			this.w = w;
			this.h = h;
			hasSize = true;
			return this;
		}

		public JSTextBuilder text(String text) {
			this.text = text == null ? "" : text;
			return this;
		}

		public JSTextBuilder color(double color) {
			this.color = color;
			return this;
		}

		public JSTextBuilder scale(double scale) {
			this.scale = scale;
			return this;
		}

		public JSTextBuilder bold(boolean bold) {
			this.bold = bold;
			return this;
		}

		/** 便捷方法：加粗文字。 */
		public JSTextBuilder bold() {
			this.bold = true;
			return this;
		}

		public JSTextBuilder centered(boolean centered) {
			this.centered = centered;
			return this;
		}

		/** 便捷方法：文字在框内水平+垂直居中（中英文混合时按字形宽度对齐），等价于 centered(true)。 */
		public JSTextBuilder centered() {
			this.centered = true;
			return this;
		}

		public void draw(JSDrawContext ctx) {
			final double drawW = hasSize ? w : ctx.getWidth() - x;
			final double drawH = hasSize ? h : ctx.getHeight() - y;
			ctx.drawText(text, x, y, drawW, drawH, color, scale, bold, centered);
		}
	}
}
