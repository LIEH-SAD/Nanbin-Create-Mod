package com.Nanbin.client.CRTRailwaySignEditor.legacy;

import it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import net.minecraft.util.math.Direction;

import javax.annotation.Nullable;
import java.util.function.LongFunction;

/**
 * 一块 CRT 铁路指示牌的渲染视图（与数据来源解耦）。
 *
 * <p>世界渲染器（{@link com.Nanbin.client.Render.RenderCRTRailwaySign}）和编辑界面的
 * 预览（{@link com.Nanbin.client.CRTRailwaySignEditor.render.CRTSignLayoutPreview}）都用它描述"牌面是什么"，再由 {@link CRTLegacySignDrawing}
 * 统一绘制，因此预览与实际渲染的画面必然一致。</p>
 *
 * <p>几何约定：内容从 (0, 0) 开始向右铺开，宽 = 格数 × {@link #cellStep}，高 = {@link #cellSize}。</p>
 */
public final class CRTLegacySignView {

	/** 每格标志 id，null/空串表示空格。 */
	public final String[] signIds;
	/** 选中数据（线路颜色 / 车站 / 站台 / 出口 id）。 */
	public final LongAVLTreeSet selectedIds;
	/** 与 selectedIds 顺序对应的线路编号（可能为空数组）。 */
	public final String[] routeNumbers;
	/** 线路颜色 -> 线路编号。 */
	public final LongFunction<String> routeNumberGetter;

	/** 每格独立线路颜色；null 表示沿用全局选择。 */
	@Nullable
	public long[][] cellColors;
	/** 每格独立底色（0 = 未设置）；null 表示全部未设置。 */
	@Nullable
	public int[] cellBackgroundColors;
	/** 预览用 Direction.UP，世界渲染用方块朝向。 */
	public Direction facing = Direction.UP;
	/** 当前生效的 JS 样式脚本 id；非 null 时整块牌由脚本渲染。 */
	@Nullable
	public String jsStyleScriptId;
	/** 内容高度（世界单位），铁路指示牌为 0.5。 */
	public float cellSize = 0.5F;
	/** 每格水平步长（世界单位），铁路指示牌为 0.5；墙挂牌为 1.0。 */
	public float cellStep = 0.5F;

	public CRTLegacySignView(String[] signIds, LongAVLTreeSet selectedIds, String[] routeNumbers, @Nullable LongFunction<String> routeNumberGetter, @Nullable String jsStyleScriptId) {
		this.signIds = signIds == null ? new String[0] : signIds;
		this.selectedIds = selectedIds == null ? new LongAVLTreeSet() : selectedIds;
		this.routeNumbers = routeNumbers == null ? new String[0] : routeNumbers;
		this.routeNumberGetter = routeNumberGetter == null ? color -> "" : routeNumberGetter;
		this.jsStyleScriptId = jsStyleScriptId;
	}

	/** 设置几何参数，返回自身（链式）。 */
	public CRTLegacySignView geometry(float cellSize, float cellStep, Direction facing) {
		this.cellSize = cellSize;
		this.cellStep = cellStep;
		this.facing = facing;
		return this;
	}

	/** 设置每格独立线路颜色 / 底色，返回自身（链式）。 */
	public CRTLegacySignView cellOverrides(@Nullable long[][] cellColors, @Nullable int[] cellBackgroundColors) {
		this.cellColors = cellColors;
		this.cellBackgroundColors = cellBackgroundColors;
		return this;
	}

	/** 牌面内容总宽（世界单位）。 */
	public float width() {
		return signIds.length * cellStep;
	}

	/** 是否所有格子都为空。 */
	public boolean isEmpty() {
		for (final String signId : signIds) {
			if (signId != null && !signId.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	/** 第 cell 格的独立线路颜色；没有则返回 null。 */
	@Nullable
	public long[] cellColor(int cell) {
		if (cellColors == null || cell < 0 || cell >= cellColors.length) {
			return null;
		}
		return cellColors[cell];
	}

	/** 第 cell 格的独立底色；0 表示未设置。 */
	public int cellBackgroundColor(int cell) {
		if (cellBackgroundColors == null || cell < 0 || cell >= cellBackgroundColors.length) {
			return 0;
		}
		return cellBackgroundColors[cell];
	}

	/** 内容指纹：格子、选中数据、线路编号、独立颜色变化时让预览立即重烘焙。 */
	public long contentKey() {
		long hash = 1125899906842597L;
		hash = hash * 31L + signIds.length;
		hash = hash * 31L + Float.floatToIntBits(cellSize);
		hash = hash * 31L + Float.floatToIntBits(cellStep);
		for (final String signId : signIds) {
			hash = hash * 31L + (signId == null ? 0 : signId.hashCode());
		}
		for (final long id : selectedIds) {
			hash = hash * 31L + (int) (id ^ (id >>> 32));
		}
		for (final String routeNumber : routeNumbers) {
			hash = hash * 31L + (routeNumber == null ? 0 : routeNumber.hashCode());
		}
		if (cellColors != null) {
			for (final long[] colors : cellColors) {
				if (colors == null) {
					hash = hash * 31L;
					continue;
				}
				hash = hash * 31L + colors.length;
				for (final long color : colors) {
					hash = hash * 31L + (int) (color ^ (color >>> 32));
				}
			}
		}
		if (cellBackgroundColors != null) {
			for (final int color : cellBackgroundColors) {
				hash = hash * 31L + color;
			}
		}
		if (jsStyleScriptId != null) {
			hash = hash * 31L + jsStyleScriptId.hashCode();
		}
		return hash;
	}
}
