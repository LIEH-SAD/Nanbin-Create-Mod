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


public final class RouteMapOverride {

	public static final Identifier ROUTE_MAP_ID = new Identifier("mtr", "textures/texture/route_map.png");

	private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
	private static final int TIMEOUT_MS = 10_000;
	private static final int MAX_DOWNLOAD_BYTES = 16 * 1024 * 1024;

	private static volatile String requestedUrl = "";
	private static volatile byte[] pngBytes;
	@Nullable
	private static AbstractTexture activeTexture;
	@Nullable
	private static volatile String pendingDownloadUrl;
	@Nullable
	private static volatile byte[] pendingDownloadBytes;

	private RouteMapOverride() {
	}

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

	public static void tick() {
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
			// 客户端tick 处理
			pendingDownloadUrl = url;
			pendingDownloadBytes = bytes;
			return;
		}

		client.execute(() -> finishDownload(url, bytes));
	}

	private static void finishDownload(String url, @Nullable byte[] bytes) {
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
		// 底层纹理对象接上
		final NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
		replaceTexture(new AbstractTexture(texture.data));
	}

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
		// 重建默认纹理
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
