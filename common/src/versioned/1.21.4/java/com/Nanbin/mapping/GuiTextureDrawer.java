package com.Nanbin.mapping;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;

/**
 * {@code DrawContext} 的「按像素 UV 绘制纹理」调用（1.21.4 版）。
 * <p>
 * 1.21.4 起这个方法第一个参数是 {@code Function<Identifier, RenderLayer>}
 * （1.21.1 没有），GUI 用 {@code RenderLayer::getGuiTextured}。
 * <p>
 * 必须<b>直接调用</b>，不能按方法名反射：发布后的 Fabric 用 intermediary 名、
 * NeoForge 用 Mojang 官方名（{@code blit}），都不是 Yarn 的 {@code drawTexture}。
 * <p>
 * 1.21.1 的签名不一样，见 {@code common/src/versioned/1.21.1}。
 */
public final class GuiTextureDrawer {

	private GuiTextureDrawer() {
	}

	public static void draw(DrawContext context, Identifier texture, int x, int y, int width, int height, float u, float v, int textureWidth, int textureHeight) {
		// 原版参数顺序是 (渲染层, 纹理, x, y, u, v, 宽, 高, 纹理宽, 纹理高)
		context.drawTexture(RenderLayer::getGuiTextured, texture, x, y, u, v, width, height, textureWidth, textureHeight);
	}
}
