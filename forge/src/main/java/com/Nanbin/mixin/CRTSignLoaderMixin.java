package com.Nanbin.mixin;

import com.Nanbin.client.CRTRailwaySignEditor.legacy.CRTSignRegistry;
import com.Nanbin.client.JavaScriptSupport.CRTJsSignLoader;
import org.mtr.mod.client.CustomResourceLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在 MTR 资源重载完成时加载本 mod 的自定义导视标志（{@code nanbin:crt_signs.json}）。
 *
 * <p>注意这里只把标志注册进渲染用的查询缓存，<b>不再</b>混进 MTR 的全局标志列表
 * （{@code CustomResourceLoader.SIGNS}）。所以原版 MTR 导视牌的编辑界面里不会再出现
 * 这些标志；它们只会按 crt_signs.json 里的 {@code whitelist} 出现在 CRT 类导视上，
 * 详见 {@link CRTSignRegistry}。</p>
 */
@Mixin(value = CustomResourceLoader.class, remap = false)
public class CRTSignLoaderMixin {

	@Inject(method = "reload", at = @At("RETURN"))
	private static void nanbin$reloadCustomSigns(CallbackInfo ci) {
		CRTSignRegistry.reload();
		CRTJsSignLoader.reload();
	}
}
