package com.Nanbin.client.Drawing;

import com.Nanbin.client.JavaScriptSupport.JSDrawContext;
import com.Nanbin.client.JavaScriptSupport.JSSignEngine;
import com.Nanbin.client.JavaScriptSupport.JSSignTextureBaker;
import com.Nanbin.client.Render.RenderCRTRailwaySign;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;
import org.mtr.mod.render.RenderRailwaySign;
import org.mtr.mod.resource.SignResource;

import java.util.HashMap;
import java.util.Map;

/**
 * CRT 铁路指示牌编辑界面的顶部预览。
 *
 * <p>预览不是另写一套画法：它调用 {@link CRTSignDrawing#drawIntoCanvas(CRTSignView)}，
 * 由 {@link JSSignTextureBaker} 把同一套绘制指令合成贴图后显示。
 * 因此编辑界面看到的画面与实际世界里渲染的画面一致。</p>
 *
 * <p>界面布局参考 CRT-Sign-Web-Editor（Apache License 2.0，见 docs/THIRD_PARTY_NOTICES.md）。</p>
 */
public final class CRTSignPreview {

	/** 预览区域背景（暗色主题）。 */
	public static final int PREVIEW_BACKGROUND = 0xFF14161A;
	/** 牌面外的描边。 */
	private static final int FRAME_COLOR = 0xFF2A3138;
	/** 选中格高亮色。 */
	private static final int SELECTION_COLOR = 0xFF4FC3F7;

	private CRTSignPreview() {
	}

	/**
	 * 在 (x, y, width, height) 区域里按内容比例画整块牌的预览。
	 *
	 * @param cacheKey     预览实例标识（同一块牌用同一个字符串，建议带方块坐标）
	 * @param selectedCell 需要高亮的格号；&lt; 0 表示不高亮
	 */
	public static void draw(GraphicsHolder graphics, float x, float y, float width, float height, CRTSignView view, String cacheKey, int selectedCell) {
		if (view == null || width <= 0.0F || height <= 0.0F) {
			return;
		}
		final GuiDrawing guiDrawing = new GuiDrawing(graphics);

		// 预览区底色
		guiDrawing.beginDrawingRectangle();
		guiDrawing.drawRectangle(x, y, x + width, y + height, PREVIEW_BACKGROUND);

		final float[] rect = contentRect(x, y, width, height, view);
		final float contentX = rect[0];
		final float contentY = rect[1];
		final float contentWidth = rect[2];
		final float contentHeight = rect[3];

		// 牌面底板（贴图烘焙失败时至少还有底色）
		final int background = resolveBackground(view);
		guiDrawing.drawRectangle(contentX, contentY, contentX + contentWidth, contentY + contentHeight, background);
		guiDrawing.finishDrawingRectangle();

		// 内容：走与游戏内相同的绘制类
		final Identifier baked = view.isEmpty() ? null : bake(view, cacheKey);
		if (baked != null) {
			guiDrawing.beginDrawingTexture(baked);
			guiDrawing.drawTexture(contentX, contentY, contentX + contentWidth, contentY + contentHeight, 0.0F, 0.0F, 1.0F, 1.0F);
			guiDrawing.finishDrawingTexture();
		}

		// 选中格高亮 + 牌面描边
		final int cellCount = view.signIds.length;
		if (cellCount > 0) {
			final float cellWidth = contentWidth / cellCount;
			if (selectedCell >= 0 && selectedCell < cellCount) {
				final float cellX = contentX + selectedCell * cellWidth;
				drawBorder(guiDrawing, cellX, contentY, cellX + cellWidth, contentY + contentHeight, 2.0F, SELECTION_COLOR);
			}
			// 空格分隔线，方便看出"格子"边界
			for (int i = 1; i < cellCount; i++) {
				final float lineX = contentX + i * cellWidth;
				drawBorder(guiDrawing, lineX - 0.5F, contentY, lineX + 0.5F, contentY + contentHeight, 0.0F, 0x33FFFFFF);
			}
		}
		drawBorder(guiDrawing, contentX, contentY, contentX + contentWidth, contentY + contentHeight, 1.5F, FRAME_COLOR);
	}

	/** 内容区（按比例居中）在预览区域内的位置：{x0, y0, width, height}。 */
	public static float[] contentRect(float x, float y, float width, float height, CRTSignView view) {
		final float contentWidth = Math.max(0.001F, view.width());
		final float contentHeight = Math.max(0.001F, view.cellSize);
		final float margin = 6.0F;
		final float availableWidth = Math.max(1.0F, width - margin * 2.0F);
		final float availableHeight = Math.max(1.0F, height - margin * 2.0F);
		final float scale = Math.min(availableWidth / contentWidth, availableHeight / contentHeight);
		final float drawWidth = contentWidth * scale;
		final float drawHeight = contentHeight * scale;
		return new float[]{x + (width - drawWidth) / 2.0F, y + (height - drawHeight) / 2.0F, drawWidth, drawHeight};
	}

	/** 鼠标点到的格号；点在牌面外返回 -1。 */
	public static int cellAt(double mouseX, double mouseY, float x, float y, float width, float height, CRTSignView view) {
		if (view == null || view.signIds.length == 0) {
			return -1;
		}
		final float[] rect = contentRect(x, y, width, height, view);
		if (mouseX < rect[0] || mouseX > rect[0] + rect[2] || mouseY < rect[1] || mouseY > rect[1] + rect[3]) {
			return -1;
		}
		final int cell = (int) ((mouseX - rect[0]) / (rect[2] / view.signIds.length));
		return Math.max(0, Math.min(view.signIds.length - 1, cell));
	}

	/** 把整块牌烘焙成一张贴图（同一块牌的预览共用缓存，内容变化时立即重烘焙）。 */
	private static Identifier bake(CRTSignView view, String cacheKey) {
		if (view.jsStyleScriptId != null) {
			return bakeJsStyle(view, cacheKey);
		}
		final JSSignTextureBaker.Baked baked = JSSignTextureBaker.bake(
				"crt_preview:" + cacheKey,
				view.contentKey(),
				0.0F,
				0.0F,
				Math.max(0.001F, view.width()),
				view.cellSize,
				() -> CRTSignDrawing.drawIntoCanvas(view)
		);
		return baked == null ? null : baked.identifier;
	}

	/** JS 样式：与游戏内一样用 JSSignEngine 产出元素后烘焙；这里只是换成预览用的缓存键。 */
	private static Identifier bakeJsStyle(CRTSignView view, String cacheKey) {
		final String scriptId = view.jsStyleScriptId;
		if (scriptId == null) {
			return null;
		}
		final String signKey = "crt_preview_js:" + cacheKey;
		final float width = Math.max(0.001F, view.width());
		final int background = resolveBackground(view);
		final Map<Long, String> routeNumberMap = new HashMap<>();
		for (final long color : view.selectedIds) {
			final String number = view.routeNumberGetter.apply(color);
			if (number != null && !number.isEmpty()) {
				routeNumberMap.put(color, number);
			}
		}
		final JSSignTextureBaker.Baked baked = JSSignTextureBaker.bake(
				signKey,
				view.contentKey(),
				0.0F,
				0.0F,
				width,
				view.cellSize,
				() -> JSSignEngine.render(
						scriptId,
						signKey,
						new JSDrawContext(null, view.facing, 0.0F, 0.0F, view.cellSize, view.signIds.length, false, background, width),
						new JSSignEngine.JSSignAPI(
								new JSSignEngine.JSSignContext(view.signIds, 0, view.selectedIds, view.routeNumbers, background, view.cellSize, signKey, BlockPos.fromLong(0L), routeNumberMap, width),
								scriptId
						)
				)
		);
		return baked == null ? null : baked.identifier;
	}

	/** 牌面底色：取第一个有底色的标志，否则白底；JS 样式默认黑底。 */
	private static int resolveBackground(CRTSignView view) {
		for (final String signId : view.signIds) {
			if (signId != null && !signId.isEmpty()) {
				final SignResource sign = RenderRailwaySign.getSign(signId);
				if (sign != null && sign.getBackgroundColor() != 0) {
					return sign.getBackgroundColor() | 0xFF000000;
				}
			}
		}
		return view.jsStyleScriptId != null ? 0xFF000000 : 0xFFFFFFFF;
	}

	private static void drawBorder(GuiDrawing guiDrawing, float x1, float y1, float x2, float y2, float thickness, int color) {
		guiDrawing.beginDrawingRectangle();
		final float t = Math.max(1.0F, thickness);
		guiDrawing.drawRectangle(x1, y1, x2, y1 + t, color);
		guiDrawing.drawRectangle(x1, y2 - t, x2, y2, color);
		guiDrawing.drawRectangle(x1, y1, x1 + t, y2, color);
		guiDrawing.drawRectangle(x2 - t, y1, x2, y2, color);
		guiDrawing.finishDrawingRectangle();
	}

	/** 供屏幕在预览为空时显示提示用（避免屏幕自己去解析白名单）。 */
	public static boolean hasWhitelistedSign(String signId) {
		return signId != null && !signId.isEmpty() && (RenderRailwaySign.getSign(signId) != null || com.Nanbin.client.ClientData.CRTSignRegistry.getSign(signId) != null);
	}

	/** 未使用：保留以便以后按牌面尺寸算缩放。 */
	@SuppressWarnings("unused")
	private static LongAVLTreeSet copySelected(LongAVLTreeSet source) {
		return source == null ? new LongAVLTreeSet() : new LongAVLTreeSet(source);
	}
}
