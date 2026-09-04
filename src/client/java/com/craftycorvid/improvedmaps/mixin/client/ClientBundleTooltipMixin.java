package com.craftycorvid.improvedmaps.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientBundleTooltip;
import com.craftycorvid.improvedmaps.AtlasFullnessHolder;

// The bundle tooltip greys out its empty slots once the contents weigh a full bundle, which an
// atlas hits at 64 maps however large atlasMapCapacity is. Let an atlas tooltip say for itself
// whether it is full.
@Mixin(ClientBundleTooltip.class)
public class ClientBundleTooltipMixin implements AtlasFullnessHolder {
    @Unique
    private Boolean improvedmaps$full;

    @Override
    public void improvedmaps$setFull(boolean full) {
        this.improvedmaps$full = full;
    }

    @ModifyVariable(method = "renderImage", at = @At("STORE"), ordinal = 0)
    private boolean improvedmaps$overrideFullness(boolean original) {
        return this.improvedmaps$full != null ? this.improvedmaps$full : original;
    }
}
