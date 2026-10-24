package com.Nanbin;

import com.Nanbin.Blocks.Blocks;
import com.Nanbin.Items.Items;
import com.Nanbin.ItemsGroup.ItemsGroup;
import com.Nanbin.Registry.SoundEvents;
import com.Nanbin.entity.BlockEntityTypes;
import com.Nanbin.mapping.Registry;
import com.Nanbin.packet.PacketAddCustomBalance;
import com.Nanbin.packet.PacketOpenBlockEntityScreen;
import com.Nanbin.packet.PacketOpenBusTicketProcessorScreen;
import com.Nanbin.packet.PacketOpenCRTPlatformScreen;
import com.Nanbin.packet.PacketOpenRoadNameScreen;
import com.Nanbin.packet.PacketOpenStationInfoScreen;
import com.Nanbin.packet.PacketOpenTicketMachineScreen;
import com.Nanbin.packet.PacketSyncCustomBalance;
import com.Nanbin.packet.PacketUpdateBusTicketProcessor;
import com.Nanbin.packet.PacketUpdateRoadNameData;
import com.Nanbin.packet.PacketUpdateRouteNameCell;
import com.Nanbin.packet.PacketUpdateStationInfoData;
import com.Nanbin.packet.PacketUpdateStationInfoSignData;
import com.Nanbin.packet.PacketUpdateTrainArrival;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mtr.Keys;

import java.util.LinkedHashMap;
import java.util.Map;


public final class Init {
	public static final String MOD_ID = "nanbin";
	public static final String MOD_NAME = "Nanbin Create Mod";
	public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);
	public static final String VERSION = "3.1";
	public static final String VERSION_DATA = "261024";
	public static boolean IS_TEST_VERSION = true;
	public static final String DESIGNED_MTR_VERSION = Keys.MOD_VERSION;
	public static final String FINAL_VERSION = VERSION + "." + VERSION_DATA + "+MTR" + DESIGNED_MTR_VERSION;

	private Init() {
	}

	public static void init() {
		LOGGER.info("Hello Nanbin! {}", FINAL_VERSION);

		final long startTime = System.currentTimeMillis();
		final Map<String, Runnable> initSteps = new LinkedHashMap<>();
		initSteps.put("ItemsGroup", ItemsGroup::init);
		initSteps.put("SoundEvents", SoundEvents::init);
		initSteps.put("Blocks", Blocks::init);
		initSteps.put("BlockEntityTypes", BlockEntityTypes::init);
		initSteps.put("Items", Items::init);
		initSteps.put("Packets", Init::registerPackets);
		initSteps.put("ItemGroups", ItemsGroup::registerTabs);

		int currentStep = 1;
		for (final Map.Entry<String, Runnable> step : initSteps.entrySet()) {
			LOGGER.info("Nanbin Create Mod is registering, please wait... {} ({}/{})", step.getKey(), currentStep, initSteps.size());
			step.getValue().run();
			currentStep++;
		}

		LOGGER.info("Nanbin Create Mod has successfully registered in {} ms.", System.currentTimeMillis() - startTime);
	}

	/**
	 * 注册自定义网络包（复用 MTR 的包通道，按类名查表）。
	 * <p>
	 * <b>相对 4.0 少了一个 {@code PacketUpdateCustomColor}：</b>那个包在源项目里就是死代码
	 * ——只有 {@code Init} 里的一行注册，没有任何发送方，它的唯一消费方
	 * {@code Registry/RegBlock/base/BlockCustomColorBase} 也没有任何方块继承，
	 * 而 {@code BlockCustomColorBase} 自己又只引用自己（同组的 {@code util/TextureGenerator}、
	 * {@code base/BlockRoadNameBase} 同样是零引用）。这些类在 4.0 就不可达，
	 * 照搬只会带进一堆永不执行的代码，所以本移植不再引入。
	 */
	private static void registerPackets() {
		Registry.registerPacket(PacketOpenBlockEntityScreen.class, PacketOpenBlockEntityScreen::new);
		Registry.registerPacket(PacketOpenCRTPlatformScreen.class, PacketOpenCRTPlatformScreen::new);
		Registry.registerPacket(PacketOpenRoadNameScreen.class, PacketOpenRoadNameScreen::new);
		Registry.registerPacket(PacketOpenStationInfoScreen.class, PacketOpenStationInfoScreen::new);
		Registry.registerPacket(PacketOpenTicketMachineScreen.class, PacketOpenTicketMachineScreen::new);
		Registry.registerPacket(PacketOpenBusTicketProcessorScreen.class, PacketOpenBusTicketProcessorScreen::new);
		Registry.registerPacket(PacketAddCustomBalance.class, PacketAddCustomBalance::new);
		Registry.registerPacket(PacketSyncCustomBalance.class, PacketSyncCustomBalance::new);
		Registry.registerPacket(PacketUpdateBusTicketProcessor.class, PacketUpdateBusTicketProcessor::new);
		Registry.registerPacket(PacketUpdateRoadNameData.class, PacketUpdateRoadNameData::new);
		Registry.registerPacket(PacketUpdateTrainArrival.class, PacketUpdateTrainArrival::new);
		Registry.registerPacket(PacketUpdateStationInfoData.class, PacketUpdateStationInfoData::new);
		Registry.registerPacket(PacketUpdateStationInfoSignData.class, PacketUpdateStationInfoSignData::new);
		Registry.registerPacket(PacketUpdateRouteNameCell.class, PacketUpdateRouteNameCell::new);
	}
}
