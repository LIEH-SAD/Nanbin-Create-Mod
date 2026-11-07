package com.Nanbin.client.Screen;

/**
 * CRT 导视牌编辑器的视觉规范。
 *
 * <p>数值直接取自开源项目 CRT-Sign-Web-Editor 的 style.css {@code :root}（浅色主题）：
 * 三栏布局 260 | auto | 260、圆角 8px、蓝色强调色、白底面板 + 浅灰画布，
 * 让 Minecraft 里的编辑器尽量接近网页项目的观感。</p>
 *
 * <p>来源与许可：CRT-Sign-Web-Editor，Apache License 2.0，
 * 上游 endcreeper861/JR-Guidance-Sign-Web-Editor，PrisamaX0124 fork；
 * 详见 docs/THIRD_PARTY_NOTICES.md。</p>
 */
public final class CRTEditorTheme {

	private CRTEditorTheme() {
	}

	// ─── 颜色（网页 :root 变量） ─────────────────────────────────────

	/** --bg 应用背景 / 画布区底色 */
	public static final int BG = 0xFFEEF1F4;
	/** --panel-bg 面板白底 */
	public static final int PANEL_BG = 0xFFFFFFFF;
	/** --border 边框 */
	public static final int BORDER = 0xFFDCE1E8;
	/** --text 主文字 */
	public static final int TEXT = 0xFF1F2733;
	/** --text-dim 次要文字 */
	public static final int TEXT_DIM = 0xFF6B7684;
	/** --accent 强调色（按钮/选中/焦点） */
	public static final int ACCENT = 0xFF2563EB;
	/** --accent-soft 强调色浅底（选中项、hover 背景） */
	public static final int ACCENT_SOFT = 0x1F2563EB;
	/** --danger 危险操作 */
	public static final int DANGER = 0xFFDC2626;
	/** --ok 成功/确认 */
	public static final int OK = 0xFF16A34A;
	/** --control-bg 输入控件底色 */
	public static final int CONTROL_BG = 0xFFFFFFFF;
	/** --surface-soft 次级面板底色 */
	public static final int SURFACE_SOFT = 0xFFF6F8FB;
	/** 面板分隔线/弱边框（比 --border 更淡） */
	public static final int BORDER_SOFT = 0xFFE8ECF1;

	// 面板按钮专用
	public static final int BUTTON_BG = 0xFFFFFFFF;
	public static final int BUTTON_BG_HOVER = 0xFFF0F4FA;
	public static final int BUTTON_PRIMARY_BG = ACCENT;
	public static final int BUTTON_PRIMARY_TEXT = 0xFFFFFFFF;
	public static final int BUTTON_DANGER_TEXT = DANGER;
	public static final int BUTTON_DISABLED_TEXT = 0xFFB4BCC7;

	// 画布
	public static final int CANVAS_BG = BG;
	public static final int CANVAS_SHADOW = 0x22000000;
	public static final int SELECTION = ACCENT;
	public static final int SELECTION_SOFT = 0x332563EB;
	public static final int ROW_BAND = 0xFFE9EDF3;
	public static final int ROW_BAND_HOVER = 0xFFDDE4EE;
	public static final int DRAG_INDICATOR = ACCENT;
	public static final int HITBOX = 0x00000000;

	// ─── 尺寸/字号（网页 token） ─────────────────────────────────────

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
