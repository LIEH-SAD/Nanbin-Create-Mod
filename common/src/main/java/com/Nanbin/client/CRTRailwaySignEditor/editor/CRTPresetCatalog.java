package com.Nanbin.client.CRTRailwaySignEditor.editor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@code nanbin:crt_signs.json} 中**未被新编辑器网页图标集覆盖**的标志清单。
 *
 * <p>合并原则（用户确认）：与当前元素/图标等价的标志不再重复出现在左栏；剩下的差异项
 * （主要是“图标 + 文字”的文字牌，以及检查/禁止标志）作为「CRT 预设」分类，点选后生成
 * 一个 {@code ICON} 元素（{@code icon = 标志 id}），渲染与世界里的贴图完全一致。</p>
 *
 * <p>已覆盖的标志（<b>不</b>出现在本清单）：方向箭头（{@code nanbin_arrow*} 10 个）、
 * {@code nanbin_cross} / {@code crt_check}、{@code crt_railway} / {@code crt_monorail}、
 * {@code crt_escalator} / {@code crt_stair} / {@code crt_ticket_sell} / {@code crt_server} /
 * {@code crt_accessible_lift} / {@code crt_waiting_room} / {@code crt_restroom} /
 * {@code crt_female_toilot} / {@code crt_male_toilot}、{@code crt_exit_up/down/left/right}。
 * 这 27 个 id 已从 {@code crt_signs.json} 合并进代码内置表（{@link CRTBuiltInSignIcons}），
 * 仍被 {@code CRTSignLayoutPainter#WEB_ICON_TO_SIGN} 映射、旧存档格子渲染也仍可用。</p>
 */
public final class CRTPresetCatalog {

	/** 一个预设项：signId 为 {@code crt_signs.json} 里的标志 id。 */
	public static final class Entry {
		public final String signId;
		/** 基础设施 id（用来查语言键 {@code gui.nanbin.crt_editor.crt_sign.<baseId>}）。 */
		public final String baseId;
		public final boolean withText;
		public final boolean flipped;
		public final boolean gray;

		private Entry(String signId, String baseId, boolean withText, boolean flipped, boolean gray) {
			this.signId = signId;
			this.baseId = baseId;
			this.withText = withText;
			this.flipped = flipped;
			this.gray = gray;
		}
	}

	/** 「图标 + 文字」文字牌：9 种设施 × 4 个变体。 */
	private static final String[] TEXT_SIGN_BASES = {
			"escalator", "stair", "ticket_sell", "server", "accessible_lift",
			"waiting_room", "restroom", "female_toilot", "male_toilot"
	};

	/** 出口文字牌：4 个方向 × 2 个变体（正/翻转）。 */
	private static final String[] EXIT_TEXT_SIGN_BASES = {"exit_up", "exit_down", "exit_left", "exit_right"};

	/** 检查/禁止类文字牌（基础 id 做显示名映射用）。 */
	private static final Entry[] CHECK_SIGNS = {
			new Entry("nanbin_cross_with_text", "cross", true, false, false),
			new Entry("crt_check_with_text", "check", true, false, false),
			new Entry("crt_check_with_text_flipped", "check", true, true, false),
			new Entry("crt_check_with_text_gray", "check", true, false, true),
			new Entry("crt_check_with_text_flipped_gray", "check", true, true, true)
	};

	private static final List<Entry> ENTRIES = createEntries();

	private CRTPresetCatalog() {
	}

	/** 全部预设项（顺序固定：检查/禁止 → 设施文字牌 → 出口文字牌）。 */
	public static List<Entry> entries() {
		return ENTRIES;
	}

	/** crt_signs.json 中未被网页图标覆盖的 id（供文档/审计用）。 */
	public static List<String> signIds() {
		final List<String> ids = new ArrayList<>();
		for (final Entry entry : ENTRIES) {
			ids.add(entry.signId);
		}
		return Collections.unmodifiableList(ids);
	}

	private static List<Entry> createEntries() {
		final List<Entry> entries = new ArrayList<>();
		Collections.addAll(entries, CHECK_SIGNS);
		// 设施文字牌：_with_text / _with_text_flipped / _with_text_gray / _with_text_flipped_gray
		for (final String base : TEXT_SIGN_BASES) {
			entries.add(new Entry("crt_" + base + "_with_text", base, true, false, false));
			entries.add(new Entry("crt_" + base + "_with_text_flipped", base, true, true, false));
			entries.add(new Entry("crt_" + base + "_with_text_gray", base, true, false, true));
			entries.add(new Entry("crt_" + base + "_with_text_flipped_gray", base, true, true, true));
		}
		// 出口文字牌：只有 _with_text / _with_text_flipped
		for (final String base : EXIT_TEXT_SIGN_BASES) {
			entries.add(new Entry("crt_" + base + "_with_text", base, true, false, false));
			entries.add(new Entry("crt_" + base + "_with_text_flipped", base, true, true, false));
		}
		return Collections.unmodifiableList(entries);
	}
}
