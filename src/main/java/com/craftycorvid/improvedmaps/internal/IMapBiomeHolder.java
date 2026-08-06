package com.craftycorvid.improvedmaps.internal;

import com.craftycorvid.improvedmaps.MapBiomes;
import net.minecraft.world.level.saveddata.maps.MapId;

// Implemented by MapItemSavedData through MapStateMixin. Vanilla's map data does not carry its own
// id - MapItem.update is handed the data alone - but the biome record for a map is filed under it.
// The resolved record is cached here too: capture runs once per scanned pixel, and looking it up
// each time would build a SavedDataType, an Identifier and a string per pixel.
public interface IMapBiomeHolder {
    MapId improvedmaps$mapId();

    void improvedmaps$setMapId(MapId id);

    MapBiomes improvedmaps$biomes();

    void improvedmaps$setBiomes(MapBiomes biomes);
}
