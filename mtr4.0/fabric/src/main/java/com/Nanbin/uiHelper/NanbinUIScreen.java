package com.Nanbin.uiHelper;

import com.mojang.blaze3d.systems.RenderSystem;
import org.mtr.mapping.holder.ClickableWidget;
import org.mtr.mapping.holder.MathHelper;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.holder.Screen;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;
import org.mtr.mapping.mapper.ScreenExtension;
import org.mtr.mapping.mapper.TextFieldWidgetExtension;
import org.mtr.mapping.mapper.TextHelper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * NanbinUI 屏幕接口。
 *
 * <p>其他屏幕只要继承本类并规定大标题，然后按顺序调用 {@link #addTitle(MutableText)} /
 * {@link #addConfig(MutableText, Runnable)} 把组件加进来，就自动获得统一风格的界面：
 * 小标题（#787878 不透明条 + 纯白文字）、配置项（20% 不透明度 #000000，悬停淡入到
 * 40% 不透明度 #FFFFFF，0.2 秒）、以及位于它们右侧的滚动条。
 * 整个界面宽高均为屏幕的 1/2 并居中显示。
 *
 * <p>大标题只是一个文本，完全由调用方指定（构造器参数，可以是 {@code TextHelper.literal}
 * 或可翻译文本）；需要动态标题时覆写 {@link #getTitle2()} 即可。
 *
 * <p>组件可以在构造器里添加，也可以覆写 {@link #buildComponents()}（只在第一次 init 时执行一次）。
 *
 * <p>内容区用 {@code RenderSystem} 的剪刀（scissor）裁剪，滚动时组件不会溢出面板。
 * 虽然 MTR 映射层没有暴露 scissor，但 {@code com.mojang.blaze3d.*} 属于 Mojang 命名空间，
 * Fabric(yarn) 与 Forge(official) 都不会重映射它，因此两个加载器都能直接调用。
 *
 * <pre>{@code
 * public class DemoConfigScreen extends NanbinUIScreen {
 *     public DemoConfigScreen(Screen parent) {
 *         super(TextHelper.literal("示例配置"), parent);
 *         addTitle(TextHelper.literal("第一栏"));                 // 小标题，可加多项
 *         addConfig(TextHelper.literal("配置项 A"), this::doA);   // 配置项
 *         addConfig(TextHelper.literal("配置项 B"));              // 无点击回调的纯展示项
 *         addTitle(TextHelper.literal("第二栏"));
 *         addConfig(TextHelper.literal("配置项 C"), this::doC);
 *     }
 * }
 * }</pre>
 */
public class NanbinUIScreen extends ScreenExtension {

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
	@Nullable
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

	public NanbinUIScreen(MutableText title) {
		this(title, (Screen) null);
	}

	public NanbinUIScreen(MutableText title, @Nullable Screen parent) {
		super(title);
		this.parent = parent;
	}

	// ---------------------------------------------------------------- 组件接口

	/** 按顺序加入一个小标题（可加入多项）。 */
	public NanbinUITitle addTitle(MutableText text) {
		return addComponent(new NanbinUITitle(text));
	}

	public NanbinUITitle addTitle(String text) {
		return addTitle(TextHelper.literal(text));
	}

	/** 按顺序加入一个纯展示的配置项。 */
	public NanbinUIConfigItem addConfig(MutableText label) {
		return addConfig(label, null);
	}

	public NanbinUIConfigItem addConfig(String label) {
		return addConfig(TextHelper.literal(label), null);
	}

	/** 按顺序加入一个可点击的配置项。 */
	public NanbinUIConfigItem addConfig(MutableText label, @Nullable Runnable onClick) {
		return addComponent(new NanbinUIConfigItem(label, onClick));
	}

	public NanbinUIConfigItem addConfig(String label, @Nullable Runnable onClick) {
		return addConfig(TextHelper.literal(label), onClick);
	}

	/**
	 * 按顺序加入一个文本框类型的配置项（label 为 null 时文本框占满整行）。
	 * 只能在 {@link #buildComponents()} 之后调用（需要已初始化的文本渲染器）。
	 */
	public NanbinUITextFieldItem addTextField(@Nullable MutableText label, String value, int maxLength) {
		return addComponent(new NanbinUITextFieldItem(getTextRendererMapped(), label, value, maxLength));
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

	/**
	 * 把控件登记为只接收输入、不参与自动渲染的屏幕子控件。
	 * 用于 {@link NanbinUITextFieldItem} 这类需要键盘焦点、但绘制顺序由组件自己控制的控件。
	 */
	public void registerInputChild(TextFieldWidgetExtension child) {
		addSelectableChild(new ClickableWidget(child));
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
	 * 屏幕背景。默认绘制原版背景（模糊 + 压暗）；
	 * 需要自带背景图的子类可以覆写本方法（映射层里 {@code renderBackground} 是 final，不能覆写）。
	 */
	protected void drawBackground(GraphicsHolder graphicsHolder) {
		renderBackground(graphicsHolder);
	}

	// ---------------------------------------------------------------- 生命周期

	@Override
	protected void init2() {
		super.init2();
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
	public void onClose2() {
		if (parent != null) {
			MinecraftClient.getInstance().openScreen(parent);
		} else {
			super.onClose2();
		}
	}

	@Override
	public boolean isPauseScreen2() {
		return false;
	}

	// ---------------------------------------------------------------- 排版

	/** 面板宽高均为屏幕的 1/2 并居中；组件按加入顺序自上而下排列。 */
	private void layout() {
		if (getWidthMapped() <= 0 || getHeightMapped() <= 0) {
			return;
		}

		panelWidth = getWidthMapped() / 2;
		panelHeight = getHeightMapped() / 2;
		panelX = (getWidthMapped() - panelWidth) / 2;
		panelY = (getHeightMapped() - panelHeight) / 2;

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

	// ---------------------------------------------------------------- 渲染

	@Override
	public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float delta) {
		drawBackground(graphicsHolder);
		super.render(graphicsHolder, mouseX, mouseY, delta);

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
			component.render(graphicsHolder, getTextRendererMapped(), contentMouseX, contentMouseY, delta);
		}
		scrollBar.render(graphicsHolder, contentHeight, getViewHeight(), scroll);
		RenderSystem.disableScissor();
	}

	/** 裁剪内容区，保证滚动时组件不会溢出面板。 */
	private void enableContentScissor() {
		final MinecraftClient client = getClientMapped();
		final double scale = client == null || client.data == null ? 1.0 : client.getWindow().getScaleFactor();
		final int x = (int) (contentX1 * scale);
		final int y = (int) ((getHeightMapped() - contentY2) * scale);
		final int w = (int) ((contentX2 - contentX1) * scale);
		final int h = (int) (getViewHeight() * scale);
		if (w > 0 && h > 0) {
			RenderSystem.enableScissor(x, y, w, h);
		}
	}

	private void drawHeader(GraphicsHolder graphicsHolder) {
		final int maxWidth = panelWidth - PADDING * 2;
		final String label = maxWidth <= 0 ? "" : getTextRendererMapped().trimToWidth(getTitle2().getString(), maxWidth);
		final float textX = panelX + (panelWidth - getTextRendererMapped().getWidth(label)) / 2.0F;
		graphicsHolder.drawText(label, (int) textX, panelY + (HEADER_HEIGHT - 8) / 2, HEADER_TEXT_COLOR, true, GraphicsHolder.getDefaultLight());
	}

	// ---------------------------------------------------------------- 输入

	@Override
	public boolean mouseScrolled2(double mouseX, double mouseY, double amount) {
		if (maxScroll > 0 && isInsidePanel(mouseX, mouseY)) {
			scroll = MathHelper.clamp(scroll - amount * SCROLL_STEP, 0, maxScroll);
			layout();
			return true;
		}
		return super.mouseScrolled2(mouseX, mouseY, amount);
	}

	@Override
	public boolean mouseClicked2(double mouseX, double mouseY, int button) {
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
		return super.mouseClicked2(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseReleased2(double mouseX, double mouseY, int button) {
		scrollBar.mouseReleased();
		return super.mouseReleased2(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged2(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (scrollBar.isDragging()) {
			scroll = MathHelper.clamp(scrollBar.mouseDragged(mouseY, contentHeight, getViewHeight(), scroll), 0, maxScroll);
			layout();
			return true;
		}
		return super.mouseDragged2(mouseX, mouseY, button, deltaX, deltaY);
	}

	private boolean isInsidePanel(double mouseX, double mouseY) {
		return mouseX >= panelX && mouseX < panelX + panelWidth && mouseY >= panelY && mouseY < panelY + panelHeight;
	}

	private boolean isInsideContent(double mouseX, double mouseY) {
		return mouseX >= contentX1 && mouseX < contentX2 && mouseY >= contentY1 && mouseY < contentY2;
	}
}
