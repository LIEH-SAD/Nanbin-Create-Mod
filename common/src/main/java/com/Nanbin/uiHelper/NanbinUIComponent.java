package com.Nanbin.uiHelper;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;

/**
 * NanbinUI 组件基类。
 *
 * <p>所有放进 {@link NanbinUIScreen} 内容区的元素（小标题、配置项……）都继承本类。
 * 组件的位置由 {@link NanbinUIScreen} 按加入顺序自上而下统一排版，
 * 子类只需要关心「自己长什么样」和「多高」。
 */
public abstract class NanbinUIComponent {

	/** 配置项类组件的常态底色：20% 不透明度 #000000。 */
	public static final int HOVER_BASE_COLOR = 0x33000000;
	/** 配置项类组件的悬停底色：40% 不透明度 #FFFFFF。 */
	public static final int HOVER_ACTIVE_COLOR = 0x66FFFFFF;
	/** 淡入 / 淡出时长：0.2 秒 = 4 tick（1 tick = 20 分之 1 秒）。 */
	public static final float FADE_TICKS = 0.2F * 20.0F;

	/** 与上一个组件之间额外的垂直间距（像素）。 */
	protected int topMargin = 2;

	protected int x;
	protected int y;
	protected int width;

	/** 组件自身高度（像素）。 */
	public abstract int getHeight();

	/** 由 {@link NanbinUIScreen} 调用的排版入口。 */
	public void setBounds(int x, int y, int width) {
		this.x = x;
		this.y = y;
		this.width = width;
		onLayout();
	}

	public final int getX() {
		return x;
	}

	public final int getY() {
		return y;
	}

	public final int getWidth() {
		return width;
	}

	public final int getBottom() {
		return y + getHeight();
	}

	public int getTopMargin() {
		return topMargin;
	}

	public void setTopMargin(int topMargin) {
		this.topMargin = topMargin;
	}

	/**
	 * 绘制组件。调用前 {@link NanbinUIScreen} 已经裁剪好内容区，
	 * 因此组件不需要自己判断是否滚动出可视范围。
	 */
	public abstract void render(MatrixStack matrices, TextRenderer textRenderer, int mouseX, int mouseY, float delta);

	/** 组件被点击时回调；返回 true 表示事件已消费。 */
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		return false;
	}

	/**
	 * 屏幕每次 init 时调用。需要屏幕转发输入（例如文本框）的组件
	 * 可以在这里用 {@link NanbinUIScreen#registerInputChild} 登记控件，实现里要自行保证幂等。
	 */
	public void attach(NanbinUIScreen screen) {
	}

	/** 位置或宽度发生变化时的回调（默认什么都不做）。 */
	protected void onLayout() {
	}

	/** 以固定步长逼近目标值，保证淡入淡出在 {@link #FADE_TICKS} 内线性完成。 */
	protected static float approach(float current, float target, float step) {
		if (current < target) {
			return Math.min(target, current + step);
		}
		if (current > target) {
			return Math.max(target, current - step);
		}
		return target;
	}

	/** 按悬停进度（0 = 常态，1 = 完全悬停）取底色。 */
	protected static int hoverColor(float amount) {
		return lerpColor(HOVER_BASE_COLOR, HOVER_ACTIVE_COLOR, amount);
	}

	/** 逐通道线性插值两个 ARGB 颜色。 */
	private static int lerpColor(int from, int to, float t) {
		final float amount = Math.max(0.0F, Math.min(1.0F, t));
		final int a = lerpChannel(from >>> 24, to >>> 24, amount);
		final int r = lerpChannel((from >> 16) & 0xFF, (to >> 16) & 0xFF, amount);
		final int g = lerpChannel((from >> 8) & 0xFF, (to >> 8) & 0xFF, amount);
		final int b = lerpChannel(from & 0xFF, to & 0xFF, amount);
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	private static int lerpChannel(int from, int to, float t) {
		return Math.round(from + (to - from) * t);
	}

	/** 鼠标是否位于组件正上方。 */
	public final boolean isMouseOver(double mouseX, double mouseY) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < getBottom();
	}

	/**
	 * 按宽度裁剪文本，超出部分直接截断（避免文字溢出组件）。
	 */
	protected final String trim(TextRenderer textRenderer, String text, int maxWidth) {
		return maxWidth <= 0 ? "" : textRenderer.trimToWidth(text, maxWidth);
	}
}
