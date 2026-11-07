package com.Nanbin.client.CRTRailwaySignEditor.render;

import com.Nanbin.client.JavaScriptSupport.JSSignTextureBaker;
import com.Nanbin.client.CRTRailwaySignEditor.editor.CRTEditorTheme;
import com.Nanbin.signage.crt.CRTSignDataContext;
import com.Nanbin.signage.crt.CRTSignLayout;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;

import javax.annotation.Nullable;

/**
 * CRT 导视牌编辑界面的画布预览（中栏顶部）。
 *
 * <p>预览不是另写一套画法：它调用 {@link CRTSignLayoutPainter#layout} 得到布局帧，再复用
 * {@link CRTSignLayoutPainter#bake} 的同一张贴图（与游戏内世界渲染完全一致），
 * 在画布区域内等比缩放绘制；选中框、行 hover 带、行分隔线等编辑器叠加层由
 * {@link GuiDrawing} 画在贴图之上（不烘焙进牌面）。</p>
 *
 * <p>返回的 {@link Transform} 负责「设计像素坐标 ↔ 屏幕 GUI 坐标」换算，
 * 编辑界面用它做命中测试（{@link Transform#hitTest}）与选中框定位。</p>
 */
public final class CRTSignLayoutPreview {

	private CRTSignLayoutPreview() {
	}

	/** 画布区域底色（网页 --bg 浅灰）。 */
	public static void drawBackground(@Nullable GraphicsHolder graphics, float x, float y, float width, float height) {
		if (graphics == null || width <= 0.0F || height <= 0.0F) {
			return;
		}
		final GuiDrawing guiDrawing = new GuiDrawing(graphics);
		guiDrawing.beginDrawingRectangle();
		guiDrawing.drawRectangle(x, y, x + width, y + height, CRTEditorTheme.BG);
		guiDrawing.finishDrawingRectangle();
	}

	/**
	 * 在 (x, y, width, height) 区域里按等比缩放画出整块牌 + 编辑器叠加层。
	 *
	 * @param cacheKey          预览实例标识（同一块牌用同一个字符串，建议带方块坐标）
	 * @param selectedElementId 需要画选中框的元素 id；null 表示无选中
	 * @param hoverRowIndex     需要高亮的行号；&lt; 0 表示无
	 * @param drawRowChrome     是否画行分隔线（行带）等编辑器行控件
	 * @return 坐标换算对象（供屏幕做命中测试/覆盖层定位）；参数非法时返回 null
	 */
	@Nullable
	public static Transform draw(@Nullable GraphicsHolder graphics, float x, float y, float width, float height,
			@Nullable CRTSignLayout layout, @Nullable CRTSignDataContext data, @Nullable String cacheKey,
			@Nullable String selectedElementId, int hoverRowIndex, boolean drawRowChrome) {
		if (graphics == null || layout == null || width <= 0.0F || height <= 0.0F) {
			return null;
		}

		// 布局（预览每帧都要用它做命中测试；文本测量有缓存，开销很小）
		final CRTSignLayoutFrame frame = CRTSignLayoutPainter.layout(layout, data);
		final float contentRatio = Math.min(width / Math.max(1.0F, frame.width), height / Math.max(1.0F, frame.height));
		final float drawWidth = Math.max(1.0F, frame.width * contentRatio);
		final float drawHeight = Math.max(1.0F, frame.height * contentRatio);
		final float contentX = x + (width - drawWidth) / 2.0F;
		final float contentY = y + (height - drawHeight) / 2.0F;
		final Transform transform = new Transform(contentX, contentY, drawWidth, drawHeight, contentRatio, frame);

		// 画布区底色
		drawBackground(graphics, x, y, width, height);

		final GuiDrawing guiDrawing = new GuiDrawing(graphics);

		// 牌面底色（贴图烘焙失败时至少还有底色）
		guiDrawing.beginDrawingRectangle();
		guiDrawing.drawRectangle(contentX, contentY, contentX + drawWidth, contentY + drawHeight, frame.backgroundColor);
		guiDrawing.finishDrawingRectangle();

		// 牌面内容：与世界渲染同一套 bake 结果贴图（烘焙异常时退化为只显示底色，不让界面崩）
		JSSignTextureBaker.Baked baked = null;
		try {
			baked = CRTSignLayoutPainter.bakeFrame(frame, layout, data, cacheKey);
		} catch (Throwable ignored) {
		}
		if (baked != null) {
			guiDrawing.beginDrawingTexture(baked.identifier);
			guiDrawing.drawTexture(contentX, contentY, contentX + drawWidth, contentY + drawHeight, 0.0F, 0.0F, 1.0F, 1.0F);
			guiDrawing.finishDrawingTexture();
		}

		// ── 编辑器叠加层（不烘焙进牌面） ──
		guiDrawing.beginDrawingRectangle();

		// 行 hover 高亮：只画上下两条边线（MC 自绘矩形对 alpha 不可靠，半透明填充会盖住牌面内容）
		if (hoverRowIndex >= 0 && hoverRowIndex < frame.rows.size()) {
			final CRTSignLayoutFrame.RowFrame hoverRow = frame.rows.get(hoverRowIndex);
			final float rowTop = transform.toScreenY(hoverRow.y);
			final float rowBottom = transform.toScreenY(hoverRow.y + hoverRow.height);
			guiDrawing.drawRectangle(contentX, rowTop, contentX + drawWidth, rowTop + 1.0F, 0xFFB4C2D6);
			guiDrawing.drawRectangle(contentX, rowBottom - 1.0F, contentX + drawWidth, rowBottom, 0xFFB4C2D6);
		}

		// 行分隔线（行带 chrome）：每行底部一条极淡的分隔线，方便看出可以拖放的行
		if (drawRowChrome) {
			for (int rowIndex = 0; rowIndex < frame.rows.size(); rowIndex++) {
				final CRTSignLayoutFrame.RowFrame row = frame.rows.get(rowIndex);
				final float lineY = transform.toScreenY(row.y + row.height) - 0.5F;
				if (rowIndex < frame.rows.size() - 1) {
					guiDrawing.drawRectangle(contentX, lineY, contentX + drawWidth, lineY + 1.0F, CRTEditorTheme.BORDER_SOFT);
				}
			}
		}

		// 牌面外描边（白色牌面在浅灰画布上需要边界）
		drawBorder(guiDrawing, contentX, contentY, contentX + drawWidth, contentY + drawHeight, 1.0F, CRTEditorTheme.BORDER);
		guiDrawing.finishDrawingRectangle();

		// 选中元素：半透明浅蓝底 + 主色描边（网页 .element.selected 的虚线框简化为实线）
		if (selectedElementId != null) {
			final CRTSignLayoutFrame.ElementFrame selected = frame.findElement(selectedElementId);
			if (selected != null) {
				final float x1 = transform.toScreenX(selected.drawX());
				final float y1 = transform.toScreenY(selected.y);
				final float x2 = transform.toScreenX(selected.drawX() + selected.boxWidth());
				final float y2 = transform.toScreenY(selected.y + selected.height);
				// 选中框只画描边（半透明填充在 MC 自绘矩形里不可靠，会盖住元素内容）
				guiDrawing.beginDrawingRectangle();
				drawBorder(guiDrawing, x1, y1, x2, y2, 2.0F, CRTEditorTheme.SELECTION);
				guiDrawing.finishDrawingRectangle();
			}
		}

		return transform;
	}

	/** 画一圈矩形边框（在已 beginDrawingRectangle 的会话里调用）。 */
	private static void drawBorder(GuiDrawing guiDrawing, float x1, float y1, float x2, float y2, float thickness, int color) {
		final float t = Math.max(1.0F, thickness);
		guiDrawing.drawRectangle(x1, y1, x2, y1 + t, color);
		guiDrawing.drawRectangle(x1, y2 - t, x2, y2, color);
		guiDrawing.drawRectangle(x1, y1, x1 + t, y2, color);
		guiDrawing.drawRectangle(x2 - t, y1, x2, y2, color);
	}

	/**
	 * 预览的坐标换算：设计像素坐标 ↔ 屏幕 GUI 坐标。
	 * 编辑界面用 {@link #toScreenX}/{@link #toScreenY} 摆放选中框/手柄，
	 * 用 {@link #toLayoutX}/{@link #toLayoutY}/{@link #hitTest} 做命中测试。
	 */
	public static final class Transform {
		/** 牌面内容区左上角屏幕 x。 */
		public final float x;
		/** 牌面内容区左上角屏幕 y。 */
		public final float y;
		/** 牌面内容区绘制宽（屏幕像素）。 */
		public final float width;
		/** 牌面内容区绘制高（屏幕像素）。 */
		public final float height;
		/** 设计像素 → 屏幕像素的缩放（width / frame.width）。 */
		public final float scale;
		/** 布局帧（命中测试用）。 */
		public final CRTSignLayoutFrame frame;

		public Transform(float x, float y, float width, float height, float scale, CRTSignLayoutFrame frame) {
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
			this.scale = scale;
			this.frame = frame;
		}

		public float toScreenX(float layoutX) {
			return x + layoutX * scale;
		}

		public float toScreenY(float layoutY) {
			return y + layoutY * scale;
		}

		public float toLayoutX(float screenX) {
			return scale == 0.0F ? 0.0F : (screenX - x) / scale;
		}

		public float toLayoutY(float screenY) {
			return scale == 0.0F ? 0.0F : (screenY - y) / scale;
		}

		/** 屏幕坐标命中的元素；没有命中返回 null。 */
		@Nullable
		public CRTSignLayoutFrame.ElementFrame hitTest(float screenX, float screenY) {
			return frame.hitTest(toLayoutX(screenX), toLayoutY(screenY));
		}

		/** 屏幕坐标命中的行号；牌面外返回 -1。 */
		public int rowAt(float screenY) {
			return frame.rowAt(toLayoutY(screenY));
		}
	}
}
