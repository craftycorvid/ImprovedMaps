package com.craftycorvid.improvedmaps;

import org.apache.commons.lang3.math.Fraction;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.component.BundleContents;

// Carries the atlas's bundle contents plus a capacity-scaled fullness so the
// client can render a bundle tooltip whose progress bar reflects atlasMapCapacity
// instead of the vanilla 64-item bundle weight.
public record AtlasTooltipData(BundleContents contents, Fraction fullness) implements TooltipComponent {
}
