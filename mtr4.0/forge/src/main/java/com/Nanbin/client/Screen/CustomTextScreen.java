package com.Nanbin.client.Screen;

import com.Nanbin.client.JavaScriptSupport.JSSignEngine;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import org.mtr.mapping.holder.ClickableWidget;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.Screen;
import org.mtr.mapping.mapper.*;
import org.mtr.mapping.tool.TextCase;
import org.mtr.mod.client.IDrawing;
import org.mtr.mod.data.IGui;

/**
 * 自定义文本屏幕：既可供 JS 样式数据屏打开（把文本写入 selectedIds 供 getCustomText 读取），
 * 也可在普通指示牌编辑器中选中自定义文本指示牌时打开。
 */
public class CustomTextScreen extends ScreenExtension implements IGui {

	private static final int FIELD_WIDTH = 200;
	private static final int FIELD_HEIGHT = 20;
	private static final int FIELD_MAX_LENGTH = 64;

	private final ScreenExtension parent;
	private final LongAVLTreeSet selectedIds;
	private final Runnable onSave;
	private final TextFieldWidgetExtension textField;

	public CustomTextScreen(ScreenExtension parent, LongAVLTreeSet selectedIds, Runnable onSave) {
		super();
		this.parent = parent;
		this.selectedIds = selectedIds;
		this.onSave = onSave;
		textField = new TextFieldWidgetExtension(0, 0, 0, FIELD_HEIGHT, FIELD_MAX_LENGTH, TextCase.DEFAULT, null, null);
	}

	@Override
	protected void init2() {
		super.init2();

		final int centerX = width / 2;
		IDrawing.setPositionAndWidth(textField, centerX - FIELD_WIDTH / 2, 40, FIELD_WIDTH);
		textField.setText2(JSSignEngine.readCustomText(selectedIds));
		addChild(new ClickableWidget(textField));

		final ButtonWidgetExtension saveButton = new ButtonWidgetExtension(centerX - 90, 70, 180, 20, TextHelper.translatable("gui.nanbin.save"), button -> save());
		addChild(new ClickableWidget(saveButton));

		final ButtonWidgetExtension cancelButton = new ButtonWidgetExtension(centerX - 90, 98, 180, 20, TextHelper.translatable("gui.cancel"), button -> onClose2());
		addChild(new ClickableWidget(cancelButton));
	}

	private void save() {
		JSSignEngine.writeCustomText(selectedIds, textField.getText2());
		if (onSave != null) {
			onSave.run();
		}
		onClose2();
	}

	@Override
	public void tick2() {
		textField.tick2();
	}

	@Override
	public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float delta) {
		renderBackground(graphicsHolder);
		super.render(graphicsHolder, mouseX, mouseY, delta);
		graphicsHolder.drawCenteredText(TextHelper.translatable("gui.nanbin.custom_text.title").getString(), width / 2, 15, ARGB_WHITE);
	}

	@Override
	public void onClose2() {
		MinecraftClient.getInstance().openScreen(new Screen(parent));
	}

	@Override
	public boolean isPauseScreen2() {
		return false;
	}
}
