package com.craftycorvid.improvedmaps.mixin.client;

import net.minecraft.client.resources.MapTextureManager;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.craftycorvid.improvedmaps.MapBiomeTints;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

// MapInstance is package-private, so it can only be targeted by name.
@Mixin(targets = "net.minecraft.client.resources.MapTextureManager$MapInstance")
public class MapInstanceMixin {
    @Shadow
    private MapItemSavedData data;

    // The instance knows its map data but not which map it is, and the tints are filed by id.
    @Unique
    private int improvedmaps$mapId;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void improvedmaps$captureId(MapTextureManager owner, int id, MapItemSavedData data,
            CallbackInfo ci) {
        this.improvedmaps$mapId = id;
    }

    // updateTextureIfNeeded is a plain double loop that turns each colour byte into an ARGB pixel,
    // so there is nothing to reimplement - just bend the colour on its way out. The pixel index is
    // the loop's own `x + y * 128`, the third int local.
    @ModifyExpressionValue(method = "updateTextureIfNeeded", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/material/MapColor;getColorFromPackedId(I)I"))
    private int improvedmaps$tint(int argb, @Local(ordinal = 2) int pixel) {
        return MapBiomeTints.tint(improvedmaps$mapId, pixel, data.colors[pixel], argb);
    }
}
