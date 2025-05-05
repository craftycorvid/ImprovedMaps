package com.craftycorvid.improvedmaps;

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
}
