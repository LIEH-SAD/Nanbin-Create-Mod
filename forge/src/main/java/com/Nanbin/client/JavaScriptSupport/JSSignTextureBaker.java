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
	private static final int MAX_ENTRIES = 192;
	private static final long TEXTURE_DESTROY_DELAY_MS = 3000L;
	private static final Identifier WHITE_TEXTURE = new Identifier("mtr", "textures/block/white.png");

	private static final Map<String, Baked> CACHE = new HashMap<>();
	private static final Map<String, Long> FAILURES = new HashMap<>();
	private static final Map<Identifier, NativeImage> IMAGE_CACHE = new HashMap<>();
	/** 贴图缓存代际：资源重载 +1，用于让外部派生的重着色缓存失效。 */
	private static int imageVersion = 0;
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

	/** 每世界单位烘焙的像素数（供其它烘焙管线计算贴图分辨率）。 */
	public static float getPixelsPerWorldUnit() {
		return PX_PER_WORLD;
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
		return bake(signKey, contentKey, originX, originY, width, height, 0.0F, painter);
	}

	/**
	 * 同上，但允许调用方指定「每坐标单位烘焙多少像素」。
	 *
	 * <p>CRT 导视牌自由布局的坐标是「设计像素」，希望 1 设计像素 ≈ 1 贴图像素，
	 * 因此传 {@code pxPerUnit = 1}（宽高也直接用设计像素），而不是 0 走世界单位自适应。
	 * 传入 &lt;= 0 时行为与原方法完全一致（按 {@link #PX_PER_WORLD} 自适应）。</p>
	 *
	 * @param pxPerUnit 每坐标单位的烘焙像素数；&lt;= 0 表示按世界单位自动计算
	 */
	@Nullable
	public static Baked bake(String signKey, long contentKey, float originX, float originY, float width, float height, float pxPerUnit, Runnable painter) {
		return bake(signKey, contentKey, originX, originY, width, height, pxPerUnit, 0L, painter);
	}

	/**
	 * 完整版：额外指定重烘焙间隔。
	 *
	 * <p>JS 脚本牌面有动画/时钟，需要每秒重烘；而 CRT 自由布局是静态内容，
	 * 内容指纹不变时没必要每秒重烘（内容一变仍会立即重烘）。调用方按需传更大的间隔。</p>
	 *
	 * @param refreshMs 内容不变时的最长复用时间（毫秒）；&lt;= 0 表示默认 {@link #REFRESH_MS}
	 */
	@Nullable
	public static Baked bake(String signKey, long contentKey, float originX, float originY, float width, float height, float pxPerUnit, long refreshMs, Runnable painter) {
		if (width <= 0.0F || height <= 0.0F) {
			return null;
		}

		final long now = System.currentTimeMillis();
		processPendingDestroy(now);

		final long maxAge = refreshMs > 0L ? refreshMs : REFRESH_MS;
		final Baked cached = CACHE.get(signKey);
		if (cached != null && cached.contentKey == contentKey && now - cached.bakedAt < maxAge) {
			return cached;
		}

		final Long failedAt = FAILURES.get(signKey);
		if (failedAt != null && now - failedAt < FAILURE_BACKOFF_MS) {
			return null;
		}

		final float density;
		if (pxPerUnit > 0.0F) {
			// 调用方指定分辨率（自由布局：1 设计像素 = 1 贴图像素）
			density = pxPerUnit;
		} else {
			float autoDensity = PX_PER_WORLD;
			while (width * autoDensity > MAX_CANVAS_WIDTH && autoDensity > MIN_PX_PER_WORLD) {
				autoDensity /= 2.0F;
			}
			density = autoDensity;
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

	/** 取贴图像素（已缓存）；给图标重着色等外部派生用途使用。 */
	@Nullable
	public static NativeImage getTextureImage(Identifier textureId) {
		return getImage(textureId);
	}

	/** 贴图缓存代际（资源重载会变化）。 */
	public static int getImageVersion() {
		return imageVersion;
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
		imageVersion++;
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
		public void drawTextureFlipped(Identifier textureId, float x, float y, float width, float height, int color) {
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
			canvas.drawImageFlipped(image, x, y, width, height, color);
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

		@Override
		public void fillRect(float x, float y, float width, float height, int color) {
			if (width <= 0.0F || height <= 0.0F) {
				return;
			}
			canvas.fillRect(x, y, width, height, color == -1 ? 0xFFFFFFFF : color);
		}

		@Override
		public void fillPolygon(float[] xs, float[] ys, int color) {
			if (xs == null || ys == null || xs.length < 3 || xs.length != ys.length) {
				return;
			}
			canvas.fillPolygon(xs, ys, color == -1 ? 0xFFFFFFFF : color);
		}

		@Override
		public void drawTextureRotated(Identifier textureId, float centerX, float centerY, float width, float height, float radians, int color) {
			if (width <= 0.0F || height <= 0.0F) {
				return;
			}
			if (WHITE_TEXTURE.equals(textureId)) {
				canvas.fillRotatedRect(centerX, centerY, width, height, radians, color == -1 ? 0xFFFFFFFF : color);
				return;
			}
			final NativeImage image = getImage(textureId);
			if (image == null) {
				complete = false;
				return;
			}
			canvas.drawImageRotated(image, centerX, centerY, width, height, radians, color);
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

		/** 与 {@link #drawImage} 相同，但按贴图源水平镜像（flipTexture）。 */
		private void drawImageFlipped(NativeImage image, float x, float y, float w, float h, int tint) {
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
					final int sourceX = sourceWidth - 1 - (int) ((long) (px - x0) * sourceWidth / destWidth);
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

		/** 实心多边形光栅化：扫描线交点 + 2×2 超采样做边缘抗锯齿（箭头元素用）。 */
		private void fillPolygon(float[] xs, float[] ys, int argb) {
			final int pointCount = xs.length;
			if (pointCount < 3) {
				return;
			}
			// 转换到画布像素坐标
			final float[] pointX = new float[pointCount];
			final float[] pointY = new float[pointCount];
			float minX = Float.MAX_VALUE;
			float minY = Float.MAX_VALUE;
			float maxX = -Float.MAX_VALUE;
			float maxY = -Float.MAX_VALUE;
			for (int i = 0; i < pointCount; i++) {
				pointX[i] = (xs[i] - originX) * density;
				pointY[i] = (ys[i] - originY) * density;
				minX = Math.min(minX, pointX[i]);
				minY = Math.min(minY, pointY[i]);
				maxX = Math.max(maxX, pointX[i]);
				maxY = Math.max(maxY, pointY[i]);
			}
			final int x0 = Math.max(0, (int) Math.floor(minX));
			final int y0 = Math.max(0, (int) Math.floor(minY));
			final int x1 = Math.min(width, (int) Math.ceil(maxX));
			final int y1 = Math.min(height, (int) Math.ceil(maxY));
			final int baseAlpha = (argb >>> 24) & 0xFF;
			final int rgb = argb & 0xFFFFFF;
			final float[] sampleOffset = {0.25F, 0.75F};
			for (int py = y0; py < y1; py++) {
				for (int px = x0; px < x1; px++) {
					int insideCount = 0;
					for (final float offsetY : sampleOffset) {
						for (final float offsetX : sampleOffset) {
							if (insidePolygon(px + offsetX, py + offsetY, pointX, pointY)) {
								insideCount++;
							}
						}
					}
					if (insideCount <= 0) {
						continue;
					}
					final int alpha = baseAlpha * insideCount / 4;
					if (alpha > 0) {
						blendPixel(px, py, (alpha << 24) | rgb);
					}
				}
			}
		}

		/** 奇偶规则点在多边形内判定。 */
		private static boolean insidePolygon(float x, float y, float[] pointX, float[] pointY) {
			boolean inside = false;
			for (int i = 0, j = pointX.length - 1; i < pointX.length; j = i++) {
				final boolean crosses = (pointY[i] > y) != (pointY[j] > y);
				if (crosses) {
					final float intersectX = (pointX[j] - pointX[i]) * (y - pointY[i]) / (pointY[j] - pointY[i]) + pointX[i];
					if (x < intersectX) {
						inside = !inside;
					}
				}
			}
			return inside;
		}

		/** 旋转贴图采样：目标像素中心逆旋转回贴图局部坐标后最近邻取样（图标 rotation 用）。 */
		private void drawImageRotated(NativeImage image, float centerX, float centerY, float w, float h, float radians, int tint) {
			final int sourceWidth = image.getWidth();
			final int sourceHeight = image.getHeight();
			if (sourceWidth <= 0 || sourceHeight <= 0) {
				return;
			}
			final float centerPixelX = (centerX - originX) * density;
			final float centerPixelY = (centerY - originY) * density;
			final float halfWidth = w * density / 2.0F;
			final float halfHeight = h * density / 2.0F;
			if (halfWidth <= 0.0F || halfHeight <= 0.0F) {
				return;
			}
			final float cos = (float) Math.cos(radians);
			final float sin = (float) Math.sin(radians);
			final float radius = (float) Math.sqrt(halfWidth * halfWidth + halfHeight * halfHeight) + 1.0F;
			final int x0 = Math.max(0, (int) Math.floor(centerPixelX - radius));
			final int x1 = Math.min(width, (int) Math.ceil(centerPixelX + radius));
			final int y0 = Math.max(0, (int) Math.floor(centerPixelY - radius));
			final int y1 = Math.min(height, (int) Math.ceil(centerPixelY + radius));
			final int tintAbgr = tint == -1 ? -1 : argbToAbgr(tint);
			for (int py = y0; py < y1; py++) {
				final float dy = py + 0.5F - centerPixelY;
				for (int px = x0; px < x1; px++) {
					final float dx = px + 0.5F - centerPixelX;
					// 逆旋转（正向 = SVG rotate 的顺时针）
					final float localX = dx * cos + dy * sin;
					final float localY = -dx * sin + dy * cos;
					if (Math.abs(localX) > halfWidth || Math.abs(localY) > halfHeight) {
						continue;
					}
					final int sourceX = Math.max(0, Math.min(sourceWidth - 1, (int) ((localX + halfWidth) / (2.0F * halfWidth) * sourceWidth)));
					final int sourceY = Math.max(0, Math.min(sourceHeight - 1, (int) ((localY + halfHeight) / (2.0F * halfHeight) * sourceHeight)));
					final int color = image.getColor(sourceX, sourceY);
					blendPixel(px, py, tintAbgr == -1 ? color : applyTint(color, tintAbgr));
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
