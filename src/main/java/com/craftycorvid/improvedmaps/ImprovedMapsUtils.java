package com.craftycorvid.improvedmaps;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.item.ItemStack;

public class ImprovedMapsUtils {
    public static String scaleToString(int scale) {
        switch (scale) {
            case 0:
                return "1:1";
            case 1:
                return "1:2";
            case 2:
                return "1:4";
            case 3:
                return "1:8";
            case 4:
                return "1:16";
            default:
                return "Unknown Scale";
        }
    }

    public static String formatDimensionString(String dimension) {
        switch (dimension) {
            case "minecraft:overworld":
                return "Overworld";
            case "minecraft:the_nether":
                return "The Nether";
            case "minecraft:the_end":
                return "The End";
            default:
                return dimension;
        }
    }

    public static ItemStack copyAtlas(ItemStack originalAtlas) {
        int originalAtlasFilledMapCount = originalAtlas
                .getOrDefault(DataComponentTypes.BUNDLE_CONTENTS, BundleContentsComponent.DEFAULT)
                .size();
        int emptyMapCount =
                originalAtlas.getOrDefault(ImprovedMapsComponentTypes.ATLAS_EMPTY_MAP_COUNT, 0);
        int newEmptyMapCount = Math.floorDiv(emptyMapCount - originalAtlasFilledMapCount, 2);
        if (newEmptyMapCount < 0) {
            return ItemStack.EMPTY;
        }
        ItemStack newAtlas = originalAtlas.copyWithCount(2);
        newAtlas.set(ImprovedMapsComponentTypes.ATLAS_EMPTY_MAP_COUNT, newEmptyMapCount);
        return newAtlas;
    }
}
