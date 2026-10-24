package com.Nanbin.client.Screen;

import org.mtr.mapping.holder.ClickableWidget;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.holder.Screen;
import org.mtr.mapping.mapper.*;
import org.mtr.mod.screen.MTRScreenBase;

/**
 * 通用二次确认屏幕
 */
public class RailwaySignClearConfirmScreen extends MTRScreenBase {

	private static final Identifier BACKGROUND_TEXTURE = new Identifier("nanbin:textures/gui/background.png");

	private final MutableText message;
	private final Runnable onConfirm;

	public RailwaySignClearConfirmScreen(ScreenExtension previousScreen, MutableText message, Runnable onConfirm) {
		super(new Screen(previousScreen));
		this.message = message;
		this.onConfirm = onConfirm;
	}

	@Override
	protected void init2() {
		super.init2();
		final int centerX = getWidthMapped() / 2;
		final int centerY = getHeightMapped() / 2;

		final ButtonWidgetExtension confirmButton = new ButtonWidgetExtension(centerX - 70, centerY + 10, 60, 20, TextHelper.translatable("gui.nanbin.confirm"), button -> {
			onConfirm.run();
			onClose2();
		});
		final ButtonWidgetExtension cancelButton = new ButtonWidgetExtension(centerX + 10, centerY + 10, 60, 20, TextHelper.translatable("gui.nanbin.cancel"), button -> onClose2());

		addChild(new ClickableWidget(confirmButton));
		addChild(new ClickableWidget(cancelButton));
	}

	@Override
	public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float delta) {
		drawBackground(graphicsHolder);
		super.render(graphicsHolder, mouseX, mouseY, delta);
		graphicsHolder.drawCenteredText(message, getWidthMapped() / 2, getHeightMapped() / 2 - 20, -1);
	}

	private void drawBackground(GraphicsHolder graphicsHolder) {
		final GuiDrawing guiDrawing = new GuiDrawing(graphicsHolder);
		guiDrawing.beginDrawingTexture(BACKGROUND_TEXTURE);
		guiDrawing.drawTexture(0, 0, getWidthMapped(), getHeightMapped(), 0, 0, 1, 1);
		guiDrawing.finishDrawingTexture();
	}
}
