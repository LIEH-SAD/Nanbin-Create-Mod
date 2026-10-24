package com.Nanbin.fabric;

import net.fabricmc.loader.api.FabricLoader;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * 让「只为修开发环境缺陷而存在」的 mixin 只在开发环境生效。
 * <p>
 * 约定：放在 {@code com.Nanbin.fabric.mixin.dev} 包下的 mixin 一律视为开发环境专用；
 * 其余 mixin 正常应用。这样生产环境里那些「补上游在 dev 下 remap 断链」的补丁不会参与，
 * 也就不会因为上游修好之后出现方法重复/目标不存在之类的副作用。
 */
public class NanbinFabricMixinPlugin implements IMixinConfigPlugin {

	private static final String DEV_ONLY_PACKAGE = "com.Nanbin.fabric.mixin.dev.";

	@Override
	public void onLoad(String mixinPackage) {
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		return !mixinClassName.startsWith(DEV_ONLY_PACKAGE) || FabricLoader.getInstance().isDevelopmentEnvironment();
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
	}

	@Override
	public List<String> getMixins() {
		return null;
	}

	@Override
	public void preApply(String targetClassName, org.objectweb.asm.tree.ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}

	@Override
	public void postApply(String targetClassName, org.objectweb.asm.tree.ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}
}
