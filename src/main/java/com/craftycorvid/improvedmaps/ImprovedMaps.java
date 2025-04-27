package com.craftycorvid.improvedmaps;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.recipe.SpecialCraftingRecipe.SpecialRecipeSerializer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.craftycorvid.improvedmaps.item.ImprovedMapsItems;
import com.craftycorvid.improvedmaps.recipe.AtlasRecipe;


public class ImprovedMaps implements ModInitializer {
	public static final String MOD_ID = "improved-maps";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);


	public static SpecialRecipeSerializer<AtlasRecipe> ATLAS_RECIPE_SERIALIZER;

	@Override
	public void onInitialize() {
		LOGGER.info("Improved Maps Initializing");

		ImprovedMapsItems.initialize();
		ImprovedMapsComponentTypes.initialize();

		ATLAS_RECIPE_SERIALIZER = Registry.register(Registries.RECIPE_SERIALIZER,
				Identifier.of(MOD_ID, "crafting_atlas"),
				new AtlasRecipe.Serializer(AtlasRecipe::new));
		ServerTickEvents.START_SERVER_TICK
				.register(ImprovedMapsLifecycleEvents::ImprovedMapsServerTick);
	}
}
