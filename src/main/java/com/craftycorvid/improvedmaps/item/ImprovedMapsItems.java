package com.craftycorvid.improvedmaps.item;

import java.util.function.Function;

import com.craftycorvid.improvedmaps.ImprovedMaps;
import com.craftycorvid.improvedmaps.ImprovedMapsComponentTypes;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import static com.craftycorvid.improvedmaps.ImprovedMaps.id;

public class ImprovedMapsItems {
        public static final Item ATLAS = register("atlas", AtlasItem::new,
                        (new Item.Settings()).maxCount(1)
                                        .component(DataComponentTypes.BUNDLE_CONTENTS, BundleContentsComponent.DEFAULT)
                                        .component(ImprovedMapsComponentTypes.ATLAS_DIMENSION, "minecraft:overworld")
                                        .component(ImprovedMapsComponentTypes.ATLAS_SCALE, (byte) 0)
                                        .component(ImprovedMapsComponentTypes.ATLAS_EMPTY_MAP_COUNT, 0)
                                        .component(ImprovedMapsComponentTypes.ATLAS_INITIALIZED, true));

        public static ItemStack getAtlasWith(String dimension, byte scale) {
                ItemStack itemStack = new ItemStack(ATLAS);
                itemStack.set(ImprovedMapsComponentTypes.ATLAS_EMPTY_MAP_COUNT, 9999);
                itemStack.set(ImprovedMapsComponentTypes.ATLAS_SCALE, scale);
                itemStack.set(ImprovedMapsComponentTypes.ATLAS_DIMENSION, dimension);
                itemStack.set(ImprovedMapsComponentTypes.ATLAS_INITIALIZED, false);
                return itemStack;
        }

        public static final ItemGroup IMPROVED_MAPS_GROUP = Registry.register(
                Registries.ITEM_GROUP,
                Identifier.of(ImprovedMaps.MOD_ID, "atlas_tab"),
                new ItemGroup.Builder(ItemGroup.Row.TOP, 0) // TOP row, first column
                        .displayName(Text.translatable("itemGroup." + ImprovedMaps.MOD_ID + ".atlas_tab"))
                        .icon(() -> new ItemStack(ImprovedMapsItems.ATLAS))
                        .entries((context, entries) -> {
                                for (int scale = 0; scale <= 4; scale++) {
                                        entries.add(getAtlasWith("minecraft:overworld", (byte) scale));
                                }
                                for (int scale = 0; scale <= 4; scale++) {
                                        entries.add(getAtlasWith("minecraft:the_nether", (byte) scale));
                                }
                                for (int scale = 0; scale <= 4; scale++) {
                                        entries.add(getAtlasWith("minecraft:the_end", (byte) scale));
                                }
                        })
                        .build()
        );


        public static void initialize() {

        }

        public static Item register(String name, Function<Item.Settings, Item> itemFactory,
                        Item.Settings settings) {
                RegistryKey<Item> itemKey = RegistryKey.of(RegistryKeys.ITEM, id(name));
                Item item = itemFactory.apply(settings.registryKey(itemKey));
                Registry.register(Registries.ITEM, itemKey, item);
                return item;
        }
}
