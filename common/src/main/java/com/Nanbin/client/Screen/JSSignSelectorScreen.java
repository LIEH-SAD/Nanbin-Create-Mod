package com.Nanbin.client.Screen;

import com.Nanbin.client.JavaScriptSupport.JSSignConfig;
import com.Nanbin.uiHelper.NanbinUIScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 全局 JS 样式选择器：列出所有可用脚本（图标 + 翻译后的名称），
 * 选中后回调 {@code onSelect(scriptId)}；选择「无样式」时回调 {@code onSelect(null)}。
 * <p>
 * 界面使用 NanbinUI 样式：大标题 + 小标题 + 配置项列表 + 右侧滚动条。
 * <p>
 * 4.1 适配：{@code ScreenExtension} → {@code Screen}；{@code TextHelper.translatable} → {@code Text.translatable}；
 * {@code MutableText} 换到原版 {@code net.minecraft.text.MutableText}；
 * {@code onClose2()} → 原版 {@code Screen#close()}（{@link NanbinUIScreen} 直接继承原版 Screen）。
 */
public class JSSignSelectorScreen extends NanbinUIScreen {

	private final Consumer<String> onSelect;
	private final String currentScriptId;
	private final List<String> scriptIds = new ArrayList<>();

	public JSSignSelectorScreen(Screen parent, String currentScriptId, Consumer<String> onSelect) {
		super(Text.translatable("gui.nanbin.js_style.title"), parent);
		this.currentScriptId = currentScriptId;
		this.onSelect = onSelect;

		scriptIds.addAll(JSSignConfig.getAllScripts().keySet());
	}

	@Override
	protected void buildComponents() {
		addTitle((MutableText) Text.translatable("gui.nanbin.js_style.select"));

		for (final String scriptId : scriptIds) {
			addConfig(getScriptName(scriptId), () -> selectScript(scriptId))
					.setIcon(JSSignConfig.getIcon(scriptId))
					.setEnabled(!scriptId.equals(currentScriptId));
		}

		addConfig((MutableText) Text.translatable("gui.nanbin.js_style.none"), () -> selectScript(null));
		addConfig((MutableText) Text.translatable("gui.nanbin.cancel"), this::close);
	}

	private void selectScript(String scriptId) {
		if (onSelect != null) {
			onSelect.accept(scriptId);
		}
		close();
	}

	/** 脚本显示名称（lang 翻译优先，回退到配置的 name / id）。 */
	public static MutableText getScriptName(String scriptId) {
		return (MutableText) Text.translatable(JSSignConfig.getDisplayNameKey(scriptId));
	}
}
