package com.craftycorvid.improvedmaps.item;

import java.util.function.Function;
import com.craftycorvid.improvedmaps.ImprovedMapsComponentTypes;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;

import static com.craftycorvid.improvedmaps.ImprovedMaps.id;

public class ImprovedMapsItems {
        public static final Item ATLAS = register("atlas", AtlasItem::new,
                        (new Item.Settings()).maxCount(1)
                                        .component(DataComponentTypes.BUNDLE_CONTENTS,
                                                        BundleContentsComponent.DEFAULT)
                                        .component(ImprovedMapsComponentTypes.ATLAS_DIMENSION,
                                                        "minecraft:overworld")
                                        .component(ImprovedMapsComponentTypes.ATLAS_SCALE, (byte) 0)
                                        .component(ImprovedMapsComponentTypes.ATLAS_EMPTY_MAP_COUNT,
                                                        0));

        public static ItemStack getAtlasWith(String dimension) {
                ItemStack itemStack = new ItemStack(ATLAS);
                itemStack.set(ImprovedMapsComponentTypes.ATLAS_DIMENSION, dimension);
                return itemStack;
        }

        public static void initialize() {
                // TODO: Add Atlas to the Creative Inventory
                ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register((itemGroup) -> {
                        itemGroup.add(getAtlasWith("minecraft:overworld"));
                        itemGroup.add(getAtlasWith("minecraft:the_nether"));
                        itemGroup.add(getAtlasWith("minecraft:the_end"));
                });
        }

        public static Item register(String name, Function<Item.Settings, Item> itemFactory,
                        Item.Settings settings) {
                RegistryKey<Item> itemKey = RegistryKey.of(RegistryKeys.ITEM, id(name));
                Item item = itemFactory.apply(settings.registryKey(itemKey));
                Registry.register(Registries.ITEM, itemKey, item);
                return item;
        }
}
