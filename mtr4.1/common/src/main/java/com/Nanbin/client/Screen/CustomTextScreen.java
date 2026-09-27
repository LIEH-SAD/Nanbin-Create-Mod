package com.Nanbin.client.Screen;

import com.Nanbin.client.JavaScriptSupport.JSSignEngine;
import com.Nanbin.mapping.GraphicsHolder;
import com.Nanbin.mapping.ScreenBase;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.mtr.data.IGui;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongAVLTreeSet;

/**
 * 自定义文本屏幕：既可供 JS 样式数据屏打开（把文本写入 selectedIds 供 getCustomText 读取），
 * 也可在普通指示牌编辑器中选中「自定义文本」标志时打开。
 * <p>
 * 4.1 适配：{@code ScreenExtension} → {@link ScreenBase}（父屏幕从 {@code Screen} 换成原版类型）；
 * {@code ClickableWidget} 包装层 → 直接 {@code addChild(原版控件)}；
 * {@code IDrawing.setPositionAndWidth} → 构造时给坐标；{@code openScreen(new Screen(x))} → {@code setScreen(x)}。
 */
public class CustomTextScreen extends ScreenBase implements IGui {

	private static final int FIELD_WIDTH = 200;
	private static final int FIELD_HEIGHT = 20;
	private static final int FIELD_MAX_LENGTH = 64;

	private final Screen parent;
	private final LongAVLTreeSet selectedIds;
	private final Runnable onSave;
	private final TextFieldWidget textField;

	public CustomTextScreen(Screen parent, LongAVLTreeSet selectedIds, Runnable onSave) {
		super(Text.translatable("gui.nanbin.custom_text.title"), parent);
		this.parent = parent;
		this.selectedIds = selectedIds;
		this.onSave = onSave;
		this.textField = new TextFieldWidget(textRenderer, 0, 0, FIELD_WIDTH, FIELD_HEIGHT, Text.empty());
	}

	@Override
	protected void init2() {
		super.init2();

		final int centerX = width / 2;
		textField.setMaxLength(FIELD_MAX_LENGTH);
		textField.setPosition(centerX - FIELD_WIDTH / 2, 40);
		textField.setWidth(FIELD_WIDTH);
		textField.setText(JSSignEngine.readCustomText(selectedIds));
		addChild(textField);

		addChild(ButtonWidget.builder(Text.translatable("gui.nanbin.save"), button -> save())
				.dimensions(centerX - 90, 70, 180, 20).build());
		addChild(ButtonWidget.builder(Text.translatable("gui.cancel"), button -> close())
				.dimensions(centerX - 90, 98, 180, 20).build());
	}

	private void save() {
		JSSignEngine.writeCustomText(selectedIds, textField.getText());
		if (onSave != null) {
			onSave.run();
		}
		close();
	}

	@Override
	public void render2(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float delta) {
		graphicsHolder.drawCenteredText(Text.translatable("gui.nanbin.custom_text.title"), width / 2, 15, ARGB_WHITE, true, GraphicsHolder.getDefaultLight());
	}

	@Override
	public boolean isPauseScreen2() {
		return false;
	}
}
