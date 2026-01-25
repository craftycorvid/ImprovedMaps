package com.craftycorvid.improvedmaps.item;

import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import com.craftycorvid.improvedmaps.ImprovedMaps;
import com.craftycorvid.improvedmaps.ImprovedMapsComponentTypes;

import static com.craftycorvid.improvedmaps.ImprovedMaps.id;

public class ImprovedMapsItems {
        public static final Item ATLAS = register("atlas", AtlasItem::new, (new Item.Properties())
                        .stacksTo(1)
                        .component(DataComponents.BUNDLE_CONTENTS,
                                        BundleContents.EMPTY)
                        .component(ImprovedMapsComponentTypes.ATLAS_DIMENSION,
                                        "minecraft:overworld")
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

        public static final CreativeModeTab IMPROVED_MAPS_GROUP = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
                        Identifier.fromNamespaceAndPath(ImprovedMaps.MOD_ID, "atlas_tab"),
                        new CreativeModeTab.Builder(CreativeModeTab.Row.TOP, 0) // TOP row, first column
                                        .title(Component.translatable("itemGroup."
                                                        + ImprovedMaps.MOD_ID + ".atlas_tab"))
                                        .icon(() -> new ItemStack(ImprovedMapsItems.ATLAS))
                                        .displayItems((context, entries) -> {
                                                for (int scale = 0; scale <= 4; scale++) {
                                                        entries.accept(getAtlasWith(
                                                                        "minecraft:overworld",
                                                                        (byte) scale));
                                                }
                                                for (int scale = 0; scale <= 4; scale++) {
                                                        entries.accept(getAtlasWith(
                                                                        "minecraft:the_nether",
                                                                        (byte) scale));
                                                }
                                                for (int scale = 0; scale <= 4; scale++) {
                                                        entries.accept(getAtlasWith(
                                                                        "minecraft:the_end",
                                                                        (byte) scale));
                                                }
                                        }).build());

        public static void initialize() {

        }

        public static Item register(String name, Function<Item.Properties, Item> itemFactory,
                        Item.Properties settings) {
                ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id(name));
                Item item = itemFactory.apply(settings.setId(itemKey));
                Registry.register(BuiltInRegistries.ITEM, itemKey, item);
                return item;
        }
}
