package com.craftycorvid.improvedmaps;

import com.mojang.serialization.Codec;
import eu.pb4.polymer.core.api.other.PolymerComponent;
import net.minecraft.component.ComponentType;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import static com.craftycorvid.improvedmaps.ImprovedMaps.id;

public class ImprovedMapsComponentTypes {
        public static final ComponentType<Integer> ATLAS_EMPTY_MAP_COUNT =
                        new ComponentType.Builder<Integer>().codec(Codec.INT)
                                        .packetCodec(PacketCodecs.VAR_INT).build();
        public static final ComponentType<Integer> ATLAS_SCALE =
                        new ComponentType.Builder<Integer>().codec(Codec.INT)
                                        .packetCodec(PacketCodecs.VAR_INT).build();
        public static final ComponentType<String> ATLAS_DIMENSION =
                        new ComponentType.Builder<String>().codec(Codec.STRING)
                                        .packetCodec(PacketCodecs.STRING).build();

        public static final Identifier ATLAS_EMPTY_MAP_DATA = id("atlas_empty_map_count");
        public static final Identifier ATLAS_SCALE_DATA = id("atlas_scale");
        public static final Identifier ATLAS_DIMENSION_DATA = id("atlas_dimension");

        public static void initialize() {
                Registry.register(Registries.DATA_COMPONENT_TYPE, ATLAS_EMPTY_MAP_DATA,
                                ATLAS_EMPTY_MAP_COUNT);
                Registry.register(Registries.DATA_COMPONENT_TYPE, ATLAS_SCALE_DATA, ATLAS_SCALE);
                Registry.register(Registries.DATA_COMPONENT_TYPE, ATLAS_DIMENSION_DATA,
                                ATLAS_DIMENSION);
                PolymerComponent.registerDataComponent(ATLAS_EMPTY_MAP_COUNT);
                PolymerComponent.registerDataComponent(ATLAS_SCALE);
                PolymerComponent.registerDataComponent(ATLAS_DIMENSION);

        }
}
