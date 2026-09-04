package com.craftycorvid.improvedmaps;

// Implemented (via mixin) by ClientBundleTooltip so the tooltip callback can tell it whether an
// atlas is full at atlasMapCapacity, instead of at the vanilla 64-item bundle weight.
public interface AtlasFullnessHolder {
    void improvedmaps$setFull(boolean full);
}
