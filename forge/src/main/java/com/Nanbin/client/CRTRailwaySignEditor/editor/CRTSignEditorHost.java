package com.Nanbin.client.CRTRailwaySignEditor.editor;

import com.Nanbin.signage.crt.CRTSignLayout;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.Direction;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import org.mtr.mod.screen.DashboardListItem;

/**
 * CRT 导视牌编辑界面的宿主回调：把"保存布局"和"MTR 数据选择列表"从界面里拆出来。
 *
 * <p>由 {@code ClientPacketHelper} 在打开编辑器时实现（保存走数据包，列表取自已选车站的
 * MTR 客户端数据），界面本身不直接发包、也不直接遍历 MTR 数据。</p>
 */
public interface CRTSignEditorHost {

	/** 布局有改动（界面会做节流；关闭时也会调用一次）。 */
	/** 当前编辑的方块位置（JS 数据屏幕需要用它查所属车站）。 */
	BlockPos getSignPos();

	/** 方块朝向（JS 整牌样式的预览/世界渲染都按它渲染，避免方向不一致）。 */
	Direction getFacing();

	/** 牌面实际格数（铁路牌 = signIds.length，墙挂牌 = 方块数 × 每块格数）；0 表示未知。 */
	int getSignCells();

	void save(CRTSignLayout layout);

	/** 当前车站可选线路（MTR DashboardListItem 列表，与 MTR 选择屏一致）。 */
	ObjectImmutableList<DashboardListItem> getRoutesForList();

	/** 当前车站（含换乘站）可选车站列表。 */
	ObjectImmutableList<DashboardListItem> getStationsForList();

	/** 当前车站可选出口列表。 */
	ObjectImmutableList<DashboardListItem> getExitsForList();

	/** 当前车站可选站台列表。 */
	ObjectImmutableList<DashboardListItem> getPlatformsForList();
}
