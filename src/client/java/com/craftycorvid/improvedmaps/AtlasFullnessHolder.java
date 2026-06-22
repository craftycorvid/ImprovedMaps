package com.craftycorvid.improvedmaps;

import org.apache.commons.lang3.math.Fraction;

// Implemented (via mixin) by ClientBundleTooltip so the tooltip callback can hand
// it an atlas's capacity-scaled fullness to use for the progress bar.
public interface AtlasFullnessHolder {
    void improvedmaps$setFullness(Fraction fullness);
}
