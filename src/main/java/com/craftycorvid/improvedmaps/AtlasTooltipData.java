package com.craftycorvid.improvedmaps;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.component.BundleContents;

// Carries the atlas's bundle contents plus whether it is actually full, so the client can render a
// bundle tooltip whose slots are greyed out at atlasMapCapacity instead of at the vanilla 64-item
// bundle weight (see ImprovedMapsClient + ClientBundleTooltipMixin).
public record AtlasTooltipData(BundleContents contents, boolean full) implements TooltipComponent {
}
