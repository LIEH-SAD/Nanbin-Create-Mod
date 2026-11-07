package com.Nanbin.uiHelper;

import com.Nanbin.mapping.GraphicsHolder;
import com.Nanbin.mapping.GuiDrawing;
import com.Nanbin.mapping.TextHelper;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;

// 水平滚动条控件：拖动滑块或点击轨道滚动，内容放不下时显示
public class NanbinHorizontalScrollBar extends ClickableWidget {

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
	protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
		final GraphicsHolder graphicsHolder = new GraphicsHolder(context);
		if (!isScrollable()) {
			return;
		}
		final int thumbWidth = getThumbWidth();
		final int thumbX = getX() + getThumbOffset(thumbWidth);
		final GuiDrawing guiDrawing = new GuiDrawing(graphicsHolder);
		guiDrawing.beginDrawingRectangle();
		guiDrawing.drawRectangle(getX(), getY(), getX() + getWidth(), getY() + getHeight(), TRACK_COLOR);
		guiDrawing.drawRectangle(thumbX, getY(), thumbX + thumbWidth, getY() + getHeight(), THUMB_COLOR);
		guiDrawing.finishDrawingRectangle();
	}

	@Override
	protected void appendClickableNarrations(net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {
		appendDefaultNarrations(builder);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button != 0 || !isScrollable() || !active || !isMouseOver(mouseX, mouseY)) {
			return false;
		}
		final int thumbWidth = getThumbWidth();
		final int thumbX = getX() + getThumbOffset(thumbWidth);
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
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (!dragging || button != 0) {
			return false;
		}
		moveThumbTo(mouseX - dragOffset);
		return true;
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (!dragging) {
			return false;
		}
		dragging = false;
		return true;
	}

	private void moveThumbTo(double thumbX) {
		final int travel = getWidth() - getThumbWidth();
		if (travel <= 0) {
			setScroll(0);
			return;
		}
		final double ratio = Math.max(0.0, Math.min(1.0, (thumbX - getX()) / travel));
		setScroll(ratio * getMaxScroll());
	}

	private int getMaxScroll() {
		return Math.max(0, contentWidth - getWidth());
	}

	private int getThumbWidth() {
		final int viewWidth = getWidth();
		if (contentWidth <= viewWidth || contentWidth <= 0) {
			return viewWidth;
		}
		return Math.min(viewWidth, Math.max(MIN_THUMB_WIDTH, (int) ((float) viewWidth * viewWidth / contentWidth)));
	}

	private int getThumbOffset(int thumbWidth) {
		final int maxScroll = getMaxScroll();
		return maxScroll <= 0 ? 0 : (int) Math.round((getWidth() - thumbWidth) * (scroll / maxScroll));
	}

	private double clamp(double value) {
		return Math.max(0, Math.min(getMaxScroll(), value));
	}
}
