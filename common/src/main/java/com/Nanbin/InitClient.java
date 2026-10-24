package com.Nanbin;

import com.Nanbin.client.FiltersGroup.FiltersGroup;
import com.Nanbin.client.FiltersGroup.FiltersMenu;
import com.Nanbin.client.Registry.BlockColor;
import com.Nanbin.client.Registry.BlockEntityRender;
import com.Nanbin.client.Registry.RenderLayerReg;
import org.apache.logging.log4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;

public final class InitClient {
	public static final Logger LOGGER = Init.LOGGER;
	private static boolean filtersInitialized;

	private InitClient() {
	}

	public static void init() {
		LOGGER.info("Nanbin Create Mod client is initializing...");

		final Map<String, Runnable> clientInitSteps = new LinkedHashMap<>();

		clientInitSteps.put("RenderLayers", RenderLayerReg::init);
		clientInitSteps.put("BlockColors", BlockColor::init);
		clientInitSteps.put("BlockEntityRenderers", BlockEntityRender::init);

		int currentStep = 1;
		for (final Map.Entry<String, Runnable> step : clientInitSteps.entrySet()) {
			LOGGER.info("Nanbin Create Mod is registering, please wait... {} ({}/{})", step.getKey(), currentStep, clientInitSteps.size());
			step.getValue().run();
			currentStep++;
		}
	}

	public static void onClientTick() {
		if (filtersInitialized) {
			return;
		}
		if (!FiltersGroup.isReady()) {
			return;
		}
		filtersInitialized = true;
		try {
			FiltersGroup.init();
			FiltersMenu.init();
			LOGGER.info("Nanbin Create Mod filters integration is ready.");
		} catch (Exception e) {
			LOGGER.error("Nanbin Create Mod filters integration failed.", e);
		}
	}
}
