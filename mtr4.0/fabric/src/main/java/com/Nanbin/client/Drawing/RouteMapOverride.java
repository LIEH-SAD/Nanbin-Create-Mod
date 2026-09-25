package com.Nanbin.client.Drawing;

import org.mtr.mapping.holder.AbstractTexture;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.NativeImage;
import org.mtr.mapping.holder.NativeImageBackedTexture;
import org.mtr.mapping.holder.TextureManager;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import static com.Nanbin.Init.LOGGER;

/**
 * 全局覆写线路图。
 *
 * <p>每次启动游戏后（以及配置改动后）从配置的网址拉取一张 PNG，
 * 以最高优先级覆盖 {@code assets/mtr/textures/texture/route_map.png}：
 * 直接把这张纹理注册到纹理管理器里，因此不管是资源包还是其它模组提供的同名贴图都会被顶掉。
 * 网址为空或拉取失败（HTTP 错误、超时、不是 PNG……）时使用默认线路图。
 */
public final class RouteMapOverride {

	/** 被覆写的纹理。 */
	public static final Identifier ROUTE_MAP_ID = new Identifier("mtr", "textures/texture/route_map.png");

	/** PNG 文件头：89 50 4E 47 0D 0A 1A 0A。 */
	private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
	private static final int TIMEOUT_MS = 10_000;
	/** 下载字节数上限（约 16MB）。 */
	private static final int MAX_DOWNLOAD_BYTES = 16 * 1024 * 1024;

	/** 最近一次请求的网址，用来丢弃过期的下载结果。 */
	private static volatile String requestedUrl = "";
	/** 下载到的 PNG 原始字节；null 表示当前没有可用的覆写图。 */
	private static volatile byte[] pngBytes;
	/** 当前生效的覆写纹理；只在渲染线程访问。 */
	@Nullable
	private static AbstractTexture activeTexture;
	/** 客户端尚未构造完成时暂存的下载结果，由第一个客户端 tick 应用。 */
	@Nullable
	private static volatile String pendingDownloadUrl;
	@Nullable
	private static volatile byte[] pendingDownloadBytes;

	private RouteMapOverride() {
	}

	/** 按网址重新加载覆写图；网址为空时恢复默认线路图。可在任意线程调用。 */
	public static void apply(@Nullable String url) {
		final String target = url == null ? "" : url.trim();
		requestedUrl = target;

		if (target.isEmpty()) {
			pngBytes = null;
			final MinecraftClient client = getClient();
			if (client != null) {
				client.execute(RouteMapOverride::restoreDefault);
			}
			return;
		}

		final Thread thread = new Thread(() -> download(target), "Nanbin RouteMap");
		thread.setDaemon(true);
		thread.start();
	}

	/**
	 * 每个客户端 tick 调用一次（渲染线程）。资源重载会清空纹理表，
	 * 这里检测到覆写图掉了就重新装回去。
	 */
	public static void tick() {
		// 客户端初始化阶段下载完的结果在这里补上
		final String pendingUrlValue = pendingDownloadUrl;
		if (pendingUrlValue != null) {
			final byte[] pendingBytes = pendingDownloadBytes;
			pendingDownloadUrl = null;
			pendingDownloadBytes = null;
			finishDownload(pendingUrlValue, pendingBytes);
		}

		final AbstractTexture current = activeTexture;
		if (current == null || pngBytes == null) {
			return;
		}
		final TextureManager textureManager = getTextureManager();
		if (textureManager == null) {
			return;
		}
		final AbstractTexture loaded = textureManager.getTexture(ROUTE_MAP_ID);
		if (loaded != null && loaded.data == current.data) {
			return;
		}
		applyBytes();
	}

	private static void download(String url) {
		final byte[] bytes = downloadPng(url);
		final MinecraftClient client = getClient();
		if (client == null) {
			// 客户端还没构造完（例如 forge 初始化阶段），交给第一个客户端 tick 处理
			pendingDownloadUrl = url;
			pendingDownloadBytes = bytes;
			return;
		}

		client.execute(() -> finishDownload(url, bytes));
	}

	/** 应用下载结果；必须在客户端线程调用。 */
	private static void finishDownload(String url, @Nullable byte[] bytes) {
		// 下载期间用户又改了配置，丢弃这次结果
		if (!url.equals(requestedUrl)) {
			return;
		}
		if (bytes == null) {
			LOGGER.error("RouteMapOverride: failed to load {}, using the default route map", url);
			pngBytes = null;
			restoreDefault();
			return;
		}
		pngBytes = bytes;
		applyBytes();
	}

	/** 用当前 pngBytes 覆盖线路图纹理；必须在渲染线程调用。 */
	private static void applyBytes() {
		final byte[] bytes = pngBytes;
		if (bytes == null) {
			return;
		}

		final NativeImage image;
		try {
			image = NativeImage.read(new ByteArrayInputStream(bytes));
		} catch (Exception e) {
			LOGGER.error("RouteMapOverride: the downloaded file is not a readable png, using the default route map", e);
			pngBytes = null;
			restoreDefault();
			return;
		}
		LOGGER.info("RouteMapOverride: route map overridden ({}x{})", image.getWidth(), image.getHeight());
		// 映射层把 NativeImageBackedTexture 与 AbstractTexture 拆成两个 holder，这里按底层纹理对象接上
		final NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
		replaceTexture(new AbstractTexture(texture.data));
	}

	/**
	 * 恢复为资源包里的默认线路图；必须在渲染线程调用。
	 *
	 * <p>映射层没有 {@code ResourceTexture}，因此改用「销毁覆写纹理 + 重新按资源位置加载」，
	 * 效果同样是让纹理管理器按资源包数据重建默认线路图。
	 */
	private static void restoreDefault() {
		if (activeTexture == null) {
			return;
		}
		final TextureManager textureManager = getTextureManager();
		if (textureManager == null) {
			return;
		}
		activeTexture = null;
		textureManager.destroyTexture(ROUTE_MAP_ID);
		// 直接在当前（渲染）线程重建默认纹理
		textureManager.loadTextureAsync(ROUTE_MAP_ID, Runnable::run);
	}

	private static void replaceTexture(AbstractTexture texture) {
		final TextureManager textureManager = getTextureManager();
		if (textureManager == null) {
			return;
		}
		final AbstractTexture previous = activeTexture;
		activeTexture = texture;
		textureManager.registerTexture(ROUTE_MAP_ID, texture);
		if (previous != null && previous != texture) {
			try {
				previous.close();
			} catch (Exception ignored) {
			}
		}
	}

	@Nullable
	private static TextureManager getTextureManager() {
		final MinecraftClient client = getClient();
		return client == null ? null : client.getTextureManager();
	}

	/** 映射层的 getInstance() 永远返回非 null 包装对象，客户端未创建时 data 才是 null。 */
	@Nullable
	private static MinecraftClient getClient() {
		final MinecraftClient client = MinecraftClient.getInstance();
		return client.data == null ? null : client;
	}

	@Nullable
	private static byte[] downloadPng(String url) {
		InputStream inputStream = null;
		try {
			final HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
			connection.setConnectTimeout(TIMEOUT_MS);
			connection.setReadTimeout(TIMEOUT_MS);
			connection.setInstanceFollowRedirects(true);
			connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Minecraft)");
			connection.setRequestProperty("Accept", "image/png");
			connection.connect();

			final int responseCode = connection.getResponseCode();
			if (responseCode < 200 || responseCode >= 300) {
				LOGGER.error("RouteMapOverride: HTTP {} for {}", responseCode, url);
				return null;
			}

			inputStream = connection.getInputStream();
			final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			final byte[] buffer = new byte[8192];
			int read;
			while ((read = inputStream.read(buffer)) != -1) {
				outputStream.write(buffer, 0, read);
				if (outputStream.size() > MAX_DOWNLOAD_BYTES) {
					LOGGER.error("RouteMapOverride: response too large for {}", url);
					return null;
				}
			}

			final byte[] bytes = outputStream.toByteArray();
			if (!isPng(bytes)) {
				LOGGER.error("RouteMapOverride: {} is not a png file", url);
				return null;
			}
			return bytes;
		} catch (Exception e) {
			LOGGER.error("RouteMapOverride: failed to download {}", url, e);
			return null;
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (Exception ignored) {
				}
			}
		}
	}

	private static boolean isPng(byte[] bytes) {
		if (bytes.length < PNG_SIGNATURE.length) {
			return false;
		}
		for (int i = 0; i < PNG_SIGNATURE.length; i++) {
			if (bytes[i] != PNG_SIGNATURE[i]) {
				return false;
			}
		}
		return true;
	}
}
