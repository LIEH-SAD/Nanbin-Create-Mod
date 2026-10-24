package com.Nanbin.client.Screen;

import com.Nanbin.client.JavaScriptSupport.JSSignEngine;
import com.Nanbin.mapping.GraphicsHolder;
import com.Nanbin.mapping.ScreenBase;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.mtr.data.IGui;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongAVLTreeSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class JSSignDataScreen extends ScreenBase implements IGui {

	private static final int BUTTON_WIDTH = 180;
	private static final int BUTTON_HEIGHT = 20;
	private static final int SPACING = 8;
	private static final int TOP_PADDING = 35;
	private static final int BOTTOM_RESERVED = BUTTON_HEIGHT + SPACING + BUTTON_HEIGHT + SPACING + 20;
	private static final int SCROLLBAR_WIDTH = 8;
	private static final int SCROLLBAR_COLOR = 0xFF888888;

	private final BlockPos signPos;
	private final LongAVLTreeSet selectedIds;
	private final Runnable onSave;
	private final String scriptId;

	private final List<DataButton> dataButtons = new ArrayList<>();
	private final List<ButtonWidget> allButtons = new ArrayList<>();

	private int scrollIndex;
	private boolean isScrolling;

	public JSSignDataScreen(Screen parent, BlockPos signPos, LongAVLTreeSet selectedIds, Runnable onSave, String scriptId) {
		super(Text.translatable("gui.nanbin.js_data.title"), parent);
		this.signPos = signPos;
		this.selectedIds = selectedIds != null ? selectedIds : new LongAVLTreeSet();
		this.onSave = onSave;
		this.scriptId = scriptId;

		// 按脚本实际调用的数据接口决定显示哪些按钮；未记录到任何接口时全部显示。
		// 选择界面直接用 MTR 自带的（见 MtrSignSelectors），数据可用性也由它判断。
		final Set<String> usedTypes = JSSignEngine.getUsedDataTypes(scriptId);
		final boolean showAll = usedTypes.isEmpty();
		addDataButton(showAll || usedTypes.contains("platform"), "platform", MtrSignSelectors.DataType.PLATFORM, "gui.nanbin.js_data.select_platform");
		addDataButton(showAll || usedTypes.contains("station"), "station", MtrSignSelectors.DataType.STATION, "gui.nanbin.js_data.select_station");
		addDataButton(showAll || usedTypes.contains("route"), "route", MtrSignSelectors.DataType.ROUTE, "gui.nanbin.js_data.select_route");
		addDataButton(showAll || usedTypes.contains("exit"), "exit", MtrSignSelectors.DataType.EXIT, "gui.nanbin.js_data.select_exit");
		if (showAll || usedTypes.contains("text")) {
			dataButtons.add(new DataButton(Text.translatable("gui.nanbin.js_data.select_text"), this::openCustomText));
		}
	}

	private void addDataButton(boolean needed, String typeKey, MtrSignSelectors.DataType dataType, String translationKey) {
		if (needed && MtrSignSelectors.hasData(dataType, signPos)) {
			dataButtons.add(new DataButton(Text.translatable(translationKey), () -> openSelector(dataType)));
		}
	}

	@Override
	protected void init2() {
		super.init2();

		final int centerX = width / 2;
		int currentY = TOP_PADDING;

		allButtons.clear();
		for (final DataButton dataButton : dataButtons) {
			final ButtonWidget button = addChild(ButtonWidget.builder(dataButton.label, b -> dataButton.action.run())
					.dimensions(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT).build());
			allButtons.add(button);
			currentY += BUTTON_HEIGHT + SPACING;
		}

		final ButtonWidget saveButton = addChild(ButtonWidget.builder(Text.translatable("gui.nanbin.save"), b -> save())
				.dimensions(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT).build());
		allButtons.add(saveButton);

		final ButtonWidget cancelButton = addChild(ButtonWidget.builder(Text.translatable("gui.cancel"), b -> close())
				.dimensions(centerX - BUTTON_WIDTH / 2, currentY + BUTTON_HEIGHT + SPACING, BUTTON_WIDTH, BUTTON_HEIGHT).build());
		allButtons.add(cancelButton);

		updateButtonPositions();
	}

	private int getScrollableHeight() {
		return allButtons.size() * (BUTTON_HEIGHT + SPACING);
	}

	private int getVisibleHeight() {
		return Math.max(1, height - TOP_PADDING - BOTTOM_RESERVED);
	}

	private int getMaxScroll() {
		return Math.max(0, (getScrollableHeight() - getVisibleHeight() + BUTTON_HEIGHT + SPACING - 1) / (BUTTON_HEIGHT + SPACING));
	}

	private void updateButtonPositions() {
		scrollIndex = Math.max(0, Math.min(scrollIndex, getMaxScroll()));
		final int startY = TOP_PADDING - scrollIndex * (BUTTON_HEIGHT + SPACING);
		final int centerX = width / 2;

		for (int i = 0; i < allButtons.size(); i++) {
			final ButtonWidget button = allButtons.get(i);
			final int buttonY = startY + i * (BUTTON_HEIGHT + SPACING);
			if (buttonY + BUTTON_HEIGHT > TOP_PADDING && buttonY < height - BOTTOM_RESERVED) {
				button.setPosition(centerX - BUTTON_WIDTH / 2, buttonY);
				button.setWidth(BUTTON_WIDTH);
				button.visible = true;
			} else {
				button.visible = false;
			}
		}
	}

	private void openSelector(MtrSignSelectors.DataType dataType) {
		// JS 数据是「站台 + 线路 + 车站 + 出口」混存在一个集合里，所以不能像告示牌格子那样先清空
		MtrSignSelectors.open(this, dataType, signPos, selectedIds, false, null);
	}

	private void openCustomText() {
		openScreen(new CustomTextScreen(this, selectedIds, onSave));
	}

	private void save() {
		if (onSave != null) {
			onSave.run();
		}
		close();
	}

	@Override
	public void render2(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float delta) {
		graphicsHolder.drawCenteredText(Text.translatable("gui.nanbin.js_data.title"), width / 2, 15, ARGB_WHITE, true, GraphicsHolder.getDefaultLight());

		if (scrollIndex > 0) {
			graphicsHolder.drawCenteredText("↑", width / 2, TOP_PADDING - 13, ARGB_GRAY, true, GraphicsHolder.getDefaultLight());
		}
		if (scrollIndex < getMaxScroll()) {
			graphicsHolder.drawCenteredText("↓", width / 2, height - BOTTOM_RESERVED + 13, ARGB_GRAY, true, GraphicsHolder.getDefaultLight());
		}

		if (getMaxScroll() > 0) {
			final int scrollbarX = width - SCROLLBAR_WIDTH - 4;
			final int scrollbarHeight = getVisibleHeight();
			final int thumbHeight = Math.max(10, scrollbarHeight * getVisibleHeight() / Math.max(1, getScrollableHeight()));
			final int thumbY = TOP_PADDING + (scrollbarHeight - thumbHeight) * scrollIndex / getMaxScroll();

			// GuiDrawing 必须先 begin/finish，否则 drawRectangle 因矩阵未初始化而变成空操作
			final com.Nanbin.mapping.GuiDrawing scrollbarDrawing = new com.Nanbin.mapping.GuiDrawing(graphicsHolder);
			scrollbarDrawing.beginDrawingRectangle();
			scrollbarDrawing.drawRectangle(scrollbarX, TOP_PADDING, scrollbarX + SCROLLBAR_WIDTH, TOP_PADDING + scrollbarHeight, 0xFF000000);
			scrollbarDrawing.drawRectangle(scrollbarX, thumbY, scrollbarX + SCROLLBAR_WIDTH, thumbY + thumbHeight, SCROLLBAR_COLOR);
			scrollbarDrawing.finishDrawingRectangle();
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (getMaxScroll() > 0) {
			scrollIndex += (int) Math.signum(-verticalAmount);
			updateButtonPositions();
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0 && getMaxScroll() > 0) {
			final int scrollbarX = width - SCROLLBAR_WIDTH - 4;
			if (mouseX >= scrollbarX && mouseX < scrollbarX + SCROLLBAR_WIDTH && mouseY >= TOP_PADDING && mouseY < TOP_PADDING + getVisibleHeight()) {
				isScrolling = true;
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (button == 0) {
			isScrolling = false;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (isScrolling && getMaxScroll() > 0) {
			final int scrollbarHeight = getVisibleHeight();
			final int thumbHeight = Math.max(10, scrollbarHeight * getVisibleHeight() / Math.max(1, getScrollableHeight()));
			final double newThumbY = mouseY - thumbHeight / 2.0;
			final double scrollRatio = (newThumbY - TOP_PADDING) / Math.max(1.0, scrollbarHeight - thumbHeight);
			scrollIndex = (int) Math.round(getMaxScroll() * Math.max(0, Math.min(1, scrollRatio)));
			updateButtonPositions();
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
	}

	@Override
	public void close() {
		// 4.0 的 onClose2 是「回到 parent」，由 ScreenBase 的 previousScreen 机制完成
		super.close();
	}

	@Override
	public boolean isPauseScreen2() {
		return false;
	}

	private record DataButton(MutableText label, Runnable action) {
	}
}
