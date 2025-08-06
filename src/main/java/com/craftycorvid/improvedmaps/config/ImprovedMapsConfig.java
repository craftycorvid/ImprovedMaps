package com.craftycorvid.improvedmaps.config;

import eu.midnightdust.lib.config.MidnightConfig;

public class ImprovedMapsConfig extends MidnightConfig {
    @Entry
    @Comment("Maximum number of maps that can be stored in an atlas")
    public static int atlasMapCapacity = 128;
    
    @Entry
    @Comment("Whether atlases should be updated even when not held in hand")
    public static boolean updateAtlasWhenNotInHand = false;
    
    @Entry
    @Comment("Whether to disable vanilla map tooltip additions")
    public static boolean disableVanillaMapTooltips = false;
}