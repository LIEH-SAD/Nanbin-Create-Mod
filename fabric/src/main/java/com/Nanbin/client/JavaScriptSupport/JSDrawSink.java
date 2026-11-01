package com.Nanbin.client.JavaScriptSupport;

import org.mtr.mapping.holder.Identifier;

/**
 * JS 指示牌元素绘制的接收器。烘焙贴图时用它收集元素（按绘制顺序合成到画布），
 * 未设置时各 render 方法仍走原来的逐元素 scheduleRender。
 */
public abstract class JSDrawSink {

	private static final ThreadLocal<JSDrawSink> CURRENT = new ThreadLocal<>();

	public static JSDrawSink current() {
		return CURRENT.get();
	}

	public static void begin(JSDrawSink sink) {
		CURRENT.set(sink);
	}

	public static void end() {
		CURRENT.remove();
	}

	/** 画一块贴图（color 为 -1 表示按贴图原色）。 */
	public abstract void drawTexture(Identifier textureId, float x, float y, float width, float height, int color);

	/** 已知像素图时直接合成（文字贴图用，省去回查纹理）。 */
	public abstract void drawImage(org.mtr.mapping.holder.NativeImage image, float x, float y, float width, float height, int color);

	/** 画一块绕中心旋转的矩形（JS 的 Line 用它画斜线）。 */
	public abstract void drawRotatedQuad(float centerX, float centerY, float width, float height, float radians, int color);
}
