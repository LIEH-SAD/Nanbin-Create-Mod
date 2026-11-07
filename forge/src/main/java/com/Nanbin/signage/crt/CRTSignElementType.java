package com.Nanbin.signage.crt;

import java.util.Locale;

/**
 * CRT（重庆轨道交通）导视牌的元素类型，与开源项目 CRT-Sign-Web-Editor 的
 * {@code ELEMENT_TYPES}（js/state.js）一一对应。
 *
 * <p>网页端类型名使用小写连字符（如 {@code "bilingual-text"}），
 * 本枚举的 {@link #id} 与之一致，方便以后直接导入/导出网页项目 JSON。</p>
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
