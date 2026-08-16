package com.Nanbin.mixin;

import com.Nanbin.client.ClientData.SignClipboard;
import com.Nanbin.client.Screen.RailwaySignClearConfirmScreen;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArraySet;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.ClickableWidget;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.Screen;
import org.mtr.mapping.mapper.ButtonWidgetExtension;
import org.mtr.mapping.mapper.ScreenExtension;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mod.screen.DashboardListItem;
import org.mtr.mod.screen.DashboardListSelectorScreen;
import org.mtr.mod.screen.RailwaySignScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 加入特点的贴图
 * 加入直观方便的工具
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

	@Inject(method = "setNewSignId", at = @At("HEAD"), cancellable = true)
	private void nanbin$openRouteNumberSelector(String signId, CallbackInfo ci) {
		final boolean isRouteName = "crt_route_name".equals(signId)
				|| "crt_route_name_flipped".equals(signId)
				|| "crt_route_number".equals(signId);
		if (!isRouteName) {
			return;
		}
		if (editingIndex < 0 || editingIndex >= signIds.length) {
			return;
		}
		signIds[editingIndex] = signId;
		MinecraftClient.getInstance().openScreen(new Screen(new DashboardListSelectorScreen(new ObjectImmutableList<>(routesForList), selectedIds, false, false, (ScreenExtension) (Object) this)));
		ci.cancel();
	}

	//神秘小按钮
	@Inject(method = "init2", at = @At("TAIL"))
	private void nanbin$addCopyPasteButtons(CallbackInfo ci) {
		final ScreenExtension screen = (ScreenExtension) (Object) this;
		final int y = 60;
		final int x = screen.width - 35;

		final ButtonWidgetExtension undoButton = new ButtonWidgetExtension(x, y + 90, 30, 20, TextHelper.translatable("gui.nanbin.undo"), button -> {
			SignClipboard.undo(signPos, length, signIds, selectedIds);
			button.setActiveMapped(SignClipboard.canUndo(signPos));
		});
		undoButton.active = SignClipboard.canUndo(signPos);

		final ButtonWidgetExtension copyButton = new ButtonWidgetExtension(x, y, 30, 20, TextHelper.translatable("gui.nanbin.copy"), button -> SignClipboard.copy(length, signIds, selectedIds));

		final ButtonWidgetExtension pasteButton = new ButtonWidgetExtension(x, y + 30, 30, 20, TextHelper.translatable("gui.nanbin.paste"), button -> {
			SignClipboard.paste(signPos, length, signIds, selectedIds);
			undoButton.setActiveMapped(SignClipboard.canUndo(signPos));
		});
		pasteButton.active = SignClipboard.canPaste(length);

		final ButtonWidgetExtension clearButton = new ButtonWidgetExtension(x, y + 60, 30, 20, TextHelper.translatable("gui.nanbin.clear"), button -> MinecraftClient.getInstance().openScreen(new Screen(new RailwaySignClearConfirmScreen((ScreenExtension) (Object) this, TextHelper.translatable("gui.nanbin.clear.question"), () -> {
			SignClipboard.clear(signPos, length, signIds, selectedIds);
			undoButton.setActiveMapped(SignClipboard.canUndo(signPos));
		}))));

		screen.addChild(new ClickableWidget(copyButton));
		screen.addChild(new ClickableWidget(pasteButton));
		screen.addChild(new ClickableWidget(clearButton));
		screen.addChild(new ClickableWidget(undoButton));
	}
}