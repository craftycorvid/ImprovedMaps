package com.craftycorvid.improvedmaps.item;

import java.util.function.Function;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;

import static com.craftycorvid.improvedmaps.ImprovedMaps.id;

public class ImprovedMapsItems {
        public static final Item ATLAS = register("atlas", AtlasItem::new,
                        (new Item.Settings()).maxCount(1).component(
                                        DataComponentTypes.BUNDLE_CONTENTS,
                                        BundleContentsComponent.DEFAULT));

        public static void initialize() {
                ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS)
                                .register((itemGroup) -> itemGroup.add(ATLAS));
        }

        public static Item register(String name, Function<Item.Settings, Item> itemFactory,
                        Item.Settings settings) {
                RegistryKey<Item> itemKey = RegistryKey.of(RegistryKeys.ITEM, id(name));
                Item item = itemFactory.apply(settings.registryKey(itemKey));
                Registry.register(Registries.ITEM, itemKey, item);
                return item;
        }
}
