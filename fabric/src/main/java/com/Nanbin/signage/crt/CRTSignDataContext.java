package com.Nanbin.signage.crt;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 布局渲染/编辑时使用的 MTR 数据访问接口（由客户端实现，见
 * {@code com.Nanbin.client.crt.render.CRTSignDataResolver}）。
 *
 * <p>把"取数据"从"画牌面"里拆出来，渲染器与编辑界面都只依赖这个接口：
 * 世界渲染、编辑界面预览、属性面板显示当前值都走同一份数据，
 * 因此 MTR 里的线路改名/换色后牌面内容会自动跟着变。</p>
 */
public interface CRTSignDataContext {

	/** 线路主名称（通常是中文，如「环线」「1号线」）；未知返回 null。 */
	@Nullable
	String getRouteName(long color);

	/** 线路英文名（MTR 数据里的第二个语言段）；没有返回 null。 */
	@Nullable
	String getRouteNameEn(long color);

	/** 线路编号（如 1 / 环 / S1）；客户端数据没有时需要服务端解析后写入方块实体，未知返回 null。 */
	@Nullable
	String getRouteNumber(long color);

	/** 合并多座车站的站名（含换乘站排版）；没有有效数据返回空列表。 */
	List<String> getStationNames(List<Long> stationIds);

	/** 出口编号/字母（如 A、B2）；未知返回 null。 */
	@Nullable
	String getExitCode(long exitId);

	/**
	 * 出口名称（MTR 4.0 {@code StationExit.getName()}，如 1、A、2A）。
	 *
	 * <p>与 {@link #getExitCode(long)} 同源（都来自 {@code EditStationScreen.deserializeExit}），
	 * 单独提供是因为出口信息条（EXIT_INFO）需要按名称去匹配目的地列表。</p>
	 */
	@Nullable
	String getExitName(long exitId);

	/**
	 * 出口目的地列表（MTR 4.0 {@code StationExit.getDestinations()}，中英混排，
	 * 中文条目在前、英文条目在后）；取不到数据时返回空列表（不返回 null）。
	 */
	List<String> getExitDestinations(long exitId);

	/** 站台名/编号（如 1 站台）；未知返回 null。 */
	@Nullable
	String getPlatformName(long platformId);

	/** 下一站站名（按线路顺序）；未知返回 null。 */
	@Nullable
	String getNextStation(long platformId);

	/** 上一站站名；未知返回 null。 */
	@Nullable
	String getPrevStation(long platformId);

	/** 空实现：取不到任何数据时用（渲染退化到字面文本）。 */
	CRTSignDataContext EMPTY = new CRTSignDataContext() {
		@Nullable
		@Override
		public String getRouteName(long color) {
			return null;
		}

		@Nullable
		@Override
		public String getRouteNameEn(long color) {
			return null;
		}

		@Nullable
		@Override
		public String getRouteNumber(long color) {
			return null;
		}

		@Override
		public List<String> getStationNames(List<Long> stationIds) {
			return java.util.Collections.emptyList();
		}

		@Nullable
		@Override
		public String getExitCode(long exitId) {
			return null;
		}

		@Nullable
		@Override
		public String getExitName(long exitId) {
			return null;
		}

		@Override
		public List<String> getExitDestinations(long exitId) {
			return java.util.Collections.emptyList();
		}

		@Nullable
		@Override
		public String getPlatformName(long platformId) {
			return null;
		}

		@Nullable
		@Override
		public String getNextStation(long platformId) {
			return null;
		}

		@Nullable
		@Override
		public String getPrevStation(long platformId) {
			return null;
		}
	};
}
