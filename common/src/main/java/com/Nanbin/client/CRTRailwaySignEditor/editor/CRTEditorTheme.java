package com.Nanbin.client.CRTRailwaySignEditor.editor;

/**
 * CRT 导视牌编辑器的视觉规范。
 *
 * <p>数值取自开源项目 CRT-Sign-Web-Editor 的 style.css {@code :root}（浅色主题），
 * 三栏布局 260 | auto | 260、圆角 8px、蓝色强调色、白底面板 + 浅灰画布，
 * 让 Minecraft 里的编辑器尽量接近网页项目的观感。</p>
 *
 * <p>颜色字段是可变静态量：客户端配置里的「深色模式」开关会调用 {@link #applyDarkMode(boolean)}
 * 在浅色/深色两套 token 之间切换，所有使用 {@code CRTEditorTheme.XXX} 的地方无需改动。
 * 尺寸/字号常量保持 {@code final}。</p>
 *
 * <p>来源与许可：CRT-Sign-Web-Editor，Apache License 2.0；
 * 上游 endcreeper861/JR-Guidance-Sign-Web-Editor，PrisamaX0124 fork；
 * 详见 docs/THIRD_PARTY_NOTICES.md。</p>
 */
public final class CRTEditorTheme {

	private CRTEditorTheme() {
	}

	// ─── 颜色（网页 :root 变量；可在浅色 / 深色之间切换） ───────────────

	/** --bg 应用背景 / 画布区底色 */
	public static int BG = 0xFFEEF1F4;
	/** --panel-bg 面板白底 */
	public static int PANEL_BG = 0xFFFFFFFF;
	/** --border 边框 */
	public static int BORDER = 0xFFDCE1E8;
	/** --text 主文字 */
	public static int TEXT = 0xFF1F2733;
	/** --text-dim 次要文字 */
	public static int TEXT_DIM = 0xFF6B7684;
	/** --accent 强调色（按钮/选中/焦点） */
	public static int ACCENT = 0xFF2563EB;
	/** --accent-soft 强调色浅底（选中项、hover 背景） */
	public static int ACCENT_SOFT = 0xFFE8EEFC;
	/** --danger 危险操作 */
	public static int DANGER = 0xFFDC2626;
	/** --ok 成功/确认 */
	public static int OK = 0xFF16A34A;
	/** --control-bg 输入控件底色 */
	public static int CONTROL_BG = 0xFFFFFFFF;
	/** --surface-soft 次级面板底色 */
	public static int SURFACE_SOFT = 0xFFF6F8FB;
	/** 面板分隔线/弱边框（比 --border 更淡） */
	public static int BORDER_SOFT = 0xFFE8ECF1;

	// 面板按钮专用
	public static int BUTTON_BG = 0xFFFFFFFF;
	public static int BUTTON_BG_HOVER = 0xFFF0F4FA;
	public static int BUTTON_PRIMARY_BG = ACCENT;
	public static int BUTTON_PRIMARY_TEXT = 0xFFFFFFFF;
	public static int BUTTON_DANGER_TEXT = DANGER;
	public static int BUTTON_DISABLED_TEXT = 0xFFB4BCC7;

	// 卡片（左栏元素卡/预设卡）底色
	public static int CARD_BG = 0xFFFAFBFC;
	public static int CARD_BG_HOVER = 0xFFFFFFFF;

	// 画布
	public static int CANVAS_BG = 0xFFE7EAEF;
	public static int CANVAS_SHADOW = 0xFFDCE1E8;
	public static int SELECTION = ACCENT;
	public static int SELECTION_SOFT = 0xFFD3E0FA;
	public static int ROW_BAND = 0xFFE9EDF3;
	public static int ROW_BAND_HOVER = 0xFFDDE4EE;
	public static int DRAG_INDICATOR = ACCENT;
	public static int HITBOX = 0x00000000;

	private static boolean darkMode;

	/** 当前是否深色模式。 */
	public static boolean isDarkMode() {
		return darkMode;
	}

	/**
	 * 切换浅色 / 深色配色（由客户端配置的「深色模式」开关调用）。
	 * 只改颜色 token，不动尺寸/字号。
	 */
	public static void applyDarkMode(boolean dark) {
		darkMode = dark;
		if (dark) {
			BG = 0xFF1B2028;
			PANEL_BG = 0xFF232A34;
			BORDER = 0xFF39424F;
			TEXT = 0xFFE6EBF2;
			TEXT_DIM = 0xFF9BA6B5;
			ACCENT = 0xFF3B82F6;
			ACCENT_SOFT = 0xFF2A3B57;
			DANGER = 0xFFEF4444;
			OK = 0xFF22C55E;
			CONTROL_BG = 0xFF2C3440;
			SURFACE_SOFT = 0xFF2A313C;
			BORDER_SOFT = 0xFF323B47;
			BUTTON_BG = 0xFF2C3440;
			BUTTON_BG_HOVER = 0xFF374252;
			BUTTON_PRIMARY_BG = ACCENT;
			BUTTON_PRIMARY_TEXT = 0xFFFFFFFF;
			BUTTON_DANGER_TEXT = DANGER;
			BUTTON_DISABLED_TEXT = 0xFF6B7684;
			CARD_BG = 0xFF2A313C;
			CARD_BG_HOVER = 0xFF353E4B;
			CANVAS_BG = 0xFF171C23;
			CANVAS_SHADOW = 0xFF2A3340;
			SELECTION = ACCENT;
			SELECTION_SOFT = 0xFF3A4E73;
			ROW_BAND = 0xFF313A47;
			ROW_BAND_HOVER = 0xFF3A4555;
			DRAG_INDICATOR = ACCENT;
			HITBOX = 0x00000000;
		} else {
			BG = 0xFFEEF1F4;
			PANEL_BG = 0xFFFFFFFF;
			BORDER = 0xFFDCE1E8;
			TEXT = 0xFF1F2733;
			TEXT_DIM = 0xFF6B7684;
			ACCENT = 0xFF2563EB;
			ACCENT_SOFT = 0xFFE8EEFC;
			DANGER = 0xFFDC2626;
			OK = 0xFF16A34A;
			CONTROL_BG = 0xFFFFFFFF;
			SURFACE_SOFT = 0xFFF6F8FB;
			BORDER_SOFT = 0xFFE8ECF1;
			BUTTON_BG = 0xFFFFFFFF;
			BUTTON_BG_HOVER = 0xFFF0F4FA;
			BUTTON_PRIMARY_BG = ACCENT;
			BUTTON_PRIMARY_TEXT = 0xFFFFFFFF;
			BUTTON_DANGER_TEXT = DANGER;
			BUTTON_DISABLED_TEXT = 0xFFB4BCC7;
			CARD_BG = 0xFFFAFBFC;
			CARD_BG_HOVER = 0xFFFFFFFF;
			CANVAS_BG = 0xFFE7EAEF;
			CANVAS_SHADOW = 0xFFDCE1E8;
			SELECTION = ACCENT;
			SELECTION_SOFT = 0xFFD3E0FA;
			ROW_BAND = 0xFFE9EDF3;
			ROW_BAND_HOVER = 0xFFDDE4EE;
			DRAG_INDICATOR = ACCENT;
			HITBOX = 0x00000000;
		}
	}

	// ─── 尺寸/字号（网页 token；固定） ───────────────────────────────

	/** 左右栏宽度（网页 --panel-width: 260px）。 */
	public static final int PANEL_WIDTH = 260;
	/** 圆角（网页 --radius: 8px）。 */
	public static final int RADIUS = 8;
	public static final int RADIUS_SMALL = 6;
	public static final int PADDING = 12;
	public static final int PADDING_LARGE = 16;
	public static final int GAP = 8;
	public static final int GAP_SMALL = 6;

	/** 常规字号（网页 body 14px）。 */
	public static final int FONT_SIZE = 14;
	/** 小字号（按钮/说明 12px）。 */
	public static final int FONT_SIZE_SMALL = 12;
	/** 极小字号（标签 10~11px）。 */
	public static final int FONT_SIZE_TINY = 10;
	/** 面板标题字号（14~15px）。 */
	public static final int FONT_SIZE_TITLE = 15;
	/** 大标题字号（弹窗 20px）。 */
	public static final int FONT_SIZE_HEADING = 20;

	/** 面板头高度。 */
	public static final int HEADER_HEIGHT = 40;
	/** 普通控件高度（网页按钮约 36px，MC 里略小）。 */
	public static final int CONTROL_HEIGHT = 20;
	/** 输入框高度。 */
	public static final int INPUT_HEIGHT = 20;
	/** 调色板卡片高度。 */
	public static final int CARD_HEIGHT = 56;
	/** 滚动条宽度。 */
	public static final int SCROLLBAR_WIDTH = 6;
}
