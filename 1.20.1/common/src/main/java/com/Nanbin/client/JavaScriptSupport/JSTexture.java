package com.Nanbin.client.JavaScriptSupport;

/**
 * JS 纹理绘制入口：{@code Texture.create().texture("nanbin:logo.png").pos(x, y).size(w, h).draw(ctx)}
 */
public final class JSTexture {

	public JSTextureBuilder create() {
		return new JSTextureBuilder();
	}

	public static final class JSTextureBuilder {

		private double x;
		private double y;
		private double w;
		private double h;
		private String texture = "";
		private double color = 0xFFFFFFFF;
		private boolean hasSize;

		public JSTextureBuilder pos(double x, double y) {
			this.x = x;
			this.y = y;
			return this;
		}

		public JSTextureBuilder size(double w, double h) {
			this.w = w;
			this.h = h;
			hasSize = true;
			return this;
		}

		public JSTextureBuilder texture(String texture) {
			this.texture = texture == null ? "" : texture;
			return this;
		}

		public JSTextureBuilder color(double color) {
			this.color = color;
			return this;
		}

		public void draw(JSDrawContext ctx) {
			final double drawW = hasSize ? w : ctx.getWidth() - x;
			final double drawH = hasSize ? h : ctx.getHeight() - y;
			ctx.drawTexture(texture, x, y, drawW, drawH, color);
		}
	}
}
