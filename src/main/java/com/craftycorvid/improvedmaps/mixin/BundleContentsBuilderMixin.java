package com.craftycorvid.improvedmaps.mixin;

import org.apache.commons.lang3.math.Fraction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.craftycorvid.improvedmaps.internal.ICustomBundleContentBuilder;
import net.fabricmc.fabric.mixin.transfer.BundleContentsAccessor;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;

// Implementation Based on
// https://github.com/FaeWulf/Diversity/blob/sub-mod-1.21.5/common-better-bundle/src/main/java/xyz/faewulf/diversity_better_bundle/mixin/item/buildingBundle/BuilderMixin.java
@Mixin(BundleContents.Mutable.class)
public abstract class BundleContentsBuilderMixin implements ICustomBundleContentBuilder {
    @Shadow
    private Fraction weight;

    @Unique
    private int maxSize = 64;

    @Inject(method = "getMaxAmountToAdd", at = @At("RETURN"), cancellable = true)
    private void getMaxAllowedInject(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        int itemValue = Mth
                .mulAndTruncate(BundleContentsAccessor.getOccupancy(stack), 64);

        int usedSpace = Mth.mulAndTruncate(this.weight, 64);
        int freeSpace = maxSize - usedSpace;

        cir.setReturnValue(Math.max(freeSpace / itemValue, 0));
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

}
