package com.Nanbin.client.Screen;

import com.Nanbin.client.JavaScriptSupport.JSSignConfig;
import org.mtr.mapping.holder.ClickableWidget;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.holder.Screen;
import org.mtr.mapping.mapper.*;
import org.mtr.mod.client.IDrawing;
import org.mtr.mod.data.IGui;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 全局 JS 样式选择器：列出所有可用脚本（图标 + 翻译后的名称），
 * 选中后回调 onSelect(scriptId)；选择“无样式”时回调 onSelect(null)。
 */
public class JSSignSelectorScreen extends ScreenExtension implements IGui {

	private static final int ENTRY_HEIGHT = 24;
	private static final int ENTRY_GAP = 4;
	private static final int TOP_PADDING = 35;
	/** 底部预留：取消按钮(24) + 边距(20) + 指示箭头(8)。 */
	private static final int BOTTOM_RESERVED = ENTRY_HEIGHT + 20 + 8;
	private static final int SCROLLBAR_WIDTH = 8; // New constant for scrollbar width
	private static final int SCROLLBAR_COLOR = 0xFF888888; // Gray color for scrollbar

	private final ScreenExtension parent;
	@Nullable
	private final String currentScriptId;
	private final Consumer<String> onSelect;

	private int startX;
	private int startY;
	private int scrollIndex;
	private boolean isScrolling; // New field to track if scrollbar is being dragged

	private final List<String> scriptIds = new ArrayList<>();
	private final ButtonWidgetExtension[] buttonsJSSigns;
	private final ButtonWidgetExtension buttonNone;
	private final ButtonWidgetExtension buttonCancel;

	public JSSignSelectorScreen(ScreenExtension parent, @Nullable String currentScriptId, Consumer<String> onSelect) {
		super();
		this.parent = parent;
		this.currentScriptId = currentScriptId;
		this.onSelect = onSelect;

		scriptIds.addAll(JSSignConfig.getAllScripts().keySet());

		buttonsJSSigns = new ButtonWidgetExtension[scriptIds.size()];
		for (int i = 0; i < scriptIds.size(); i++) {
			final int index = i;
			final String scriptId = scriptIds.get(i);
			buttonsJSSigns[i] = new ButtonWidgetExtension(0, 0, 0, ENTRY_HEIGHT, getScriptName(scriptId), button -> selectScript(scriptId));
		}

		buttonNone = new ButtonWidgetExtension(0, 0, 0, ENTRY_HEIGHT, TextHelper.translatable("gui.nanbin.js_style.none"), button -> selectScript(null));
		buttonCancel = new ButtonWidgetExtension(0, 0, 0, ENTRY_HEIGHT, TextHelper.translatable("gui.cancel"), button -> onClose2());
	}

	@Override
	protected void init2() {
		super.init2();

		final int buttonWidth = getButtonWidth();
		startX = (width - buttonWidth) / 2;
		startY = TOP_PADDING;

		// 子控件只添加一次，滚动时仅通过 updateButtonPositions 调整位置
		for (int i = 0; i < buttonsJSSigns.length; i++) {
			addChild(new ClickableWidget(buttonsJSSigns[i]));
		}
		addChild(new ClickableWidget(buttonNone));
		addChild(new ClickableWidget(buttonCancel));
		updateButtonPositions();
	}

	private int getButtonWidth() {
		// Adjust button width to make space for the scrollbar
		return Math.min(220, width - 40 - SCROLLBAR_WIDTH - 4); // Added SCROLLBAR_WIDTH and some padding
	}

	/** 当前可视区域能显示的脚本条目数（不含无样式/取消按钮）。 */
	private int getVisibleCount() {
		return Math.max(1, (height - startY - BOTTOM_RESERVED) / (ENTRY_HEIGHT + ENTRY_GAP));
	}

	/** 能向上滚动到的最大索引（0 表示没有可滚动的）。 */
	private int getMaxScrollIndex() {
		return Math.max(0, scriptIds.size() - getVisibleCount());
	}

	/** 根据 scrollIndex 布局可见按钮；不可见的移到屏幕外并禁用。 */
	private void updateButtonPositions() {
		scrollIndex = Math.max(0, Math.min(scrollIndex, getMaxScrollIndex()));
		final int visibleCount = getVisibleCount();
		for (int i = 0; i < buttonsJSSigns.length; i++) {
			final int visibleIndex = i - scrollIndex;
			if (visibleIndex >= 0 && visibleIndex < visibleCount) {
				IDrawing.setPositionAndWidth(buttonsJSSigns[i], startX, startY + visibleIndex * (ENTRY_HEIGHT + ENTRY_GAP), getButtonWidth());
				buttonsJSSigns[i].active = !scriptIds.get(i).equals(currentScriptId);
			} else {
				IDrawing.setPositionAndWidth(buttonsJSSigns[i], startX, -ENTRY_HEIGHT, getButtonWidth());
				buttonsJSSigns[i].active = false;
			}
		}

		final int noneY = startY + visibleCount * (ENTRY_HEIGHT + ENTRY_GAP);
		IDrawing.setPositionAndWidth(buttonNone, startX, noneY, getButtonWidth());

		final int cancelY = height - ENTRY_HEIGHT - 20;
		IDrawing.setPositionAndWidth(buttonCancel, (width - getButtonWidth()) / 2, cancelY, getButtonWidth());
	}

	@Override
	public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float delta) {
		renderBackground(graphicsHolder);
		super.render(graphicsHolder, mouseX, mouseY, delta);

		graphicsHolder.drawCenteredText(TextHelper.translatable("gui.nanbin.js_style.title").getString(), width / 2, 15, ARGB_WHITE);

		// 只在可视范围内绘制脚本图标
		final int visibleCount = getVisibleCount();
		for (int i = scrollIndex; i < Math.min(scriptIds.size(), scrollIndex + visibleCount); i++) {
			final String scriptId = scriptIds.get(i);
			final int iconY = startY + (i - scrollIndex) * (ENTRY_HEIGHT + ENTRY_GAP) + (ENTRY_HEIGHT - 16) / 2;
			final GuiDrawing guiDrawing = new GuiDrawing(graphicsHolder);
			guiDrawing.beginDrawingTexture(JSSignConfig.getIcon(scriptId));
			guiDrawing.drawTexture(startX + 4, iconY, startX + 20, iconY + 16, 0, 0, 1, 1);
			guiDrawing.finishDrawingTexture();
		}

		// 滚动指示箭头
		if (scrollIndex > 0) {
			graphicsHolder.drawCenteredText("↑", width / 2, startY - 13, ARGB_GRAY);
		}
		if (scrollIndex < getMaxScrollIndex()) {
			graphicsHolder.drawCenteredText("↓", width / 2, height - ENTRY_HEIGHT - 20 - 13, ARGB_GRAY);
		}

		// Draw scrollbar
		if (getMaxScrollIndex() > 0) {
			final int scrollbarX = startX + getButtonWidth() + 4; // Position scrollbar to the right of buttons
			final int scrollbarY = startY;
			final int scrollbarHeight = height - startY - BOTTOM_RESERVED;
			final int thumbHeight = Math.max(10, scrollbarHeight * getVisibleCount() / scriptIds.size());
			final int thumbY = scrollbarY + (scrollbarHeight - thumbHeight) * scrollIndex / getMaxScrollIndex();

			new GuiDrawing(graphicsHolder).drawRectangle(scrollbarX, scrollbarY, scrollbarX + SCROLLBAR_WIDTH, scrollbarY + scrollbarHeight, 0xFF000000); // Scrollbar background
			new GuiDrawing(graphicsHolder).drawRectangle(scrollbarX, thumbY, scrollbarX + SCROLLBAR_WIDTH, thumbY + thumbHeight, SCROLLBAR_COLOR); // Scrollbar thumb
		}
	}

	@Override
	public boolean mouseScrolled2(double mouseX, double mouseY, double amount) {
		if (scriptIds.size() > getVisibleCount()) {
			scrollIndex += (int) Math.signum(-amount);
			updateButtonPositions();
		}
		return super.mouseScrolled2(mouseX, mouseY, amount);
	}

	@Override
	public boolean mouseClicked2(double mouseX, double mouseY, int button) {
		if (button == 0 && getMaxScrollIndex() > 0) { // Left click
			final int scrollbarX = startX + getButtonWidth() + 4;
			final int scrollbarY = startY;
			final int scrollbarHeight = height - startY - BOTTOM_RESERVED;

			if (mouseX >= scrollbarX && mouseX < scrollbarX + SCROLLBAR_WIDTH && mouseY >= scrollbarY && mouseY < scrollbarY + scrollbarHeight) {
				isScrolling = true;
				return true; // Consume the event
			}
		}
		return super.mouseClicked2(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseReleased2(double mouseX, double mouseY, int button) {
		if (button == 0) { // Left click released
			isScrolling = false;
		}
		return super.mouseReleased2(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged2(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (isScrolling && getMaxScrollIndex() > 0) {
			final int scrollbarY = startY;
			final int scrollbarHeight = height - startY - BOTTOM_RESERVED;
			final int thumbHeight = Math.max(10, scrollbarHeight * getVisibleCount() / scriptIds.size());

			final double newThumbY = mouseY - thumbHeight / 2.0;
			final double scrollRatio = (newThumbY - scrollbarY) / (scrollbarHeight - thumbHeight);
			scrollIndex = (int) Math.round(getMaxScrollIndex() * Math.max(0, Math.min(1, scrollRatio)));
			updateButtonPositions();
			return true; // Consume the event
		}
		return super.mouseDragged2(mouseX, mouseY, button, deltaX, deltaY);
	}

	private void selectScript(@Nullable String scriptId) {
		if (onSelect != null) {
			onSelect.accept(scriptId);
		}
		MinecraftClient.getInstance().openScreen(new Screen(parent));
	}

	/** 脚本显示名称（lang 翻译优先，回退到配置的 name / id）。 */
	public static MutableText getScriptName(String scriptId) {
		return TextHelper.translatable(JSSignConfig.getDisplayNameKey(scriptId));
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