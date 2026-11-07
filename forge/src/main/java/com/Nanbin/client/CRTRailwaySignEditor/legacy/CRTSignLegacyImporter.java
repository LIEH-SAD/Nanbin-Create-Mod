package com.Nanbin.client.CRTRailwaySignEditor.legacy;

import com.Nanbin.client.JavaScriptSupport.JSSignConfig;
import com.Nanbin.client.JavaScriptSupport.JSSignEngine;
import com.Nanbin.client.Render.RenderCRTRailwaySign;
import com.Nanbin.signage.crt.CRTSignElement;
import com.Nanbin.signage.crt.CRTSignElementType;
import com.Nanbin.signage.crt.CRTSignLayout;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongAVLTreeSet;

import javax.annotation.Nullable;

/**
 * 旧存档（MTR 格子 signIds + selectedIds）→ 新自由布局 {@link CRTSignLayout} 的一次性转换。
 *
 * <p>老指示牌没有布局 JSON 时，编辑界面打开前先转换一次，让玩家可以继续用新编辑器修改，
 * 渲染端仍然在"没有布局"时走旧的格子渲染，所以不打开编辑器也不会改变外观。</p>
 *
 * <p>规则：每格 256px（一个设计格）；空格转 space；已知的文本/线路/出口标志转成对应元素类型
 * 并带上 MTR 数据绑定；其余（箭头、设施图标等）转成 icon 元素，值直接存 signs.json 的标志 id。</p>
 */
public final class CRTSignLegacyImporter {

	private CRTSignLegacyImporter() {
	}

	/**
	 * 转换一张旧指示牌。
	 *
	 * @param signIds     每格标志 id（可含 null）
	 * @param selectedIds 旧界面的数据选择（线路颜色 / 车站 / 出口 id）
	 * @param routeNumber 线路编号获取（颜色 -> 编号），可为 null
	 */
	public static CRTSignLayout convert(@Nullable String[] signIds, @Nullable LongAVLTreeSet selectedIds, @Nullable java.util.function.LongFunction<String> routeNumber) {
		final CRTSignLayout layout = new CRTSignLayout();
		layout.rows.clear();
		final CRTSignLayout.Row row = new CRTSignLayout.Row();
		layout.rows.add(row);
		final int cellCount = signIds == null ? 0 : signIds.length;
		layout.width = Math.max(CRTSignLayout.GRID_SIZE, Math.max(1, cellCount) * CRTSignLayout.GRID_SIZE);
		layout.rowHeight = CRTSignLayout.GRID_SIZE;
		final LongAVLTreeSet selection = selectedIds == null ? new LongAVLTreeSet() : selectedIds;

		// JS 整牌样式：旧数据里第 0 格是 crt_js_style_<scriptId> 标记，直接转成布局的 JS 样式模式
		if (JSSignConfig.hasJSStyle(signIds)) {
			final String scriptId = JSSignConfig.getStyleScriptId(signIds);
			if (scriptId != null) {
				layout.jsStyleId = scriptId;
				layout.jsSelectedIds.addAll(selection);
				return layout;
			}
		}

		for (int i = 0; i < cellCount; i++) {
			final String signId = signIds[i];
			final CRTSignElement element = convertCell(signId, selection, routeNumber, i, cellCount);
			if (element != null) {
				row.elements.add(element);
			}
		}
		return layout;
	}

	@Nullable
	private static CRTSignElement convertCell(@Nullable String signId, LongAVLTreeSet selectedIds, @Nullable java.util.function.LongFunction<String> routeNumber, int cell, int cellCount) {
		if (signId == null || signId.isEmpty()) {
			// 空格：保留一个格宽，避免其它元素挤在一起
			final CRTSignElement space = new CRTSignElement(CRTSignElementType.SPACE);
			space.widthRatio = 1.0F;
			space.paddingAuto = false;
			return space;
		}

		if (RenderCRTRailwaySign.isRouteNumberV2(signId)) {
			final CRTSignElement element = new CRTSignElement(CRTSignElementType.NUMBER_LINE);
			element.align = signId.endsWith("_flipped") ? CRTSignElement.TextAlign.RIGHT : CRTSignElement.TextAlign.LEFT;
			element.binding = CRTSignElement.DataBinding.ROUTE_LINES;
			element.bindingIds.addAll(selectedIds);
			if (element.bindingIds.isEmpty()) {
				element.binding = CRTSignElement.DataBinding.NONE;
			}
			return element;
		}

		if (signId.startsWith("crt_route_number")) {
			final CRTSignElement element = new CRTSignElement(CRTSignElementType.BIG_NUMBER);
			element.binding = CRTSignElement.DataBinding.ROUTE_NUMBER;
			element.bindingIds.addAll(selectedIds);
			if (element.bindingIds.isEmpty()) {
				element.binding = CRTSignElement.DataBinding.NONE;
				element.text = "1";
			}
			return element;
		}

		if (signId.startsWith("crt_station_name")) {
			final CRTSignElement element = new CRTSignElement(CRTSignElementType.BILINGUAL_TEXT);
			element.binding = CRTSignElement.DataBinding.STATION;
			element.bindingIds.addAll(selectedIds);
			if (element.bindingIds.isEmpty()) {
				element.binding = CRTSignElement.DataBinding.NONE;
			}
			return element;
		}

		if (signId.startsWith("nanbin_custom_text")) {
			final String customText = JSSignEngine.readCustomText(selectedIds);
			final CRTSignElement element = new CRTSignElement(CRTSignElementType.BILINGUAL_TEXT);
			final String[] parts = customText.isEmpty() ? new String[0] : customText.split("\\|", -1);
			element.textZh = parts.length > 0 ? parts[0] : "";
			element.textEn = parts.length > 1 ? parts[1] : "";
			return element;
		}

		if (signId.startsWith("crt_exit_")) {
			final CRTSignElement element = new CRTSignElement(CRTSignElementType.EXIT);
			element.binding = CRTSignElement.DataBinding.EXIT;
			element.bindingIds.addAll(selectedIds);
			if (element.bindingIds.isEmpty()) {
				element.binding = CRTSignElement.DataBinding.NONE;
			}
			element.align = signId.contains("_flipped") ? CRTSignElement.TextAlign.RIGHT : CRTSignElement.TextAlign.LEFT;
			return element;
		}

		// 其它一律当作图标（值存 signs.json 标志 id，渲染端直接取其贴图）
		final CRTSignElement icon = new CRTSignElement(CRTSignElementType.ICON);
		icon.icon = signId;
		icon.rotation = 0;
		if (signId.startsWith("nanbin_arrow")) {
			// 旧箭头标志的翻转/方向已包含在贴图里，无需旋转
			icon.rotation = 0;
		}
		return icon;
	}
}
