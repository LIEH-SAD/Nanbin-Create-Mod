package com.Nanbin;

import com.Nanbin.Blocks.Blocks;
import com.Nanbin.Items.Items;
import com.Nanbin.ItemsGroup.ItemsGroup;
import com.Nanbin.Registry.SoundEvents;
import com.Nanbin.entity.BlockEntityTypes;
import com.Nanbin.packet.PacketOpenCRTPlatformScreen;
import com.Nanbin.packet.PacketOpenRoadNameScreen;
import com.Nanbin.packet.PacketUpdateCustomColor;
import com.Nanbin.packet.PacketUpdateRoadNameData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mtr.core.data.Position;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.registry.Registry;

import java.util.LinkedHashMap;
import java.util.Map;

public final class Init {
    public static final String MOD_ID = "nanbin";
    public static final Logger LOGGER = LogManager.getLogger("Nanbin Create Mod");
    public static final Registry REGISTRY = new Registry();    //内部版本号（VERSION_DATA = 编译时间）
    public static final String VERSION = "3.0";
    public static final String VERSION_DATA = "26801";
    public static final String DESIGNED_MTR_VERSION = "4.0.0";
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
            REGISTRY.registerPacket(PacketOpenCRTPlatformScreen.class, PacketOpenCRTPlatformScreen::new);
            REGISTRY.registerPacket(PacketOpenRoadNameScreen.class, PacketOpenRoadNameScreen::new);
            REGISTRY.registerPacket(PacketUpdateCustomColor.class, PacketUpdateCustomColor::new);
            REGISTRY.registerPacket(PacketUpdateRoadNameData.class, PacketUpdateRoadNameData::new);
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

    //接下来我们来写一些简单的调用
    public static Position blockPosToPosition(BlockPos blockPos) {
        return new Position(blockPos.getX(), blockPos.getY(), blockPos.getZ());
    }

    public static BlockPos positionToBlockPos(Position position) {
        return new BlockPos((int) position.getX(), (int) position.getY(), (int) position.getZ());
    }

}