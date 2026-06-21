package com.craftycorvid.improvedmaps;

import eu.pb4.polymer.networking.api.client.PolymerClientNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.nbt.IntTag;

public class ImprovedMapsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		PolymerClientNetworking.setClientMetadata(ImprovedMaps.HELLO_PACKET, IntTag.valueOf(1));
		HudElementRegistry.addLast(ImprovedMaps.id("minimap"), MinimapHud::render);
	}
}
