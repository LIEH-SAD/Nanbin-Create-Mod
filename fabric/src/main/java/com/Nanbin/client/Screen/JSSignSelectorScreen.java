package com.Nanbin.client.Screen;

import com.Nanbin.client.JavaScriptSupport.JSSignConfig;
import com.Nanbin.uiHelper.NanbinUIScreen;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.holder.Screen;
import org.mtr.mapping.mapper.ScreenExtension;
import org.mtr.mapping.mapper.TextHelper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 全局 JS 样式选择器：列出所有可用脚本（图标 + 翻译后的名称），
 * 选中后回调 onSelect(scriptId)；选择“无样式”时回调 onSelect(null)。
 *
 * <p>界面使用 NanbinUI 样式：大标题 + 小标题 + 配置项列表 + 右侧滚动条。
 */
public class JSSignSelectorScreen extends NanbinUIScreen {

	private final Consumer<String> onSelect;
	@Nullable
	private final String currentScriptId;
	private final List<String> scriptIds = new ArrayList<>();

	public JSSignSelectorScreen(ScreenExtension parent, @Nullable String currentScriptId, Consumer<String> onSelect) {
		super(TextHelper.translatable("gui.nanbin.js_style.title"), parent == null ? null : new Screen(parent));
		this.currentScriptId = currentScriptId;
		this.onSelect = onSelect;

		scriptIds.addAll(JSSignConfig.getAllScripts().keySet());
	}

	@Override
	protected void buildComponents() {
		addTitle(TextHelper.translatable("gui.nanbin.js_style.select"));

		for (final String scriptId : scriptIds) {
			addConfig(getScriptName(scriptId), () -> selectScript(scriptId))
					.setIcon(JSSignConfig.getIcon(scriptId))
					.setEnabled(!scriptId.equals(currentScriptId));
		}

		addConfig(TextHelper.translatable("gui.nanbin.js_style.none"), () -> selectScript(null));
		addConfig(TextHelper.translatable("gui.nanbin.cancel"), this::onClose2);
	}

	private void selectScript(@Nullable String scriptId) {
		if (onSelect != null) {
			onSelect.accept(scriptId);
		}
		onClose2();
	}

	/** 脚本显示名称（lang 翻译优先，回退到配置的 name / id）。 */
	public static MutableText getScriptName(String scriptId) {
		return TextHelper.translatable(JSSignConfig.getDisplayNameKey(scriptId));
	}
}
