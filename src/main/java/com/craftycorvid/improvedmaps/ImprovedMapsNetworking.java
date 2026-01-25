package com.craftycorvid.improvedmaps;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import eu.pb4.polymer.networking.api.server.PolymerServerNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.nbt.IntTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

public final class ImprovedMapsNetworking {
    public static final Set<UUID> PLAYERS_WITH_CLIENT = new HashSet<>();

    public static void initialize() {
        ServerPlayConnectionEvents.JOIN.register(
                (ServerGamePacketListenerImpl handler, PacketSender sender, MinecraftServer server) -> {
                    if (PolymerServerNetworking.getMetadata(handler, ImprovedMaps.HELLO_PACKET,
                            IntTag.TYPE) != null) {
                        PLAYERS_WITH_CLIENT.add(handler.getPlayer().getUUID());
                    }
                });
        ServerPlayConnectionEvents.DISCONNECT
                .register((ServerGamePacketListenerImpl handler, MinecraftServer server) -> {
                    PLAYERS_WITH_CLIENT.remove(handler.getPlayer().getUUID());
                });
    }
}
