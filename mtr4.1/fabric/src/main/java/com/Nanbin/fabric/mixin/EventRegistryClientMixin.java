package com.Nanbin.fabric.mixin;

import com.Nanbin.fabric.MtrCustomResourceReloadFix;
import net.fabricmc.loader.api.FabricLoader;
import org.mtr.registry.EventRegistryClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 开发环境专用：拦掉 MTR 自己那个坏掉的资源重载监听器注册。
 * <p>
 * MTR 4.1.0 的 {@code EventRegistryClient$1} 在开发环境 intermediary -&gt; named 重映射后缺少
 * {@code reload(ResourceManager)} 的实现（继承链断在 fabric-api 接口上），每次资源重载都会抛
 * {@code AbstractMethodError} 并让整次重载失败。这里改成由
 * {@link MtrCustomResourceReloadFix} 负责调用 MTR 的自定义资源加载。
 * <p>
 * 生产环境的 MTR jar 本身就是 intermediary，没有这个缺陷，所以只在本 mixin 处于
 * Fabric 开发环境时生效（该配置文件也只在 Fabric 侧加载）。
 */
@Mixin(value = EventRegistryClient.class, remap = false)
public abstract class EventRegistryClientMixin {

	@Inject(method = "registerResourceReloadEvent", at = @At("HEAD"), cancellable = true, remap = false)
	private static void nanbin$skipBrokenReloadListener(Runnable runnable, CallbackInfo callbackInfo) {
		if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
			callbackInfo.cancel();
		}
	}
}
