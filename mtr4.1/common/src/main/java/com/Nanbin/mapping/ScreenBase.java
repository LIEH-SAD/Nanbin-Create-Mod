package com.Nanbin.mapping;

import com.Nanbin.packet.ClientPacketHelper;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

/**
 * 取代 MTR 4.0 的 {@code org.mtr.mod.screen.MTRScreenBase}。
 * <p>
 * 4.1 里 MTR 自己也没有这个基类了（界面直接继承原版 {@link Screen}），
 * 所以这里按 4.0 它的调用面重建一层薄适配，让移植过来的界面代码只需要改基类名：
 * <ul>
 *     <li>{@code init2()} → {@link #init()}</li>
 *     <li>{@code render(GraphicsHolder, ...)} → {@link #render(DrawContext, int, int, float)}（内部包一层 GraphicsHolder）</li>
 *     <li>{@code tick2()} / {@code isPauseScreen2()} / {@code onClose2()} → {@code tick()} / {@code shouldPause()} / {@code close()}</li>
 *     <li>{@code addChild(ClickableWidget)} → {@link #addDrawableChild}</li>
 *     <li>{@code getWidthMapped()/getHeightMapped()} → {@code width} / {@code height}</li>
 * </ul>
 */
public abstract class ScreenBase extends Screen {

	private Screen previousScreen;

	protected ScreenBase(Text title) {
		super(title);
	}

	protected ScreenBase(Text title, Screen previousScreen) {
		super(title);
		this.previousScreen = previousScreen;
	}

	public Screen getPreviousScreen() {
		return previousScreen;
	}

	public int getWidthMapped() {
		return width;
	}

	public int getHeightMapped() {
		return height;
	}

	/** 把控件登记为可绘制子控件（4.0 的 addChild 语义）。 */
	public <T extends ClickableWidget> T addChild(T widget) {
		return addDrawableChild(widget);
	}

	/**
	 * 子类实现绘制（4.0 命名的钩子）。绘制在控件<b>之下</b>，用于背景面板、标注文字。
	 * <p>
	 * 需要画在控件<b>之上</b>的内容（例如告示牌编辑器里的标志预览图，它要盖住
	 * 透明的选择按钮）请用 {@link #renderOverlay2}。
	 */
	public void render2(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float delta) {
	}

	/**
	 * 控件之上的一层绘制（4.0 里子类在 {@code super.render} 之后画的那部分）。
	 * 默认什么都不画。
	 */
	public void renderOverlay2(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float delta) {
	}

	/** 4.0 的初始化钩子。 */
	protected void init2() {
	}

	/** 4.0 的 tick 钩子。 */
	public void tick2() {
	}

	/** 4.0 的「是否暂停游戏」钩子。 */
	public boolean isPauseScreen2() {
		return true;
	}

	/** 4.0 的关闭钩子。 */
	public void onClose2() {
	}

	@Override
	protected void init() {
		super.init();
		init2();
	}

	@Override
	public void tick() {
		super.tick();
		tick2();
	}

	@Override
	public boolean shouldPause() {
		return isPauseScreen2();
	}

	@Override
	public void close() {
		onClose2();
		if (previousScreen != null) {
			if (client != null) {
				client.setScreen(previousScreen);
			}
		} else {
			super.close();
		}
	}

	/**
	 * 把 {@link #render2} 挂在原版背景之后、控件之前。
	 * <p>
	 * 4.0 的绘制顺序是「自绘背景 → 文字 → 控件」（4.0 的子类都在 render 末尾才调
	 * {@code super.render}，控件永远在最上层），而原版 {@code Screen.render} 的顺序是
	 * 「renderBackground → 控件」。如果把自绘内容放在 {@code super.render} 之后画，
	 * 自绘的背景面板就会盖住按钮 —— 售票界面 / 清空确认界面的按钮「不可见」就是这个原因。
	 * <p>
	 * 原版 {@code renderBackground} 在 1.21.1 与 1.21.4 的签名一致（都是
	 * {@code (DrawContext, int, int, float)}），所以这里可以直接覆写：
	 * 先让原版画模糊 + 压暗，再画子类内容，最后原版 {@code Screen.render} 画控件。
	 */
	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
		super.renderBackground(context, mouseX, mouseY, delta);
		render2(new GraphicsHolder(context), mouseX, mouseY, delta);
	}

	/** 原版 {@code render}：背景（含 {@link #render2}）→ 控件 → {@link #renderOverlay2}。 */
	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		renderOverlay2(new GraphicsHolder(context), mouseX, mouseY, delta);
	}

	/** 打开另一个界面（4.0 用 MinecraftClient.openScreen）。 */
	protected void openScreen(Screen screen) {
		ClientPacketHelper.openScreen(screen);
	}
}
