package com.Nanbin.mixin;

import com.Nanbin.client.ClientData.SignClipboard;
import com.Nanbin.client.JavaScriptSupport.JSSignConfig;
import com.Nanbin.client.Screen.CustomTextScreen;
import com.Nanbin.client.Screen.JSSignDataScreen;
import com.Nanbin.client.Screen.JSSignSelectorScreen;
import com.Nanbin.client.Screen.RailwaySignClearConfirmScreen;
import it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import mtr.client.ClientData;
import mtr.data.NameColorDataBase;
import mtr.data.Station;
import mtr.mappings.Text;
import mtr.packet.PacketTrainDataGuiClient;
import mtr.screen.DashboardListSelectorScreen;
import mtr.screen.RailwaySignScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 为原版 MTR 告示牌编辑器加入撤销/复制/粘贴/清空工具按钮，以及 JS 样式选择按钮。
 * 通过 mixin 注入 {@link RailwaySignScreen}，不影响原版功能。
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
	private Set<Long> selectedIds;

	@Shadow
	@Final
	private List<NameColorDataBase> routesForList;

	@Shadow
	@Final
	private int length;

	@Shadow
	@Final
	private BlockPos signPos;

	@Shadow
	@Final
	private ButtonWidget[] buttonsEdit;

	@Shadow
	@Final
	private ButtonWidget[] buttonsSelection;

	@Shadow
	@Final
	private ButtonWidget buttonClear;

	@Unique
	private List<NameColorDataBase> nanbin$stationsForList;

	@Unique
	private ButtonWidget nanbin$undoButton;
	@Unique
	private ButtonWidget nanbin$copyButton;
	@Unique
	private ButtonWidget nanbin$pasteButton;
	@Unique
	private ButtonWidget nanbin$clearButton;
	@Unique
	private ButtonWidget nanbin$jsStyleButton;

	@Inject(method = "<init>", at = @At("RETURN"))
	private void nanbin$initStationsForList(CallbackInfo ci) {
		final Station station = findStation(signPos);
		if (station == null) {
			nanbin$stationsForList = new ArrayList<>();
		} else {
			final Set<Station> connectingStationsIncludingThisOne = new LinkedHashSet<>(ClientData.DATA_CACHE.getConnectingStationsIncludingThisOne(station));
			connectingStationsIncludingThisOne.add(station);
			nanbin$stationsForList = new ArrayList<>(connectingStationsIncludingThisOne);
		}
	}

	@Inject(method = "setNewSignId", at = @At("HEAD"), cancellable = true)
	private void nanbin$openRouteNumberSelector(String signId, CallbackInfo ci) {
		// JS 样式已启用时整屏锁定，禁止修改格子内容
		if (nanbin$hasJSStyle()) {
			ci.cancel();
			return;
		}
		// 自定义文本指示牌：直接打开文本输入屏幕
		if ("nanbin_custom_text".equals(signId) || "nanbin_custom_text_flipped".equals(signId)) {
			if (editingIndex < 0 || editingIndex >= signIds.length) {
				return;
			}
			signIds[editingIndex] = signId;
			final RailwaySignScreen self = (RailwaySignScreen) (Object) this;
			MinecraftClient.getInstance().setScreen(new CustomTextScreen(self, selectedIds, this::sendUpdate));
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
			MinecraftClient.getInstance().setScreen(new DashboardListSelectorScreen(() -> MinecraftClient.getInstance().setScreen((Screen) (Object) this), new ArrayList<>(nanbin$stationsForList), selectedIds, false, false));
		} else {
			MinecraftClient.getInstance().setScreen(new DashboardListSelectorScreen(() -> MinecraftClient.getInstance().setScreen((Screen) (Object) this), new ArrayList<>(routesForList), selectedIds, false, false));
		}
		ci.cancel();
	}

	/**
	 * JS 样式下点击编辑按钮时：不进入格子编辑（布局保持锁定），
	 * 而是打开数据选择屏幕，供脚本读取站台/线路/出口数据。
	 */
	//有混淆
	@Inject(method = "edit", at = @At("HEAD"), cancellable = true)
	private void nanbin$lockEdit(int index, CallbackInfo ci) {
		if (nanbin$hasJSStyle()) {
			editingIndex = -1;
			final RailwaySignScreen self = (RailwaySignScreen) (Object) this;
			MinecraftClient.getInstance().setScreen(new JSSignDataScreen(self, signPos, selectedIds, this::sendUpdate, nanbin$getStyleScriptId()));
			ci.cancel();
		}
	}

	// 神秘小按钮
	// 反混淆：开发环境（Loom/Yarn）为 init，Fabric 发布为 method_25426，Forge 发布为 m_7856_。
	@Inject(method = "init", at = @At("TAIL"), require = 0)
	private void nanbin$addCopyPasteButtons(CallbackInfo ci) {
		nanbin$addToolbarButtons();
	}

	@Inject(method = "method_25426", at = @At("TAIL"), require = 0)
	private void nanbin$addCopyPasteButtonsIntermediary(CallbackInfo ci) {
		nanbin$addToolbarButtons();
	}

	@Inject(method = "m_7856_", at = @At("TAIL"), require = 0)
	private void nanbin$addCopyPasteButtonsSrg(CallbackInfo ci) {
		nanbin$addToolbarButtons();
	}

	@Unique
	private void nanbin$addToolbarButtons() {
		final RailwaySignScreen self = (RailwaySignScreen) (Object) this;
		final int y = 60;
		final int x = self.width - 35;

		nanbin$undoButton = new ButtonWidget(x, y + 90, 30, 20, Text.translatable("gui.nanbin.undo"), button -> {
			final LongAVLTreeSet working = new LongAVLTreeSet(selectedIds);
			SignClipboard.undo(signPos, length, signIds, working);
			writeBackSelectedIds(working);
			button.active = SignClipboard.canUndo(signPos);
		});
		nanbin$undoButton.active = SignClipboard.canUndo(signPos);

		nanbin$copyButton = new ButtonWidget(x, y, 30, 20, Text.translatable("gui.nanbin.copy"), button -> SignClipboard.copy(length, signIds, new LongAVLTreeSet(selectedIds)));

		nanbin$pasteButton = new ButtonWidget(x, y + 30, 30, 20, Text.translatable("gui.nanbin.paste"), button -> {
			final LongAVLTreeSet working = new LongAVLTreeSet(selectedIds);
			SignClipboard.paste(signPos, length, signIds, working);
			writeBackSelectedIds(working);
			nanbin$undoButton.active = SignClipboard.canUndo(signPos);
		});
		nanbin$pasteButton.active = SignClipboard.canPaste(length);

		nanbin$clearButton = new ButtonWidget(x, y + 60, 30, 20, Text.translatable("gui.nanbin.clear"), button -> MinecraftClient.getInstance().setScreen(new RailwaySignClearConfirmScreen(self, Text.translatable("gui.nanbin.clear.question"), () -> {
			final LongAVLTreeSet working = new LongAVLTreeSet(selectedIds);
			SignClipboard.clear(signPos, length, signIds, working);
			writeBackSelectedIds(working);
			nanbin$undoButton.active = SignClipboard.canUndo(signPos);
		})));

		self.addDrawableChild(nanbin$copyButton);
		self.addDrawableChild(nanbin$pasteButton);
		self.addDrawableChild(nanbin$clearButton);
		self.addDrawableChild(nanbin$undoButton);

		// 屏幕底部 JS 样式选择按钮，完全覆盖版本
		//1.19等按钮存在大小错位，所以必须缩小按钮
		nanbin$jsStyleButton = new ButtonWidget(self.width / 4, self.height - 20, self.width / 2, 20, nanbin$getJSStyleButtonMessage(), button -> nanbin$openJSStyleSelector());
		self.addDrawableChild(nanbin$jsStyleButton);

		nanbin$applyLockState();
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
	private net.minecraft.text.Text nanbin$getJSStyleButtonMessage() {
		final String styleScriptId = nanbin$getStyleScriptId();
		if (styleScriptId != null) {
			return JSSignSelectorScreen.getScriptName(styleScriptId);
		}
		return Text.translatable("gui.nanbin.js_style.select");
	}

	/** Open JS style selector; clear all sign content when a style is selected. */
	@Unique
	private void nanbin$openJSStyleSelector() {
		final RailwaySignScreen self = (RailwaySignScreen) (Object) this;
		MinecraftClient.getInstance().setScreen(new JSSignSelectorScreen(self, nanbin$getStyleScriptId(), scriptId -> {
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
			sendUpdate();
		}));
	}

	/** 通过 MTR 包把告示牌格子数据保存到服务端。 */
	@Unique
	private void sendUpdate() {
		PacketTrainDataGuiClient.sendSignIdsC2S(signPos, selectedIds, signIds);
	}

	/** Update edit control states based on JS style status. */
	@Unique
	private void nanbin$applyLockState() {
		final boolean locked = nanbin$hasJSStyle();
		final boolean hasSelection = editingIndex >= 0;
		if (locked) {
			editingIndex = -1;
		}
		// Keep edit buttons active when JS style is locked, so nanbin$lockEdit can intercept and open JSSignDataScreen
		for (final ButtonWidget button : buttonsEdit) {
			button.active = true;
		}
		for (final ButtonWidget button : buttonsSelection) {
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
			nanbin$jsStyleButton.setMessage(nanbin$getJSStyleButtonMessage());
		}
	}

	/** 把工作副本的选中数据写回原版 selectedIds（HashSet），保证编辑器状态同步。 */
	@Unique
	private void writeBackSelectedIds(LongAVLTreeSet working) {
		selectedIds.clear();
		selectedIds.addAll(working);
	}

	/** 按方块坐标就近查找所属车站。 */
	@Unique
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
}
