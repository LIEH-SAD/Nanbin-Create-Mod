package com.Nanbin.client.Screen;

import com.Nanbin.mapping.GraphicsHolder;
import com.Nanbin.mapping.GuiDrawing;
import com.Nanbin.mapping.ScreenBase;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * 通用二次确认屏幕。
 * <p>
 * 4.1 适配：基类从 4.0 的 {@code MTRScreenBase} 换成 {@link ScreenBase}；
 * {@code ButtonWidgetExtension} + {@code ClickableWidget} 包装层 → 原版 {@link ButtonWidget}.
 */
public class RailwaySignClearConfirmScreen extends ScreenBase {

	private static final Identifier BACKGROUND_TEXTURE = Identifier.of("nanbin", "textures/gui/background.png");

	private final MutableText message;
	private final Runnable onConfirm;

	public RailwaySignClearConfirmScreen(Screen previousScreen, MutableText message, Runnable onConfirm) {
		super(message, previousScreen);
		this.message = message;
		this.onConfirm = onConfirm;
	}

	@Override
	protected void init2() {
		super.init2();
		final int centerX = getWidthMapped() / 2;
		final int centerY = getHeightMapped() / 2;

		addChild(ButtonWidget.builder(Text.translatable("gui.nanbin.confirm"), button -> {
			onConfirm.run();
			close();
		}).dimensions(centerX - 70, centerY + 10, 60, 20).build());
		addChild(ButtonWidget.builder(Text.translatable("gui.nanbin.cancel"), button -> close())
				.dimensions(centerX + 10, centerY + 10, 60, 20).build());
	}

	@Override
	public void render2(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float delta) {
		drawBackground(graphicsHolder);
		graphicsHolder.drawCenteredText(message, getWidthMapped() / 2, getHeightMapped() / 2 - 20, -1, true, GraphicsHolder.getDefaultLight());
	}

	private void drawBackground(GraphicsHolder graphicsHolder) {
		final GuiDrawing guiDrawing = new GuiDrawing(graphicsHolder);
		guiDrawing.beginDrawingTexture(BACKGROUND_TEXTURE);
		guiDrawing.drawTexture(0, 0, getWidthMapped(), getHeightMapped(), 0, 0, 1, 1);
		guiDrawing.finishDrawingTexture();
	}
}
