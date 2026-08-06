package com.craftycorvid.improvedmaps;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapId;
import com.craftycorvid.improvedmaps.ImprovedMapsNetworking.MapBiomesPayload;

import static com.craftycorvid.improvedmaps.ImprovedMaps.MOD_CONFIG;

// Shifts a map pixel's vanilla colour towards the colour its biome is drawn in the world, using the
// biome the server recorded when it sampled the block. Vanilla maps have no tint at all - every
// biome's grass is MapColor.GRASS - so a swamp, a jungle and a plains all come out the same green.
public final class MapBiomeTints {
    private static final int GRASS = 0;
    private static final int FOLIAGE = 1;
    private static final int WATER = 2;
    private static final ColorResolver[] RESOLVERS = {BiomeColors.GRASS_COLOR_RESOLVER,
            BiomeColors.FOLIAGE_COLOR_RESOLVER, BiomeColors.WATER_COLOR_RESOLVER};
    // The biome each vanilla palette entry is treated as already representing. Tinting is applied as
    // a ratio against these, so in plains and ocean the output is exactly what vanilla draws today
    // and everywhere else shifts by as much as that biome differs. Multiplying the palette colour by
    // the tint outright would double-apply it - MapColor.GRASS is already a tinted green, not the
    // greyscale texture the world renderer starts from.
    private static final ResourceKey<Biome>[] REFERENCES = referenceBiomes();

    // Per map: the palette the server sent plus one palette index per pixel (0 = never recorded).
    private record Entry(List<ResourceKey<Biome>> palette, byte[] indices) {
    }

    private static final Map<MapId, Entry> MAPS = new HashMap<>();
    // Biome to its tint under each resolver. Grass and foliage colours come out of the resource
    // pack's colormaps, so this is dropped whenever resources reload.
    private static final Map<ResourceKey<Biome>, int[]> TINTS = new HashMap<>();
    private static int[] references;

    @SuppressWarnings("unchecked")
    private static ResourceKey<Biome>[] referenceBiomes() {
        return new ResourceKey[] {Biomes.PLAINS, Biomes.PLAINS, Biomes.OCEAN};
    }

    public static void accept(MapBiomesPayload payload) {
        List<ResourceKey<Biome>> palette = payload.palette().stream()
                .map(id -> ResourceKey.create(Registries.BIOME, id)).toList();
        MAPS.put(payload.id(), new Entry(palette, payload.indices()));
    }

    // Map ids are per-world, so none of this may outlive the connection.
    public static void forget() {
        MAPS.clear();
        dropTints();
    }

    // A pack swap changes what colour a biome is, so every cached tint and every already-built map
    // texture is stale.
    public static void resourcesReloaded() {
        dropTints();
        refresh();
    }

    // Re-draw every map texture under the current settings. Needed when the tint is switched on or
    // off, because a map nobody is carrying - an atlas view, a wall of item frames - gets no packets
    // to trigger a rebuild of its own. Cheap: textures rebuild lazily from the MapItemSavedData the
    // client already holds.
    public static void refresh() {
        Minecraft.getInstance().getMapTextureManager().resetData();
    }

    private static void dropTints() {
        TINTS.clear();
        references = null;
    }

    // Called once per pixel while a map texture is being built. `packed` is the pixel's vanilla
    // colour byte and `argb` the colour vanilla resolved it to, brightness already applied -
    // brightness is itself a per-channel multiply, so shifting afterwards gives the same result as
    // shifting first.
    public static int tint(int rawMapId, int pixel, byte packed, int argb) {
        // This client's own switch, not the server's: a server that never sends anything leaves
        // MAPS empty and tints nothing anyway, so there is nothing to check twice.
        if (!MOD_CONFIG.client_showBiomeMapColors)
            return argb;

        Entry entry = MAPS.get(new MapId(rawMapId));
        if (entry == null)
            return argb;

        int paletteIndex = entry.indices()[pixel] & 0xFF;
        if (paletteIndex == 0 || paletteIndex > entry.palette().size())
            return argb; // drawn before the server started recording, or a short palette

        // The server sent the biome but not the block, so which tint applies has to come from the
        // vanilla palette entry. These three are the only ones whose blocks are tinted in world;
        // everything else is drawn exactly as it is today.
        int resolver = resolverFor((packed & 0xFF) >> 2);
        if (resolver < 0)
            return argb;

        int[] tints = tintsFor(entry.palette().get(paletteIndex - 1));
        int[] reference = referenceTints();
        if (tints == null || reference == null)
            return argb;
        return shift(argb, tints[resolver], reference[resolver]);
    }

    private static int resolverFor(int mapColorId) {
        if (mapColorId == MapColor.GRASS.id)
            return GRASS;
        if (mapColorId == MapColor.PLANT.id)
            return FOLIAGE;
        if (mapColorId == MapColor.WATER.id)
            return WATER;
        return -1;
    }

    private static int[] tintsFor(ResourceKey<Biome> key) {
        // Not computeIfAbsent: a biome the client cannot resolve caches as null, and computeIfAbsent
        // would re-resolve it for every pixel of every map.
        if (TINTS.containsKey(key))
            return TINTS.get(key);

        int[] tints = resolve(key);
        TINTS.put(key, tints);
        return tints;
    }

    private static int[] resolve(ResourceKey<Biome> key) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null)
            return null;

        Biome biome = level.registryAccess().lookupOrThrow(Registries.BIOME).get(key)
                .map(Holder::value).orElse(null);
        if (biome == null)
            return null;

        int[] tints = new int[RESOLVERS.length];
        for (int i = 0; i < RESOLVERS.length; i++) {
            // The coordinates only feed swamp's per-position noise wobble. We deliberately have no
            // world coordinates on the client - biomes arrive by pixel index - and one flat swamp
            // green reads better than noise at one block per pixel anyway.
            tints[i] = RESOLVERS[i].getColor(biome, 0, 0);
        }
        return tints;
    }

    private static int[] referenceTints() {
        if (references != null)
            return references;

        int[] tints = new int[RESOLVERS.length];
        for (int i = 0; i < RESOLVERS.length; i++) {
            int[] resolved = tintsFor(REFERENCES[i]);
            if (resolved == null)
                return null;
            tints[i] = resolved[i];
        }
        references = tints;
        return references;
    }

    private static int shift(int argb, int tint, int reference) {
        return (argb & 0xFF000000) | channel(argb, tint, reference, 16) << 16
                | channel(argb, tint, reference, 8) << 8 | channel(argb, tint, reference, 0);
    }

    private static int channel(int argb, int tint, int reference, int offset) {
        int base = argb >> offset & 0xFF;
        int against = reference >> offset & 0xFF;
        if (against == 0)
            return base;
        return Math.clamp(base * (tint >> offset & 0xFF) / against, 0, 255);
    }
}
