package com.Nanbin.client;

import com.Nanbin.client.ItemsGroup.FiltersGroup;
import com.Nanbin.client.Registry.RenderLayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mtr.core.data.Position;
import org.mtr.mapping.holder.BlockPos;

import java.util.LinkedHashMap;
import java.util.Map;

public final class InitClient {
    public static final Logger LOGGER = LogManager.getLogger("Nanbin Create Mod");

    public static void init() {
        long startTime = System.currentTimeMillis();
        Map<String, Runnable> initSteps = new LinkedHashMap<>();

        initSteps.put("FiltersGroup", FiltersGroup::init);
        initSteps.put("RenderLayer", RenderLayer::init);

        int currentStep = 6;

        for (Map.Entry<String, Runnable> step : initSteps.entrySet()) {
            LOGGER.info("Nanbin Create Mod is registering , Please wait... {} ({}/{})", step.getKey(), currentStep, initSteps.size()+5);
            step.getValue().run();
            currentStep++;
        }
    }

    public static Position blockPosToPosition(BlockPos blockPos) {
        return new Position(blockPos.getX(), blockPos.getY(), blockPos.getZ());
    }

    public static BlockPos positionToBlockPos(Position position) {
        return new BlockPos((int) position.getX(), (int) position.getY(), (int) position.getZ());
    }
}