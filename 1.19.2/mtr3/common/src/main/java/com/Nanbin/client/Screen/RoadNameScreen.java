package com.Nanbin.client.Screen;

import com.Nanbin.Init;
import com.Nanbin.client.ClientData.RoadNameClipboard;
import com.Nanbin.client.Drawing.CustomFontTextureCache;
import com.Nanbin.client.Drawing.CustomFontTextureCache.FittedTextTexture;
import com.Nanbin.client.Drawing.CustomFontTextureCache.FontType;
import com.Nanbin.packet.ClientPacketHelper;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Matrix4f;

import java.awt.*;

public class RoadNameScreen extends Screen {

	private static final Identifier ROAD_NAME_TEXTURE = new Identifier("nanbin", "textures/gui/road_name_block.png");
	private static final FontType FONT_TYPE = FontType.SOURCE_HAN;

	private static final int COLOR_TEXT_MAIN = 0xFFFFFFFF;
	private static final int COLOR_TEXT_CORNER = 0xFF0059C6;
	private static final int ARGB_WHITE = 0xFFFFFFFF;

	/** 默认字号 */
	private static final int FONT_SIZE_MAIN = 98;
	private static final int FONT_SIZE_SUB = 46;
	private static final int FONT_SIZE_CORNER = 46;

	private static final int TEXT_FIELD_HEIGHT = 18;
	private static final int ROW_SPACING = 30;
	private static final int LABEL_HEIGHT = 10;
	private static final int MARGIN = 20;
	private static final int FIELD_MAX_LENGTH = 64;

	private final BlockPos blockPos;
	/** 屏幕当前显示的文本，随复制/粘贴/清空/撤回更新，屏幕重新初始化时据此恢复，避免被 initialTexts 覆盖。 */
	private String[] currentTexts;
	private final TextFieldWidget[] textFields = new TextFieldWidget[4];

	private int textureX;
	private int textureY;
	private int textureWidth;
	private int textureHeight;
	private int fieldX;
	private int fieldWidth;

	public RoadNameScreen(BlockPos blockPos, String[] initialTexts) {
		super(Text.translatable("gui.nanbin.road_name"));
		this.blockPos = blockPos;
		this.currentTexts = initialTexts != null && initialTexts.length >= 4 ? initialTexts.clone() : new String[]{"", "", "", ""};
	}

	@Override
	protected void init() {
		super.init();

		// 屏幕重新初始化（如窗口尺寸变化）时，先保留用户已输入的内容
		if (textFields[0] != null) {
			getTexts();
		}

		textureHeight = (int) (this.height * 0.5F);
		textureWidth = textureHeight * 2;
		final int maxTextureWidth = this.width - 260;
		if (textureWidth > maxTextureWidth) {
			textureWidth = Math.max(maxTextureWidth, 160);
			textureHeight = textureWidth / 2;
		}
		textureX = MARGIN;
		textureY = (this.height - textureHeight) / 2;

		fieldX = textureX + textureWidth + 20;
		fieldWidth = Math.max(this.width - fieldX - MARGIN - 40, 120);

		int fieldY = 40;
		for (int i = 0; i < textFields.length; i++) {
			final TextFieldWidget textField = new TextFieldWidget(this.textRenderer, fieldX, fieldY, fieldWidth, TEXT_FIELD_HEIGHT, Text.translatable("gui.nanbin.road_name_" + (i + 1)));
			textField.setMaxLength(FIELD_MAX_LENGTH);
			textField.setText(i < currentTexts.length ? currentTexts[i] : "");
			textFields[i] = textField;
			addDrawableChild(textField);
			fieldY += ROW_SPACING;
		}

		// 保存按钮：把四个文本框内容发回服务器并关闭
		final ButtonWidget saveButton = new ButtonWidget(fieldX, fieldY + 6, fieldWidth, TEXT_FIELD_HEIGHT, Text.translatable("gui.nanbin.road_name_save"), button -> {
			final String[] texts = new String[4];
			for (int i = 0; i < textFields.length; i++) {
				texts[i] = textFields[i].getText();
			}
			ClientPacketHelper.saveRoadNameScreen(blockPos, texts);
			this.close();
		});
		addDrawableChild(saveButton);

		// 复制 / 粘贴 / 清空 / 撤回四个便捷按钮
		final int buttonX = this.width - 35;
		final int buttonY = 60;

		final ButtonWidget undoButton = new ButtonWidget(buttonX, buttonY + 90, 30, 20, Text.translatable("gui.nanbin.undo"), button -> {
			final String[] texts = getTexts();
			RoadNameClipboard.undo(blockPos, texts);
			setTexts(texts);
			button.active = RoadNameClipboard.canUndo(blockPos);
		});
		undoButton.active = RoadNameClipboard.canUndo(blockPos);

		final ButtonWidget copyButton = new ButtonWidget(buttonX, buttonY, 30, 20, Text.translatable("gui.nanbin.copy"), button -> RoadNameClipboard.copy(getTexts()));

		final ButtonWidget pasteButton = new ButtonWidget(buttonX, buttonY + 30, 30, 20, Text.translatable("gui.nanbin.paste"), button -> {
			final String[] texts = getTexts();
			RoadNameClipboard.paste(blockPos, texts);
			setTexts(texts);
			undoButton.active = RoadNameClipboard.canUndo(blockPos);
		});
		pasteButton.active = RoadNameClipboard.canPaste();

		final ButtonWidget clearButton = new ButtonWidget(buttonX, buttonY + 60, 30, 20, Text.translatable("gui.nanbin.clear"), button -> MinecraftClient.getInstance().setScreen(new RailwaySignClearConfirmScreen(this, Text.translatable("gui.nanbin.clear.question"), () -> {
			final String[] texts = getTexts();
			RoadNameClipboard.clear(blockPos, texts);
			setTexts(texts);
			undoButton.active = RoadNameClipboard.canUndo(blockPos);
		})));

		addDrawableChild(copyButton);
		addDrawableChild(pasteButton);
		addDrawableChild(clearButton);
		addDrawableChild(undoButton);
	}

	/** 收集四个文本框的当前内容。 */
	private String[] getTexts() {
		final String[] texts = new String[textFields.length];
		for (int i = 0; i < textFields.length; i++) {
			texts[i] = textFields[i].getText();
		}
		currentTexts = texts.clone();
		return texts;
	}

	/** 将内容写回四个文本框，并同步记录为当前文本。 */
	private void setTexts(String[] texts) {
		currentTexts = texts == null ? new String[]{"", "", "", ""} : texts.clone();
		for (int i = 0; i < Math.min(textFields.length, currentTexts.length); i++) {
			textFields[i].setText(currentTexts[i]);
		}
	}

	@Override
	public void tick() {
		for (final TextFieldWidget textField : textFields) {
			textField.tick();
		}
	}

	public String getText(int index) {
		if (index < 0 || index >= textFields.length) {
			return "";
		}
		return textFields[index].getText();
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	@Override
	public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
		try {
			this.renderBackground(matrices);
			DrawableHelper.drawCenteredText(matrices, this.textRenderer, Text.translatable("gui.nanbin.road_name"), this.width / 2, 8, ARGB_WHITE);

			drawTextureAndDisplays(matrices);

			int fieldY = 40;
			for (int i = 0; i < textFields.length; i++) {
				DrawableHelper.drawTextWithShadow(matrices, this.textRenderer, Text.translatable("gui.nanbin.road_name_" + (i + 1)), fieldX, fieldY - LABEL_HEIGHT, ARGB_WHITE);
				fieldY += ROW_SPACING;
			}

			super.render(matrices, mouseX, mouseY, delta);
		} catch (Exception e) {
			Init.LOGGER.error("Failed to render RoadNameScreen", e);
		}
	}

	private void drawTextureAndDisplays(MatrixStack matrices) {
		drawTexture(matrices, ROAD_NAME_TEXTURE, textureX, textureY, textureX + textureWidth, textureY + textureHeight);

		drawDisplay(matrices, textFields[0].getText(), 0.5F, 0.35F, 0.92F, 0.40F, FONT_SIZE_MAIN, COLOR_TEXT_MAIN, 0);
		drawDisplay(matrices, textFields[1].getText(), 0.5F, 0.55F, 0.90F, 0.20F, FONT_SIZE_SUB, COLOR_TEXT_MAIN, 0);
		drawDisplay(matrices, textFields[2].getText(), 0.04F, 0.9375F, 0.92F, 0.12F, FONT_SIZE_CORNER, COLOR_TEXT_CORNER, -1);
		drawDisplay(matrices, textFields[3].getText(), 0.96F, 0.9375F, 0.92F, 0.12F, FONT_SIZE_CORNER, COLOR_TEXT_CORNER, 1);
	}

	private void drawDisplay(MatrixStack matrices, String text, float centerX, float centerY, float maxW, float maxH, int fontSize, int color, int alignX) {
		if (text.isEmpty()) {
			return;
		}
		final FittedTextTexture fitted = CustomFontTextureCache.instance.getFittedTextTexture(text, FONT_TYPE, fontSize, new Color(color, true));
		if (fitted.identifier == null || fitted.width <= 0 || fitted.height <= 0) {
			return;
		}
		final float availableW = textureWidth * maxW;
		final float availableH = textureHeight * maxH;
		final float scale = Math.min(1.0F, Math.min(availableW / fitted.width, availableH / fitted.height));
		final float w = fitted.width * scale;
		final float h = fitted.height * scale;
		final float cx = textureX + textureWidth * centerX;
		final float cy = textureY + textureHeight * centerY;
		final float x;
		if (alignX == -1) {
			x = cx;             // 左对齐：centerX 表示文字左边
		} else if (alignX == 1) {
			x = cx - w;          // 右对齐：centerX 表示文字右边
		} else {
			x = cx - w / 2.0F;   // 居中
		}
		final float y = cy - h / 2.0F;

		drawTexture(matrices, fitted.identifier, x, y, x + w, y + h);
	}

	private void drawTexture(MatrixStack matrices, Identifier texture, float x1, float y1, float x2, float y2) {
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderTexture(0, texture);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder bufferBuilder = tessellator.getBuffer();
		bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);

		Matrix4f matrix = matrices.peek().getPositionMatrix();

		bufferBuilder.vertex(matrix, x1, y2, 0.0F).texture(0.0F, 1.0F).next();
		bufferBuilder.vertex(matrix, x2, y2, 0.0F).texture(1.0F, 1.0F).next();
		bufferBuilder.vertex(matrix, x2, y1, 0.0F).texture(1.0F, 0.0F).next();
		bufferBuilder.vertex(matrix, x1, y1, 0.0F).texture(0.0F, 0.0F).next();

		tessellator.draw();
	}
}