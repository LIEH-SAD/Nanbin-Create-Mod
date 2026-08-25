package com.Nanbin;

import com.Nanbin.Blocks.Blocks;
import com.Nanbin.Items.Items;
import com.Nanbin.ItemsGroup.ItemsGroup;
import com.Nanbin.Registry.SoundEvents;
import com.Nanbin.entity.BlockEntityTypes;
import com.Nanbin.packet.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.registry.Registry;
import org.mtr.mod.Keys;

import java.util.LinkedHashMap;
import java.util.Map;

public final class Init {
    public static final String MOD_ID = "nanbin";
    public static final Logger LOGGER = LogManager.getLogger("Nanbin Create Mod");
    public static final Registry REGISTRY = new Registry();
    public static final String VERSION = "3.1";     //南滨扩展会引用这些
    public static final String VERSION_DATA = "26819";
    public static final String DESIGNED_MTR_VERSION = Keys.MOD_VERSION;
    public static final String FINAL_VERSION = VERSION + "." + VERSION_DATA + " + MTR" + DESIGNED_MTR_VERSION;

    public static void init() {
        LOGGER.info("Hello Nanbin!");

        long startTime = System.currentTimeMillis();
        Map<String, Runnable> initSteps = new LinkedHashMap<>();

        initSteps.put("ItemsGroup", ItemsGroup::init);
        initSteps.put("SoundEvents", SoundEvents::init);
        initSteps.put("Blocks", Blocks::init);
        initSteps.put("BlockEntityTypes", BlockEntityTypes::init);
        initSteps.put("Items", Items::init);
        initSteps.put("Packet", () -> {
            REGISTRY.setupPackets(new Identifier(MOD_ID, "packet"));
            REGISTRY.registerPacket(PacketOpenBlockEntityScreen.class, PacketOpenBlockEntityScreen::new);
            REGISTRY.registerPacket(PacketOpenCRTPlatformScreen.class, PacketOpenCRTPlatformScreen::new);
            REGISTRY.registerPacket(PacketOpenRoadNameScreen.class, PacketOpenRoadNameScreen::new);
            REGISTRY.registerPacket(PacketUpdateCustomColor.class, PacketUpdateCustomColor::new);
            REGISTRY.registerPacket(PacketUpdateRoadNameData.class, PacketUpdateRoadNameData::new);
            REGISTRY.registerPacket(PacketUpdateRouteNameCell.class, PacketUpdateRouteNameCell::new);
            REGISTRY.registerPacket(PacketUpdateTrainArrival.class, PacketUpdateTrainArrival::new);
            REGISTRY.registerPacket(PacketOpenTicketMachineScreen.class, PacketOpenTicketMachineScreen::new);
            REGISTRY.registerPacket(PacketAddCustomBalance.class, PacketAddCustomBalance::new);
            REGISTRY.registerPacket(PacketSyncCustomBalance.class, PacketSyncCustomBalance::new);
            REGISTRY.registerPacket(PacketOpenBusTicketProcessorScreen.class, PacketOpenBusTicketProcessorScreen::new);
            REGISTRY.registerPacket(PacketUpdateBusTicketProcessor.class, PacketUpdateBusTicketProcessor::new);
            REGISTRY.registerPacket(PacketOpenStationInfoScreen.class, PacketOpenStationInfoScreen::new);
            REGISTRY.registerPacket(PacketUpdateStationInfoData.class, PacketUpdateStationInfoData::new);
            REGISTRY.registerPacket(PacketUpdateStationInfoSignData.class, PacketUpdateStationInfoSignData::new);
        });

        int currentStep = 1;

        for (Map.Entry<String, Runnable> step : initSteps.entrySet()) {
            LOGGER.info("Nanbin Create Mod is registering , Please wait... {} ({}/{})", step.getKey(), currentStep, initSteps.size());
            step.getValue().run();
            currentStep++;
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        LOGGER.info("Nanbin Create Mod has successfully registered in {} ms.", duration);
        REGISTRY.init();
    }
}