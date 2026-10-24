package com.Nanbin.client.Screen;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.math.BlockPos;
import org.mtr.core.data.Platform;
import org.mtr.core.data.Route;
import org.mtr.core.data.Station;
import org.mtr.core.data.StationExit;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.resource.SignResource;
import org.mtr.resource.SignType;
import org.mtr.screen.PlatformListSelectorScreen;
import org.mtr.screen.RailwaySignScreen;
import org.mtr.screen.RouteListSelectorScreen;
import org.mtr.screen.StationExitListSelectorScreen;
import org.mtr.screen.StationListSelectorScreen;
import org.mtr.screen.WindowBase;


public final class MtrSignSelectors {

	private MtrSignSelectors() {
	}

	public enum DataType {
		PLATFORM,
		ROUTE,
		STATION,
		EXIT,
		NONE
	}

	public static DataType getDataType(String signId) {
		if (signId == null || signId.isEmpty()) {
			return DataType.NONE;
		}
		final SignResource sign = org.mtr.client.CustomResourceLoader.getSignById(signId);
		final SignType signType = sign == null ? SignType.NORMAL : sign.getSignType();
		return switch (signType) {
			case EXIT -> DataType.EXIT;
			case PLATFORM -> DataType.PLATFORM;
			case ROUTE -> DataType.ROUTE;
			case STATION -> DataType.STATION;
			default -> bySignId(signId);
		};
	}

	private static DataType bySignId(String signId) {
		return switch (signId) {
			case "exit_letter", "exit_letter_flipped" -> DataType.EXIT;
			case "platform", "platform_flipped" -> DataType.PLATFORM;
			case "line", "line_flipped", "crt_route_name", "crt_route_name_flipped", "crt_route_number", "crt_route_number_flipped" -> DataType.ROUTE;
			case "station", "station_flipped", "crt_station_name", "crt_station_name_flipped" -> DataType.STATION;
			default -> DataType.NONE;
		};
	}

	public static boolean hasData(DataType type, BlockPos signPos) {
		return switch (type) {
			case PLATFORM -> !SignResource.getPlatforms(signPos).isEmpty();
			case ROUTE -> !SignResource.getRoutesForRailwaySign(signPos).isEmpty();
			case STATION -> !SignResource.getStations(signPos).isEmpty();
			case EXIT -> !SignResource.getStationExits(signPos).isEmpty();
			case NONE -> false;
		};
	}

	public static boolean open(Screen parent, DataType type, BlockPos signPos, LongAVLTreeSet selectedIds, boolean clearBefore, Runnable onChanged) {
		final WindowBase parentWindow = new ParentWindow(parent);
		return switch (type) {
			case PLATFORM -> {
				final ObjectArrayList<Platform> platforms = SignResource.getPlatforms(signPos);
				if (platforms.isEmpty()) {
					yield false;
				}
				final PlatformListSelectorScreen selector = new PlatformListSelectorScreen(selected -> {
					apply(selectedIds, clearBefore);
					for (final Platform platform : selected) {
						selectedIds.add(platform.getId());
					}
					if (onChanged != null) {
						onChanged.run();
					}
				}, parentWindow);
				selector.setAvailableList(platforms);
				platforms.forEach(platform -> {
					if (selectedIds.contains(platform.getId())) {
						selector.selectData(platform);
					}
				});
				com.Nanbin.packet.ClientPacketHelper.openScreen(selector);
				yield true;
			}
			case ROUTE -> {
				final ObjectArrayList<Route> routes = SignResource.getRoutesForRailwaySign(signPos);
				if (routes.isEmpty()) {
					yield false;
				}
				// 后三个布尔值与 MTR 自己的 createRouteListSelectorScreen 一致
				final RouteListSelectorScreen selector = new RouteListSelectorScreen(selected -> {
					apply(selectedIds, clearBefore);
					for (final Route route : selected) {
						selectedIds.add(route.getColor());
					}
					if (onChanged != null) {
						onChanged.run();
					}
				}, false, false, true, parentWindow);
				selector.setAvailableList(routes);
				routes.forEach(route -> {
					if (selectedIds.contains(route.getColor())) {
						selector.selectData(route);
					}
				});
				com.Nanbin.packet.ClientPacketHelper.openScreen(selector);
				yield true;
			}
			case STATION -> {
				final ObjectArrayList<Station> stations = SignResource.getStations(signPos);
				if (stations.isEmpty()) {
					yield false;
				}
				final StationListSelectorScreen selector = new StationListSelectorScreen(selected -> {
					apply(selectedIds, clearBefore);
					for (final Station station : selected) {
						selectedIds.add(station.getId());
					}
					if (onChanged != null) {
						onChanged.run();
					}
				}, parentWindow);
				selector.setAvailableList(stations);
				stations.forEach(station -> {
					if (selectedIds.contains(station.getId())) {
						selector.selectData(station);
					}
				});
				com.Nanbin.packet.ClientPacketHelper.openScreen(selector);
				yield true;
			}
			case EXIT -> {
				final ObjectArrayList<StationExit> exits = SignResource.getStationExits(signPos);
				if (exits.isEmpty()) {
					yield false;
				}
				final StationExitListSelectorScreen selector = new StationExitListSelectorScreen(selected -> {
					apply(selectedIds, clearBefore);
					for (final StationExit exit : selected) {
						selectedIds.add(RailwaySignScreen.serializeExit(exit.getName()));
					}
					if (onChanged != null) {
						onChanged.run();
					}
				}, parentWindow);
				selector.setAvailableList(exits);
				exits.forEach(exit -> {
					if (selectedIds.contains(RailwaySignScreen.serializeExit(exit.getName()))) {
						selector.selectData(exit);
					}
				});
				com.Nanbin.packet.ClientPacketHelper.openScreen(selector);
				yield true;
			}
			case NONE -> false;
		};
	}

	private static void apply(LongAVLTreeSet selectedIds, boolean clearBefore) {
		if (clearBefore) {
			selectedIds.clear();
		}
	}

	public static final class ParentWindow extends WindowBase {

		public ParentWindow(Screen previousScreen) {
			super(previousScreen);
		}
	}
}
