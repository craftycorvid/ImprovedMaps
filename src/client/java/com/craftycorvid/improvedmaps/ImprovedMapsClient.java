package com.craftycorvid.improvedmaps;

import eu.pb4.polymer.networking.api.client.PolymerClientNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.nbt.NbtInt;

public class ImprovedMapsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		PolymerClientNetworking.setClientMetadata(ImprovedMaps.HELLO_PACKET, NbtInt.of(1));
	}
}
