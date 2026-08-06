package com.craftycorvid.improvedmaps.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.craftycorvid.improvedmaps.MapBiomes;
import com.craftycorvid.improvedmaps.internal.IMapBiomeHolder;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import static com.craftycorvid.improvedmaps.ImprovedMaps.MOD_CONFIG;

@Mixin(MapItem.class)
public class MapBiomeCaptureMixin {
    // Every route to a map's data goes through here - held maps, item frames, our own atlas code -
    // so it is the one place the id and the data are both in hand. Stamp it on as it passes;
    // MapItem.update is handed the data alone and would otherwise have no way to file what it sees.
    @Inject(
            method = "getSavedData(Lnet/minecraft/world/level/saveddata/maps/MapId;Lnet/minecraft/world/level/Level;)Lnet/minecraft/world/level/saveddata/maps/MapItemSavedData;",
            at = @At("RETURN"))
    private static void stampMapId(MapId id, Level level,
            CallbackInfoReturnable<MapItemSavedData> cir) {
        MapItemSavedData data = cir.getReturnValue();
        if (data != null)
            ((IMapBiomeHolder) data).improvedmaps$setMapId(id);
    }

    // Vanilla collapses the block it sampled into a 6-bit MapColor id and drops everything else.
    // Catch the biome on the way past.
    //
    // The wrapped call supplies the pixel's own x and z, so the only local needed is the position
    // vanilla sampled - the first of the method's two MutableBlockPos, the one it walks down to the
    // surface. (The second is the scratch position for water depth.)
    @WrapOperation(method = "update", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/saveddata/maps/MapItemSavedData;updateColor(IIB)Z"))
    private boolean recordBiome(MapItemSavedData data, int x, int z, byte colour,
            Operation<Boolean> original, @Local(argsOnly = true) Level level,
            @Local(ordinal = 0) BlockPos.MutableBlockPos sampled) {
        boolean changed = original.call(data, x, z, colour);
        if (!MOD_CONFIG.server_cacheBiomeMapColors || !(level instanceof ServerLevel serverLevel))
            return changed;

        MapBiomes biomes = improvedmaps$biomesFor(data, serverLevel);
        // Recording only when the colour changed would never fill in a map explored before the
        // feature was switched on: walking back over it changes nothing, so nothing would ever be
        // recorded. Recording whenever the pixel has no biome yet is what lets an existing map
        // catch up as it is revisited, and costs one array read per pixel once it has.
        if (biomes == null || (!changed && biomes.has(x, z)))
            return changed;

        // ponytail: above scale 0 a pixel covers many columns and this is the last one vanilla
        // sampled, not the most common - vanilla picks the modal colour, we take a neighbour's
        // biome. Accumulate a multiset alongside vanilla's if a scale-4 map ever looks wrong at a
        // biome border.
        level.getBiome(sampled).unwrapKey().ifPresent(biome -> biomes.record(x, z, biome));
        return changed;
    }

    @Unique
    private static MapBiomes improvedmaps$biomesFor(MapItemSavedData data, ServerLevel level) {
        IMapBiomeHolder holder = (IMapBiomeHolder) data;
        MapBiomes biomes = holder.improvedmaps$biomes();
        if (biomes != null)
            return biomes;

        MapId id = holder.improvedmaps$mapId();
        if (id == null)
            return null;

        biomes = MapBiomes.getOrCreate(level.getServer(), id);
        holder.improvedmaps$setBiomes(biomes);
        return biomes;
    }
}
