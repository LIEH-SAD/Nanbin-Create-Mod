package com.Nanbin.uiHelper;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * 小标题组件。
 *
 * <p>视觉规格：不透明 #787878 背景条，文字统一纯白。
 * 一个屏幕可以按顺序加入多项小标题（用于把配置项分区），互相独立。
 */
public class NanbinUITitle extends NanbinUIComponent {

	public static final int HEIGHT = 16;
	/** 不透明的 #787878。 */
	public static final int BACKGROUND_COLOR = 0xFF787878;
	/** 标题文字统一纯白。 */
	public static final int TEXT_COLOR = 0xFFFFFFFF;

	private static final int TEXT_INSET = 4;

	private final Text text;
	private int backgroundColor = BACKGROUND_COLOR;
	private int textColor = TEXT_COLOR;

	public NanbinUITitle(Text text) {
		this.text = text;
		this.topMargin = 6;
	}

	public NanbinUITitle(String text) {
		this(Text.literal(text));
	}

	public Text getText() {
		return text;
	}

	public NanbinUITitle setBackgroundColor(int backgroundColor) {
		this.backgroundColor = backgroundColor;
		return this;
	}

	public NanbinUITitle setTextColor(int textColor) {
		this.textColor = textColor;
		return this;
	}

	@Override
	public int getHeight() {
		return HEIGHT;
	}

	@Override
	public void render(DrawContext context, TextRenderer textRenderer, int mouseX, int mouseY, float delta) {
		context.fill(x, y, x + width, y + HEIGHT, backgroundColor);
		final String label = trim(textRenderer, text.getString(), width - TEXT_INSET * 2);
		context.drawTextWithShadow(textRenderer, label, x + TEXT_INSET, y + (HEIGHT - 8) / 2, textColor);
	}
}
