package com.Nanbin.mapping;

import com.Nanbin.Init;
import net.minecraft.util.math.BlockPos;
import org.mtr.core.data.Data;
import org.mtr.core.data.Platform;
import org.mtr.core.data.Station;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * MTR 4.1 服务端数据访问（唯一需要反射的一处）。
 * <p>
 * 站名牌、铁路告示牌、站台信息屏都需要「线路编号」{@code Route#getRouteNumber()}，
 * 而客户端拿不到它：MTR 同步给客户端的只有 {@code SimplifiedRoute}，
 * 而 {@code SimplifiedRoute} 的构造函数（字节码）只取 {@code Route.getName()} / {@code getColor()} /
 * {@code getCircularState()}，<b>根本没有 routeNumber 字段</b>。
 * 表现就是：普通线路看着正常（编号本来就是从名字里取的前缀），
 * 而轻铁这类「名字与编号分开填」的线路完全取不到编号。
 * <p>
 * 所以必须像 4.0 那样在服务端解析一次、写进方块实体 NBT 再同步给客户端。
 * MTR 4.1 没有公开的服务端 {@code Data} 入口，只能沿 4.0 的思路反射：
 * <pre>
 * org.mtr.MTR.main            (private static org.mtr.core.Main)
 *   └─ org.mtr.core.Main.simulators  (private final ObjectImmutableList&lt;Simulator&gt;)
 *        └─ Simulator extends org.mtr.core.data.Data   ← 之后全是公开 API
 * </pre>
 * 4.0 的路径是 {@code org.mtr.mod.Init.main}，4.1 换成了 {@code org.mtr.MTR.main}。
 * <p>
 * 只有 {@code MTR.main} 与 {@code Main.simulators} 两个字段需要反射，
 * 往下的 {@code Data.platformIdMap} / {@code Station.savedRails} / {@code Platform.routes} /
 * {@code Route.getRouteNumber()} 全部是公开 API。
 */
public final class MtrServerData {

	/** MTR 服务端主类与其中持有 simulator 列表的字段名（4.0 是 org.mtr.mod.Init，4.1 是 org.mtr.MTR）。 */
	private static final String MTR_CLASS_NAME = "org.mtr.MTR";
	private static final String MAIN_FIELD_NAME = "main";
	private static final String SIMULATORS_FIELD_NAME = "simulators";

	private static Field mainField;
	private static Field simulatorsField;
	private static boolean warned;

	private MtrServerData() {
	}

	/**
	 * MTR 服务端的 simulator 列表，每个元素都是一份 {@link Data}；
	 * 反射失败（MTR 改了内部结构等）时返回空列表。
	 * <p>
	 * 这里每次都会重新读 {@code main}（不缓存实例）：单人世界重开时 MTR 会重建这个对象，
	 * 缓存字段引用没问题，缓存实例会拿到过期数据。
	 */
	public static List<Data> getDataList() {
		try {
			if (mainField == null) {
				mainField = Class.forName(MTR_CLASS_NAME).getDeclaredField(MAIN_FIELD_NAME);
				mainField.setAccessible(true);
			}
			final Object main = mainField.get(null);
			if (main == null) {
				return Collections.emptyList();
			}
			if (simulatorsField == null) {
				simulatorsField = main.getClass().getDeclaredField(SIMULATORS_FIELD_NAME);
				simulatorsField.setAccessible(true);
			}
			final Object simulators = simulatorsField.get(main);
			if (!(simulators instanceof Iterable<?> iterable)) {
				return Collections.emptyList();
			}
			final List<Data> result = new ArrayList<>();
			for (final Object simulator : iterable) {
				if (simulator instanceof Data data) {
					result.add(data);
				}
			}
			return result;
		} catch (Exception e) {
			if (!warned) {
				warned = true;
				Init.LOGGER.error("MtrServerData: 无法反射访问 MTR 服务端数据（服务端线路编号将无法同步到客户端）", e);
			}
			return Collections.emptyList();
		}
	}

	/** 方块坐标所属的站点；找不到返回 {@code null}。 */
	public static Station findStation(BlockPos pos) {
		if (pos == null) {
			return null;
		}
		try {
			final org.mtr.core.data.Position position = org.mtr.MTR.blockPosToPosition(pos);
			for (final Data data : getDataList()) {
				for (final Station station : data.stations) {
					if (station.inArea(position)) {
						return station;
					}
				}
			}
		} catch (Exception e) {
			Init.LOGGER.error("MtrServerData: 查找站点失败 at {}", pos, e);
		}
		return null;
	}

	/** 站台 id 对应的站台；找不到返回 {@code null}。 */
	public static Platform findPlatform(long platformId) {
		if (platformId == 0) {
			return null;
		}
		for (final Data data : getDataList()) {
			final Platform platform = data.platformIdMap.get(platformId);
			if (platform != null) {
				return platform;
			}
		}
		return null;
	}

	/** 站台所属的线路编号；没有非空编号时返回空串。 */
	public static String getRouteNumber(Platform platform) {
		if (platform == null) {
			return "";
		}
		for (final org.mtr.core.data.Route route : platform.routes) {
			final String number = route.getRouteNumber();
			if (number != null && !number.isEmpty()) {
				return number;
			}
		}
		return "";
	}
}
