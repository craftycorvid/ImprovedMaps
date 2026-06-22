package com.craftycorvid.improvedmaps;

import eu.pb4.polymer.networking.api.client.PolymerClientNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.ClientTooltipComponentCallback;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientBundleTooltip;
import net.minecraft.nbt.IntTag;

public class ImprovedMapsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		PolymerClientNetworking.setClientMetadata(ImprovedMaps.HELLO_PACKET, IntTag.valueOf(1));
		HudElementRegistry.addLast(ImprovedMaps.id("minimap"), MinimapHud::render);

		// Render an atlas's bundle tooltip with a capacity-scaled fullness bar.
		ClientTooltipComponentCallback.EVENT.register(data -> {
			if (data instanceof AtlasTooltipData atlas) {
				ClientBundleTooltip tooltip = new ClientBundleTooltip(atlas.contents());
				((AtlasFullnessHolder) tooltip).improvedmaps$setFullness(atlas.fullness());
				return tooltip;
			}
			return null;
		});
	}
}
