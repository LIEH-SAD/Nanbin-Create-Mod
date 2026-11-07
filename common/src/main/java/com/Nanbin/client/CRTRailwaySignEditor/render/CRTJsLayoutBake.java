package com.Nanbin.client.CRTRailwaySignEditor.render;

import com.Nanbin.client.JavaScriptSupport.JSDrawContext;
import com.Nanbin.client.JavaScriptSupport.JSSignConfig;
import com.Nanbin.client.JavaScriptSupport.JSSignEngine;
import com.Nanbin.client.JavaScriptSupport.JSSignTextureBaker;
import com.Nanbin.signage.crt.CRTSignDataContext;
import com.Nanbin.signage.crt.CRTSignLayout;
import it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * 整牌 JS 样式的烘焙：把 {@link CRTSignLayout#jsStyleId} 指定的脚本渲染成一张贴图。
 *
 * <p>与旧格子编辑器的 JS 支持等价：脚本通过 {@link JSSignEngine} 产出绘制指令，由
 * {@link JSSignTextureBaker} 合成为贴图；设计师像素空间与格子空间的换算方式是
 * 「一个格子 = {@code layout.rowHeight} 设计像素」，格子数 = 布局宽度 / 行高，
 * 因此旧 JS 脚本（按格绘制）在布局牌面上仍然对得上位置。</p>
 *
 * <p>MTR 数据：优先使用 {@link CRTSignLayout#jsSelectedIds}（JS 数据屏幕里选过的
 * 线路/车站/出口 id），为空时退化为元素绑定里出现过的全部 id；线路编号通过
 * {@link CRTSignDataContext#getRouteNumber(long)} 解析后交给脚本。</p>
 */
public final class CRTJsLayoutBake {

	private CRTJsLayoutBake() {
	}

	/** 布局是否启用 JS 整牌样式（脚本必须存在）。 */
	public static boolean isJsStyle(@Nullable CRTSignLayout layout) {
		return layout != null && layout.hasJsStyle() && JSSignConfig.hasScript(layout.jsStyleId);
	}

	/**
	 * 烘焙整牌 JS 样式贴图。
	 *
	 * @param layout     牌面布局（jsStyleId 非空）
	 * @param data       MTR 数据（可为 null）
	 * @param cacheKey   贴图缓存键前缀（同一块牌唯一）
	 * @param contentKey 内容签名（布局 JSON 哈希）
	 * @param facing     牌面朝向（脚本可能按朝向画箭头）
	 * @return 烘焙结果；脚本不存在/渲染失败返回 null（调用方回退到普通布局渲染）
	 */
	@Nullable
	public static JSSignTextureBaker.Baked bake(@Nullable CRTSignLayout layout, @Nullable CRTSignDataContext data,
			String cacheKey, long contentKey, Direction facing) {
		return bake(layout, data, cacheKey, contentKey, facing, 0);
	}

	/**
	 * 同上，但由调用方给出牌面实际格数（铁路牌 = signIds.length，墙挂牌 = 方块数 × 每块格数）。
	 * JS 贴图宽高比必须与牌面一致（牌宽 = 格数 × 0.5、牌高 = 0.5），否则世界渲染会等比缩放留黑边，
	 * 表现为"JS 内容没有铺满牌面"，与编辑器预览不一致。
	 */
	@Nullable
	public static JSSignTextureBaker.Baked bake(@Nullable CRTSignLayout layout, @Nullable CRTSignDataContext data,
			String cacheKey, long contentKey, Direction facing, int plateCells) {
		if (!isJsStyle(layout)) {
			return null;
		}
		try {
			final String scriptId = layout.jsStyleId;
			// JS 脚本沿用旧格子编辑器的世界单位：1 格 = 0.5 世界单位。
			// 不能把设计像素（如 3584）直接当 JS 单位——烘焙器会按 176 px/单位出图，
			// 3584 单位会生成 10 万像素宽的贴图，瞬间卡死/爆内存。
			final float cellSize = 0.5F;
			// 牌面实际格数优先（与世界渲染 plateWidth = cells × 0.5 完全一致）
			final int cellCount = plateCells > 0
					? Math.min(64, Math.max(1, plateCells))
					: Math.min(64, Math.max(1, Math.round(layout.layoutWidth() / (float) Math.max(1, layout.rowHeight))));
			final float width = cellCount * cellSize;
			final String[] lineIds = new String[cellCount];
			for (int i = 0; i < cellCount; i++) {
				lineIds[i] = "";
			}
			final LongAVLTreeSet selectedIds = new LongAVLTreeSet();
			if (layout.jsSelectedIds.isEmpty()) {
				for (final Long id : layout.collectBindingIds()) {
					idsAdd(selectedIds, id);
				}
			} else {
				for (final Long id : layout.jsSelectedIds) {
					idsAdd(selectedIds, id);
				}
			}
			final Map<Long, String> routeNumberMap = new HashMap<>();
			for (final long id : selectedIds) {
				final String number = data == null ? null : data.getRouteNumber(id);
				if (number != null && !number.isEmpty()) {
					routeNumberMap.put(id, number);
				}
			}
			final int background = layout.backgroundColor | 0xFF000000;
			final String signKey = "crt_js_layout:" + cacheKey;
			final String[] routeNumbers = new String[cellCount];
			return JSSignTextureBaker.bake(signKey, contentKey, 0.0F, 0.0F, width, cellSize, () -> JSSignEngine.render(
					scriptId,
					signKey,
					new JSDrawContext(null, null, facing, 0.0F, 0.0F, cellSize, cellCount, false, background),
					new JSSignEngine.JSSignAPI(
							new JSSignEngine.JSSignContext(lineIds, 0, selectedIds, routeNumbers, background, cellSize, signKey,
									BlockPos.ORIGIN, routeNumberMap),
							scriptId)));
		} catch (Throwable e) {
			final long now = System.currentTimeMillis();
			if (now - lastFailureLogAt > 5000L) {
				lastFailureLogAt = now;
				com.Nanbin.Init.LOGGER.warn("CRT JS layout bake failed (script={}, key={})", layout.jsStyleId, cacheKey, e);
			}
			return null;
		}
	}

	private static long lastFailureLogAt = 0L;

	private static void idsAdd(LongAVLTreeSet set, long id) {
		set.add(id);
	}
}
