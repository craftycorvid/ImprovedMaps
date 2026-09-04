package com.craftycorvid.improvedmaps.mixin.client;

import net.minecraft.client.gui.MapRenderer;
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
@Mixin(targets = "net.minecraft.client.gui.MapRenderer$MapInstance")
public class MapInstanceMixin {
    @Shadow
    private MapItemSavedData data;

    // The instance knows its map data but not which map it is, and the tints are filed by id.
    @Unique
    private int improvedmaps$mapId;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void improvedmaps$captureId(MapRenderer owner, int id, MapItemSavedData data,
            CallbackInfo ci) {
        this.improvedmaps$mapId = id;
    }

    // updateTexture is a plain double loop that turns each colour byte into a pixel, so there is
    // nothing to reimplement - just bend the colour on its way out. The pixel index is the loop's
    // own `j + i * 128`, the third int local.
    //
    // The colour here is packed for NativeImage, which is ABGR; the tints come out of the world
    // renderer's resolvers, which are ARGB. Swap on the way in and back out again.
    @ModifyExpressionValue(method = "updateTexture", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/material/MapColor;getColorFromPackedId(I)I"))
    private int improvedmaps$tint(int abgr, @Local(ordinal = 2) int pixel) {
        return improvedmaps$swap(MapBiomeTints.tint(improvedmaps$mapId, pixel, data.colors[pixel],
                improvedmaps$swap(abgr)));
    }

    @Unique
    private static int improvedmaps$swap(int colour) {
        return colour & 0xFF00FF00 | (colour & 0xFF) << 16 | colour >> 16 & 0xFF;
    }
}
