package com.Nanbin.client.Drawing;

import com.Nanbin.client.JavaScriptSupport.JSDrawSink;
import com.Nanbin.client.Render.RenderCRTRailwaySign;
import org.mtr.core.data.SimplifiedRoute;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectBidirectionalIterator;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mod.client.IDrawing;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.render.MainRenderer;
import org.mtr.mod.render.QueuedRenderLayer;
import org.mtr.mod.render.RenderRailwaySign;
import org.mtr.mod.render.StoredMatrixTransformations;
import org.mtr.mod.resource.SignResource;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * CRT 铁路指示牌的绘制类：把一块牌的底板、各格标志、线路编号区域画出来。
 *
 * <p><b>预览与实际渲染共用这一处实现</b>：
 * <ul>
 *   <li>世界渲染：{@link com.Nanbin.client.Render.RenderCRTRailwaySign} 传入
 *       {@link StoredMatrixTransformations}，绘制指令进入 MTR 的渲染队列；</li>
 *   <li>界面预览：{@link CRTSignPreview} 在 {@link com.Nanbin.client.JavaScriptSupport.JSSignTextureBaker}
 *       的画布上下文中调用 {@link #drawIntoCanvas(CRTSignView)}，同一套指令被合成成一张贴图后显示。</li>
 * </ul>
 * 两种模式的分支只在于"贴图/矩形画到哪里"，几何与内容解析完全一致。</p>
 *
 * <p>界面布局与元素分类参考了开源项目 CRT-Sign-Web-Editor（Apache License 2.0，
 * 原作者 endcreeper861 的 JR-Guidance-Sign-Web-Editor，PrisamaX0124 修改版）：
 * https://github.com/prisamax0124/CRT-Sign-Web-Editor 。本类是 Minecraft 端的 Java 重实现，
 * 只沿用其界面组织方式和重庆轨道交通导视风格，未复制其源码。</p>
 */
public final class CRTSignDrawing {

	private static final Identifier WHITE_TEXTURE = new Identifier("mtr", "textures/block/white.png");
	private static final BlockPos ZERO_POS = BlockPos.fromLong(0L);

	private CRTSignDrawing() {
	}

	/**
	 * 世界渲染入口：把整块牌的内容画进 MTR 的渲染队列。
	 *
	 * @param graphicsHolder 当前方块实体的 GraphicsHolder（MTR 的 drawSign 需要）
	 * @param smt            牌面在世界中的变换
	 * @param drawBackground 是否由本类画牌面底板；世界渲染器已单独画底板时传 false，避免重复调度
	 */
	public static void draw(CRTSignView view, GraphicsHolder graphicsHolder, @Nullable StoredMatrixTransformations smt, boolean drawBackground) {
		drawInternal(view, graphicsHolder, smt, drawBackground);
	}

	/**
	 * 预览烘焙入口：调用前必须已设置 {@link JSDrawSink}（由 JSSignTextureBaker 设置）。
	 * 所有绘制都会写进画布，因此预览和世界渲染用的是同一套几何。
	 */
	public static void drawIntoCanvas(CRTSignView view) {
		drawInternal(view, null, null, true);
	}

	private static void drawInternal(CRTSignView view, @Nullable GraphicsHolder graphicsHolder, @Nullable StoredMatrixTransformations smt, boolean drawBackground) {
		final String[] signIds = view.signIds;
		if (signIds.length == 0) {
			return;
		}
		// 每格底色来自 crt_signs.json 的 backgroundColor
		final int[] cellBackgroundColors = new int[signIds.length];
		int firstBackground = 0;
		for (int i = 0; i < signIds.length; i++) {
			if (signIds[i] != null && !signIds[i].isEmpty()) {
				final SignResource sign = RenderRailwaySign.getSign(signIds[i]);
				if (sign != null && sign.getBackgroundColor() != 0) {
					cellBackgroundColors[i] = sign.getBackgroundColor();
					if (firstBackground == 0) {
						firstBackground = sign.getBackgroundColor();
					}
				}
			}
		}
		final boolean isJSStyle = view.jsStyleScriptId != null;
		// CRT 指示牌始终有底板；JS 样式默认黑底，其余默认白底
		final int backgroundColor = (firstBackground != 0 ? firstBackground : (isJSStyle ? 0 : 0xFFFFFF)) | 0xFF000000;
		int globalBackground = 0;
		for (final int color : cellBackgroundColors) {
			if (color != 0) {
				globalBackground = color;
				break;
			}
		}
		if (globalBackground == 0) {
			globalBackground = 0xFFFFFF;
		}

		// 牌面底板
		if (drawBackground) {
			drawTextureZ(WHITE_TEXTURE, 0.0F, 0.0F, 0.003125F, view.width(), view.cellSize, 0.003125F, backgroundColor, view.facing, smt);
		}

		if (isJSStyle) {
			// JS 样式整块交给脚本渲染（预览侧由 CRTSignPreview 单独烘焙），这里不画格子
			return;
		}

		final Map<Long, String> routeNameByColor = buildRouteNameByColor();
		final RenderRailwaySign.DrawTexture textureDrawer = textureDrawer(view.facing, smt);

		for (int i = 0; i < signIds.length; i++) {
			if (signIds[i] != null && !RenderCRTRailwaySign.isRouteNumberV2(signIds[i])) {
				final int cellBg = cellBackgroundColors[i] != 0 ? cellBackgroundColors[i] : 0xFFFFFF;
				final float cellX = view.cellStep * i;
				RenderCRTRailwaySign.drawSignCRT(
						graphicsHolder,
						smt,
						ZERO_POS,
						signIds[i],
						cellX,
						0.0F,
						view.cellSize,
						view.cellStep,
						RenderRailwaySign.getMaxWidth(signIds, i, false),
						RenderRailwaySign.getMaxWidth(signIds, i, true),
						view.selectedIds,
						view.routeNumbers,
						view.facing,
						cellBg,
						textureDrawer,
						false,
						signIds,
						globalBackground
				);
			}
		}
		RenderCRTRailwaySign.renderRouteNumberV2Regions(smt, signIds, view.selectedIds, view.routeNumberGetter, view::cellColor, view.facing, view.cellSize, view.cellStep, routeNameByColor, cellBackgroundColors);
	}

	/** 当前颜色 -> 线路名。换乘站名等文字由 MTR 数据提供。 */
	public static Map<Long, String> buildRouteNameByColor() {
		final Map<Long, String> routeNameByColor = new HashMap<>();
		try {
			final ObjectBidirectionalIterator<SimplifiedRoute> iterator = MinecraftClientData.getInstance().simplifiedRoutes.iterator();
			while (iterator.hasNext()) {
				final SimplifiedRoute route = iterator.next();
				routeNameByColor.put((long) route.getColor(), route.getName());
			}
		} catch (Exception ignored) {
		}
		return routeNameByColor;
	}

	/** 贴图绘制适配：世界模式进渲染队列，画布模式写进 JSDrawSink。 */
	public static RenderRailwaySign.DrawTexture textureDrawer(Direction facing, @Nullable StoredMatrixTransformations smt) {
		final JSDrawSink sink = JSDrawSink.current();
		if (sink != null) {
			return (textureId, x, y, size, flipTexture) -> {
				if (flipTexture) {
					sink.drawTextureFlipped(textureId, x, y, size, size, -1);
				} else {
					sink.drawTexture(textureId, x, y, size, size, -1);
				}
			};
		}
		return (textureId, x, y, size, flipTexture) -> MainRenderer.scheduleRender(textureId, true, QueuedRenderLayer.LIGHT_TRANSLUCENT, (graphicsHolderNew, offset) -> {
			smt.transform(graphicsHolderNew, offset);
			IDrawing.drawTexture(graphicsHolderNew, x, y, size, size, flipTexture ? 1.0F : 0.0F, 0.0F, flipTexture ? 0.0F : 1.0F, 1.0F, facing, -1, GraphicsHolder.getDefaultLight());
			graphicsHolderNew.pop();
		});
	}

	/** 带 z 偏移的贴图（底板用）：世界模式进渲染队列，画布模式直接填充。 */
	private static void drawTextureZ(Identifier texture, float x1, float y1, float z1, float x2, float y2, float z2, int color, Direction facing, @Nullable StoredMatrixTransformations smt) {
		final JSDrawSink sink = JSDrawSink.current();
		if (sink != null) {
			sink.drawTexture(texture, x1, y1, x2 - x1, y2 - y1, color);
			return;
		}
		MainRenderer.scheduleRender(texture, false, QueuedRenderLayer.LIGHT_TRANSLUCENT, (graphicsHolderNew, offset) -> {
			smt.transform(graphicsHolderNew, offset);
			IDrawing.drawTexture(graphicsHolderNew, x1, y1, z1, x2, y2, z2, facing, color, GraphicsHolder.getDefaultLight());
			graphicsHolderNew.pop();
		});
	}

	/**
	 * 画布模式下画"纯图标格"（MTR 的 drawSign 内部会走 MainRenderer 调度，收不进画布，
	 * 因此画布模式用同样的几何直接画底色 + 贴图）。
	 *
	 * @param cellX     格子左缘
	 * @param cellWidth 格子宽度
	 * @param size      内容高度
	 */
	public static void drawIconIntoCanvas(JSDrawSink sink, SignResource sign, float cellX, float cellWidth, float y, float size, int backgroundColor) {
		// 底色（MTR 的 drawSign 会画，画布模式这里补上）
		sink.drawTexture(WHITE_TEXTURE, cellX, y, cellWidth, size, backgroundColor | 0xFF000000);
		final float signSize = (sign.getSmall() ? 0.75F : 1.0F) * size;
		final float x = cellX + (cellWidth - signSize) / 2.0F;
		final float yOffset = y + (size - signSize) / 2.0F;
		if (sign.getFlipTexture()) {
			sink.drawTextureFlipped(sign.getTexture(), x, yOffset, signSize, signSize, -1);
		} else {
			sink.drawTexture(sign.getTexture(), x, yOffset, signSize, signSize, -1);
		}
	}
}
