package com.Nanbin.mapping;

public final class PlatformHolder {

	private static Platform platform;

	private PlatformHolder() {
	}

	public static void set(Platform platform) {
		PlatformHolder.platform = platform;
	}

	public static Platform get() {
		if (platform == null) {
			throw new IllegalStateException("Platform 尚未初始化，请确认模组入口已经运行");
		}
		return platform;
	}

	public static boolean isModLoaded(String modId) {
		return platform != null && platform.isModLoaded(modId);
	}
}
