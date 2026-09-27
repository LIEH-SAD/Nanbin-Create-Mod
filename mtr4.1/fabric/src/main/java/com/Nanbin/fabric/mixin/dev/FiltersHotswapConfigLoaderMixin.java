package com.Nanbin.fabric.mixin.dev;

import net.minecraft.resource.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;

import java.lang.reflect.Method;

/**
 * 开发环境专用：修掉 filters 在 dev 下「资源重载监听器缺实现」的问题。
 * <p>
 * 现象（每次启动都能在日志里看到）：
 * <pre>
 * java.lang.AbstractMethodError: Receiver class ziyue.filters.hotswap.HotswapConfigLoader
 * does not define or inherit an implementation of the resolved method
 * 'abstract void reload(net.minecraft.resource.ResourceManager)' of interface
 * net.minecraft.resource.SynchronousResourceReloader.
 * </pre>
 * 后果不只是刷一条错误：整个「加载资源包」的 future 会失败，于是 Minecraft 打印
 * {@code Caught error loading resourcepacks, removing all selected resourcepacks}、
 * 把玩家启用的资源包全部关掉，然后<b>再重载一遍</b> —— 启动时间翻倍，玩家每次进游戏
 * 都要重新选资源包。
 * <p>
 * 根因和 MTR 4.1 那个是同一类：filters 是从 intermediary 发布、由 loom 重映射成 named 的，
 * 而 {@code HotswapConfigLoader} 的继承链经过 fabric-api 的
 * {@code SimpleSynchronousResourceReloadListener}（loom 解析不到它是
 * {@code SynchronousResourceReloader} 的子接口），于是它的重载方法名没被改成 {@code reload}
 * （jar 里仍是 {@code method_14491}），接口方法查找就失败了。
 * <p>
 * 修法：给这个类补一个符合接口签名的 {@code reload}，内部用反射转发给原本的实现，
 * 所以 filters 的热重载行为保持不变；找不到方法就安静跳过，不会因为上游变化而崩。
 * 生产环境的 filters 名字本来就是对的，不需要这个补丁，所以本 mixin 被
 * {@link com.Nanbin.fabric.NanbinFabricMixinPlugin} 限制为只在开发环境应用。
 */
@Mixin(targets = "ziyue.filters.hotswap.HotswapConfigLoader", remap = false)
public class FiltersHotswapConfigLoaderMixin {

	public void reload(ResourceManager manager) {
		try {
			final Object self = this;
			final Method original = self.getClass().getDeclaredMethod("method_14491", ResourceManager.class);
			original.setAccessible(true);
			original.invoke(self, manager);
		} catch (ReflectiveOperationException e) {
			// 上游修好之后这里就找不到方法了，忽略即可
		}
	}
}
