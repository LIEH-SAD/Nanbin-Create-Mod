package com.Nanbin.client.Screen;

import com.Nanbin.client.JavaScriptSupport.JSSignEngine;
import org.mtr.core.data.Station;
import org.mtr.libraries.it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArraySet;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.ButtonWidgetExtension;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;
import org.mtr.mapping.mapper.ScreenExtension;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mod.InitClient;
import org.mtr.mod.client.IDrawing;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.data.IGui;
import org.mtr.mod.screen.DashboardListItem;
import org.mtr.mod.screen.DashboardListSelectorScreen;
import org.mtr.mod.screen.EditStationScreen;
import org.mtr.mod.screen.PIDSConfigScreen;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * JS 样式数据选择屏幕：点击编辑按钮后打开，动态列出当前站点可用的数据类别
 * （站台 / 车站 / 线路 / 出口 / 自定义文本），分别打开对应的选择器供脚本读取。
 * 只显示脚本实际调用的数据接口对应的按钮；脚本未调用任何数据接口时显示全部。
 * 选完后点击“保存”统一提交。
 */
public class JSSignDataScreen extends ScreenExtension implements IGui {

	private static final int BUTTON_WIDTH = 180;
	private static final int BUTTON_HEIGHT = 20;
	private static final int SPACING = 8;

 	private static final int TOP_PADDING = 35;
	private static final int BOTTOM_RESERVED = BUTTON_HEIGHT + SPACING + BUTTON_HEIGHT + SPACING + 20; // Save and Cancel buttons + padding
	private static final int SCROLLBAR_WIDTH = 8;
	private static final int SCROLLBAR_COLOR = 0xFF888888;

	private final ScreenExtension parent;
	private final BlockPos signPos;
	private final LongAVLTreeSet selectedIds;
	private final Runnable onSave;
	private final String scriptId;

	private final ObjectImmutableList<DashboardListItem> platformsForList;
	private final ObjectImmutableList<DashboardListItem> exitsForList;
	private final ObjectImmutableList<DashboardListItem> routesForList;
	private final ObjectImmutableList<DashboardListItem> stationsForList;

	private final List<DataButton> dataButtons = new ArrayList<>();
	private final List<ButtonWidgetExtension> allButtons = new ArrayList<>();

	private int scrollIndex;
	private boolean isScrolling;

	public JSSignDataScreen(ScreenExtension parent, BlockPos signPos, LongAVLTreeSet selectedIds, Runnable onSave, String scriptId) {
		super();
		this.parent = parent;
		this.signPos = signPos;
		this.selectedIds = selectedIds;
		this.onSave = onSave;
		this.scriptId = scriptId;

		final Station station = InitClient.findStation(signPos);
		if (station == null) {
			platformsForList = ObjectImmutableList.of();
			exitsForList = ObjectImmutableList.of();
			routesForList = ObjectImmutableList.of();
			stationsForList = ObjectImmutableList.of();
			return;
		}

		platformsForList = new ObjectImmutableList<>(PIDSConfigScreen.getPlatformsForList(new ObjectArrayList<>(station.savedRails)));

		exitsForList = new ObjectImmutableList<>(EditStationScreen.getExitsForDashboardList(EditStationScreen.getStationExits(station, true)));

		final ObjectArraySet<Station> connectingStationsIncludingThisOne = new ObjectArraySet<>(station.connectedStations);
		connectingStationsIncludingThisOne.add(station);
		stationsForList = new ObjectImmutableList<>(MinecraftClientData.convertDataSet(connectingStationsIncludingThisOne));

		final LongAVLTreeSet platformIds = new LongAVLTreeSet();
		connectingStationsIncludingThisOne.forEach(connectingStation -> connectingStation.savedRails.forEach(platform -> platformIds.add(platform.getId())));

		final ObjectArraySet<DashboardListItem> routes = new ObjectArraySet<>();
		final IntAVLTreeSet addedColors = new IntAVLTreeSet();
		MinecraftClientData.getInstance().simplifiedRoutes.forEach(simplifiedRoute -> {
			final int color = simplifiedRoute.getColor();
			if (!addedColors.contains(color) && simplifiedRoute.getPlatforms().stream().anyMatch(simplifiedRoutePlatform -> platformIds.contains(simplifiedRoutePlatform.getPlatformId()))) {
				routes.add(new DashboardListItem(color, simplifiedRoute.getName().split("\\|\\|")[0], color));
				addedColors.add(color);
			}
		});
		routesForList = new ObjectImmutableList<>(routes);

		// 根据脚本实际调用的数据接口动态调整按钮；未记录到任何接口时显示全部
		final Set<String> usedTypes = JSSignEngine.getUsedDataTypes(scriptId);
		final boolean showAll = usedTypes.isEmpty();
		if (showAll || usedTypes.contains("platform")) {
			if (!platformsForList.isEmpty()) {
				dataButtons.add(new DataButton(TextHelper.translatable("gui.nanbin.js_data.select_platform"), () -> openSelector(platformsForList)));
			}
		}
		if (showAll || usedTypes.contains("station")) {
			if (!stationsForList.isEmpty()) {
				dataButtons.add(new DataButton(TextHelper.translatable("gui.nanbin.js_data.select_station"), () -> openSelector(stationsForList)));
			}
		}
		if (showAll || usedTypes.contains("route")) {
			if (!routesForList.isEmpty()) {
				dataButtons.add(new DataButton(TextHelper.translatable("gui.nanbin.js_data.select_route"), () -> openSelector(routesForList)));
			}
		}
		if (showAll || usedTypes.contains("exit")) {
			if (!exitsForList.isEmpty()) {
				dataButtons.add(new DataButton(TextHelper.translatable("gui.nanbin.js_data.select_exit"), () -> openSelector(exitsForList)));
			}
		}
		if (showAll || usedTypes.contains("text")) {
			dataButtons.add(new DataButton(TextHelper.translatable("gui.nanbin.js_data.select_text"), () -> openCustomText()));
		}
	}

	@Override
	protected void init2() {
		super.init2();

		final int centerX = width / 2;
		int currentY = TOP_PADDING;

		allButtons.clear();

		for (final DataButton dataButton : dataButtons) {
			final ButtonWidgetExtension button = new ButtonWidgetExtension(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT, dataButton.label, b -> dataButton.action.run());
			allButtons.add(button);
			addChild(new ClickableWidget(button));
			currentY += BUTTON_HEIGHT + SPACING;
		}

		final ButtonWidgetExtension saveButton = new ButtonWidgetExtension(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT, TextHelper.translatable("gui.nanbin.save"), b -> save());
		allButtons.add(saveButton);
		addChild(new ClickableWidget(saveButton));

		final ButtonWidgetExtension cancelButton = new ButtonWidgetExtension(centerX - BUTTON_WIDTH / 2, currentY + BUTTON_HEIGHT + SPACING, BUTTON_WIDTH, BUTTON_HEIGHT, TextHelper.translatable("gui.cancel"), b -> onClose2());
		allButtons.add(cancelButton);
		addChild(new ClickableWidget(cancelButton));

		updateButtonPositions();
	}

	private int getScrollableHeight() {
		return allButtons.size() * (BUTTON_HEIGHT + SPACING);
	}

	private int getVisibleHeight() {
		return height - TOP_PADDING - BOTTOM_RESERVED;
	}

	private int getMaxScroll() {
		return Math.max(0, (getScrollableHeight() - getVisibleHeight() + BUTTON_HEIGHT + SPACING - 1) / (BUTTON_HEIGHT + SPACING));
	}

	private void updateButtonPositions() {
		scrollIndex = Math.max(0, Math.min(scrollIndex, getMaxScroll()));
		final int startY = TOP_PADDING - scrollIndex * (BUTTON_HEIGHT + SPACING);

		for (int i = 0; i < allButtons.size(); i++) {
			final ButtonWidgetExtension button = allButtons.get(i);
			final int buttonY = startY + i * (BUTTON_HEIGHT + SPACING);
			final int centerX = width / 2;

			// Check if the button is within the visible area
			if (buttonY + BUTTON_HEIGHT > TOP_PADDING && buttonY < height - BOTTOM_RESERVED) {
				IDrawing.setPositionAndWidth(button, centerX - BUTTON_WIDTH / 2, buttonY, BUTTON_WIDTH);
				button.visible = true;
			} else {
				button.visible = false;
			}
		}
	}

	/** 打开对应类别的选择器，选完返回本屏幕。 */
	private void openSelector(ObjectImmutableList<DashboardListItem> list) {
		MinecraftClient.getInstance().openScreen(new Screen(new DashboardListSelectorScreen(list, selectedIds, false, false, this)));
	}

	/** 打开自定义文本编辑屏幕，保存后文本写入 selectedIds 供脚本 getCustomText() 读取。 */
	private void openCustomText() {
		MinecraftClient.getInstance().openScreen(new Screen(new CustomTextScreen(this, selectedIds, onSave)));
	}

	private void save() {
		if (onSave != null) {
			onSave.run();
		}
		onClose2();
	}

	@Override
	public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float delta) {
		renderBackground(graphicsHolder);
		super.render(graphicsHolder, mouseX, mouseY, delta);

		graphicsHolder.drawCenteredText(TextHelper.translatable("gui.nanbin.js_data.title").getString(), width / 2, 15, ARGB_WHITE);

		// Draw scroll indicators
		if (scrollIndex > 0) {
			graphicsHolder.drawCenteredText("↑", width / 2, TOP_PADDING - 13, ARGB_GRAY);
		}
		if (scrollIndex < getMaxScroll()) {
			graphicsHolder.drawCenteredText("↓", width / 2, height - BOTTOM_RESERVED + 13, ARGB_GRAY);
		}

		// Draw scrollbar
		if (getMaxScroll() > 0) {
			final int scrollbarX = width - SCROLLBAR_WIDTH - 4;
			final int scrollbarY = TOP_PADDING;
			final int scrollbarHeight = getVisibleHeight();
			final int thumbHeight = Math.max(10, scrollbarHeight * getVisibleHeight() / getScrollableHeight());
			final int thumbY = scrollbarY + (scrollbarHeight - thumbHeight) * scrollIndex / getMaxScroll();

			new GuiDrawing(graphicsHolder).drawRectangle(scrollbarX, scrollbarY, scrollbarX + SCROLLBAR_WIDTH, scrollbarY + scrollbarHeight, 0xFF000000); // Scrollbar background
			new GuiDrawing(graphicsHolder).drawRectangle(scrollbarX, thumbY, scrollbarX + SCROLLBAR_WIDTH, thumbY + thumbHeight, SCROLLBAR_COLOR); // Scrollbar thumb
		}
	}

	@Override
	public boolean mouseScrolled2(double mouseX, double mouseY, double amount) {
		if (getMaxScroll() > 0) {
			scrollIndex += (int) Math.signum(-amount);
			updateButtonPositions();
		}
		return super.mouseScrolled2(mouseX, mouseY, amount);
	}

	@Override
	public boolean mouseClicked2(double mouseX, double mouseY, int button) {
		if (button == 0 && getMaxScroll() > 0) { // Left click
			final int scrollbarX = width - SCROLLBAR_WIDTH - 4;
			final int scrollbarY = TOP_PADDING;
			final int scrollbarHeight = getVisibleHeight();

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
		if (isScrolling && getMaxScroll() > 0) {
			final int scrollbarY = TOP_PADDING;
			final int scrollbarHeight = getVisibleHeight();
			final int thumbHeight = Math.max(10, scrollbarHeight * getVisibleHeight() / getScrollableHeight());

			final double newThumbY = mouseY - thumbHeight / 2.0;
			final double scrollRatio = (newThumbY - scrollbarY) / (scrollbarHeight - thumbHeight);
			scrollIndex = (int) Math.round(getMaxScroll() * Math.max(0, Math.min(1, scrollRatio)));
			updateButtonPositions();
			return true; // Consume the event
		}
		return super.mouseDragged2(mouseX, mouseY, button, deltaX, deltaY);
	}

	@Override
	public void onClose2() {
		MinecraftClient.getInstance().openScreen(new Screen(parent));
	}

	@Override
	public boolean isPauseScreen2() {
		return false;
	}

	/** 数据按钮：标题 + 打开选择器的动作。 */
	private static final class DataButton {
		private final MutableText label;
		private final Runnable action;

		private DataButton(MutableText label, Runnable action) {
			this.label = label;
			this.action = action;
		}
	}
}