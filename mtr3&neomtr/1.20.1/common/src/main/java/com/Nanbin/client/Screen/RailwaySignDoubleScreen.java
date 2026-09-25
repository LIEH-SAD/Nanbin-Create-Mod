package com.Nanbin.client.Screen;

import com.Nanbin.Init;
import com.Nanbin.Registry.RegBlock.BlockCRTStationInfo1;
import com.Nanbin.Registry.RegBlock.BlockCRTStationInfo1Double;
import com.Nanbin.client.ClientData.SignClipboard;
import com.Nanbin.client.JavaScriptSupport.JSSignConfig;
import it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import mtr.MTR;
import mtr.block.BlockRailwaySign;
import mtr.client.ClientData;
import mtr.client.CustomResources;
import mtr.client.IDrawing;
import mtr.data.DataConverter;
import mtr.data.IGui;
import mtr.data.NameColorDataBase;
import mtr.data.Platform;
import mtr.data.RailwayData;
import mtr.data.Station;
import mtr.mappings.ScreenMapper;
import mtr.mappings.Text;
import mtr.mappings.UtilitiesClient;
import mtr.render.RenderRailwaySign;
import mtr.screen.DashboardListSelectorScreen;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 多行指示牌编辑器（{@link #SIGN_LINES} 行 × {@code length} 格），供 CRT 站台信息屏使用。
 * 格子与图标列表均按 MTR 原版样式用 {@link RenderRailwaySign#drawSign} 绘制图标，
 * 按钮只负责接收点击；图标按「带自定义文本 / 不带」分两类排布并分页。
 * 另提供复制 / 粘贴 / 撤回 / 清空 / JS 样式等工具按钮。
 *
 * @see mtr.screen.RailwaySignScreen
 */
public class RailwaySignDoubleScreen extends ScreenMapper implements IGui {

	protected int line;
	protected int editingIndex;
	protected int page;
	protected int totalPages;
	protected int columns;
	protected int rows;

	protected final BlockPos signPos;
	protected final boolean isRailwaySign;
	protected final int length;
	protected final String[][] signIds;
	protected final List<LongAVLTreeSet> selectedIds;
	protected final List<NameColorDataBase> exitsForList = new ArrayList<>();
	protected final List<NameColorDataBase> platformsForList = new ArrayList<>();
	protected final List<NameColorDataBase> routesForList = new ArrayList<>();
	protected final List<NameColorDataBase> stationsForList = new ArrayList<>();
	protected final List<String> allSignIds = new ArrayList<>();

	protected final ButtonWidget[][] buttonsEdit;
	protected final ButtonWidget[] buttonsSelection;
	protected final ButtonWidget buttonClear;
	protected final TexturedButtonWidget buttonPrevPage;
	protected final TexturedButtonWidget buttonNextPage;

	/** 数据变更后的回写动作（把格子数据保存到服务端）。 */
	private final Runnable onSave;

	protected static final int SIGN_LINES = 2;
	protected static final int SIGN_SIZE = 32;
	protected static final int SIGN_BUTTON_SIZE = 16;
	protected static final int BUTTON_Y_START = (SQUARE_SIZE + SIGN_SIZE) * SIGN_LINES + SIGN_BUTTON_SIZE / 2;

	private ButtonWidget undoButton;
	private ButtonWidget copyButton;
	private ButtonWidget pasteButton;
	private ButtonWidget clearAllButton;
	private ButtonWidget jsStyleButton;

	/** 图标列表遍历回调：index 为图标序号，x / y 为相对偏移，isBig 表示该图标带自定义文本（占 3 格宽）。 */
	@FunctionalInterface
	public interface LoopSignsCallback {
		void loopSignsCallback(int index, int x, int y, boolean isBig);
	}

	public RailwaySignDoubleScreen(BlockPos signPos, boolean isFront, Runnable onSave) {
		super(Text.literal(""));
		editingIndex = -1;
		this.signPos = signPos;
		this.onSave = onSave;
		final World world = MinecraftClient.getInstance().world;

		for (final BlockRailwaySign.SignType signType : BlockRailwaySign.SignType.values()) {
			allSignIds.add(signType.toString());
		}
		final List<String> sortedKeys = new ArrayList<>(CustomResources.CUSTOM_SIGNS.keySet());
		Collections.sort(sortedKeys);
		allSignIds.addAll(sortedKeys);

		try {
			final Station station = RailwayData.getStation(ClientData.STATIONS, ClientData.DATA_CACHE, signPos);
			if (station != null) {
				final Map<String, List<String>> exits = station.getGeneratedExits();
				final List<String> exitParents = new ArrayList<>(exits.keySet());
				exitParents.sort(String::compareTo);
				exitParents.forEach(exitParent -> {
					final List<String> destinations = exits.get(exitParent);
					exitsForList.add(new DataConverter(Station.serializeExit(exitParent), exitParent + " " + (!destinations.isEmpty() ? destinations.get(0) : ""), 0));
				});

				final List<Platform> platforms = new ArrayList<>(ClientData.DATA_CACHE.requestStationIdToPlatforms(station.id).values());
				Collections.sort(platforms);
				platforms.stream().map(platform -> new DataConverter(platform.id, platform.name + " " + IGui.mergeStations(ClientData.DATA_CACHE.requestPlatformIdToRoutes(platform.id).stream().map(route -> route.stationDetails.get(route.stationDetails.size() - 1).stationName).collect(Collectors.toList())), 0)).forEach(platformsForList::add);

				ClientData.DATA_CACHE.getAllRoutesIncludingConnectingStations(station).forEach((color, route) -> routesForList.add(new DataConverter(route.color, route.name, route.color)));
				ClientData.DATA_CACHE.getConnectingStationsIncludingThisOne(station).forEach(connectingStation -> stationsForList.add(new DataConverter(connectingStation.id, connectingStation.name, connectingStation.color)));
			}
		} catch (Exception e) {
			Init.LOGGER.error("Failed to load station data for railway sign screen at {}", signPos.toShortString(), e);
		}

		String[][] loadedSignIds = null;
		List<LongAVLTreeSet> loadedSelectedIds = null;
		if (world != null) {
			final BlockEntity entity = world.getBlockEntity(signPos);
			if (entity instanceof BlockCRTStationInfo1.BlockEntity infoEntity) {
				loadedSignIds = infoEntity.getSignIds();
				loadedSelectedIds = infoEntity.getSelectedIds();
			} else if (entity instanceof BlockCRTStationInfo1Double.BlockEntity infoEntity) {
				loadedSignIds = infoEntity.getSignIds(isFront);
				loadedSelectedIds = infoEntity.getSelectedIds(isFront);
			}
		}

		if (loadedSignIds != null) {
			signIds = loadedSignIds;
			selectedIds = loadedSelectedIds;
			isRailwaySign = true;
			length = signIds.length > 0 && signIds[0] != null ? signIds[0].length : BlockCRTStationInfo1.BlockEntity.SIGN_LENGTH;
		} else {
			length = BlockCRTStationInfo1.BlockEntity.SIGN_LENGTH;
			signIds = new String[SIGN_LINES][length];
			selectedIds = new ArrayList<>();
			for (int i = 0; i < SIGN_LINES; i++) {
				selectedIds.add(new LongAVLTreeSet());
			}
			isRailwaySign = false;
		}

		buttonsEdit = new ButtonWidget[SIGN_LINES][length];
		for (int i = 0; i < SIGN_LINES; i++) {
			for (int j = 0; j < buttonsEdit[i].length; j++) {
				final int index = j;
				final int finalI = i;
				buttonsEdit[i][j] = UtilitiesClient.newButton(Text.translatable("selectWorld.edit"), button -> edit(finalI, index));
			}
		}

		buttonsSelection = new ButtonWidget[allSignIds.size()];
		for (int i = 0; i < allSignIds.size(); i++) {
			final int index = i;
			buttonsSelection[i] = UtilitiesClient.newButton(SIGN_BUTTON_SIZE, Text.literal(""), button -> setNewSignId(allSignIds.get(index)));
		}

		buttonClear = UtilitiesClient.newButton(Text.translatable("gui.mtr.reset_sign"), button -> setNewSignId(null));
		buttonPrevPage = new TexturedButtonWidget(0, 0, 0, SQUARE_SIZE, 0, 0, 20, new Identifier(MTR.MOD_ID, "textures/gui/icon_left.png"), 20, 40, button -> setPage(page - 1));
		buttonNextPage = new TexturedButtonWidget(0, 0, 0, SQUARE_SIZE, 0, 0, 20, new Identifier(MTR.MOD_ID, "textures/gui/icon_right.png"), 20, 40, button -> setPage(page + 1));
	}

	@Override
	protected void init() {
		super.init();

		for (int i = 0; i < SIGN_LINES; i++) {
			for (int j = 0; j < buttonsEdit[i].length; j++) {
				IDrawing.setPositionAndWidth(buttonsEdit[i][j], (width - SIGN_SIZE * length) / 2 + j * SIGN_SIZE, i * (SIGN_SIZE + SQUARE_SIZE) + SIGN_SIZE, SIGN_SIZE);
				addDrawableChild(buttonsEdit[i][j]);
			}
		}

		columns = Math.max((width - SIGN_BUTTON_SIZE * 3) / (SIGN_BUTTON_SIZE * 8) * 2, 1);
		rows = Math.max((height - SIGN_SIZE * SIGN_LINES - SQUARE_SIZE * 4 - SQUARE_SIZE / 2) / SIGN_BUTTON_SIZE, 1);

		final int xOffsetSmall = (width - SIGN_BUTTON_SIZE * (columns * 4 + 3)) / 2 + SIGN_BUTTON_SIZE;
		final int xOffsetBig = xOffsetSmall + SIGN_BUTTON_SIZE * (columns + 1);

		totalPages = loopSigns((index, x, y, isBig) -> {
			IDrawing.setPositionAndWidth(buttonsSelection[index], (isBig ? xOffsetBig : xOffsetSmall) + x, BUTTON_Y_START + y, isBig ? SIGN_BUTTON_SIZE * 3 : SIGN_BUTTON_SIZE);
			buttonsSelection[index].visible = false;
			addDrawableChild(buttonsSelection[index]);
		}, true);

		final int buttonClearX = (width - PANEL_WIDTH - SQUARE_SIZE * 4) / 2;
		final int buttonY = height - SQUARE_SIZE * 2;

		IDrawing.setPositionAndWidth(buttonClear, buttonClearX, buttonY, PANEL_WIDTH);
		buttonClear.visible = false;
		addDrawableChild(buttonClear);

		IDrawing.setPositionAndWidth(buttonPrevPage, buttonClearX + PANEL_WIDTH, buttonY, SQUARE_SIZE);
		buttonPrevPage.visible = false;
		addDrawableChild(buttonPrevPage);
		IDrawing.setPositionAndWidth(buttonNextPage, buttonClearX + PANEL_WIDTH + SQUARE_SIZE * 3, buttonY, SQUARE_SIZE);
		buttonNextPage.visible = false;
		addDrawableChild(buttonNextPage);

		addCopyPasteButtons();

		// 底部 JS 样式选择按钮（位置与尺寸对齐 RailwaySignScreenMixin）
		jsStyleButton = ButtonWidget.builder(getJSStyleButtonMessage(), button -> openJSStyleSelector()).dimensions(width / 4, height - 20, width / 2, 20).build();
		addDrawableChild(jsStyleButton);

		applyLockState();

		if (!isRailwaySign) {
			UtilitiesClient.setScreen(MinecraftClient.getInstance(), new DashboardListSelectorScreen(this::close, platformsForList, selectedIds.get(line), true, false));
		}
	}

	/** 复制 / 粘贴 / 清空 / 撤回四个工具按钮（布局与行为对齐 {@link com.Nanbin.mixin.RailwaySignScreenMixin}）。 */
	private void addCopyPasteButtons() {
		final int y = 60;
		final int x = width - 35;

		undoButton = ButtonWidget.builder(Text.translatable("gui.nanbin.undo"), button -> {
			SignClipboard.undo(signPos, SIGN_LINES, length, signIds, selectedIds);
			button.active = SignClipboard.canUndo(signPos);
			save();
		}).dimensions(x, y + 90, 30, 20).build();
		undoButton.active = SignClipboard.canUndo(signPos);

		copyButton = ButtonWidget.builder(Text.translatable("gui.nanbin.copy"), button -> SignClipboard.copy(SIGN_LINES, length, signIds, selectedIds)).dimensions(x, y, 30, 20).build();

		pasteButton = ButtonWidget.builder(Text.translatable("gui.nanbin.paste"), button -> {
			SignClipboard.paste(signPos, SIGN_LINES, length, signIds, selectedIds);
			undoButton.active = SignClipboard.canUndo(signPos);
			save();
		}).dimensions(x, y + 30, 30, 20).build();
		pasteButton.active = SignClipboard.canPaste(SIGN_LINES, length);

		// 清空：先弹二次确认（与 RailwaySignScreenMixin 一致）
		clearAllButton = ButtonWidget.builder(Text.translatable("gui.nanbin.clear"), button -> MinecraftClient.getInstance().setScreen(new RailwaySignClearConfirmScreen(this, Text.translatable("gui.nanbin.clear.question"), () -> {
			SignClipboard.clear(signPos, SIGN_LINES, length, signIds, selectedIds);
			undoButton.active = SignClipboard.canUndo(signPos);
			save();
		}))).dimensions(x, y + 60, 30, 20).build();

		addDrawableChild(copyButton);
		addDrawableChild(pasteButton);
		addDrawableChild(clearAllButton);
		addDrawableChild(undoButton);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		try {
			renderBackground(context);
			super.render(context, mouseX, mouseY, delta);
			if (client == null) {
				return;
			}

			final MatrixStack matrices = context.getMatrices();

			for (int i = 0; i < SIGN_LINES && i < signIds.length; i++) {
				final String[] lineIds = signIds[i];
				final String lineStyleScriptId = JSSignConfig.getStyleScriptId(lineIds);
				final LongAVLTreeSet lineSelected = i < selectedIds.size() ? selectedIds.get(i) : new LongAVLTreeSet();
				for (int j = 0; j < lineIds.length; j++) {
					final String signId = lineIds[j];
					if (signId == null) {
						continue;
					}
					if (lineStyleScriptId != null) {
						// 整行 JS 样式：用脚本名代替图标
						if (j == 0) {
							context.drawCenteredTextWithShadow(textRenderer, JSSignSelectorScreen.getScriptName(lineStyleScriptId), (width - SIGN_SIZE * length) / 2 + SIGN_SIZE * length / 2, i * (SQUARE_SIZE + SIGN_SIZE) + SIGN_SIZE / 2 - 4, ARGB_WHITE);
						}
						continue;
					}
					RenderRailwaySign.drawSign(matrices, null, null, textRenderer, signPos, signId, (width - SIGN_SIZE * length) / 2F + j * SIGN_SIZE, i * (SQUARE_SIZE + SIGN_SIZE), SIGN_SIZE, RenderRailwaySign.getMaxWidth(lineIds, j, false), RenderRailwaySign.getMaxWidth(lineIds, j, true), lineSelected, Direction.UP, 0, (textureId, x, y, size, flipTexture) -> {
						UtilitiesClient.beginDrawingTexture(textureId);
						context.drawTexture(textureId, (int) x, (int) y, 0.0F, 0.0F, (int) size, (int) size, (int) (flipTexture ? -size : size), (int) size);
					});
				}
			}

			if (editingIndex >= 0) {
				final int xOffsetSmall = (width - SIGN_BUTTON_SIZE * (columns * 4 + 3)) / 2 + SIGN_BUTTON_SIZE;
				final int xOffsetBig = xOffsetSmall + SIGN_BUTTON_SIZE * (columns + 1);

				loopSigns((index, x, y, isBig) -> {
					final String signId = allSignIds.get(index);
					if (JSSignConfig.isJSStyle(signId)) {
						// JS 样式图标：用脚本名代替贴图
						context.drawCenteredTextWithShadow(textRenderer, JSSignSelectorScreen.getScriptName(signId.substring(JSSignConfig.JS_STYLE_PREFIX.length())), (isBig ? xOffsetBig : xOffsetSmall) + x + (isBig ? SIGN_BUTTON_SIZE * 3 : SIGN_BUTTON_SIZE) / 2, BUTTON_Y_START + y + 4, ARGB_WHITE);
						return;
					}
					final CustomResources.CustomSign sign = RenderRailwaySign.getSign(signId);
					if (sign != null) {
						final boolean moveRight = sign.hasCustomText() && sign.flipCustomText;
						UtilitiesClient.beginDrawingTexture(sign.textureId);
						RenderRailwaySign.drawSign(matrices, null, null, textRenderer, signPos, signId, (isBig ? xOffsetBig : xOffsetSmall) + x + (moveRight ? SIGN_BUTTON_SIZE * 2 : 0), BUTTON_Y_START + y, SIGN_BUTTON_SIZE, 6, 6, selectedIds.get(line), Direction.UP, 0, (textureId, x1, y1, size, flipTexture) -> context.drawTexture(textureId, (int) x1, (int) y1, 0.0F, 0.0F, (int) size, (int) size, (int) (flipTexture ? -size : size), (int) size));
					}
				}, false);

				final String pageText = String.format("%s/%s", page + 1, totalPages);
				context.drawText(textRenderer, pageText, (width - PANEL_WIDTH - SQUARE_SIZE * 4) / 2 + PANEL_WIDTH + SQUARE_SIZE * 2 - textRenderer.getWidth(pageText) / 2, height - SQUARE_SIZE * 2 + TEXT_PADDING, ARGB_WHITE, false);
			}
		} catch (Exception e) {
			Init.LOGGER.error("Failed to render RailwaySignDoubleScreen", e);
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
		setPage(page + (int) Math.signum(-amount));
		return super.mouseScrolled(mouseX, mouseY, amount);
	}

	@Override
	public void close() {
		save();
		super.close();
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	@Override
	public void resize(MinecraftClient client, int width, int height) {
		super.resize(client, width, height);
		for (int i = 0; i < SIGN_LINES; i++) {
			for (final ButtonWidget button : buttonsEdit[i]) {
				button.active = true;
			}
		}
		for (final ButtonWidget button : buttonsSelection) {
			button.visible = false;
		}
		editingIndex = -1;
	}

	/**
	 * 遍历全部可选图标：按「带自定义文本 / 不带」分成两类分别排布（带文本的占 3 格宽），
	 * 返回两类的最大页数。
	 *
	 * @param ignorePage true = 忽略当前页码（用于初始化时给所有按钮定位）
	 */
	protected int loopSigns(LoopSignsCallback loopSignsCallback, boolean ignorePage) {
		final int pageCount = rows * columns;
		int indexSmall = 0;
		int indexBig = 0;
		int columnSmall = 0;
		int columnBig = 0;
		int rowSmall = 0;
		int rowBig = 0;
		int totalPagesSmallCount = 1;
		int totalPagesBigCount = 1;
		for (int i = 0; i < allSignIds.size(); i++) {
			final CustomResources.CustomSign sign = RenderRailwaySign.getSign(allSignIds.get(i));
			final boolean isBig = sign != null && sign.hasCustomText();

			final boolean onPage = (isBig ? indexBig : indexSmall) / pageCount == page;
			buttonsSelection[i].visible = onPage;
			if (ignorePage || onPage) {
				loopSignsCallback.loopSignsCallback(i, (isBig ? columnBig * 3 : columnSmall) * SIGN_BUTTON_SIZE, (isBig ? rowBig : rowSmall) * SIGN_BUTTON_SIZE, isBig);
			}

			if (isBig) {
				columnBig++;
				if (totalPagesBigCount < 0) {
					totalPagesBigCount = -totalPagesBigCount + 1;
				}
				if (columnBig >= columns) {
					columnBig = 0;
					rowBig++;
					if (rowBig >= rows) {
						rowBig = 0;
						totalPagesBigCount = -totalPagesBigCount;
					}
				}
				indexBig++;
			} else {
				columnSmall++;
				if (totalPagesSmallCount < 0) {
					totalPagesSmallCount = -totalPagesSmallCount + 1;
				}
				if (columnSmall >= columns) {
					columnSmall = 0;
					rowSmall++;
					if (rowSmall >= rows) {
						rowSmall = 0;
						totalPagesSmallCount = -totalPagesSmallCount;
					}
				}
				indexSmall++;
			}
		}
		return Math.max(Math.abs(totalPagesBigCount), Math.abs(totalPagesSmallCount));
	}

	protected void edit(int line, int editingIndex) {
		// 先记录被点击的行与格子：即使是 JS 样式行也要记录行号，
		// 否则返回后底部 JS 样式按钮仍指向旧行，无法清除该行的 JS 样式。
		this.line = line;
		this.editingIndex = editingIndex;
		applyLockState();

		if (isCurrentLineJSStyle()) {
			// JS 样式行：不进入图标编辑（布局由脚本决定），改为打开 JS 数据选择屏幕
			MinecraftClient.getInstance().setScreen(new JSSignDataScreen(this, signPos, selectedIds.get(line), this::save, JSSignConfig.getStyleScriptId(signIds[line])));
			return;
		}

		setPage(page);
		if (editingIndex >= 0) {
			buttonsEdit[line][editingIndex].active = false;
		}
	}

	protected void setNewSignId(@Nullable String newSignId) {
		if (editingIndex < 0 || editingIndex >= length || line < 0 || line >= SIGN_LINES) {
			return;
		}
		signIds[line][editingIndex] = newSignId;

		// 自定义文本指示牌：直接打开文本输入屏幕
		if ("nanbin_custom_text".equals(newSignId) || "nanbin_custom_text_flipped".equals(newSignId)) {
			MinecraftClient.getInstance().setScreen(new CustomTextScreen(this, selectedIds.get(line), this::save));
			return;
		}

		final boolean isExitLetter = signIsExit(newSignId);
		final boolean isLine = signIsLine(newSignId);
		final boolean isPlatform = signIsPlatform(newSignId);
		final boolean isStation = signIsStation(newSignId);
		if (isExitLetter || isPlatform || isLine || isStation) {
			// 需要选择出口 / 站台 / 线路 / 车站的图标：打开选择器，选完立即回到本屏幕并保存，
			// 这样信息屏会马上刷新（不必等关闭编辑器）。
			final List<NameColorDataBase> list = isExitLetter ? exitsForList : isPlatform ? platformsForList : isLine ? routesForList : stationsForList;
			MinecraftClient.getInstance().setScreen(new DashboardListSelectorScreen(() -> {
				MinecraftClient.getInstance().setScreen(this);
				save();
			}, list, selectedIds.get(line), false, false));
		} else {
			save();
		}
	}

	protected void setPage(int newPage) {
		page = MathHelper.clamp(newPage, 0, totalPages - 1);
		buttonPrevPage.visible = editingIndex >= 0 && page > 0;
		buttonNextPage.visible = editingIndex >= 0 && page < totalPages - 1;
		refreshSignButtons();
	}

	/**
	 * 按当前页码重新排布图标按钮并刷新可见性。
	 * {@link #loopSigns} 会按页码设置 visible，这里只负责在「未选中格子」或
	 * 「该行由脚本渲染」时统一隐藏，避免覆盖分页结果。
	 */
	private void refreshSignButtons() {
		final int xOffsetSmall = (width - SIGN_BUTTON_SIZE * (columns * 4 + 3)) / 2 + SIGN_BUTTON_SIZE;
		final int xOffsetBig = xOffsetSmall + SIGN_BUTTON_SIZE * (columns + 1);
		loopSigns((index, x, y, isBig) -> IDrawing.setPositionAndWidth(buttonsSelection[index], (isBig ? xOffsetBig : xOffsetSmall) + x, BUTTON_Y_START + y, isBig ? SIGN_BUTTON_SIZE * 3 : SIGN_BUTTON_SIZE), false);
		if (editingIndex < 0 || isCurrentLineJSStyle()) {
			for (final ButtonWidget button : buttonsSelection) {
				button.visible = false;
			}
		}
	}

	/** 当前编辑行是否启用了 JS 样式（只有该行需要锁定图标编辑）。 */
	protected boolean isCurrentLineJSStyle() {
		return line >= 0 && line < signIds.length && JSSignConfig.hasJSStyle(signIds[line]);
	}

	/** 任意一行启用 JS 样式。 */
	protected boolean hasJSStyle() {
		for (final String[] lineIds : signIds) {
			if (JSSignConfig.hasJSStyle(lineIds)) {
				return true;
			}
		}
		return false;
	}

	private net.minecraft.text.Text getJSStyleButtonMessage() {
		if (line >= 0 && line < signIds.length) {
			final String styleScriptId = JSSignConfig.getStyleScriptId(signIds[line]);
			if (styleScriptId != null) {
				return JSSignSelectorScreen.getScriptName(styleScriptId);
			}
		}
		return Text.translatable("gui.nanbin.js_style.select");
	}

	/** 打开 JS 样式选择器：选中后整行交给脚本渲染；选择「无样式」时清除该行样式标记并恢复可编辑。 */
	private void openJSStyleSelector() {
		if (line < 0 || line >= signIds.length) {
			return;
		}
		MinecraftClient.getInstance().setScreen(new JSSignSelectorScreen(this, JSSignConfig.getStyleScriptId(signIds[line]), scriptId -> {
			if (scriptId == null) {
				// 清除该行 JS 样式：恢复为普通格子编辑
				for (int i = 0; i < length; i++) {
					signIds[line][i] = null;
				}
			} else {
				signIds[line][0] = JSSignConfig.JS_STYLE_PREFIX + scriptId;
				// 切换样式后清空该行其余格子与所选数据
				for (int i = 1; i < length; i++) {
					signIds[line][i] = null;
				}
				selectedIds.get(line).clear();
			}
			save();
			applyLockState();
			if (!isCurrentLineJSStyle()) {
				setPage(page);
			}
		}));
	}

	/**
	 * 刷新按钮状态。锁定按「当前编辑行」判定：某一行启用 JS 样式只影响该行，
	 * 其余行仍可正常编辑（与单行 mixin 的整屏锁定不同，双层指示牌必须按行处理）。
	 */
	private void applyLockState() {
		final boolean locked = isCurrentLineJSStyle();
		if (locked) {
			editingIndex = -1;
		}
		final boolean hasSelection = editingIndex >= 0;
		// JS 样式行下仍保持格子按钮可用，以便 edit() 拦截并打开 JS 数据选择屏幕
		for (int i = 0; i < SIGN_LINES; i++) {
			for (final ButtonWidget button : buttonsEdit[i]) {
				button.active = true;
			}
		}
		if (locked || !hasSelection) {
			for (final ButtonWidget button : buttonsSelection) {
				button.visible = false;
			}
		}
		buttonClear.visible = hasSelection && !locked;
		buttonClear.active = !locked;
		if (undoButton != null) {
			undoButton.active = !locked && SignClipboard.canUndo(signPos);
		}
		if (copyButton != null) {
			copyButton.active = !locked;
		}
		if (pasteButton != null) {
			pasteButton.active = !locked && SignClipboard.canPaste(SIGN_LINES, length);
		}
		if (clearAllButton != null) {
			clearAllButton.active = !locked;
		}
		if (jsStyleButton != null) {
			jsStyleButton.setMessage(getJSStyleButtonMessage());
		}
	}

	private void save() {
		if (onSave != null) {
			onSave.run();
		}
	}

	private static boolean signIsExit(@Nullable String signId) {
		return "exit_letter".equals(signId) || "exit_letter_flipped".equals(signId);
	}

	private static boolean signIsPlatform(@Nullable String signId) {
		return "platform".equals(signId) || "platform_flipped".equals(signId);
	}

	private static boolean signIsLine(@Nullable String signId) {
		return "line".equals(signId) || "line_flipped".equals(signId)
				|| "crt_route_name".equals(signId) || "crt_route_name_flipped".equals(signId) || "crt_route_number".equals(signId);
	}

	private static boolean signIsStation(@Nullable String signId) {
		return "station".equals(signId) || "station_flipped".equals(signId) || "crt_station_name".equals(signId);
	}
}
