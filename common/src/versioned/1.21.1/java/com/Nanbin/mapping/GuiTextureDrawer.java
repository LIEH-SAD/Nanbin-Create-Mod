package com.Nanbin.mapping;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/**
 * {@code DrawContext} 的「按像素 UV 绘制纹理」调用（1.21.1 版）。
 * <p>
 * 必须<b>直接调用</b>，不能按方法名反射：发布后的 Fabric 用的是 intermediary 名
 * （{@code method_25294} 之类），NeoForge 用的是 Mojang 官方名（{@code blit}），
 * 两边都不是 Yarn 的 {@code drawTexture}——字符串不参与重映射，反射必然找不到，
 * 一打开界面就抛「当前版本的 DrawContext 没有可用的 drawTexture 重载」。
 * <p>
 * 1.21.4 的这个方法第一个参数多了一个 {@code Function<Identifier, RenderLayer>}，
 * 见 {@code common/src/versioned/1.21.4}。
 */
public final class GuiTextureDrawer {

	private GuiTextureDrawer() {
	}

	public static void draw(DrawContext context, Identifier texture, int x, int y, int width, int height, float u, float v, int textureWidth, int textureHeight) {
		// 原版参数顺序是 (纹理, x, y, u, v, 宽, 高, 纹理宽, 纹理高)
		context.drawTexture(texture, x, y, u, v, width, height, textureWidth, textureHeight);
	}
}
