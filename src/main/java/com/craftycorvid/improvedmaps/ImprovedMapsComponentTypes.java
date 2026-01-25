package com.craftycorvid.improvedmaps;

import com.mojang.serialization.Codec;
import eu.pb4.polymer.core.api.other.PolymerComponent;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;

import static com.craftycorvid.improvedmaps.ImprovedMaps.id;

public class ImprovedMapsComponentTypes {
        public static final DataComponentType<Integer> ATLAS_EMPTY_MAP_COUNT = new DataComponentType.Builder<Integer>().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build();
        public static final DataComponentType<Byte> ATLAS_SCALE = new DataComponentType.Builder<Byte>().persistent(Codec.BYTE).networkSynchronized(ByteBufCodecs.BYTE).build();
        public static final DataComponentType<String> ATLAS_DIMENSION = new DataComponentType.Builder<String>().persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8).build();
        public static final DataComponentType<Boolean> ATLAS_INITIALIZED = new DataComponentType.Builder<Boolean>().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build();

        public static final ResourceLocation ATLAS_EMPTY_MAP_DATA = id("atlas_empty_map_count");
        public static final ResourceLocation ATLAS_SCALE_DATA = id("atlas_scale");
        public static final ResourceLocation ATLAS_DIMENSION_DATA = id("atlas_dimension");
        public static final ResourceLocation ATLAS_INITIALIZED_DATA = id("atlas_initialized");


        public static void initialize() {
                Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, ATLAS_EMPTY_MAP_DATA, ATLAS_EMPTY_MAP_COUNT);
                Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, ATLAS_SCALE_DATA, ATLAS_SCALE);
                Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, ATLAS_DIMENSION_DATA, ATLAS_DIMENSION);
                Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, ATLAS_INITIALIZED_DATA, ATLAS_INITIALIZED);
                PolymerComponent.registerDataComponent(ATLAS_EMPTY_MAP_COUNT);
                PolymerComponent.registerDataComponent(ATLAS_SCALE);
                PolymerComponent.registerDataComponent(ATLAS_DIMENSION);
                PolymerComponent.registerDataComponent(ATLAS_INITIALIZED);

        }
}
