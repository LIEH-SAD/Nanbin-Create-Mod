package com.Nanbin.uiHelper;

import org.mtr.mapping.mapper.ClickableWidgetExtension;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;
import org.mtr.mapping.mapper.TextHelper;

// 水平滚动条控件：拖动滑块或点击轨道滚动，内容放不下时显示
public class NanbinHorizontalScrollBar extends ClickableWidgetExtension {

	private static final int TRACK_COLOR = 0x40000000;
	private static final int THUMB_COLOR = 0xB0FFFFFF;
	private static final int MIN_THUMB_WIDTH = 10;

	private final Runnable onChange;
	private int contentWidth;
	private double scroll;
	private boolean dragging;
	private double dragOffset;

	public NanbinHorizontalScrollBar(int x, int y, int width, int height, Runnable onChange) {
		super(x, y, width, height, TextHelper.literal(""));
		this.onChange = onChange;
	}

	public void setContentWidth(int contentWidth) {
		this.contentWidth = Math.max(0, contentWidth);
		scroll = clamp(scroll);
	}

	public double getScroll() {
		return scroll;
	}

	public void setScroll(double scroll) {
		final double clamped = clamp(scroll);
		if (clamped != this.scroll) {
			this.scroll = clamped;
			if (onChange != null) {
				onChange.run();
			}
		}
	}

	public boolean isScrollable() {
		return getMaxScroll() > 0;
	}

	@Override
	public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float delta) {
		if (!isScrollable()) {
			return;
		}
		final int thumbWidth = getThumbWidth();
		final int thumbX = getX2() + getThumbOffset(thumbWidth);
		final GuiDrawing guiDrawing = new GuiDrawing(graphicsHolder);
		guiDrawing.beginDrawingRectangle();
		guiDrawing.drawRectangle(getX2(), getY2(), getX2() + getWidth2(), getY2() + getHeight2(), TRACK_COLOR);
		guiDrawing.drawRectangle(thumbX, getY2(), thumbX + thumbWidth, getY2() + getHeight2(), THUMB_COLOR);
		guiDrawing.finishDrawingRectangle();
	}

	@Override
	public boolean mouseClicked2(double mouseX, double mouseY, int button) {
		if (button != 0 || !isScrollable() || !getActiveMapped() || !isMouseOver2(mouseX, mouseY)) {
			return false;
		}
		final int thumbWidth = getThumbWidth();
		final int thumbX = getX2() + getThumbOffset(thumbWidth);
		if (mouseX >= thumbX && mouseX < thumbX + thumbWidth) {
			dragOffset = mouseX - thumbX;
		} else {
			dragOffset = thumbWidth / 2.0;
			moveThumbTo(mouseX - dragOffset);
		}
		dragging = true;
		return true;
	}

	@Override
	public boolean mouseDragged2(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (!dragging || button != 0) {
			return false;
		}
		moveThumbTo(mouseX - dragOffset);
		return true;
	}

	@Override
	public boolean mouseReleased2(double mouseX, double mouseY, int button) {
		if (!dragging) {
			return false;
		}
		dragging = false;
		return true;
	}

	private void moveThumbTo(double thumbX) {
		final int travel = getWidth2() - getThumbWidth();
		if (travel <= 0) {
			setScroll(0);
			return;
		}
		final double ratio = Math.max(0.0, Math.min(1.0, (thumbX - getX2()) / travel));
		setScroll(ratio * getMaxScroll());
	}

	private int getMaxScroll() {
		return Math.max(0, contentWidth - getWidth2());
	}

	private int getThumbWidth() {
		final int viewWidth = getWidth2();
		if (contentWidth <= viewWidth || contentWidth <= 0) {
			return viewWidth;
		}
		return Math.min(viewWidth, Math.max(MIN_THUMB_WIDTH, (int) ((float) viewWidth * viewWidth / contentWidth)));
	}

	private int getThumbOffset(int thumbWidth) {
		final int maxScroll = getMaxScroll();
		return maxScroll <= 0 ? 0 : (int) Math.round((getWidth2() - thumbWidth) * (scroll / maxScroll));
	}

	private double clamp(double value) {
		return Math.max(0, Math.min(getMaxScroll(), value));
	}
}
