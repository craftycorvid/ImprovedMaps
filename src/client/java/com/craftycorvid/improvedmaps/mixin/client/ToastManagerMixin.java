package com.craftycorvid.improvedmaps.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import net.minecraft.client.gui.components.toasts.ToastManager;
import com.craftycorvid.improvedmaps.MinimapHud;

// Toasts hug the right screen edge, where a top-right minimap would cover them.
// They position off this GUI width, so narrowing it slides them left of the
// minimap - and keeps them sliding in from its edge instead of the screen's.
@Mixin(ToastManager.class)
public class ToastManagerMixin {
    @ModifyVariable(method = "extractRenderState", at = @At("STORE"), ordinal = 0)
    private int improvedmaps$makeRoomForMinimap(int guiWidth) {
        return guiWidth - MinimapHud.rightInset();
    }
}
