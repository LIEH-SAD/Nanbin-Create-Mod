package com.Nanbin.client.Screen;

import com.Nanbin.Init;
import com.Nanbin.client.Drawing.CustomFontTextureCache;
import com.Nanbin.client.Drawing.CustomFontTextureCache.FittedTextTexture;
import com.Nanbin.client.Drawing.CustomFontTextureCache.FontType;
import com.Nanbin.packet.ClientPacketHelper;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.ClickableWidget;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.mapper.ButtonWidgetExtension;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;
import org.mtr.mapping.mapper.TextFieldWidgetExtension;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mapping.tool.TextCase;
import org.mtr.mod.client.IDrawing;
import org.mtr.mod.data.IGui;
import org.mtr.mod.screen.MTRScreenBase;

import java.awt.Color;

/**
 * 路名方块配置屏幕。
 * 左侧显示 2:1 的路名贴图，并在贴图上实时预览四个文本框的内容；
 * 右侧提供四个文本框（标题为占位翻译键，待补充）。
 * 显示框使用 {@link CustomFontTextureCache} 的 SOURCE_HAN 字体，无边框无背景，
 * 文本过大时自动等比缩小。
 */
public class RoadNameScreen extends MTRScreenBase implements IGui {

	private static final Identifier ROAD_NAME_TEXTURE = new Identifier("nanbin", "textures/gui/road_name_block.png");
	private static final FontType FONT_TYPE = FontType.SOURCE_HAN;

	private static final int COLOR_TEXT_MAIN = 0xFFFFFFFF;
	private static final int COLOR_TEXT_CORNER = 0xFF0059C6;

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
	private final String[] initialTexts;
	private final TextFieldWidgetExtension[] textFields = new TextFieldWidgetExtension[4];

	private int textureX;
	private int textureY;
	private int textureWidth;
	private int textureHeight;
	private int fieldX;
	private int fieldWidth;

	public RoadNameScreen(BlockPos blockPos, String[] initialTexts) {
		super();
		this.blockPos = blockPos;
		this.initialTexts = initialTexts != null && initialTexts.length >= 4 ? initialTexts : new String[]{"", "", "", ""};
		for (int i = 0; i < textFields.length; i++) {
			textFields[i] = new TextFieldWidgetExtension(0, 0, 0, TEXT_FIELD_HEIGHT, FIELD_MAX_LENGTH, TextCase.DEFAULT, null, null);
		}
	}

	@Override
	protected void init2() {
		super.init2();

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
		fieldWidth = Math.max(this.width - fieldX - MARGIN, 120);

		int fieldY = 40;
		for (int i = 0; i < textFields.length; i++) {
			IDrawing.setPositionAndWidth(textFields[i], fieldX, fieldY, fieldWidth);
			textFields[i].setText2(i < initialTexts.length ? initialTexts[i] : "");
			addChild(new ClickableWidget(textFields[i]));
			fieldY += ROW_SPACING;
		}

		// 保存按钮：把四个文本框内容发回服务器并关闭
		final ButtonWidgetExtension saveButton = new ButtonWidgetExtension(fieldX, fieldY + 6, fieldWidth, TEXT_FIELD_HEIGHT, TextHelper.translatable("gui.nanbin.road_name_save"), button -> {
			final String[] texts = new String[4];
			for (int i = 0; i < textFields.length; i++) {
				texts[i] = textFields[i].getText2();
			}
			ClientPacketHelper.saveRoadNameScreen(blockPos, texts);
			this.onClose2();
		});
		addChild(new ClickableWidget(saveButton));
	}

	@Override
	public void tick2() {
		for (final TextFieldWidgetExtension textField : textFields) {
			textField.tick2();
		}
	}

	/** 获取第 index 个文本框的内容（0-3），供后续保存/渲染使用。 */
	public String getText(int index) {
		if (index < 0 || index >= textFields.length) {
			return "";
		}
		return textFields[index].getText2();
	}

	@Override
	public boolean isPauseScreen2() {
		return false;
	}

	@Override
	public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float delta) {
		try {
			renderBackground(graphicsHolder);
			graphicsHolder.drawCenteredText(TextHelper.translatable("gui.nanbin.road_name"), this.width / 2, 8, ARGB_WHITE);

			drawTextureAndDisplays(graphicsHolder);

			int fieldY = 40;
			for (int i = 0; i < textFields.length; i++) {
				graphicsHolder.drawText(TextHelper.translatable("gui.nanbin.road_name_" + (i + 1)), fieldX, fieldY - LABEL_HEIGHT, ARGB_WHITE, false, GraphicsHolder.getDefaultLight());
				fieldY += ROW_SPACING;
			}

			super.render(graphicsHolder, mouseX, mouseY, delta);
		} catch (Exception e) {
			Init.LOGGER.error("Failed to render RoadNameScreen", e);
		}
	}

	private void drawTextureAndDisplays(GraphicsHolder graphicsHolder) {
		final GuiDrawing guiDrawing = new GuiDrawing(graphicsHolder);

		guiDrawing.beginDrawingTexture(ROAD_NAME_TEXTURE);
		guiDrawing.drawTexture(textureX, textureY, textureX + textureWidth, textureY + textureHeight, 0F, 0F, 1F, 1F);
		guiDrawing.finishDrawingTexture();

		// 显示框 1：白色，居中于整个贴图，默认 48，文本过大自动等比缩小
		drawDisplay(guiDrawing, textFields[0].getText2(), 0.5F, 0.35F, 0.92F, 0.40F, FONT_SIZE_MAIN, COLOR_TEXT_MAIN, 0);
		// 显示框 2：白色，位于显示框 1 下方避免重叠，默认 16，文本过大自动等比缩小
		drawDisplay(guiDrawing, textFields[1].getText2(), 0.5F, 0.55F, 0.90F, 0.20F, FONT_SIZE_SUB, COLOR_TEXT_MAIN, 0);
		// 显示框 3：蓝色，贴图左下角（底部白条区域）；文字左边距贴图左边 0.04F
		drawDisplay(guiDrawing, textFields[2].getText2(), 0.04F, 0.9375F, 0.92F, 0.12F, FONT_SIZE_CORNER, COLOR_TEXT_CORNER, -1);
		// 显示框 4：蓝色，贴图右下角（底部白条区域）；文字右边距贴图右边 0.04F
		drawDisplay(guiDrawing, textFields[3].getText2(), 0.96F, 0.9375F, 0.92F, 0.12F, FONT_SIZE_CORNER, COLOR_TEXT_CORNER, 1);
	}

	/**
	 * 将文本渲染成紧贴内容的贴图，并以 (centerX, centerY) 为中心等比缩放到最大 maxW x maxH 的显示框内。
	 * 文本较短时保持默认字号，文本过大时自动等比缩小。
	 *
	 * @param alignX 水平对齐方式：0=居中（centerX 为文字中心），-1=左对齐（centerX 为文字左边），1=右对齐（centerX 为文字右边）
	 */
	private void drawDisplay(GuiDrawing guiDrawing, String text, float centerX, float centerY, float maxW, float maxH, int fontSize, int color, int alignX) {
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

		guiDrawing.beginDrawingTexture(fitted.identifier);
		guiDrawing.drawTexture(x, y, x + w, y + h, 0F, 0F, 1F, 1F);
		guiDrawing.finishDrawingTexture();
	}
}
