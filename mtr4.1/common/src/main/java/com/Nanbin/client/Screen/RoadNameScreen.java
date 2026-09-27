package com.Nanbin.client.Screen;

import com.Nanbin.InitClient;
import com.Nanbin.client.ClientData.RoadNameClipboard;
import com.Nanbin.client.Drawing.CustomFontTextureCache;
import com.Nanbin.client.Drawing.CustomFontTextureCache.FittedTextTexture;
import com.Nanbin.client.Drawing.CustomFontTextureCache.FontType;
import com.Nanbin.mapping.GraphicsHolder;
import com.Nanbin.mapping.GuiDrawing;
import com.Nanbin.mapping.ScreenBase;
import com.Nanbin.packet.ClientPacketHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.mtr.data.IGui;

import java.awt.Color;

/**
 * 路名方块配置屏幕。
 * <p>
 * 4.1 适配：基类换成 {@link ScreenBase}；{@code TextFieldWidgetExtension}/`ButtonWidgetExtension`
 * → 原版控件；{@code IDrawing.setPositionAndWidth} → {@code setPosition/setWidth}；
 * {@code setActiveMapped} → {@code active} 字段；{@code openScreen(new Screen(...))} → {@code setScreen(...)}。
 */
public class RoadNameScreen extends ScreenBase implements IGui {

	private static final Identifier ROAD_NAME_TEXTURE = Identifier.of("nanbin", "textures/gui/road_name_block.png");
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
	/** 屏幕当前显示的文本，随复制/粘贴/清空/撤回更新，屏幕重新初始化时据此恢复。 */
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
	protected void init2() {
		super.init2();

		textureHeight = (int) (height * 0.5F);
		textureWidth = textureHeight * 2;
		final int maxTextureWidth = width - 260;
		if (textureWidth > maxTextureWidth) {
			textureWidth = Math.max(maxTextureWidth, 160);
			textureHeight = textureWidth / 2;
		}
		textureX = MARGIN;
		textureY = (height - textureHeight) / 2;

		fieldX = textureX + textureWidth + 20;
		fieldWidth = Math.max(width - fieldX - MARGIN - 40, 120);

		int fieldY = 40;
		for (int i = 0; i < textFields.length; i++) {
			textFields[i] = new TextFieldWidget(textRenderer, fieldX, fieldY, fieldWidth, TEXT_FIELD_HEIGHT, Text.empty());
			textFields[i].setMaxLength(FIELD_MAX_LENGTH);
			textFields[i].setText(i < currentTexts.length ? currentTexts[i] : "");
			addChild(textFields[i]);
			fieldY += ROW_SPACING;
		}

		// 保存按钮：把四个文本框内容发回服务器并关闭
		addChild(ButtonWidget.builder(Text.translatable("gui.nanbin.save"), button -> {
			ClientPacketHelper.saveRoadNameScreen(blockPos, getTexts());
			close();
		}).dimensions(fieldX, fieldY + 6, fieldWidth, TEXT_FIELD_HEIGHT).build());

		// 便捷按钮
		final int buttonX = width - 35;
		final int buttonY = 60;

		final ButtonWidget undoButton = ButtonWidget.builder(Text.translatable("gui.nanbin.undo"), button -> {
			final String[] texts = getTexts();
			RoadNameClipboard.undo(blockPos, texts);
			setTexts(texts);
			button.active = RoadNameClipboard.canUndo(blockPos);
		}).dimensions(buttonX, buttonY + 90, 30, 20).build();
		undoButton.active = RoadNameClipboard.canUndo(blockPos);

		addChild(ButtonWidget.builder(Text.translatable("gui.nanbin.copy"), button -> RoadNameClipboard.copy(getTexts()))
				.dimensions(buttonX, buttonY, 30, 20).build());

		final ButtonWidget pasteButton = ButtonWidget.builder(Text.translatable("gui.nanbin.paste"), button -> {
			final String[] texts = getTexts();
			RoadNameClipboard.paste(blockPos, texts);
			setTexts(texts);
			undoButton.active = RoadNameClipboard.canUndo(blockPos);
		}).dimensions(buttonX, buttonY + 30, 30, 20).build();
		pasteButton.active = RoadNameClipboard.canPaste();

		addChild(ButtonWidget.builder(Text.translatable("gui.nanbin.clear"), button -> {
			final MinecraftClient client = MinecraftClient.getInstance();
			if (client != null) {
				client.setScreen(new RailwaySignClearConfirmScreen(this, Text.translatable("gui.nanbin.clear.question"), () -> {
					final String[] texts = getTexts();
					RoadNameClipboard.clear(blockPos, texts);
					setTexts(texts);
					undoButton.active = RoadNameClipboard.canUndo(blockPos);
				}));
			}
		}).dimensions(buttonX, buttonY + 60, 30, 20).build());

		addChild(pasteButton);
		addChild(undoButton);
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

	/** 获取第 index 个文本框的内容（0-3）。 */
	public String getText(int index) {
		if (index < 0 || index >= textFields.length) {
			return "";
		}
		return textFields[index].getText();
	}

	@Override
	public boolean isPauseScreen2() {
		return false;
	}

	@Override
	public void render2(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float delta) {
		try {
			graphicsHolder.drawCenteredText(Text.translatable("gui.nanbin.road_name"), width / 2, 8, ARGB_WHITE, true, GraphicsHolder.getDefaultLight());

			drawTextureAndDisplays(graphicsHolder);

			int fieldY = 40;
			for (int i = 0; i < textFields.length; i++) {
				graphicsHolder.drawText(Text.translatable("gui.nanbin.road_name_" + (i + 1)), fieldX, fieldY - LABEL_HEIGHT, ARGB_WHITE, false, GraphicsHolder.getDefaultLight());
				fieldY += ROW_SPACING;
			}
		} catch (Exception e) {
			InitClient.LOGGER.error("Failed to render RoadNameScreen", e);
		}
	}

	private void drawTextureAndDisplays(GraphicsHolder graphicsHolder) {
		final GuiDrawing guiDrawing = new GuiDrawing(graphicsHolder);

		guiDrawing.beginDrawingTexture(ROAD_NAME_TEXTURE);
		guiDrawing.drawTexture(textureX, textureY, textureX + textureWidth, textureY + textureHeight, 0F, 0F, 1F, 1F);
		guiDrawing.finishDrawingTexture();

		// 显示框 1：白色，居中于整个贴图
		drawDisplay(guiDrawing, textFields[0].getText(), 0.5F, 0.35F, 0.92F, 0.40F, FONT_SIZE_MAIN, COLOR_TEXT_MAIN, 0);
		// 显示框 2：白色，位于显示框 1 下方避免重叠
		drawDisplay(guiDrawing, textFields[1].getText(), 0.5F, 0.55F, 0.90F, 0.20F, FONT_SIZE_SUB, COLOR_TEXT_MAIN, 0);
		// 显示框 3：蓝色，贴图左下角
		drawDisplay(guiDrawing, textFields[2].getText(), 0.04F, 0.9375F, 0.92F, 0.12F, FONT_SIZE_CORNER, COLOR_TEXT_CORNER, -1);
		// 显示框 4：蓝色，贴图右下角
		drawDisplay(guiDrawing, textFields[3].getText(), 0.96F, 0.9375F, 0.92F, 0.12F, FONT_SIZE_CORNER, COLOR_TEXT_CORNER, 1);
	}

	/**
	 * 将文本渲染成紧贴内容的贴图，并以 (centerX, centerY) 为中心等比缩放到最大 maxW x maxH 的显示框内。
	 *
	 * @param alignX 水平对齐方式：0=居中（centerX 为文字中心）、-1=左对齐（centerX 为文字左边）、1=右对齐（centerX 为文字右边）
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
			x = cx;
		} else if (alignX == 1) {
			x = cx - w;
		} else {
			x = cx - w / 2.0F;
		}
		final float y = cy - h / 2.0F;

		guiDrawing.beginDrawingTexture(fitted.identifier);
		guiDrawing.drawTexture(x, y, x + w, y + h, 0F, 0F, 1F, 1F);
		guiDrawing.finishDrawingTexture();
	}
}
