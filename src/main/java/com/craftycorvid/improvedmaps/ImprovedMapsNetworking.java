package com.craftycorvid.improvedmaps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import com.craftycorvid.improvedmaps.item.ImprovedMapsItems;
import com.google.common.collect.Lists;
import eu.pb4.polymer.core.api.utils.PolymerSyncUtils;
import eu.pb4.polymer.core.api.utils.PolymerUtils;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.VarInt;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import static com.craftycorvid.improvedmaps.ImprovedMaps.MOD_CONFIG;

public final class ImprovedMapsNetworking {
    // Read from Netty encode threads, written from the server thread.
    public static final Set<UUID> PLAYERS_WITH_CLIENT = ConcurrentHashMap.newKeySet();
    // Per player, the MapBiomes version last sent for each map. Biome data keeps growing as a player
    // explores, so "sent once" is not enough - but it changes at vanilla's own map-update cadence,
    // so re-sending whenever the version moves costs about what a map patch does.
    private static final Map<UUID, Map<MapId, Integer>> SENT_BIOMES = new HashMap<>();
    // Every map is 128x128 colour bytes, so an unbounded request list is an unbounded reply. The
    // codec rejects anything longer, so the client must ask in batches no larger than this.
    public static final int MAX_REQUESTED_MAPS = 1024;
    private static final int MAP_SIZE = 128;
    // Matches the palette limit MapBiomes writes, so a well-formed record always survives the trip.
    private static final int MAX_BIOME_PALETTE = 255;

    // A map's biomes come in large contiguous runs, so the raw byte-per-pixel array (16 KB a map,
    // 8 MB to fill the atlas view) is worth run-length encoding. Measured: 4 bytes for a map inside
    // one biome, 6 for a map split down the middle.
    //
    // ponytail: no raw fallback, so a map whose biome changes on most adjacent pixels encodes to
    // 32 KB - twice the raw array. Terrain does not fragment anything like that finely, even at
    // scale 4. Add a leading "raw" flag byte if a real map is ever measured above 16 KB.
    private static final StreamCodec<ByteBuf, byte[]> RUN_LENGTH_ENCODED =
            new StreamCodec<ByteBuf, byte[]>() {
                @Override
                public byte[] decode(ByteBuf buf) {
                    byte[] indices = new byte[MapBiomes.PIXELS];
                    int at = 0;
                    while (at < MapBiomes.PIXELS) {
                        int run = VarInt.read(buf);
                        // A hostile or broken sender must not be able to walk off the array.
                        if (run <= 0 || run > MapBiomes.PIXELS - at)
                            throw new DecoderException("Bad map biome run " + run + " at " + at);
                        Arrays.fill(indices, at, at + run, buf.readByte());
                        at += run;
                    }
                    return indices;
                }

                @Override
                public void encode(ByteBuf buf, byte[] indices) {
                    if (indices.length != MapBiomes.PIXELS)
                        throw new IllegalArgumentException(
                                "Map biomes must cover " + MapBiomes.PIXELS + " pixels");
                    int at = 0;
                    while (at < MapBiomes.PIXELS) {
                        int run = 1;
                        while (at + run < MapBiomes.PIXELS && indices[at + run] == indices[at])
                            run++;
                        VarInt.write(buf, run);
                        buf.writeByte(indices[at]);
                        at += run;
                    }
                }
            };

    // Until the client acks Polymer's registry sync it cannot resolve our item id, so the atlas has
    // to go out as a vanilla stand-in - the sync packet itself carries one.
    public record ClientReady() implements CustomPacketPayload {
        public static final ClientReady INSTANCE = new ClientReady();
        public static final CustomPacketPayload.Type<ClientReady> TYPE =
                new CustomPacketPayload.Type<>(ImprovedMaps.id("client_ready"));
        public static final StreamCodec<ByteBuf, ClientReady> STREAM_CODEC =
                StreamCodec.unit(INSTANCE);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

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

    // Vanilla map pixels are a 6-bit MapColor id, so every biome's grass is the same green. The
    // client cannot widen that palette on its own - MapColor is a hardcoded table - and it cannot
    // work the biome out either, since a map usually covers terrain no client has loaded. So the
    // server, which sampled the block in the first place, says what was there.
    public record MapBiomesPayload(MapId id, List<Identifier> palette, byte[] indices)
            implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<MapBiomesPayload> TYPE =
                new CustomPacketPayload.Type<>(ImprovedMaps.id("map_biomes"));
        public static final StreamCodec<ByteBuf, MapBiomesPayload> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.<ByteBuf, Identifier, List<Identifier>>collection(
                                ArrayList::new, Identifier.STREAM_CODEC, MAX_BIOME_PALETTE),
                        MapBiomesPayload::palette, RUN_LENGTH_ENCODED,
                        MapBiomesPayload::indices, MapId.STREAM_CODEC, MapBiomesPayload::id,
                        (palette, indices, id) -> new MapBiomesPayload(id, palette, indices));

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void initialize() {
        PayloadTypeRegistry.serverboundPlay().register(AtlasViewRequest.TYPE,
                AtlasViewRequest.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ClientReady.TYPE, ClientReady.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(AtlasMapCenters.TYPE,
                AtlasMapCenters.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(MapBiomesPayload.TYPE,
                MapBiomesPayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(AtlasViewRequest.TYPE,
                (payload, context) -> sendAtlasView(context.player(), payload.ids()));

        ServerPlayNetworking.registerGlobalReceiver(ClientReady.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            // Nothing rate limits a serverbound payload, and the refresh below is not free.
            if (!PLAYERS_WITH_CLIENT.add(player.getUUID()))
                return;
            // Everything sent before now shows the stand-in, the creative tab included.
            PolymerUtils.reloadInventory(player);
            PolymerSyncUtils.synchronizeCreativeTabs(player.connection);
        });

        // Polymer resyncs mid-session too, on a client language change.
        PolymerSyncUtils.ON_SYNC_STARTED
                .register(handler -> PLAYERS_WITH_CLIENT.remove(handler.getPlayer().getUUID()));

        ServerPlayConnectionEvents.DISCONNECT
                .register((ServerGamePacketListenerImpl handler, MinecraftServer server) -> {
                    PLAYERS_WITH_CLIENT.remove(handler.getPlayer().getUUID());
                    SENT_BIOMES.remove(handler.getPlayer().getUUID());
                });
    }

    // Called wherever the server hands a player a map's pixels: MapBiomeSyncMixin for held maps and
    // item frames, sendAtlasView for the maps behind the atlas view.
    public static void sendBiomes(ServerPlayer player, MapId id) {
        if (!MOD_CONFIG.server_cacheBiomeMapColors || !PLAYERS_WITH_CLIENT.contains(player.getUUID()))
            return;

        MapBiomes biomes = MapBiomes.find(player.level().getServer(), id);
        if (biomes == null) // nothing recorded yet - this map has not been walked since it was on
            return;

        Map<MapId, Integer> sent = SENT_BIOMES.computeIfAbsent(player.getUUID(),
                uuid -> new HashMap<>());
        if (Integer.valueOf(biomes.version()).equals(sent.get(id)))
            return;
        sent.put(id, biomes.version());

        List<Identifier> palette =
                biomes.palette().stream().map(ResourceKey::identifier).toList();
        ServerPlayNetworking.send(player,
                new MapBiomesPayload(id, palette, biomes.indices().clone()));
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
            // These pixels bypass getUpdatePacket, so the sync mixin never sees them.
            sendBiomes(player, id);
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
