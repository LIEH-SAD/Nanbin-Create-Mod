package com.Nanbin.client.Screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Matrix4f;

/**
 * 通用二次确认屏幕
 */
public class RailwaySignClearConfirmScreen extends Screen {

	private static final Identifier BACKGROUND_TEXTURE = new Identifier("nanbin", "textures/gui/background.png");

	private final Screen parent;
	private final Text message;
	private final Runnable onConfirm;

	public RailwaySignClearConfirmScreen(Screen previousScreen, Text message, Runnable onConfirm) {
		super(Text.translatable("gui.nanbin.confirm"));
		this.parent = previousScreen;
		this.message = message;
		this.onConfirm = onConfirm;
	}

	@Override
	protected void init() {
		super.init();
		final int centerX = width / 2;
		final int centerY = height / 2;

		final ButtonWidget confirmButton = new ButtonWidget(centerX - 70, centerY + 10, 60, 20, Text.translatable("gui.nanbin.confirm"), button -> {
			onConfirm.run();
			close();
		});
		final ButtonWidget cancelButton = new ButtonWidget(centerX + 10, centerY + 10, 60, 20, Text.translatable("gui.nanbin.cancel"), button -> close());

		addDrawableChild(confirmButton);
		addDrawableChild(cancelButton);
	}

	@Override
	public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
		this.drawBackground(matrices);
		super.render(matrices, mouseX, mouseY, delta);
		DrawableHelper.drawCenteredText(matrices, textRenderer, message, width / 2, height / 2 - 20, -1);
	}

	@Override
	public void close() {
		if (this.client != null && this.parent != null) {
			this.client.setScreen(this.parent);
		} else {
			super.close();
		}
	}

	private void drawBackground(MatrixStack matrices) {
		drawTexture(matrices, BACKGROUND_TEXTURE,
				0, 0, this.width, this.height,
				0.0F, 0.0F, 1.0F, 1.0F);
	}

	private void drawTexture(MatrixStack matrices, Identifier texture,
							 float x, float y, float width, float height,
							 float u, float v, float uWidth, float vHeight) {
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderTexture(0, texture);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder bufferBuilder = tessellator.getBuffer();
		bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);

		Matrix4f matrix = matrices.peek().getPositionMatrix();

		bufferBuilder.vertex(matrix, x, y + height, 0.0F).texture(u, v + vHeight).next();
		bufferBuilder.vertex(matrix, x + width, y + height, 0.0F).texture(u + uWidth, v + vHeight).next();
		bufferBuilder.vertex(matrix, x + width, y, 0.0F).texture(u + uWidth, v).next();
		bufferBuilder.vertex(matrix, x, y, 0.0F).texture(u, v).next();

		tessellator.draw();
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}