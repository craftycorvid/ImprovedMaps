package com.craftycorvid.improvedmaps;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.CustomRecipe.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.craftycorvid.improvedmaps.config.ModConfig;
import com.craftycorvid.improvedmaps.item.ImprovedMapsItems;
import com.craftycorvid.improvedmaps.recipe.AtlasCopyRecipe;
import com.craftycorvid.improvedmaps.recipe.AtlasRecipe;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;

public final class ImprovedMaps implements ModInitializer {
	public static final String MOD_ID = "improved-maps";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final Identifier HELLO_PACKET = id("hello");
	public static ModConfig MOD_CONFIG;

	public static Serializer<AtlasRecipe> ATLAS_RECIPE_SERIALIZER;
	public static Serializer<AtlasCopyRecipe> ATLAS_COPY_RECIPE_SERIALIZER;

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		LOGGER.info("Improved Maps Initializing");
		MOD_CONFIG = ModConfig.loadConfig();

		ImprovedMapsNetworking.initialize();
		ImprovedMapsItems.initialize();
		ImprovedMapsComponentTypes.initialize();
		ImprovedMapsModifyVanillaItems.initialize();
		PolymerResourcePackUtils.addModAssets(MOD_ID);

		ATLAS_RECIPE_SERIALIZER = Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
				id("crafting_atlas"), new AtlasRecipe.Serializer(AtlasRecipe::new));
		ATLAS_COPY_RECIPE_SERIALIZER = Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
				id("copy_atlas"), new AtlasCopyRecipe.Serializer(AtlasCopyRecipe::new));
		ServerTickEvents.START_SERVER_TICK
				.register(ImprovedMapsLifecycleEvents::ImprovedMapsServerTick);
	}
}
