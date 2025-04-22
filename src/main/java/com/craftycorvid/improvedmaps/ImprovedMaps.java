package com.craftycorvid.improvedmaps;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.craftycorvid.improvedmaps.item.ImprovedMapsItems;


public class ImprovedMaps implements ModInitializer {
	public static final String MOD_ID = "improved-maps";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Improved Maps Initializing");

		ImprovedMapsItems.initialize();
		ImprovedMapsComponentTypes.initialize();

		ServerTickEvents.START_SERVER_TICK
				.register(ImprovedMapsLifecycleEvents::ImprovedMapsServerTick);
	}
}
