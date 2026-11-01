package com.Nanbin.client.JavaScriptSupport;

import com.Nanbin.InitClient;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.NativeImage;
import org.mtr.mapping.holder.NativeImageBackedTexture;
import org.mtr.mapping.mapper.ResourceManagerHelper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 把一帧 JS 指示牌的所有绘制元素烘焙成一张动态贴图。
 *
 * <p>脚本只负责生产元素，元素按绘制顺序合成进一张 NativeImage，牌面最后只画这一张贴图。
 * 这样元素之间的遮挡只由合成顺序决定，不会因为逐个 scheduleRender 的批次/深度问题丢失。
 */
public final class JSSignTextureBaker {

	// 每世界单位烘焙的像素数：与 CustomFontTextureCache 的文字贴图分辨率对齐（0.5 高 = 88px），
	// 合成时基本 1:1 不会糊。过宽时逐级减半，避免贴图失控。
	private static final float PX_PER_WORLD = 176.0F;
	private static final int MAX_CANVAS_WIDTH = 3072;
	private static final float MIN_PX_PER_WORLD = 32.0F;
	// 定时重烘焙，保证时钟、动画类脚本能刷新。
	private static final long REFRESH_MS = 1000L;
	// 取不到某些元素像素时暂停烘焙的时间，期间上层回退到逐元素渲染。
	private static final long FAILURE_BACKOFF_MS = 5000L;
	private static final int MAX_ENTRIES = 64;
	private static final long TEXTURE_DESTROY_DELAY_MS = 3000L;
	private static final Identifier WHITE_TEXTURE = new Identifier("mtr", "textures/block/white.png");

	private static final Map<String, Baked> CACHE = new HashMap<>();
	private static final Map<String, Long> FAILURES = new HashMap<>();
	private static final Map<Identifier, NativeImage> IMAGE_CACHE = new HashMap<>();
	private static final Set<Identifier> MISSING_IMAGES = new HashSet<>();
	private static final Map<Identifier, Long> PENDING_DESTROY = new HashMap<>();
	private static int textureCounter = 0;

	private JSSignTextureBaker() {
	}

	public static final class Baked {
		public final Identifier identifier;
		public final float width;
		public final float height;
		private final NativeImage image;
		private final NativeImageBackedTexture texture;
		private long contentKey;
		private long bakedAt;

		private Baked(Identifier identifier, NativeImageBackedTexture texture, NativeImage image, float width, float height, long contentKey, long bakedAt) {
			this.identifier = identifier;
			this.texture = texture;
			this.image = image;
			this.width = width;
			this.height = height;
			this.contentKey = contentKey;
			this.bakedAt = bakedAt;
		}
	}

	/**
	 * 取（必要时重烘焙）某块 JS 牌面的贴图。
	 *
	 * @param signKey    牌面实例标识（同脚本不同牌面各自独立）
	 * @param contentKey 内容指纹（哈希），变化时立即重烘焙
	 * @param originX    牌面内容区左上角在脚本坐标系中的 x
	 * @param originY    牌面内容区左上角在脚本坐标系中的 y
	 * @param width      内容区宽（世界单位）
	 * @param height     内容区高（世界单位）
	 * @param painter    运行脚本的执行体（内部会以收集模式调用各 render 方法）
	 * @return 烘焙结果；无法烘焙时返回 null，调用方应回退到逐元素渲染
	 */
	@Nullable
	public static Baked bake(String signKey, long contentKey, float originX, float originY, float width, float height, Runnable painter) {
		if (width <= 0.0F || height <= 0.0F) {
			return null;
		}

		final long now = System.currentTimeMillis();
		processPendingDestroy(now);

		final Baked cached = CACHE.get(signKey);
		if (cached != null && cached.contentKey == contentKey && now - cached.bakedAt < REFRESH_MS) {
			return cached;
		}

		final Long failedAt = FAILURES.get(signKey);
		if (failedAt != null && now - failedAt < FAILURE_BACKOFF_MS) {
			return null;
		}

		float density = PX_PER_WORLD;
		while (width * density > MAX_CANVAS_WIDTH && density > MIN_PX_PER_WORLD) {
			density /= 2.0F;
		}

		final Canvas canvas = new Canvas(Math.max(1, Math.round(width * density)), Math.max(1, Math.round(height * density)), originX, originY, density);
		final CanvasSink sink = new CanvasSink(canvas);
		JSDrawSink.begin(sink);
		try {
			painter.run();
		} catch (Throwable e) {
			InitClient.LOGGER.error("Failed to bake JS sign texture for {}", signKey, e);
		} finally {
			JSDrawSink.end();
		}

		if (!sink.complete) {
			// 有元素取不到像素（例如资源包里没有这个贴图）：继续用上一张烘焙结果，或让上层回退
			FAILURES.put(signKey, now);
			return cached;
		}
		FAILURES.remove(signKey);

		// 尺寸没变时复用同一张纹理，只重新上传像素，避免每秒新建纹理
		if (cached != null && cached.image.getWidth() == canvas.getWidth() && cached.image.getHeight() == canvas.getHeight()) {
			canvas.writeTo(cached.image);
			cached.texture.upload();
			cached.contentKey = contentKey;
			cached.bakedAt = now;
			return cached;
		}

		final NativeImage image = canvas.toNativeImage();
		final NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
		final Identifier identifier = MinecraftClient.getInstance().getTextureManager().registerDynamicTexture("nanbin_js_bake_" + (textureCounter++), texture);
		final Baked baked = new Baked(identifier, texture, image, width, height, contentKey, now);
		final Baked previous = CACHE.put(signKey, baked);
		if (previous != null) {
			PENDING_DESTROY.put(previous.identifier, now);
		}
		trimCache(now);
		return baked;
	}

	private static void trimCache(long now) {
		if (CACHE.size() <= MAX_ENTRIES) {
			return;
		}
		final ArrayList<Map.Entry<String, Baked>> entries = new ArrayList<>(CACHE.entrySet());
		entries.sort((a, b) -> Long.compare(a.getValue().bakedAt, b.getValue().bakedAt));
		final int removeCount = entries.size() - MAX_ENTRIES;
		for (int i = 0; i < removeCount; i++) {
			final Baked removed = CACHE.remove(entries.get(i).getKey());
			if (removed != null) {
				PENDING_DESTROY.put(removed.identifier, now);
			}
		}
	}

	// 纹理可能还被这一帧的渲染队列引用，延迟销毁。
	private static void processPendingDestroy(long now) {
		if (PENDING_DESTROY.isEmpty()) {
			return;
		}
		PENDING_DESTROY.entrySet().removeIf(entry -> {
			if (now - entry.getValue() < TEXTURE_DESTROY_DELAY_MS) {
				return false;
			}
			try {
				MinecraftClient.getInstance().getTextureManager().destroyTexture(entry.getKey());
			} catch (Exception ignored) {
			}
			return true;
		});
	}

	/** 取贴图像素：按资源路径读 PNG 并缓存。 */
	@Nullable
	private static NativeImage getImage(Identifier textureId) {
		if (IMAGE_CACHE.containsKey(textureId)) {
			return IMAGE_CACHE.get(textureId);
		}
		if (MISSING_IMAGES.contains(textureId)) {
			return null;
		}
		final NativeImage loaded = loadImage(textureId);
		if (loaded == null) {
			MISSING_IMAGES.add(textureId);
		} else {
			IMAGE_CACHE.put(textureId, loaded);
		}
		return loaded;
	}

	@Nullable
	private static NativeImage loadImage(Identifier textureId) {
		try {
			String path = textureId.getPath();
			if (!path.endsWith(".png")) {
				path = path + ".png";
			}
			final Identifier resourceId = new Identifier(textureId.getNamespace(), path);
			final NativeImage[] holder = new NativeImage[1];
			ResourceManagerHelper.readResource(resourceId, stream -> {
				try {
					holder[0] = NativeImage.read(stream);
				} catch (Exception ignored) {
				}
			});
			return holder[0];
		} catch (Exception e) {
			InitClient.LOGGER.warn("Failed to load JS sign texture: {}", textureId, e);
			return null;
		}
	}

	/** 资源重载后清空，避免继续用已失效的像素缓存。 */
	public static void clearCaches() {
		for (final Baked baked : CACHE.values()) {
			PENDING_DESTROY.put(baked.identifier, System.currentTimeMillis());
		}
		CACHE.clear();
		FAILURES.clear();
		IMAGE_CACHE.clear();
		MISSING_IMAGES.clear();
	}

	private static final class CanvasSink extends JSDrawSink {

		private final Canvas canvas;
		private boolean complete = true;

		private CanvasSink(Canvas canvas) {
			this.canvas = canvas;
		}

		@Override
		public void drawTexture(Identifier textureId, float x, float y, float width, float height, int color) {
			if (width <= 0.0F || height <= 0.0F) {
				return;
			}
			if (WHITE_TEXTURE.equals(textureId)) {
				canvas.fillRect(x, y, width, height, color == -1 ? 0xFFFFFFFF : color);
				return;
			}
			final NativeImage image = getImage(textureId);
			if (image == null) {
				complete = false;
				return;
			}
			canvas.drawImage(image, x, y, width, height, color);
		}

		@Override
		public void drawImage(NativeImage image, float x, float y, float width, float height, int color) {
			if (width > 0.0F && height > 0.0F) {
				canvas.drawImage(image, x, y, width, height, color);
			}
		}

		@Override
		public void drawRotatedQuad(float centerX, float centerY, float width, float height, float radians, int color) {
			if (width <= 0.0F || height <= 0.0F) {
				return;
			}
			canvas.fillRotatedRect(centerX, centerY, width, height, radians, color == -1 ? 0xFFFFFFFF : color);
		}
	}

	/** 直通式 ARGB 画布，内部按 NativeImage 的 ABGR 存储。 */
	private static final class Canvas {

		private final int width;
		private final int height;
		private final float originX;
		private final float originY;
		private final float density;
		private final int[] pixels;

		private Canvas(int width, int height, float originX, float originY, float density) {
			this.width = width;
			this.height = height;
			this.originX = originX;
			this.originY = originY;
			this.density = density;
			this.pixels = new int[width * height];
		}

		private int toPixelX(float worldX) {
			return Math.round((worldX - originX) * density);
		}

		private int toPixelY(float worldY) {
			return Math.round((worldY - originY) * density);
		}

		private void fillRect(float x, float y, float w, float h, int argb) {
			final int x0 = Math.max(0, toPixelX(x));
			final int y0 = Math.max(0, toPixelY(y));
			final int x1 = Math.min(width, toPixelX(x + w));
			final int y1 = Math.min(height, toPixelY(y + h));
			final int abgr = argbToAbgr(argb);
			for (int py = y0; py < y1; py++) {
				for (int px = x0; px < x1; px++) {
					blendPixel(px, py, abgr);
				}
			}
		}

		private void drawImage(NativeImage image, float x, float y, float w, float h, int tint) {
			final int sourceWidth = image.getWidth();
			final int sourceHeight = image.getHeight();
			if (sourceWidth <= 0 || sourceHeight <= 0) {
				return;
			}
			final int x0 = toPixelX(x);
			final int y0 = toPixelY(y);
			final int x1 = toPixelX(x + w);
			final int y1 = toPixelY(y + h);
			final int destWidth = x1 - x0;
			final int destHeight = y1 - y0;
			if (destWidth <= 0 || destHeight <= 0) {
				return;
			}
			final int tintAbgr = tint == -1 ? -1 : argbToAbgr(tint);
			for (int py = Math.max(0, y0); py < Math.min(height, y1); py++) {
				final int sourceY = (int) ((long) (py - y0) * sourceHeight / destHeight);
				for (int px = Math.max(0, x0); px < Math.min(width, x1); px++) {
					final int sourceX = (int) ((long) (px - x0) * sourceWidth / destWidth);
					final int color = image.getColor(sourceX, sourceY);
					blendPixel(px, py, tintAbgr == -1 ? color : applyTint(color, tintAbgr));
				}
			}
		}

		private void fillRotatedRect(float centerX, float centerY, float w, float h, float radians, int argb) {
			final float halfW = w * density / 2.0F;
			final float halfH = h * density / 2.0F;
			final float centerPxX = (centerX - originX) * density;
			final float centerPxY = (centerY - originY) * density;
			final float cos = (float) Math.cos(-radians);
			final float sin = (float) Math.sin(-radians);
			final float radius = (float) Math.sqrt(halfW * halfW + halfH * halfH) + 1.0F;
			final int abgr = argbToAbgr(argb);
			final int x0 = Math.max(0, (int) Math.floor(centerPxX - radius));
			final int x1 = Math.min(width, (int) Math.ceil(centerPxX + radius));
			final int y0 = Math.max(0, (int) Math.floor(centerPxY - radius));
			final int y1 = Math.min(height, (int) Math.ceil(centerPxY + radius));
			for (int py = y0; py < y1; py++) {
				final float dy = py + 0.5F - centerPxY;
				for (int px = x0; px < x1; px++) {
					final float dx = px + 0.5F - centerPxX;
					final float localX = dx * cos - dy * sin;
					final float localY = dx * sin + dy * cos;
					if (Math.abs(localX) <= halfW + 0.5F && Math.abs(localY) <= halfH + 0.5F) {
						blendPixel(px, py, abgr);
					}
				}
			}
		}

		private void blendPixel(int x, int y, int abgr) {
			final int sourceAlpha = (abgr >>> 24) & 0xFF;
			if (sourceAlpha <= 0) {
				return;
			}
			final int index = y * width + x;
			if (sourceAlpha >= 255) {
				pixels[index] = abgr;
				return;
			}
			final int destination = pixels[index];
			final int destinationAlpha = (destination >>> 24) & 0xFF;
			final float sa = sourceAlpha / 255.0F;
			final float da = destinationAlpha / 255.0F * (1.0F - sa);
			final float outAlpha = sa + da;
			if (outAlpha <= 0.0F) {
				pixels[index] = 0;
				return;
			}
			final int r = Math.min(255, Math.round((((abgr & 0xFF) * sa) + ((destination & 0xFF) * da)) / outAlpha));
			final int g = Math.min(255, Math.round(((((abgr >>> 8) & 0xFF) * sa) + (((destination >>> 8) & 0xFF) * da)) / outAlpha));
			final int b = Math.min(255, Math.round(((((abgr >>> 16) & 0xFF) * sa) + (((destination >>> 16) & 0xFF) * da)) / outAlpha));
			final int a = Math.min(255, Math.round(outAlpha * 255.0F));
			pixels[index] = (a << 24) | (b << 16) | (g << 8) | r;
		}

		private int getWidth() {
			return width;
		}

		private int getHeight() {
			return height;
		}

		// 全量写入：NativeImage(w, h, false) 不保证清零
		private void writeTo(NativeImage image) {
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					image.setPixelColor(x, y, pixels[y * width + x]);
				}
			}
		}

		private NativeImage toNativeImage() {
			final NativeImage image = new NativeImage(width, height, false);
			writeTo(image);
			return image;
		}

		// ARGB -> ABGR 字节重排（NativeImage 的像素接口用 ABGR）
		private static int argbToAbgr(int argb) {
			return (argb & 0xFF00FF00) | ((argb & 0xFF) << 16) | ((argb >>> 16) & 0xFF);
		}

		// 贴图颜色 × 染色（染色为 ABGR）
		private static int applyTint(int abgr, int tintAbgr) {
			final int a = ((abgr >>> 24) & 0xFF) * ((tintAbgr >>> 24) & 0xFF) / 255;
			final int b = ((abgr >>> 16) & 0xFF) * ((tintAbgr >>> 16) & 0xFF) / 255;
			final int g = ((abgr >>> 8) & 0xFF) * ((tintAbgr >>> 8) & 0xFF) / 255;
			final int r = (abgr & 0xFF) * (tintAbgr & 0xFF) / 255;
			return (a << 24) | (b << 16) | (g << 8) | r;
		}
	}
}
