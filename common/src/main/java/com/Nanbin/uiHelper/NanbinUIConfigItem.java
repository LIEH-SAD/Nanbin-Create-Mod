package com.Nanbin.uiHelper;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

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

	private final Text label;
	private final Runnable onClick;
	/** 0 表示常态，1 表示完全悬停。 */
	private float hoverAmount;
	@Nullable
	private Identifier icon;
	private boolean enabled = true;

	public NanbinUIConfigItem(Text label) {
		this(label, null);
	}

	public NanbinUIConfigItem(String label) {
		this(Text.literal(label), null);
	}

	public NanbinUIConfigItem(Text label, Runnable onClick) {
		this.label = label;
		this.onClick = onClick;
		this.topMargin = 2;
	}

	public Text getLabel() {
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
	public void render(MatrixStack matrices, TextRenderer textRenderer, int mouseX, int mouseY, float delta) {
		final float target = enabled && isMouseOver(mouseX, mouseY) ? 1.0F : 0.0F;
		hoverAmount = approach(hoverAmount, target, delta / FADE_TICKS);

		DrawableHelper.fill(matrices, x, y, x + width, y + HEIGHT, hoverColor(hoverAmount));

		int textX = x + TEXT_INSET;
		if (icon != null) {
			RenderSystem.setShader(GameRenderer::getPositionTexShader);
			RenderSystem.setShaderTexture(0, icon);
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			DrawableHelper.drawTexture(matrices, x + TEXT_INSET, y + (HEIGHT - ICON_SIZE) / 2, ICON_SIZE, ICON_SIZE, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
			textX += ICON_SIZE + ICON_SPACING;
		}

		final String text = trim(textRenderer, label.getString(), x + width - TEXT_INSET - textX);
		textRenderer.drawWithShadow(matrices, text, textX, y + (HEIGHT - 8) / 2.0F, enabled ? TEXT_COLOR : DISABLED_TEXT_COLOR);
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
