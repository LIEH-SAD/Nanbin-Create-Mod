package com.Nanbin.client.Screen;

import com.Nanbin.client.JavaScriptSupport.JSSignEngine;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.util.Set;

/**
 * 自定义文本屏幕：既可供 JS 样式数据屏打开（把文本写入 selectedIds 供 getCustomText 读取），
 * 也可在普通指示牌编辑器中选中自定义文本指示牌时打开。
 */
public class CustomTextScreen extends Screen {

	private static final int FIELD_WIDTH = 200;
	private static final int FIELD_HEIGHT = 20;
	private static final int FIELD_MAX_LENGTH = 64;
	private static final int ARGB_WHITE = 0xFFFFFFFF;

	private final Screen parent;
	private final Set<Long> selectedIds;
	private final Runnable onSave;
	private final TextFieldWidget textField;

	public CustomTextScreen(Screen parent, Set<Long> selectedIds, Runnable onSave) {
		super(Text.translatable("gui.nanbin.custom_text.title"));
		this.parent = parent;
		this.selectedIds = selectedIds;
		this.onSave = onSave;
		textField = new TextFieldWidget(textRenderer, 0, 0, FIELD_WIDTH, FIELD_HEIGHT, Text.translatable("gui.nanbin.custom_text.title"));
		textField.setMaxLength(FIELD_MAX_LENGTH);
	}

	@Override
	protected void init() {
		super.init();

		final int centerX = width / 2;
		textField.setX(centerX - FIELD_WIDTH / 2);
		textField.y = 40;
		textField.setText(JSSignEngine.readCustomText(selectedIds));
		addDrawableChild(textField);

		final ButtonWidget saveButton = new ButtonWidget(centerX - 90, 70, 180, 20, Text.translatable("gui.nanbin.save"), button -> save());
		addDrawableChild(saveButton);

		final ButtonWidget cancelButton = new ButtonWidget(centerX - 90, 98, 180, 20, Text.translatable("gui.cancel"), button -> close());
		addDrawableChild(cancelButton);
	}

	private void save() {
		JSSignEngine.writeCustomText(selectedIds, textField.getText());
		if (onSave != null) {
			onSave.run();
		}
		close();
	}

	@Override
	public void tick() {
		textField.tick();
	}

	@Override
	public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
		renderBackground(matrices);
		super.render(matrices, mouseX, mouseY, delta);
		DrawableHelper.drawCenteredText(matrices, textRenderer, Text.translatable("gui.nanbin.custom_text.title"), width / 2, 15, ARGB_WHITE);
	}

	@Override
	public void close() {
		if (parent != null) {
			client.setScreen(parent);
		} else {
			super.close();
		}
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}