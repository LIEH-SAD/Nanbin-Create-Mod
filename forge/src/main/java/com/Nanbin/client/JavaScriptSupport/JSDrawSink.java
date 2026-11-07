package com.Nanbin.client.JavaScriptSupport;

import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.NativeImage;

/**
 * JS 指示牌元素绘制的接收器。烘焙贴图时用它收集元素（按绘制顺序合成到画布），
 * 未设置时各 render 方法仍走原来的逐元素 scheduleRender。
 *
 * <p>除既有的贴图/像素图/旋转矩形接口外，本类还提供一组带默认实现的几何接口
 * （矩形、多边形、旋转贴图），供 CRT 导视牌自由布局的绘制使用；
 * 默认实现退化为贴图/不绘制，因此不影响任何既有调用方。</p>
 */
public abstract class JSDrawSink {

	/** MTR 纯白贴图：默认 fillRect 用它合成纯色矩形（CanvasSink 会识别并直接填色）。 */
	public static final Identifier WHITE_TEXTURE = new Identifier("mtr", "textures/block/white.png");

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

	/** 画一块水平镜像的贴图（crt_signs.json 的 flipTexture）；默认退化为不镜像。 */
	public void drawTextureFlipped(Identifier textureId, float x, float y, float width, float height, int color) {
		drawTexture(textureId, x, y, width, height, color);
	}

	/** 已知像素图时直接合成（文字贴图用，省去回查纹理）。 */
	public abstract void drawImage(NativeImage image, float x, float y, float width, float height, int color);

	/** 画一块绕中心旋转的矩形（JS 的 Line 用它画斜线）。 */
	public abstract void drawRotatedQuad(float centerX, float centerY, float width, float height, float radians, int color);

	// ─── 新增几何接口（带默认实现，只增不改既有行为） ─────────────────

	/**
	 * 画一块纯色矩形（色条、元素盒背景、灰框等）。
	 * 默认实现退化为 MTR 纯白贴图 + 颜色合成。
	 */
	public void fillRect(float x, float y, float width, float height, int color) {
		if (width <= 0.0F || height <= 0.0F) {
			return;
		}
		drawTexture(WHITE_TEXTURE, x, y, width, height, color == -1 ? 0xFFFFFFFF : color);
	}

	/**
	 * 画一个实心多边形（CRT 箭头元素用它绘制三块箭头形状）。
	 * 点集为交错的 {@code xs[i], ys[i]}，坐标与其它绘制接口同一空间。
	 * 默认实现不绘制（没有多边形光栅化能力的 sink 直接跳过）。
	 */
	public void fillPolygon(float[] xs, float[] ys, int color) {
	}

	/**
	 * 画一块绕自身中心旋转的贴图（图标元素的 rotation 属性）。
	 * 参数为贴图中心点与未旋转时的尺寸；radians 为顺时针弧度（与 SVG rotate 正方向一致）。
	 * 默认实现忽略旋转，退化为不旋转的贴图。
	 */
	public void drawTextureRotated(Identifier textureId, float centerX, float centerY, float width, float height, float radians, int color) {
		if (width <= 0.0F || height <= 0.0F) {
			return;
		}
		drawTexture(textureId, centerX - width / 2.0F, centerY - height / 2.0F, width, height, color);
	}
}
