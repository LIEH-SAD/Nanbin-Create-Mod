package com.Nanbin.fabric;

import com.Nanbin.Init;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.SynchronousResourceReloader;
import net.minecraft.util.Identifier;

/**
 * 开发环境下的 MTR 自定义资源加载补丁。
 * <p>
 * MTR 4.1.0 的 {@code org.mtr.registry.EventRegistryClient$1} 是「匿名类实现 fabric-api 接口」的写法。
 * 开发环境里 MTR 的 jar 需要 intermediary -&gt; named 重映射，而重映射器的继承链断在 fabric-api 那个接口上，
 * 于是这个匿名类的 {@code reload(ResourceManager)} 没有被改名，MTR 自己的资源重载回调永远不会执行，
 * 结果是 MTR 的自定义资源（车辆 / 标志 / 轨道）在开发环境里加载不出来。
 * <p>
 * 生产环境的 MTR jar 本身就是 intermediary，不存在这个问题，所以补丁只在开发环境注册。
 * <p>
 * 注意本类直接实现 MC 的 {@link SynchronousResourceReloader}（而不是 fabric-api 的
 * {@code SimpleSynchronousResourceReloadListener}），这样发布包重映射时不会有同样的继承链问题。
 */
public final class MtrCustomResourceReloadFix implements IdentifiableResourceReloadListener, SynchronousResourceReloader {

	private static final Identifier ID = Identifier.of(Init.MOD_ID, "mtr_custom_resource_reload_fix");

	private MtrCustomResourceReloadFix() {
	}

	public static void register() {
		if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
			ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new MtrCustomResourceReloadFix());
		}
	}

	@Override
	public Identifier getFabricId() {
		return ID;
	}

	@Override
	public void reload(ResourceManager manager) {
		org.mtr.client.CustomResourceLoader.reload();
	}
}
