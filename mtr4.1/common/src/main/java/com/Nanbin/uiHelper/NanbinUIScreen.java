package com.Nanbin.uiHelper;

import com.Nanbin.mapping.GraphicsHolder;
import com.Nanbin.mapping.GuiDrawing;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * NanbinUI 屏幕基类。
 * <p>
 * 其他屏幕只要继承本类并规定大标题，然后按顺序调用 {@link #addTitle(MutableText)} /
 * {@link #addConfig(MutableText, Runnable)} 把组件加进来，就自动获得统一风格的界面：
 * 小标题（#787878 不透明条 + 纯白文字）、配置项（20% 不透明度 #000000，悬停淡入到
 * 40% 不透明度 #FFFFFF，0.2 秒），以及位于它们右侧的滚动条。
 * 整个界面宽高均为屏幕的 1/2 并居中显示。
 * <p>
 * 4.1 适配：4.0 的 {@code ScreenExtension} 包装层已移除（MTR 4.1 也没有 MTRScreenBase），
 * 这里直接继承原版 {@link Screen}；{@code *2} 钩子改名为原版方法名。
 * <p>
 * 内容区用 scissor 裁剪，滚动时组件不会溢出面版：{@code com.mojang.blaze3d.*} 属于 Mojang 命名空间，
 * Fabric 与 NeoForge 都不重映射它，所以两个加载器都能直接调用。
 *
 * <pre>{@code
 * public class DemoConfigScreen extends NanbinUIScreen {
 *     public DemoConfigScreen(Screen parent) {
 *         super(Text.literal("示例配置"), parent);
 *         addTitle(Text.literal("第一栏"));                 // 小标题，可加多栏
 *         addConfig(Text.literal("配置项 A"), this::doA);   // 配置项
 *         addConfig(Text.literal("配置项 B"));              // 无点击回调的纯展示项
 *     }
 * }
 * }</pre>
 */
public class NanbinUIScreen extends Screen {

	public static final int PADDING = 8;
	public static final int HEADER_HEIGHT = 16;
	public static final int HEADER_GAP = 6;
	public static final int SCROLLBAR_GAP = 4;
	/** 面板底色，默认完全透明（不遮挡游戏画面），可由子类覆写 {@link #getPanelBackgroundColor()}。 */
	public static final int PANEL_BACKGROUND_COLOR = 0x00000000;
	/** 大标题文字颜色。 */
	public static final int HEADER_TEXT_COLOR = 0xFFFFFFFF;

	private static final double SCROLL_STEP = 16.0;

	private final List<NanbinUIComponent> components = new ArrayList<>();
	private final NanbinUIScrollBar scrollBar = new NanbinUIScrollBar();
	private final Screen parent;

	private boolean componentsBuilt;
	private double scroll;

	private int panelX;
	private int panelY;
	private int panelWidth;
	private int panelHeight;
	private int contentX1;
	private int contentY1;
	private int contentX2;
	private int contentY2;
	private int contentHeight;
	private int maxScroll;

	public NanbinUIScreen(Text title) {
		this(title, null);
	}

	public NanbinUIScreen(Text title, Screen parent) {
		super(title);
		this.parent = parent;
	}

	// 组件接口

	/** 按顺序加入一个小标题（可加入多项）。 */
	public NanbinUITitle addTitle(MutableText text) {
		return addComponent(new NanbinUITitle(text));
	}

	public NanbinUITitle addTitle(String text) {
		return addTitle(Text.literal(text));
	}

	/** 按顺序加入一个纯展示的配置项。 */
	public NanbinUIConfigItem addConfig(MutableText label) {
		return addConfig(label, null);
	}

	public NanbinUIConfigItem addConfig(String label) {
		return addConfig(Text.literal(label), null);
	}

	/** 按顺序加入一个可点击的配置项。 */
	public NanbinUIConfigItem addConfig(MutableText label, Runnable onClick) {
		return addComponent(new NanbinUIConfigItem(label, onClick));
	}

	public NanbinUIConfigItem addConfig(String label, Runnable onClick) {
		return addConfig(Text.literal(label), onClick);
	}

	/**
	 * 按顺序加入一个文本框类型的配置项（label 为 null 时文本框占满整行）。
	 * 只能在 {@link #buildComponents()} 之后调用（需要已初始化的文本渲染器）。
	 */
	public NanbinUITextFieldItem addTextField(MutableText label, String value, int maxLength) {
		return addComponent(new NanbinUITextFieldItem(this, label, value, maxLength));
	}

	public <T extends NanbinUIComponent> T addComponent(T component) {
		components.add(component);
		layout();
		return component;
	}

	public List<NanbinUIComponent> getComponents() {
		return Collections.unmodifiableList(components);
	}

	public void clearComponents() {
		components.clear();
		layout();
	}

	public NanbinUIScrollBar getScrollBar() {
		return scrollBar;
	}

	/** 原版把文本渲染器暴露为字段 {@code textRenderer}，这里补一个 getter 方便组件使用。 */
	public net.minecraft.client.font.TextRenderer getTextRenderer() {
		return textRenderer;
	}

	/**
	 * 把控件登记为只接收输入、不参与自动渲染的屏幕子控件。
	 * 用于 {@link NanbinUITextFieldItem} 这类需要键盘焦点、但绘制顺序由组件自己控制的控件。
	 */
	public void registerInputChild(TextFieldWidget child) {
		addSelectableChild(child);
	}

	public int getPanelX() {
		return panelX;
	}

	public int getPanelY() {
		return panelY;
	}

	public int getPanelWidth() {
		return panelWidth;
	}

	public int getPanelHeight() {
		return panelHeight;
	}

	/** 子类可覆写，用于在第一次初始化时批量添加组件。 */
	protected void buildComponents() {
	}

	/** 面板底色，默认透明。 */
	protected int getPanelBackgroundColor() {
		return PANEL_BACKGROUND_COLOR;
	}

	/**
	 * 屏幕背景。默认交给原版 {@code Screen#render} 绘制（模糊 + 压暗）；
	 * 需要自带背景图的子类可以覆写本方法（注意原版 renderBackground 在 1.21.1 与 1.21.4 之间签名不同，
	 * 所以这里不直接调用它，而是由 {@code super.render(...)} 处理）。
	 */
	protected void drawBackground(GraphicsHolder graphicsHolder) {
	}

	// 生命周期

	@Override
	protected void init() {
		super.init();
		if (!componentsBuilt) {
			buildComponents();
			componentsBuilt = true;
		}
		for (final NanbinUIComponent component : components) {
			component.attach(this);
		}
		layout();
	}

	@Override
	public void close() {
		if (parent != null) {
			if (client != null) {
				client.setScreen(parent);
			}
		} else {
			super.close();
		}
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	// 排版

	/** 面板宽高均为屏幕的 1/2 并居中；组件按加入顺序自上而下排列。 */
	private void layout() {
		if (width <= 0 || height <= 0) {
			return;
		}

		panelWidth = width / 2;
		panelHeight = height / 2;
		panelX = (width - panelWidth) / 2;
		panelY = (height - panelHeight) / 2;

		contentX1 = panelX + PADDING;
		contentY1 = panelY + HEADER_HEIGHT + HEADER_GAP;
		contentX2 = panelX + panelWidth - PADDING;
		contentY2 = panelY + panelHeight - PADDING;

		contentHeight = 0;
		for (final NanbinUIComponent component : components) {
			contentHeight += component.getTopMargin() + component.getHeight();
		}

		final int viewHeight = getViewHeight();
		maxScroll = Math.max(0, contentHeight - viewHeight);
		scroll = MathHelper.clamp(scroll, 0, maxScroll);

		final int itemRight = contentX2 - NanbinUIScrollBar.WIDTH - SCROLLBAR_GAP;
		final int itemWidth = Math.max(0, itemRight - contentX1);
		int cursor = contentY1 - (int) Math.round(scroll);
		for (final NanbinUIComponent component : components) {
			cursor += component.getTopMargin();
			component.setBounds(contentX1, cursor, itemWidth);
			cursor += component.getHeight();
		}

		scrollBar.setBounds(contentX2 - NanbinUIScrollBar.WIDTH, contentY1, viewHeight);
	}

	private int getViewHeight() {
		return Math.max(0, contentY2 - contentY1);
	}

	// 渲染

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		// 原版 render 内部会调用 renderBackground（两版签名不同，交给它自己处理）
		super.render(context, mouseX, mouseY, delta);

		final GraphicsHolder graphicsHolder = new GraphicsHolder(context);
		drawBackground(graphicsHolder);

		final GuiDrawing panelDrawing = new GuiDrawing(graphicsHolder);
		panelDrawing.beginDrawingRectangle();
		panelDrawing.drawRectangle(panelX, panelY, panelX + panelWidth, panelY + panelHeight, getPanelBackgroundColor());
		panelDrawing.finishDrawingRectangle();

		drawHeader(graphicsHolder);

		// 鼠标不在内容区时传 -1，避免滚出可视范围的组件被判定为悬停
		final boolean mouseInContent = isInsideContent(mouseX, mouseY);
		final int contentMouseX = mouseInContent ? mouseX : -1;
		final int contentMouseY = mouseInContent ? mouseY : -1;

		enableContentScissor();
		for (final NanbinUIComponent component : components) {
			component.render(graphicsHolder, textRenderer, contentMouseX, contentMouseY, delta);
		}
		scrollBar.render(graphicsHolder, contentHeight, getViewHeight(), scroll);
		RenderSystem.disableScissor();
	}

	// 裁剪内容区，保证滚动时组件不会溢出面版。
	private void enableContentScissor() {
		final double scale = client == null ? 1.0 : client.getWindow().getScaleFactor();
		final int x = (int) (contentX1 * scale);
		final int y = (int) ((height - contentY2) * scale);
		final int w = (int) ((contentX2 - contentX1) * scale);
		final int h = (int) (getViewHeight() * scale);
		if (w > 0 && h > 0) {
			RenderSystem.enableScissor(x, y, w, h);
		}
	}

	private void drawHeader(GraphicsHolder graphicsHolder) {
		final int maxWidth = panelWidth - PADDING * 2;
		final String label = maxWidth <= 0 ? "" : textRenderer.trimToWidth(getTitle().getString(), maxWidth);
		final float textX = panelX + (panelWidth - textRenderer.getWidth(label)) / 2.0F;
		graphicsHolder.drawText(label, (int) textX, panelY + (HEADER_HEIGHT - 8) / 2, HEADER_TEXT_COLOR, true, GraphicsHolder.getDefaultLight());
	}

	// 输入

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (maxScroll > 0 && isInsidePanel(mouseX, mouseY)) {
			scroll = MathHelper.clamp(scroll - verticalAmount * SCROLL_STEP, 0, maxScroll);
			layout();
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (scrollBar.mouseClicked(mouseX, mouseY, button, contentHeight, getViewHeight(), scroll)) {
			return true;
		}
		if (isInsideContent(mouseX, mouseY)) {
			for (final NanbinUIComponent component : components) {
				if (component.mouseClicked(mouseX, mouseY, button)) {
					return true;
				}
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		scrollBar.mouseReleased();
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (scrollBar.isDragging()) {
			scroll = MathHelper.clamp(scrollBar.mouseDragged(mouseY, contentHeight, getViewHeight(), scroll), 0, maxScroll);
			layout();
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
	}

	private boolean isInsidePanel(double mouseX, double mouseY) {
		return mouseX >= panelX && mouseX < panelX + panelWidth && mouseY >= panelY && mouseY < panelY + panelHeight;
	}

	private boolean isInsideContent(double mouseX, double mouseY) {
		return mouseX >= contentX1 && mouseX < contentX2 && mouseY >= contentY1 && mouseY < contentY2;
	}
}
