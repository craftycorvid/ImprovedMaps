package com.craftycorvid.improvedmaps.mixin.client;

import org.apache.commons.lang3.math.Fraction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientBundleTooltip;
import com.craftycorvid.improvedmaps.AtlasFullnessHolder;

// Lets an atlas tooltip override the fullness fraction that drives the progress bar,
// so it scales to atlasMapCapacity instead of the vanilla 64-item bundle weight.
@Mixin(ClientBundleTooltip.class)
public class ClientBundleTooltipMixin implements AtlasFullnessHolder {
    @Unique
    private Fraction improvedmaps$fullness;

    @Override
    public void improvedmaps$setFullness(Fraction fullness) {
        this.improvedmaps$fullness = fullness;
    }

    @ModifyVariable(method = "extractBundleWithItemsTooltip", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Fraction improvedmaps$overrideFullness(Fraction original) {
        return this.improvedmaps$fullness != null ? this.improvedmaps$fullness : original;
    }
}
