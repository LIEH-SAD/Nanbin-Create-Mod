package com.Nanbin.uiHelper;

import com.Nanbin.mapping.GraphicsHolder;
import com.Nanbin.mapping.GuiDrawing;
import com.Nanbin.mapping.TextHelper;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import javax.annotation.Nullable;
import java.util.function.BooleanSupplier;

/**
 * 选择型配置项：左侧名称，右侧一个 2:1 的开关按钮（贴图实现，非代码绘制）。
 *
 * <p>移植自 Fake_Redstone 项目的 {@code NanbinUISelectButtonItem}：
 * <ul>
 *   <li>{@code select_button_off.png}：未开启状态的完整按钮（64×32）；</li>
 *   <li>{@code select_button_on.png}：已开启状态的完整按钮（64×32）；</li>
 * </ul>
 * 绘制时按 32×16 的比例整张贴图显示；选中状态通过 {@link BooleanSupplier} 实时读取，
 * 点击整行触发 {@link Runnable}，组件本身不保存状态。</p>
 */
public class NanbinUISelectButtonItem extends NanbinUIComponent {

	public static final int HEIGHT = 20;
	/** 按钮绘制尺寸（贴图 64×32，按 2:1 缩到 32×16）。 */
	public static final int BUTTON_WIDTH = 32;
	public static final int BUTTON_HEIGHT = 16;
	/** 右侧按钮距行右缘的间距。 */
	private static final int BUTTON_INSET = 4;
	private static final int TEXT_INSET = 4;
	private static final int ICON_SIZE = 16;
	private static final int ICON_SPACING = 4;
	private static final int TEXT_COLOR = 0xFFFFFFFF;
	private static final int DISABLED_TEXT_COLOR = 0xFF8C8C8C;

	private static final Identifier OFF_TEXTURE = new Identifier("nanbin:textures/gui/select_button_off.png");
	private static final Identifier ON_TEXTURE = new Identifier("nanbin:textures/gui/select_button_on.png");

	private Text label;
	private final BooleanSupplier selected;
	@Nullable
	private final Runnable onClick;
	private float hoverAmount;
	@Nullable
	private Identifier icon;
	private boolean enabled = true;

	public NanbinUISelectButtonItem(Text label, BooleanSupplier selected, @Nullable Runnable onClick) {
		this.label = label;
		this.selected = selected;
		this.onClick = onClick;
		this.topMargin = 2;
	}

	public NanbinUISelectButtonItem(String label, BooleanSupplier selected, @Nullable Runnable onClick) {
		this(TextHelper.literal(label), selected, onClick);
	}

	public Text getLabel() {
		return label;
	}

	public NanbinUISelectButtonItem setLabel(Text label) {
		this.label = label;
		return this;
	}

	/** 左侧图标（可选）。 */
	public NanbinUISelectButtonItem setIcon(@Nullable Identifier icon) {
		this.icon = icon;
		return this;
	}

	public NanbinUISelectButtonItem setEnabled(boolean enabled) {
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
	public void render(DrawContext context, TextRenderer textRenderer, int mouseX, int mouseY, float delta) {
		final GraphicsHolder graphicsHolder = new GraphicsHolder(context);
		final float target = enabled && isMouseOver(mouseX, mouseY) ? 1.0F : 0.0F;
		hoverAmount = approach(hoverAmount, target, delta / FADE_TICKS);

		final GuiDrawing guiDrawing = new GuiDrawing(graphicsHolder);
		guiDrawing.beginDrawingRectangle();
		guiDrawing.drawRectangle(x, y, x + width, y + HEIGHT, hoverColor(hoverAmount));
		guiDrawing.finishDrawingRectangle();

		int textX = x + TEXT_INSET;
		if (icon != null) {
			final int iconY = y + (HEIGHT - ICON_SIZE) / 2;
			final GuiDrawing iconDrawing = new GuiDrawing(graphicsHolder);
			iconDrawing.beginDrawingTexture(icon);
			iconDrawing.drawTexture(textX, iconY, textX + ICON_SIZE, iconY + ICON_SIZE, 0.0F, 0.0F, 1.0F, 1.0F);
			iconDrawing.finishDrawingTexture();
			textX += ICON_SIZE + ICON_SPACING;
		}

		final int buttonX = x + width - BUTTON_INSET - BUTTON_WIDTH;
		final String text = trim(textRenderer, label.getString(), Math.max(0, buttonX - TEXT_INSET - textX));
		graphicsHolder.drawText(text, textX, y + (HEIGHT - 8) / 2,
				enabled ? TEXT_COLOR : DISABLED_TEXT_COLOR, true, GraphicsHolder.getDefaultLight());
		drawToggleTexture(guiDrawing, buttonX, y + (HEIGHT - BUTTON_HEIGHT) / 2);
	}

	/** 一张贴图就是一个完整状态，整张按 2:1 直接绘制。 */
	private void drawToggleTexture(GuiDrawing guiDrawing, int buttonX, int buttonY) {
		final boolean on = selected != null && selected.getAsBoolean();
		final Identifier texture = on ? ON_TEXTURE : OFF_TEXTURE;
		// 重置着色器颜色，避免贴图被上一处绘制颜色染色（1.20.1 用 setShaderColor）
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		guiDrawing.beginDrawingTexture(texture);
		guiDrawing.drawTexture(buttonX, buttonY, buttonX + BUTTON_WIDTH, buttonY + BUTTON_HEIGHT,
				0.0F, 0.0F, 1.0F, 1.0F);
		guiDrawing.finishDrawingTexture();
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
