package com.craftycorvid.improvedmaps;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.saveddata.maps.MapId;

// The biome behind every pixel of one map. Vanilla throws this away: a map pixel is a 6-bit MapColor
// id, so every biome's grass ends up the same green. Recorded here so a client with the mod can tint
// the palette, and recorded server-side rather than sampled per-client so that every player sees the
// same colours on the same map - a client sampling its own loaded chunks can only colour terrain
// that player personally visited.
//
// One byte per pixel plus a palette. Biomes run in large contiguous blocks, so the gzipped file
// lands near vanilla's own map_<n>.dat rather than the 16 KB the raw array suggests.
public final class MapBiomes extends SavedData {
    public static final int PIXELS = 128 * 128;
    private static final int MAP_SIZE = 128;
    // Palette index 0 means "never recorded" - a pixel drawn before this feature was switched on,
    // which the client leaves at its vanilla colour. Palette entry i is stored as index i + 1.
    private static final int MAX_PALETTE = 255;

    public static final Codec<MapBiomes> CODEC = RecordCodecBuilder.create(instance -> instance
            .group(ResourceKey.codec(Registries.BIOME).listOf().fieldOf("palette")
                    .forGetter(biomes -> biomes.palette),
                    Codec.BYTE_BUFFER.fieldOf("indices")
                            .forGetter(biomes -> ByteBuffer.wrap(biomes.indices)))
            .apply(instance, MapBiomes::new));

    private final List<ResourceKey<Biome>> palette;
    private final byte[] indices;
    // Bumped whenever a pixel's biome changes, so a player who already has this map can be sent the
    // difference. Deliberately not persisted: it only has to be comparable within one connection.
    private transient int version;

    public MapBiomes() {
        this(new ArrayList<>(), new byte[PIXELS]);
    }

    private MapBiomes(List<ResourceKey<Biome>> palette, ByteBuffer indices) {
        this(new ArrayList<>(palette), read(indices));
    }

    private MapBiomes(List<ResourceKey<Biome>> palette, byte[] indices) {
        this.palette = palette;
        this.indices = indices;
    }

    // A hand-edited or truncated file must not take the server down with it.
    private static byte[] read(ByteBuffer buffer) {
        byte[] indices = new byte[PIXELS];
        buffer.get(0, indices, 0, Math.min(buffer.remaining(), PIXELS));
        return indices;
    }

    private static SavedDataType<MapBiomes> type(MapId id) {
        // SAVED_DATA_MAP_DATA is what this data is about; its fixers only touch vanilla map fields,
        // which we never write, so they no-op on ours.
        return new SavedDataType<>(ImprovedMaps.id("map_biomes_" + id.id()), MapBiomes::new, CODEC,
                DataFixTypes.SAVED_DATA_MAP_DATA);
    }

    // Map data lives in the overworld's storage whichever dimension the map is of, so the biome
    // record has to sit beside it or the two drift apart.
    public static MapBiomes getOrCreate(MinecraftServer server, MapId id) {
        return server.overworld().getDataStorage().computeIfAbsent(type(id));
    }

    // Null when nothing has ever been recorded for this map, which is the normal state for maps
    // explored before the feature was switched on. Kept separate from getOrCreate so that merely
    // looking at a map does not write a file for it.
    public static MapBiomes find(MinecraftServer server, MapId id) {
        return server.overworld().getDataStorage().get(type(id));
    }

    // Whether this pixel has ever been recorded. Lets the capture skip the biome lookup for ground
    // it already knows, so revisiting a finished map costs one array read per pixel.
    public boolean has(int x, int z) {
        return indices[x + z * MAP_SIZE] != 0;
    }

    public void record(int x, int z, ResourceKey<Biome> biome) {
        int entry = palette.indexOf(biome);
        if (entry < 0) {
            // ponytail: a map spanning >255 biomes keeps the first 255 and leaves the rest vanilla.
            // Real maps see a handful; widen the index to a short if that ever stops being true.
            if (palette.size() >= MAX_PALETTE)
                return;
            palette.add(biome);
            entry = palette.size() - 1;
        }

        byte value = (byte) (entry + 1);
        int pixel = x + z * MAP_SIZE;
        if (indices[pixel] == value)
            return;
        indices[pixel] = value;
        version++;
        setDirty();
    }

    public int version() {
        return version;
    }

    public List<ResourceKey<Biome>> palette() {
        return palette;
    }

    public byte[] indices() {
        return indices;
    }
}
