package com.Nanbin.client.Screen;

import com.Nanbin.client.JavaScriptSupport.JSSignEngine;
import it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import mtr.client.ClientData;
import mtr.data.DataConverter;
import mtr.data.NameColorDataBase;
import mtr.data.Platform;
import mtr.data.Route;
import mtr.data.Station;
import mtr.screen.DashboardListSelectorScreen;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * JS 样式数据选择屏幕：点击编辑按钮后打开，动态列出当前站点可用的数据类别
 * （站台 / 车站 / 线路 / 出口 / 自定义文本），分别打开对应的选择器供脚本读取。
 * 只显示脚本实际调用的数据接口对应的按钮；脚本未调用任何数据接口时显示全部。
 * 选完后点击“保存”统一提交。
 */
public class JSSignDataScreen extends Screen {

	private static final int BUTTON_WIDTH = 180;
	private static final int BUTTON_HEIGHT = 20;
	private static final int SPACING = 8;
	private static final int TOP_PADDING = 35;
	private static final int BOTTOM_RESERVED = BUTTON_HEIGHT + SPACING + BUTTON_HEIGHT + SPACING + 20; // Save and Cancel buttons + padding
	private static final int SCROLLBAR_WIDTH = 8;
	private static final int SCROLLBAR_COLOR = 0xFF888888;
	private static final int ARGB_WHITE = 0xFFFFFFFF;
	private static final int ARGB_GRAY = 0xFFAAAAAA;

	private final Screen parent;
	private final BlockPos signPos;
	private final Set<Long> selectedIds;
	private final Runnable onSave;
	private final String scriptId;

	private final List<NameColorDataBase> platformsForList = new ArrayList<>();
	private final List<NameColorDataBase> exitsForList = new ArrayList<>();
	private final List<NameColorDataBase> routesForList = new ArrayList<>();
	private final List<NameColorDataBase> stationsForList = new ArrayList<>();

	private final List<DataButton> dataButtons = new ArrayList<>();
	private final List<ButtonWidget> allButtons = new ArrayList<>();

	private int scrollIndex;
	private boolean isScrolling;

	public JSSignDataScreen(Screen parent, BlockPos signPos, Set<Long> selectedIds, Runnable onSave, String scriptId) {
		super(Text.translatable("gui.nanbin.js_data.title"));
		this.parent = parent;
		this.signPos = signPos;
		this.selectedIds = selectedIds;
		this.onSave = onSave;
		this.scriptId = scriptId;

		final Station station = findStation(signPos);
		if (station == null) {
			return;
		}

		// 站台列表
		final Set<Station> connectingStations = new LinkedHashSet<>(ClientData.DATA_CACHE.getConnectingStationsIncludingThisOne(station));
		connectingStations.add(station);

		// 本站点台
		final Set<Long> platformIds = new LongAVLTreeSet();
		for (final Platform platform : ClientData.PLATFORMS) {
			final Station owner = ClientData.DATA_CACHE.platformIdToStation.get(platform.id);
			if (owner != null && connectingStations.contains(owner)) {
				platformsForList.add(platform);
				platformIds.add(platform.id);
			}
		}

		// 车站列表
		for (final Station s : connectingStations) {
			stationsForList.add(s);
		}

		// 出口列表（station.exits: Map<出口名, List<目的地>>）
		// 选择器只写入 NameColorDataBase.id，因此必须用 serializeExit 编码出口名，
		// 脚本侧 getExitNumbers() 才能反解出出口编号。
		final Map<String, List<String>> exits = station.getGeneratedExits();
		if (exits != null) {
			for (final Map.Entry<String, List<String>> entry : exits.entrySet()) {
				exitsForList.add(new DataConverter(Station.serializeExit(entry.getKey()), entry.getKey(), 0xFFFFFF));
			}
		}

		// 线路列表（按颜色去重，途经本片站点）
		final Set<Integer> addedColors = new LinkedHashSet<>();
		for (final Route route : ClientData.ROUTES) {
			final int color = route.color;
			if (addedColors.contains(color)) {
				continue;
			}
			boolean passesThrough = false;
			for (final Long pid : platformIds) {
				if (route.getPlatformIdIndex(pid) >= 0) {
					passesThrough = true;
					break;
				}
			}
			if (passesThrough) {
				// 线路项 id 必须为 route.color（与 MTR 线路牌 / CRT 渲染器一致），
				// 多选时 selectedIds 中会累积多个线路色值，脚本按 getSelectedColors() 逐条读取。
				routesForList.add(new DataConverter(route.color, route.name.split("\\|\\|")[0], color));
				addedColors.add(color);
			}
		}

		// 根据脚本实际调用的数据接口动态调整按钮；未记录到任何接口时显示全部
		final Set<String> usedTypes = JSSignEngine.getUsedDataTypes(scriptId);
		final boolean showAll = usedTypes.isEmpty();
		if (showAll || usedTypes.contains("platform")) {
			if (!platformsForList.isEmpty()) {
				dataButtons.add(new DataButton(Text.translatable("gui.nanbin.js_data.select_platform"), () -> openSelector(platformsForList)));
			}
		}
		if (showAll || usedTypes.contains("station")) {
			if (!stationsForList.isEmpty()) {
				dataButtons.add(new DataButton(Text.translatable("gui.nanbin.js_data.select_station"), () -> openSelector(stationsForList)));
			}
		}
		if (showAll || usedTypes.contains("route")) {
			if (!routesForList.isEmpty()) {
				dataButtons.add(new DataButton(Text.translatable("gui.nanbin.js_data.select_route"), () -> openSelector(routesForList)));
			}
		}
		if (showAll || usedTypes.contains("exit")) {
			if (!exitsForList.isEmpty()) {
				dataButtons.add(new DataButton(Text.translatable("gui.nanbin.js_data.select_exit"), () -> openSelector(exitsForList)));
			}
		}
		if (showAll || usedTypes.contains("text")) {
			dataButtons.add(new DataButton(Text.translatable("gui.nanbin.js_data.select_text"), this::openCustomText));
		}
	}

	@Override
	protected void init() {
		super.init();

		final int centerX = width / 2;
		int currentY = TOP_PADDING;

		allButtons.clear();

		for (final DataButton dataButton : dataButtons) {
			final ButtonWidget button = new ButtonWidget(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT, dataButton.label, b -> dataButton.action.run());
			allButtons.add(button);
			addDrawableChild(button);
			currentY += BUTTON_HEIGHT + SPACING;
		}

		final ButtonWidget saveButton = new ButtonWidget(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT, Text.translatable("gui.nanbin.save"), b -> save());
		allButtons.add(saveButton);
		addDrawableChild(saveButton);

		final ButtonWidget cancelButton = new ButtonWidget(centerX - BUTTON_WIDTH / 2, currentY + BUTTON_HEIGHT + SPACING, BUTTON_WIDTH, BUTTON_HEIGHT, Text.translatable("gui.cancel"), b -> close());
		allButtons.add(cancelButton);
		addDrawableChild(cancelButton);

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
			final ButtonWidget button = allButtons.get(i);
			final int buttonY = startY + i * (BUTTON_HEIGHT + SPACING);
			final int centerX = width / 2;

			if (buttonY + BUTTON_HEIGHT > TOP_PADDING && buttonY < height - BOTTOM_RESERVED) {
				button.x = centerX - BUTTON_WIDTH / 2;
				button.y = buttonY;
				button.visible = true;
			} else {
				button.visible = false;
			}
		}
	}

	/** 打开对应类别的选择器，选完返回本屏幕。 */
	private void openSelector(List<NameColorDataBase> list) {
		final DashboardListSelectorScreen selector = new DashboardListSelectorScreen(() -> client.setScreen(JSSignDataScreen.this), list, selectedIds, false, false);
		client.setScreen(selector);
	}

	/** 打开自定义文本编辑屏幕，保存后文本写入 selectedIds 供脚本 getCustomText() 读取。 */
	private void openCustomText() {
		client.setScreen(new CustomTextScreen(this, selectedIds, onSave));
	}

	private void save() {
		if (onSave != null) {
			onSave.run();
		}
		close();
	}

	@Override
	public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
		renderBackground(matrices);
		super.render(matrices, mouseX, mouseY, delta);

		DrawableHelper.drawCenteredText(matrices, textRenderer, Text.translatable("gui.nanbin.js_data.title"), width / 2, 15, ARGB_WHITE);

		// Draw scroll indicators
		if (scrollIndex > 0) {
			DrawableHelper.drawCenteredText(matrices, textRenderer, Text.literal("↑"), width / 2, TOP_PADDING - 13, ARGB_GRAY);
		}
		if (scrollIndex < getMaxScroll()) {
			DrawableHelper.drawCenteredText(matrices, textRenderer, Text.literal("↓"), width / 2, height - BOTTOM_RESERVED + 13, ARGB_GRAY);
		}

		// Draw scrollbar
		if (getMaxScroll() > 0) {
			final int scrollbarX = width - SCROLLBAR_WIDTH - 4;
			final int scrollbarY = TOP_PADDING;
			final int scrollbarHeight = getVisibleHeight();
			final int thumbHeight = Math.max(10, scrollbarHeight * getVisibleHeight() / getScrollableHeight());
			final int thumbY = scrollbarY + (scrollbarHeight - thumbHeight) * scrollIndex / getMaxScroll();

			fill(matrices, scrollbarX, scrollbarY, scrollbarX + SCROLLBAR_WIDTH, scrollbarY + scrollbarHeight, 0xFF000000);
			fill(matrices, scrollbarX, thumbY, scrollbarX + SCROLLBAR_WIDTH, thumbY + thumbHeight, SCROLLBAR_COLOR);
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
		if (getMaxScroll() > 0) {
			scrollIndex += (int) Math.signum(-amount);
			updateButtonPositions();
		}
		return super.mouseScrolled(mouseX, mouseY, amount);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0 && getMaxScroll() > 0) { // Left click
			final int scrollbarX = width - SCROLLBAR_WIDTH - 4;
			final int scrollbarY = TOP_PADDING;
			final int scrollbarHeight = getVisibleHeight();

			if (mouseX >= scrollbarX && mouseX < scrollbarX + SCROLLBAR_WIDTH && mouseY >= scrollbarY && mouseY < scrollbarY + scrollbarHeight) {
				isScrolling = true;
				return true; // Consume the event
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (button == 0) { // Left click released
			isScrolling = false;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
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
		return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
	}

	@Override
	public void close() {
		if (parent != null) {
			client.setScreen(parent);
		} else {
			super.close();
		}
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	/** 按方块坐标就近查找所属车站。 */
	private static Station findStation(BlockPos pos) {
		final Station direct = ClientData.DATA_CACHE.blockPosToStation.get(pos);
		if (direct != null) {
			return direct;
		}
		for (final Station station : ClientData.STATIONS) {
			if (station.inArea(pos.getX(), pos.getZ())) {
				return station;
			}
		}
		return null;
	}

	/** 数据按钮：标题 + 打开选择器的动作。 */
	private static final class DataButton {
		private final Text label;
		private final Runnable action;

		private DataButton(Text label, Runnable action) {
			this.label = label;
			this.action = action;
		}
	}
}