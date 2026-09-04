package com.craftycorvid.improvedmaps.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import com.craftycorvid.improvedmaps.MinimapHud;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

// Status effect icons stack down from the top-right corner, which is where the minimap sits by
// default. They are placed off the GUI width, so narrowing it for this one method slides them
// clear. (Toasts get the same treatment in ToastComponentMixin.)
@Mixin(Gui.class)
public class GuiEffectsMixin {
    @ModifyExpressionValue(method = "renderEffects", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;guiWidth()I"))
    private int improvedmaps$makeRoomForMinimap(int guiWidth) {
        return guiWidth - MinimapHud.rightInset();
    }
}
