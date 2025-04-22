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
}
