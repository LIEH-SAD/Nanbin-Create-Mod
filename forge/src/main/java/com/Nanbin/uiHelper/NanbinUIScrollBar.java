package com.Nanbin.uiHelper;

import org.mtr.mapping.holder.TextRenderer;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;

/**
 * 滚动条组件：由 {@link NanbinUIScreen} 摆放在小标题与配置项的右侧，
 * 负责展示当前滚动位置并支持按住拖动。
 */
public class NanbinUIScrollBar {

	public static final int WIDTH = 6;
	private static final int TRACK_COLOR = 0x40000000;
	private static final int THUMB_COLOR = 0xB0FFFFFF;
	private static final int MIN_THUMB_HEIGHT = 10;

	private int x;
	private int y;
	private int height;

	private boolean dragging;
	/** 按下时鼠标与滑块顶端的偏移，避免拖动时滑块跳变。 */
	private double dragOffset;

	public void setBounds(int x, int y, int height) {
		this.x = x;
		this.y = y;
		this.height = Math.max(0, height);
	}

	public int getX() {
		return x;
	}

	public int getWidth() {
		return WIDTH;
	}

	public boolean isDragging() {
		return dragging;
	}

	/** 内容超出可视范围时才需要显示滚动条。 */
	public boolean isScrollable(int contentHeight, int viewHeight) {
		return contentHeight > viewHeight && height > 0;
	}

	public void render(GraphicsHolder graphicsHolder, int contentHeight, int viewHeight, double scroll) {
		if (!isScrollable(contentHeight, viewHeight)) {
			return;
		}

		final int thumbHeight = getThumbHeight(contentHeight, viewHeight);
		final int thumbY = y + (int) Math.round((height - thumbHeight) * (scroll / getMaxScroll(contentHeight, viewHeight)));
		final GuiDrawing guiDrawing = new GuiDrawing(graphicsHolder);
		guiDrawing.beginDrawingRectangle();
		guiDrawing.drawRectangle(x, y, x + WIDTH, y + height, TRACK_COLOR);
		guiDrawing.drawRectangle(x, thumbY, x + WIDTH, thumbY + thumbHeight, THUMB_COLOR);
		guiDrawing.finishDrawingRectangle();
	}

	/** 点击命中滚动条时开始拖动，返回 true 表示事件已消费。 */
	public boolean mouseClicked(double mouseX, double mouseY, int button, int contentHeight, int viewHeight, double scroll) {
		if (button != 0 || !isScrollable(contentHeight, viewHeight)) {
			return false;
		}
		if (mouseX < x || mouseX >= x + WIDTH || mouseY < y || mouseY >= y + height) {
			return false;
		}

		final int thumbHeight = getThumbHeight(contentHeight, viewHeight);
		dragging = true;
		dragOffset = mouseY - (y + getThumbOffset(thumbHeight, contentHeight, viewHeight, scroll));
		return true;
	}

	public void mouseReleased() {
		dragging = false;
	}

	/** 拖动中根据鼠标位置换算新的滚动量；未在拖动时返回原值。 */
	public double mouseDragged(double mouseY, int contentHeight, int viewHeight, double scroll) {
		if (!dragging || !isScrollable(contentHeight, viewHeight)) {
			return scroll;
		}

		final int thumbHeight = getThumbHeight(contentHeight, viewHeight);
		final double thumbTop = mouseY - dragOffset - y;
		final double travel = height - thumbHeight;
		if (travel <= 0) {
			return 0;
		}

		final double ratio = Math.max(0.0, Math.min(1.0, thumbTop / travel));
		return ratio * getMaxScroll(contentHeight, viewHeight);
	}

	private static double getMaxScroll(int contentHeight, int viewHeight) {
		return Math.max(0, contentHeight - viewHeight);
	}

	private int getThumbHeight(int contentHeight, int viewHeight) {
		return Math.max(MIN_THUMB_HEIGHT, (int) ((float) height * viewHeight / contentHeight));
	}

	private int getThumbOffset(int thumbHeight, int contentHeight, int viewHeight, double scroll) {
		final double maxScroll = getMaxScroll(contentHeight, viewHeight);
		return maxScroll <= 0 ? 0 : (int) Math.round((height - thumbHeight) * (scroll / maxScroll));
	}
}
