package com.craftycorvid.improvedmaps;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import com.craftycorvid.improvedmaps.item.ImprovedMapsItems;
import com.google.common.collect.Lists;
import eu.pb4.polymer.networking.api.server.PolymerServerNetworking;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.IntTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

public final class ImprovedMapsNetworking {
    public static final Set<UUID> PLAYERS_WITH_CLIENT = new HashSet<>();
    // Every map is 128x128 colour bytes, so an unbounded request list is an unbounded reply. The
    // codec rejects anything longer, so the client must ask in batches no larger than this.
    public static final int MAX_REQUESTED_MAPS = 1024;
    private static final int MAP_SIZE = 128;

    // The client asks for the maps of the atlas it is viewing: those it holds no pixels for, or
    // no centre for. Both halves of the reply are per-map, so it only ever asks once.
    public record AtlasViewRequest(List<MapId> ids) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<AtlasViewRequest> TYPE =
                new CustomPacketPayload.Type<>(ImprovedMaps.id("atlas_view_request"));
        public static final StreamCodec<ByteBuf, AtlasViewRequest> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.<ByteBuf, MapId, List<MapId>>collection(ArrayList::new,
                                MapId.STREAM_CODEC, MAX_REQUESTED_MAPS),
                        AtlasViewRequest::ids, AtlasViewRequest::new);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    // A map's centre never reaches a vanilla client - MapItemSavedData.createForClient leaves it
    // at 0,0 and ClientboundMapItemDataPacket has no field for it - but the atlas view needs it to
    // lay the maps out in a grid. The pixels still ride on the vanilla packet.
    public record MapCenter(MapId id, int x, int z) {
        public static final StreamCodec<ByteBuf, MapCenter> STREAM_CODEC = StreamCodec.composite(
                MapId.STREAM_CODEC, MapCenter::id,
                ByteBufCodecs.INT, MapCenter::x,
                ByteBufCodecs.INT, MapCenter::z,
                MapCenter::new);
    }

    public record AtlasMapCenters(List<MapCenter> centers) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<AtlasMapCenters> TYPE =
                new CustomPacketPayload.Type<>(ImprovedMaps.id("atlas_map_centers"));
        public static final StreamCodec<ByteBuf, AtlasMapCenters> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.<ByteBuf, MapCenter, List<MapCenter>>collection(
                                ArrayList::new, MapCenter.STREAM_CODEC, MAX_REQUESTED_MAPS),
                        AtlasMapCenters::centers, AtlasMapCenters::new);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void initialize() {
        PayloadTypeRegistry.serverboundPlay().register(AtlasViewRequest.TYPE,
                AtlasViewRequest.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(AtlasMapCenters.TYPE,
                AtlasMapCenters.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(AtlasViewRequest.TYPE,
                (payload, context) -> sendAtlasView(context.player(), payload.ids()));

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

    private static void sendAtlasView(ServerPlayer player, List<MapId> requested) {
        Set<MapId> carried = mapsInCarriedAtlases(player);
        List<MapCenter> centers = new ArrayList<>();

        for (MapId id : requested) {
            // Answer only for maps the player is actually carrying an atlas of. Without this a
            // modified client could ask for, and be handed, every map on the server.
            if (!carried.contains(id))
                continue;

            MapItemSavedData data = MapItem.getSavedData(id, player.level());
            if (data == null)
                continue;

            centers.add(new MapCenter(id, data.centerX, data.centerZ));
            // A client that has never held this map creates its own copy from this packet, so the
            // pixels need no handler of ours. Colours are copied because encoding happens later,
            // off the server thread.
            player.connection.send(new ClientboundMapItemDataPacket(id, data.scale, data.locked,
                    Lists.newArrayList(data.getDecorations()),
                    new MapItemSavedData.MapPatch(0, 0, MAP_SIZE, MAP_SIZE, data.colors.clone())));
        }

        if (!centers.isEmpty())
            ServerPlayNetworking.send(player, new AtlasMapCenters(centers));
    }

    private static Set<MapId> mapsInCarriedAtlases(ServerPlayer player) {
        Set<MapId> ids = new HashSet<>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.is(ImprovedMapsItems.ATLAS))
                continue;
            stack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY)
                    .itemCopyStream().forEach(map -> {
                        MapId id = map.get(DataComponents.MAP_ID);
                        if (id != null)
                            ids.add(id);
                    });
        }
        return ids;
    }
}
