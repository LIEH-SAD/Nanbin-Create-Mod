package com.Nanbin.uiHelper;

import com.mojang.blaze3d.systems.RenderSystem;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.holder.TextRenderer;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;
import org.mtr.mapping.mapper.TextHelper;

import javax.annotation.Nullable;

/**
 * 配置项组件。
 *
 * <p>视觉规格：常态为 20% 不透明度的 #000000；当鼠标位于其正上方时淡入为
 * 40% 不透明度的 #FFFFFF，离开时淡出回常态，单程动画时长 0.2 秒（4 tick）。
 */
public class NanbinUIConfigItem extends NanbinUIComponent {

	public static final int HEIGHT = 20;
	private static final int TEXT_COLOR = 0xFFFFFFFF;
	private static final int DISABLED_TEXT_COLOR = 0xFF8C8C8C;
	/** 左侧图标的边长。 */
	public static final int ICON_SIZE = 16;
	/** 图标与文字之间的间距。 */
	public static final int ICON_SPACING = 4;

	private static final int TEXT_INSET = 4;

	private final MutableText label;
	private final Runnable onClick;
	/** 0 表示常态，1 表示完全悬停。 */
	private float hoverAmount;
	@Nullable
	private Identifier icon;
	private boolean enabled = true;

	public NanbinUIConfigItem(MutableText label) {
		this(label, null);
	}

	public NanbinUIConfigItem(String label) {
		this(TextHelper.literal(label), null);
	}

	public NanbinUIConfigItem(MutableText label, @Nullable Runnable onClick) {
		this.label = label;
		this.onClick = onClick;
		this.topMargin = 2;
	}

	public MutableText getLabel() {
		return label;
	}

	/** 是否绑定了点击回调。 */
	public boolean isClickable() {
		return onClick != null;
	}

	/** 左侧图标，null 表示不绘制图标。 */
	public NanbinUIConfigItem setIcon(@Nullable Identifier icon) {
		this.icon = icon;
		return this;
	}

	/** 不可用的条目不会响应点击，也不会出现悬停高亮。 */
	public NanbinUIConfigItem setEnabled(boolean enabled) {
		this.enabled = enabled;
		return this;
	}

	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public int getHeight() {
		return HEIGHT;
	}

	@Override
	public void render(GraphicsHolder graphicsHolder, TextRenderer textRenderer, int mouseX, int mouseY, float delta) {
		final float target = enabled && isMouseOver(mouseX, mouseY) ? 1.0F : 0.0F;
		hoverAmount = approach(hoverAmount, target, delta / FADE_TICKS);

		final GuiDrawing guiDrawing = new GuiDrawing(graphicsHolder);
		guiDrawing.beginDrawingRectangle();
		guiDrawing.drawRectangle(x, y, x + width, y + HEIGHT, hoverColor(hoverAmount));
		guiDrawing.finishDrawingRectangle();

		int textX = x + TEXT_INSET;
		if (icon != null) {
			final int iconY = y + (HEIGHT - ICON_SIZE) / 2;
			// 参考实现会先把着色器颜色重置为白色，避免图标被上一步的着色器颜色染上颜色
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			final GuiDrawing iconDrawing = new GuiDrawing(graphicsHolder);
			iconDrawing.beginDrawingTexture(icon);
			iconDrawing.drawTexture(x + TEXT_INSET, iconY, x + TEXT_INSET + ICON_SIZE, iconY + ICON_SIZE, 0.0F, 0.0F, 1.0F, 1.0F);
			iconDrawing.finishDrawingTexture();
			textX += ICON_SIZE + ICON_SPACING;
		}

		final String text = trim(textRenderer, label.getString(), x + width - TEXT_INSET - textX);
		graphicsHolder.drawText(text, textX, y + (HEIGHT - 8) / 2, enabled ? TEXT_COLOR : DISABLED_TEXT_COLOR, true, GraphicsHolder.getDefaultLight());
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0 && enabled && onClick != null && isMouseOver(mouseX, mouseY)) {
			onClick.run();
			return true;
		}
		return false;
	}
}
