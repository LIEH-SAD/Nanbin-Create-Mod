package com.Nanbin.uiHelper;

import com.Nanbin.mapping.GraphicsHolder;
import com.Nanbin.mapping.GuiDrawing;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

/**
 * 文本框类型的配置项：样式与 {@link NanbinUIConfigItem} 一致
 * （常态 20% 不透明度 #000000，悬停或聚焦时淡入到 40% 不透明度 #FFFFFF），
 * 但内容是一个可以直接输入的内嵌文本框，而不是二级菜单。
 * <p>
 * 文本框不在屏幕的自动渲染列表里（避免盖住行底色），而是由本组件在内容区里自己绘制；
 * 输入由 {@link NanbinUIScreen#registerInputChild} 登记的控件负责。
 * <p>
 * 4.1 适配：{@code TextFieldWidgetExtension}/{@code TextCase} 包装层已移除，
 * 直接用原版 {@link TextFieldWidget}（输入过滤改用 {@code setTextPredicate}，这里保持默认不过滤）。
 */
public class NanbinUITextFieldItem extends NanbinUIComponent {

	public static final int HEIGHT = 24;
	public static final int FIELD_HEIGHT = 20;
	private static final int TEXT_INSET = 4;
	private static final int LABEL_GAP = 8;
	private static final int MIN_FIELD_WIDTH = 40;

	private final TextRenderer textRenderer;
	private final MutableText label;
	private final TextFieldWidget textField;
	private String suggestion;
	private float hoverAmount;
	/** 文本框左边界，布局时算好后续绘制阶段使用。 */
	private int fieldX;

	public NanbinUITextFieldItem(NanbinUIScreen screen, MutableText label, String value, int maxLength) {
		this.textRenderer = screen.getTextRenderer();
		this.label = label;
		this.textField = new TextFieldWidget(this.textRenderer, 0, 0, MIN_FIELD_WIDTH, FIELD_HEIGHT,
				label == null ? Text.literal("") : label);
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
	 * <p>
	 * 注意占位文本只在内容为空时交给控件，避免它「跟随光标」贴在已输入内容后面一起画出来。
	 */
	public NanbinUITextFieldItem setSuggestion(String suggestion) {
		this.suggestion = suggestion;
		return this;
	}

	/** 文本变化时的回调。 */
	public NanbinUITextFieldItem setChangedListener(java.util.function.Consumer<String> listener) {
		this.textField.setChangedListener(listener);
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
		textField.setFocused(false);
		screen.registerInputChild(textField);
	}

	@Override
	protected void onLayout() {
		fieldX = x + TEXT_INSET + (label == null ? 0 : textRenderer.getWidth(label.getString()) + LABEL_GAP);
		textField.setX(fieldX);
		textField.setY(y + (HEIGHT - FIELD_HEIGHT) / 2);
		textField.setWidth(Math.max(MIN_FIELD_WIDTH, x + width - TEXT_INSET - fieldX));
	}

	@Override
	public void render(GraphicsHolder graphicsHolder, TextRenderer textRenderer, int mouseX, int mouseY, float delta) {
		// 聚焦期间保持高亮，鼠标移开也不会闪回常态
		final boolean active = isMouseOver(mouseX, mouseY) || textField.isFocused();
		hoverAmount = approach(hoverAmount, active ? 1.0F : 0.0F, delta / FADE_TICKS);

		final GuiDrawing guiDrawing = new GuiDrawing(graphicsHolder);
		guiDrawing.beginDrawingRectangle();
		guiDrawing.drawRectangle(x, y, x + width, y + HEIGHT, hoverColor(hoverAmount));
		guiDrawing.finishDrawingRectangle();

		if (label != null) {
			final String text = trim(textRenderer, label.getString(), fieldX - TEXT_INSET - LABEL_GAP - x);
			graphicsHolder.drawText(text, x + TEXT_INSET, y + (HEIGHT - 8) / 2, 0xFFFFFFFF, true, GraphicsHolder.getDefaultLight());
		}
		// 有内容时清掉占位文本，避免它跟在已输入内容后面显示出来
		textField.setPlaceholder(textField.getText().isEmpty() ? (suggestion == null ? Text.empty() : Text.literal(suggestion)) : Text.empty());
		final DrawContext context = graphicsHolder.getContext();
		textField.render(context, mouseX, mouseY, delta);
	}
}
