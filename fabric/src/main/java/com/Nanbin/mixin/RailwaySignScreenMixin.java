package com.Nanbin.mixin;

import com.Nanbin.client.ClientData.CRTSignRegistry;
import com.Nanbin.client.ClientData.SignClipboard;
import com.Nanbin.client.JavaScriptSupport.JSSignConfig;
import com.Nanbin.client.Screen.CustomTextScreen;
import com.Nanbin.client.Screen.JSSignDataScreen;
import com.Nanbin.client.Screen.JSSignSelectorScreen;
import com.Nanbin.client.Screen.RailwaySignClearConfirmScreen;
import com.Nanbin.uiHelper.NanbinHorizontalScrollBar;
import org.mtr.core.data.Station;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArraySet;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.ButtonWidgetExtension;
import org.mtr.mapping.mapper.ScreenExtension;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mod.InitClient;
import org.mtr.mod.client.CustomResourceLoader;
import org.mtr.mod.client.IDrawing;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.packet.PacketUpdateRailwaySignConfig;
import org.mtr.mod.screen.DashboardListItem;
import org.mtr.mod.screen.DashboardListSelectorScreen;
import org.mtr.mod.screen.RailwaySignScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 扩展 MTR 的 {@link RailwaySignScreen}：加入复制／粘贴／清空／撤销按钮，并支持用 JS 脚本整屏渲染指示牌内容。
 */
@Mixin(value = RailwaySignScreen.class, remap = false)
public abstract class RailwaySignScreenMixin {

	@Shadow
	private int editingIndex;

	@Shadow
	@Final
	private String[] signIds;

	@Shadow
	@Final
	private LongAVLTreeSet selectedIds;

	@Shadow
	@Final
	private ObjectArraySet<DashboardListItem> routesForList;

	@Shadow
	@Final
	private int length;

	@Shadow
	@Final
	private BlockPos signPos;

	@Shadow
	@Final
	private ButtonWidgetExtension[] buttonsEdit;

	@Shadow
	@Final
	private ButtonWidgetExtension[] buttonsSelection;

	@Shadow
	@Final
	private ButtonWidgetExtension buttonClear;

	@Unique
	private ObjectArraySet<DashboardListItem> nanbin$stationsForList;

	@Unique
	private ButtonWidgetExtension nanbin$undoButton;
	@Unique
	private ButtonWidgetExtension nanbin$copyButton;
	@Unique
	private ButtonWidgetExtension nanbin$pasteButton;
	@Unique
	private ButtonWidgetExtension nanbin$clearButton;
	@Unique
	private ButtonWidgetExtension nanbin$jsStyleButton;

	@Unique
	private NanbinHorizontalScrollBar nanbin$editScrollBar;

	@Inject(method = "<init>", at = @At("RETURN"))
	private void nanbin$initStationsForList(CallbackInfo ci) {
		final Station station = InitClient.findStation(signPos);
		if (station == null) {
			nanbin$stationsForList = new ObjectArraySet<>();
		} else {
			final org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArraySet<Station> connectingStationsIncludingThisOne = new org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArraySet<>(station.connectedStations);
			connectingStationsIncludingThisOne.add(station);
			nanbin$stationsForList = MinecraftClientData.convertDataSet(connectingStationsIncludingThisOne);
		}
	}

	/**
	 * 只在 CRT 类导视上追加 crt_signs.json 里的标志；原版 MTR 导视牌保持原版可选列表不变。
	 *
	 * <p>必须在 MTR 构造函数读取 {@code CustomResourceLoader.getSortedSignIds()} 的那一刻替换返回值：
	 * MTR 紧接着会用 {@code allSignIds.size()} 分配 {@code buttonsSelection} 数组，
	 * 在构造函数返回后再追加会让数组变短，{@code init2()} 一打开界面就数组越界。</p>
	 */
	@Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/mtr/mod/client/CustomResourceLoader;getSortedSignIds()Lorg/mtr/libraries/it/unimi/dsi/fastutil/objects/ObjectArrayList;"))
	private ObjectArrayList<String> nanbin$appendCrtSigns() {
		final ObjectArrayList<String> signIds = CustomResourceLoader.getSortedSignIds();
		if (CRTSignRegistry.isCrtSignPosition(signPos)) {
			CRTSignRegistry.appendSignsFor(signIds, CRTSignRegistry.SURFACE_CRT);
		}
		return signIds;
	}

	@Inject(method = "setNewSignId", at = @At("HEAD"), cancellable = true)
	private void nanbin$openRouteNumberSelector(String signId, CallbackInfo ci) {
		// JS 样式已启用时整屏锁定，禁止修改格子内容
		if (nanbin$hasJSStyle()) {
			ci.cancel();
			return;
		}
		// 自定义文本指示牌：直接打开文本输入屏幕（兼容历史遗留的 nanbin_custom_text1 等 id）
		if (signId != null && signId.startsWith("nanbin_custom_text")) {
			if (editingIndex < 0 || editingIndex >= signIds.length) {
				return;
			}
			signIds[editingIndex] = signId;
			MinecraftClient.getInstance().openScreen(new Screen(new CustomTextScreen((ScreenExtension) (Object) this, selectedIds, () -> InitClient.REGISTRY_CLIENT.sendPacketToServer(new PacketUpdateRailwaySignConfig(signPos, selectedIds, signIds)))));
			ci.cancel();
			return;
		}
		final boolean isRouteName = "crt_route_name".equals(signId)
				|| "crt_route_name_flipped".equals(signId)
				|| "crt_route_number".equals(signId);
		final boolean isStationName = "crt_station_name".equals(signId);
		if (!isRouteName && !isStationName) {
			return;
		}
		if (editingIndex < 0 || editingIndex >= signIds.length) {
			return;
		}
		signIds[editingIndex] = signId;
		if (isStationName) {
			MinecraftClient.getInstance().openScreen(new Screen(new DashboardListSelectorScreen(new ObjectImmutableList<>(nanbin$stationsForList), selectedIds, false, false, (ScreenExtension) (Object) this)));
		} else {
			MinecraftClient.getInstance().openScreen(new Screen(new DashboardListSelectorScreen(new ObjectImmutableList<>(routesForList), selectedIds, false, false, (ScreenExtension) (Object) this)));
		}
		ci.cancel();
	}

	/**
	 * JS 样式下点击编辑按钮时：不进入格子编辑（布局保持锁定），
	 * 而是打开数据选择屏幕，供脚本读取站台/线路/出口数据。
	 */
	@Inject(method = "edit", at = @At("HEAD"), cancellable = true)
	private void nanbin$lockEdit(int index, CallbackInfo ci) {
		if (nanbin$hasJSStyle()) {
			editingIndex = -1;
			MinecraftClient.getInstance().openScreen(new Screen(new JSSignDataScreen((ScreenExtension) (Object) this, signPos, selectedIds, () -> InitClient.REGISTRY_CLIENT.sendPacketToServer(new PacketUpdateRailwaySignConfig(signPos, selectedIds, signIds)), nanbin$getStyleScriptId())));
			ci.cancel();
		}
	}

	// 复制／粘贴／清空／撤销按钮
	@Inject(method = "init2", at = @At("TAIL"))
	private void nanbin$addCopyPasteButtons(CallbackInfo ci) {
		final ScreenExtension screen = (ScreenExtension) (Object) this;
		final int y = 60;
		final int x = screen.width - 35;

		nanbin$undoButton = new ButtonWidgetExtension(x, y + 90, 30, 20, TextHelper.translatable("gui.nanbin.undo"), button -> {
			SignClipboard.undo(signPos, length, signIds, selectedIds);
			button.setActiveMapped(SignClipboard.canUndo(signPos));
		});
		nanbin$undoButton.active = SignClipboard.canUndo(signPos);

		nanbin$copyButton = new ButtonWidgetExtension(x, y, 30, 20, TextHelper.translatable("gui.nanbin.copy"), button -> SignClipboard.copy(length, signIds, selectedIds));

		nanbin$pasteButton = new ButtonWidgetExtension(x, y + 30, 30, 20, TextHelper.translatable("gui.nanbin.paste"), button -> {
			SignClipboard.paste(signPos, length, signIds, selectedIds);
			nanbin$undoButton.setActiveMapped(SignClipboard.canUndo(signPos));
		});
		nanbin$pasteButton.active = SignClipboard.canPaste(length);

		nanbin$clearButton = new ButtonWidgetExtension(x, y + 60, 30, 20, TextHelper.translatable("gui.nanbin.clear"), button -> MinecraftClient.getInstance().openScreen(new Screen(new RailwaySignClearConfirmScreen((ScreenExtension) (Object) this, TextHelper.translatable("gui.nanbin.clear.question"), () -> {
			SignClipboard.clear(signPos, length, signIds, selectedIds);
			nanbin$undoButton.setActiveMapped(SignClipboard.canUndo(signPos));
		}))));

		screen.addChild(new ClickableWidget(nanbin$copyButton));
		screen.addChild(new ClickableWidget(nanbin$pasteButton));
		screen.addChild(new ClickableWidget(nanbin$clearButton));
		screen.addChild(new ClickableWidget(nanbin$undoButton));

		// 屏幕底部通栏的 JS 样式选择按钮
		nanbin$jsStyleButton = new ButtonWidgetExtension(0, screen.height - 20, screen.width, 20, nanbin$getJSStyleButtonMessage(), button -> nanbin$openJSStyleSelector());
		screen.addChild(new ClickableWidget(nanbin$jsStyleButton));

		nanbin$applyLockState();
	}

	// 格子过多时，在最上方编辑按钮下方显示横向滚动条
	@Inject(method = "init2", at = @At("TAIL"))
	private void nanbin$initEditScrollBar(CallbackInfo ci) {
		final ScreenExtension screen = (ScreenExtension) (Object) this;
		final double previousScroll = nanbin$editScrollBar == null ? 0 : nanbin$editScrollBar.getScroll();
		nanbin$editScrollBar = new NanbinHorizontalScrollBar(8, 54, Math.max(0, screen.width - 16), 6, () -> nanbin$layoutEditRow());
		nanbin$editScrollBar.setContentWidth(buttonsEdit.length * 32);
		nanbin$editScrollBar.setScroll(previousScroll);
		nanbin$editScrollBar.setVisibleMapped(nanbin$editScrollBar.isScrollable());
		screen.addChild(new ClickableWidget(nanbin$editScrollBar));
		nanbin$layoutEditRow();
	}

	@Unique
	private void nanbin$layoutEditRow() {
		final int startX = nanbin$editRowStartX();
		for (int i = 0; i < buttonsEdit.length; i++) {
			IDrawing.setPositionAndWidth(buttonsEdit[i], startX + i * 32, 32, 32);
		}
	}

	// 放得下就居中，放不下就从左端（x = 8）开始并随滚动条左移，滚动上限正好等于 内容宽 - 可视宽
	@Unique
	private int nanbin$editRowStartX() {
		final ScreenExtension screen = (ScreenExtension) (Object) this;
		final int contentWidth = buttonsEdit.length * 32;
		return contentWidth <= screen.width - 16 || nanbin$editScrollBar == null ? (screen.width - contentWidth) / 2 : 8 - (int) Math.round(nanbin$editScrollBar.getScroll());
	}

	// 顶部格子预览跟随编辑按钮一起横向滚动
	@ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lorg/mtr/mod/render/RenderRailwaySign;drawSign(Lorg/mtr/mapping/mapper/GraphicsHolder;Lorg/mtr/mod/render/StoredMatrixTransformations;Lorg/mtr/mapping/holder/BlockPos;Ljava/lang/String;FFFFFLorg/mtr/libraries/it/unimi/dsi/fastutil/longs/LongAVLTreeSet;Lorg/mtr/mapping/holder/Direction;ILorg/mtr/mod/render/RenderRailwaySign$DrawTexture;)V"), index = 4)
	private float nanbin$scrollSignPreview(float x) {
		final ScreenExtension screen = (ScreenExtension) (Object) this;
		return x - (screen.width - buttonsEdit.length * 32) / 2 + nanbin$editRowStartX();
	}

	// 鼠标在顶部格子区域时滚轮改为横向滚动
	@Inject(method = "mouseScrolled2", at = @At("HEAD"), cancellable = true)
	private void nanbin$scrollEditRow(double mouseX, double mouseY, double amount, CallbackInfoReturnable<Boolean> cir) {
		final ScreenExtension screen = (ScreenExtension) (Object) this;
		if (nanbin$editScrollBar != null && nanbin$editScrollBar.isScrollable() && mouseX >= 0 && mouseX < screen.width && mouseY >= 0 && mouseY < 60) {
			nanbin$editScrollBar.setScroll(nanbin$editScrollBar.getScroll() - amount * 16);
			cir.setReturnValue(true);
		}
	}

	/** 当前是否已启用全局 JS 样式（第 0 格为样式标记）。 */
	@Unique
	private boolean nanbin$hasJSStyle() {
		return JSSignConfig.hasJSStyle(signIds);
	}

	/** 当前启用的样式脚本 id；未启用时返回 null。 */
	@Unique
	private String nanbin$getStyleScriptId() {
		return JSSignConfig.getStyleScriptId(signIds);
	}

	@Unique
	private org.mtr.mapping.holder.MutableText nanbin$getJSStyleButtonMessage() {
		final String styleScriptId = nanbin$getStyleScriptId();
		if (styleScriptId != null) {
			return JSSignSelectorScreen.getScriptName(styleScriptId);
		}
		return TextHelper.translatable("gui.nanbin.js_style.select");
	}

	/** Open JS style selector; clear all sign content when a style is selected. */
	@Unique
	private void nanbin$openJSStyleSelector() {
		MinecraftClient.getInstance().openScreen(new Screen(new JSSignSelectorScreen((ScreenExtension) (Object) this, nanbin$getStyleScriptId(), scriptId -> {
			if (scriptId == null) {
				signIds[0] = null;
			} else {
				signIds[0] = JSSignConfig.JS_STYLE_PREFIX + scriptId;
			}
			// Clear all cells and selected data when switching JS style
			for (int i = 1; i < signIds.length; i++) {
				signIds[i] = null;
			}
			selectedIds.clear();
			InitClient.REGISTRY_CLIENT.sendPacketToServer(new PacketUpdateRailwaySignConfig(signPos, selectedIds, signIds));
			// 切换样式后立即刷新锁定状态与按钮文字，否则要等下次 init 才生效
			nanbin$applyLockState();
		})));
	}

	@Unique
	private void nanbin$applyLockState() {
		final boolean locked = nanbin$hasJSStyle();
		final boolean hasSelection = editingIndex >= 0;
		if (locked) {
			editingIndex = -1;
		}
		// Keep edit buttons active when JS style is locked, so nanbin$lockEdit can intercept and open JSSignDataScreen
		for (final ButtonWidgetExtension button : buttonsEdit) {
			button.active = true;
		}
		for (final ButtonWidgetExtension button : buttonsSelection) {
			button.visible = hasSelection && !locked;
		}
		buttonClear.active = !locked;
		if (nanbin$undoButton != null) {
			nanbin$undoButton.active = !locked && SignClipboard.canUndo(signPos);
		}
		if (nanbin$copyButton != null) {
			nanbin$copyButton.active = !locked;
		}
		if (nanbin$pasteButton != null) {
			nanbin$pasteButton.active = !locked && SignClipboard.canPaste(length);
		}
		if (nanbin$clearButton != null) {
			nanbin$clearButton.active = !locked;
		}
		if (nanbin$jsStyleButton != null) {
			nanbin$jsStyleButton.setMessage2(Text.cast(nanbin$getJSStyleButtonMessage()));
		}
	}
}