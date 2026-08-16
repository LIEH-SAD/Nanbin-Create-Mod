package com.Nanbin.client.Drawing;

import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.MinecraftClient;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.Nanbin.InitClient.LOGGER;

/**
 * 网络图片加载器：在后台线程用 HttpURLConnection 下载图片字节流，
 * 再用 ImageIO.read 解析（原生支持 PNG / JPEG / BMP 三大格式），
 * 最后回到渲染线程注册为动态纹理并缓存，供方块渲染使用。
 *
 * 用法（渲染器中）：
 * <pre>
 * WebImageCache.instance.request(url);        // 触发异步下载（内部去重，可每帧调用）
 * Identifier id = WebImageCache.instance.get(url); // 未下载完成时返回 null
 * </pre>
 */
public class WebImageCache {

	public static final WebImageCache instance = new WebImageCache();

	/** 连接 / 读取超时（毫秒） */
	private static final int TIMEOUT_MS = 10000;
	/** 图片最大尺寸（像素），防止超大图片拖垮渲染线程 */
	private static final int MAX_IMAGE_DIMENSION = 4096;
	/** 下载字节数上限（约 16MB） */
	private static final int MAX_DOWNLOAD_BYTES = 16 * 1024 * 1024;
	/** 下载失败后的重试间隔（毫秒），避免每帧反复请求刷屏日志 */
	private static final long RETRY_INTERVAL_MS = 60_000L;

	/** url -> 已注册的动态纹理标识（仅渲染线程写入，读取线程安全） */
	private final Map<String, Identifier> imageCache = new ConcurrentHashMap<>();
	/** 正在下载中的 url 集合（去重，避免重复发起请求） */
	private final Set<String> loadingUrls = ConcurrentHashMap.newKeySet();
	/** url -> 上次失败时间戳（毫秒），失败后进入冷却期，冷却期内不重试、不报错 */
	private final Map<String, Long> failedUrls = new ConcurrentHashMap<>();

	private WebImageCache() {
	}

	/** 返回 url 对应的已下载纹理；未下载或失败时返回 null。渲染线程调用。 */
	public Identifier get(String url) {
		if (url == null || url.isEmpty()) {
			return null;
		}
		return imageCache.get(url);
	}

	/** 请求异步下载 url 的图片。可每帧调用：已在缓存/下载中/失败冷却期内时自动忽略。 */
	public void request(String url) {
		if (url == null || url.isEmpty()) {
			return;
		}
		if (imageCache.containsKey(url) || loadingUrls.contains(url)) {
			return;
		}
		// 失败冷却期内不重试，避免每帧反复下载刷屏报错（最多每分钟一次）
		final Long lastFailedTime = failedUrls.get(url);
		if (lastFailedTime != null && System.currentTimeMillis() - lastFailedTime < RETRY_INTERVAL_MS) {
			return;
		}
		loadingUrls.add(url);
		final Thread thread = new Thread(() -> download(url), "Nanbin WebImage " + url);
		thread.setDaemon(true);
		thread.start();
	}

	/** 下载失败时清除标记，使后续 request 可重试。 */
	public void forget(String url) {
		if (url != null) {
			imageCache.remove(url);
			loadingUrls.remove(url);
			failedUrls.remove(url);
		}
	}

	/** 记录失败时间戳，进入冷却期。 */
	private void recordFailure(String url) {
		failedUrls.put(url, System.currentTimeMillis());
	}

	private void download(String url) {
		boolean success = false;
		InputStream inputStream = null;
		try {
			final HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
			connection.setConnectTimeout(TIMEOUT_MS);
			connection.setReadTimeout(TIMEOUT_MS);
			connection.setInstanceFollowRedirects(true);
			connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Minecraft)");
			connection.setRequestProperty("Accept", "image/png,image/jpeg,image/bmp,*/*");
			connection.connect();

			final int responseCode = connection.getResponseCode();
			if (responseCode < 200 || responseCode >= 300) {
				LOGGER.error("WebImageCache: HTTP {} for {}", responseCode, url);
				recordFailure(url);
				return;
			}

			inputStream = connection.getInputStream();
			final byte[] bytes = readAll(inputStream);
			if (bytes.length > MAX_DOWNLOAD_BYTES) {
				LOGGER.error("WebImageCache: response too large ({} bytes) for {}", bytes.length, url);
				recordFailure(url);
				return;
			}

			final BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
			if (image == null) {
				LOGGER.error("WebImageCache: ImageIO could not decode {} (only png/jpg/bmp are supported)", url);
				recordFailure(url);
				return;
			}
			if (image.getWidth() > MAX_IMAGE_DIMENSION || image.getHeight() > MAX_IMAGE_DIMENSION) {
				LOGGER.error("WebImageCache: image {} too large ({}x{}), max is {}", url, image.getWidth(), image.getHeight(), MAX_IMAGE_DIMENSION);
				recordFailure(url);
				return;
			}

			final org.mtr.mapping.holder.NativeImage nativeImage = FontRenderUtils.toNativeImage(image);
			// 动态纹理必须在渲染线程注册
			MinecraftClient.getInstance().execute(() -> {
				try {
					final Identifier id = CustomFontTextureCache.instance.registerWebTexture(nativeImage);
					imageCache.put(url, id);
				} catch (Exception e) {
					LOGGER.error("WebImageCache: failed to register texture for {}", url, e);
					recordFailure(url);
				} finally {
					loadingUrls.remove(url);
				}
			});
			success = true;
		} catch (Exception e) {
			LOGGER.error("WebImageCache: failed to load image from {}", url, e);
			recordFailure(url);
		} finally {
			try {
				if (inputStream != null) {
					inputStream.close();
				}
			} catch (Exception ignored) {
			}
			if (!success) {
				loadingUrls.remove(url);
			}
		}
	}

	private static byte[] readAll(InputStream inputStream) throws Exception {
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		final byte[] buffer = new byte[8192];
		int read;
		while ((read = inputStream.read(buffer)) != -1) {
			outputStream.write(buffer, 0, read);
			if (outputStream.size() > MAX_DOWNLOAD_BYTES) {
				throw new Exception("Response too large");
			}
		}
		return outputStream.toByteArray();
	}
}
