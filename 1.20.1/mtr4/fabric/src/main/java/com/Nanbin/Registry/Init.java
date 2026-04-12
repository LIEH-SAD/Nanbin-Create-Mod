package com.Nanbin.Registry;

import com.Nanbin.Blocks.Blocks;
import com.Nanbin.Items.Items;
import com.Nanbin.ItemsGroup.ItemsGroup;
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
    public static final Registry REGISTRY = new Registry();


    public static void init() {
        long startTime = System.currentTimeMillis();
        Map<String, Runnable> initSteps = new LinkedHashMap<>();

        initSteps.put("Blocks", Blocks::init);
        initSteps.put("Items", Items::init);
        initSteps.put("ItemsGroup", ItemsGroup::init);
        initSteps.put("SoundEvents", NanbinSoundEvents::init);
        initSteps.put("MTR Packet", () -> {
            REGISTRY.setupPackets(new Identifier(MOD_ID, "packet"));
           REGISTRY.init();
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
    }

    public static Position blockPosToPosition(BlockPos blockPos) {
        return new Position(blockPos.getX(), blockPos.getY(), blockPos.getZ());
    }

    public static BlockPos positionToBlockPos(Position position) {
        return new BlockPos((int) position.getX(), (int) position.getY(), (int) position.getZ());
    }
}