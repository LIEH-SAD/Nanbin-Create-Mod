package com.Nanbin.uiHelper;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * 文本框类型的配置项：样式与 {@link NanbinUIConfigItem} 一致
 * （常态 20% 不透明度 #000000，悬停或聚焦时淡入到 40% 不透明度 #FFFFFF），
 * 但内容是一个可以直接输入的内嵌文本框，而不是二级菜单。
 *
 * <p>文本框不在屏幕的自动渲染列表里（避免盖住行底色），而是由本组件在内容区里自己绘制；
 * 输入由 {@link NanbinUIScreen#registerInputChild} 登记的控件负责。
 */
public class NanbinUITextFieldItem extends NanbinUIComponent {

	public static final int HEIGHT = 24;
	public static final int FIELD_HEIGHT = 20;
	private static final int TEXT_INSET = 4;
	private static final int LABEL_GAP = 8;
	private static final int MIN_FIELD_WIDTH = 40;

	private final TextRenderer textRenderer;
	@Nullable
	private final Text label;
	private final TextFieldWidget textField;
	@Nullable
	private String suggestion;
	private float hoverAmount;

	public NanbinUITextFieldItem(TextRenderer textRenderer, @Nullable Text label, String value, int maxLength) {
		this.textRenderer = textRenderer;
		this.label = label;
		this.textField = new TextFieldWidget(textRenderer, 0, 0, MIN_FIELD_WIDTH, FIELD_HEIGHT, label == null ? Text.empty() : label);
		this.textField.setMaxLength(maxLength);
		this.textField.setText(value == null ? "" : value);
		this.topMargin = 2;
	}

	@Override
	public int getHeight() {
		return HEIGHT;
	}

	/**
	 * 输入框为空时显示的灰色占位文本。
	 *
	 * <p>注意：原版 {@link TextFieldWidget#setSuggestion} 是"跟随光标"的补全提示，
	 * 输入内容后会紧跟在文字后面一起画出来，不能直接当占位符用；
	 * 这里只在内容为空时才把它交给控件。
	 */
	public NanbinUITextFieldItem setSuggestion(@Nullable String suggestion) {
		this.suggestion = suggestion;
		return this;
	}

	/** 文本变化时的回调。 */
	public NanbinUITextFieldItem setChangedListener(@Nullable Consumer<String> listener) {
		textField.setChangedListener(listener);
		return this;
	}

	public String getText() {
		return textField.getText();
	}

	public void setText(String text) {
		textField.setText(text == null ? "" : text);
	}

	public TextFieldWidget getTextField() {
		return textField;
	}

	@Override
	public void attach(NanbinUIScreen screen) {
		// 屏幕每次 init 都会清空控件列表，这里重新登记一次（只登记输入，绘制由本组件负责）
		textField.setTextFieldFocused(false);
		screen.registerInputChild(textField);
	}

	@Override
	protected void onLayout() {
		final int fieldX = x + TEXT_INSET + (label == null ? 0 : textRenderer.getWidth(label) + LABEL_GAP);
		textField.setX(fieldX);
		textField.y = y + (HEIGHT - FIELD_HEIGHT) / 2;
		textField.setWidth(Math.max(MIN_FIELD_WIDTH, x + width - TEXT_INSET - fieldX));
	}

	@Override
	public void render(MatrixStack matrices, TextRenderer textRenderer, int mouseX, int mouseY, float delta) {
		// 聚焦期间保持高亮，鼠标移开也不会闪回常态
		final boolean active = isMouseOver(mouseX, mouseY) || textField.isFocused();
		hoverAmount = approach(hoverAmount, active ? 1.0F : 0.0F, delta / FADE_TICKS);

		DrawableHelper.fill(matrices, x, y, x + width, y + HEIGHT, hoverColor(hoverAmount));
		if (label != null) {
			final String text = trim(textRenderer, label.getString(), textField.x - TEXT_INSET - LABEL_GAP - x);
			textRenderer.drawWithShadow(matrices, text, x + TEXT_INSET, y + (HEIGHT - 8) / 2.0F, 0xFFFFFFFF);
		}
		// 有内容时清掉建议文本，避免它跟在输入内容后面显示出来
		textField.setSuggestion(textField.getText().isEmpty() ? suggestion : null);
		textField.render(matrices, mouseX, mouseY, delta);
	}
}
