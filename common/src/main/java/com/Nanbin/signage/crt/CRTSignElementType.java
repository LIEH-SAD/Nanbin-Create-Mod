package com.Nanbin.signage.crt;

import java.util.Locale;

/**
 * CRT（重庆轨道交通）导视牌的元素类型，与开源项目 CRT-Sign-Web-Editor 的
 * {@code ELEMENT_TYPES}（js/state.js）一一对应。
 *
 * <p>网页端类型名使用小写连字符（如 {@code "bilingual-text"}），
 * 本枚举的 {@link #id} 与之一致，方便以后直接导入/导出网页项目 JSON。
 * 唯一例外是 {@link #EXIT_INFO}（本 mod 扩展的出口信息条），网页端没有对应类型。</p>
 */
public enum CRTSignElementType {

	ARROW("arrow"),
	BILINGUAL_TEXT("bilingual-text"),
	SMALL_BILINGUAL_TEXT("small-bilingual-text"),
	BIG_NUMBER("big-number"),
	NUMBER_LINE("number-line"),
	TEXT_LINE("text-line"),
	ENTRANCE("entrance"),
	EXIT("exit"),
	/**
	 * 出口信息条（本 mod 扩展，网页编辑器没有此类型）：箭头 + 出口编号 + 「出口 / Exit」标签 +
	 * 中英双语目的地列表，内容来自 MTR 4.0 的 {@code StationExit}。
	 */
	EXIT_INFO("exit-info"),
	ROUTE_INFO("route-info"),
	SPACE("space"),
	ICON("icon");

	/** 网页端（以及本 mod 存档 JSON）使用的类型名。 */
	public final String id;

	CRTSignElementType(String id) {
		this.id = id;
	}

	/** 按网页端的类型名解析；未知类型返回 null（与网页 sanitizeElement 剔除未知元素的行为一致）。 */
	public static CRTSignElementType fromId(String id) {
		if (id == null) {
			return null;
		}
		final String normalized = id.trim().toLowerCase(Locale.ROOT);
		for (final CRTSignElementType type : values()) {
			if (type.id.equals(normalized)) {
				return type;
			}
		}
		return null;
	}
}
