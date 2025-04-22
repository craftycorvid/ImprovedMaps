package com.craftycorvid.improvedmaps;

import com.mojang.serialization.Codec;
import eu.pb4.polymer.core.api.other.PolymerComponent;
import net.minecraft.component.ComponentType;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ImprovedMapsComponentTypes {
        public static final ComponentType<Integer> ATLAS_EMPTY_MAP_COUNT =
                        new ComponentType.Builder<Integer>().codec(Codec.INT)
                                        .packetCodec(PacketCodecs.VAR_INT).build();
        public static final ComponentType<Integer> ATLAS_SCALE =
                        new ComponentType.Builder<Integer>().codec(Codec.INT)
                                        .packetCodec(PacketCodecs.VAR_INT).build();

        public static void initialize() {
                Registry.register(Registries.DATA_COMPONENT_TYPE,
                                Identifier.of(ImprovedMaps.MOD_ID, "atlas_empty_map_count"),
                                ATLAS_EMPTY_MAP_COUNT);
                Registry.register(Registries.DATA_COMPONENT_TYPE,
                                Identifier.of(ImprovedMaps.MOD_ID, "atlas_scale"), ATLAS_SCALE);
                PolymerComponent.registerDataComponent(ATLAS_EMPTY_MAP_COUNT);
                PolymerComponent.registerDataComponent(ATLAS_SCALE);
        }
}
