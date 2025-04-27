package com.craftycorvid.improvedmaps.mixin;

import org.apache.commons.lang3.math.Fraction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.craftycorvid.improvedmaps.internal.ICustomBundleContentBuilder;
import net.fabricmc.fabric.mixin.transfer.BundleContentsComponentAccessor;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;

@Mixin(BundleContentsComponent.Builder.class)
public abstract class BundleContentsBuilderMixin implements ICustomBundleContentBuilder {
    @Shadow
    private Fraction occupancy;

    @Unique
    private int maxSize = 64;

    @Inject(method = "getMaxAllowed", at = @At("RETURN"), cancellable = true)
    private void getMaxAllowedInject(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        int itemValue = MathHelper
                .multiplyFraction(BundleContentsComponentAccessor.getOccupancy(stack), 64);

        int usedSpace = MathHelper.multiplyFraction(this.occupancy, 64);
        int freeSpace = maxSize - usedSpace;

        cir.setReturnValue(Math.max(freeSpace / itemValue, 0));
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

}
