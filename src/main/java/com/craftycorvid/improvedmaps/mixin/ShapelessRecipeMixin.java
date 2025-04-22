package com.craftycorvid.improvedmaps.mixin;

import java.util.List;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.craftycorvid.improvedmaps.ImprovedMapsComponentTypes;
import com.craftycorvid.improvedmaps.item.ImprovedMapsItems;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.ShapelessRecipe;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper;

@Mixin(ShapelessRecipe.class)
public abstract class ShapelessRecipeMixin {
    @Shadow
    @Final
    List<Ingredient> ingredients;

    @Shadow
    @Final
    ItemStack result;

    // This would obviously be better in a SpecialRecipe, but how do we do that and remain
    // compatible with vanilla clients?
    @Inject(method = "craft", at = @At("HEAD"), cancellable = true)
    public void craftAtlas(CraftingRecipeInput craftingRecipeInput,
            RegistryWrapper.WrapperLookup wrapperLookup, CallbackInfoReturnable<ItemStack> ci) {
        if (this.result.isOf(ImprovedMapsItems.ATLAS)) {
            ItemStack atlas = new ItemStack(ImprovedMapsItems.ATLAS);
            ItemStack map = craftingRecipeInput.getStacks().stream()
                    .filter(stack -> stack.isOf(Items.FILLED_MAP)).findFirst().orElse(null);

            if (map != null) {
                BundleContentsComponent.Builder builder =
                        new BundleContentsComponent.Builder(BundleContentsComponent.DEFAULT);
                builder.add(map);
                map.increment(1);
                atlas.set(DataComponentTypes.BUNDLE_CONTENTS, builder.build());
                atlas.set(ImprovedMapsComponentTypes.ATLAS_EMPTY_MAP_COUNT, 0);
            } else {
                ItemStack originalAtlas = craftingRecipeInput.getStackInSlot(0);
                ItemStack secondAtlas = craftingRecipeInput.getStackInSlot(1);
                int originalFilledMaps = originalAtlas.get(DataComponentTypes.BUNDLE_CONTENTS)
                        .getNumberOfStacksShown();
                int emptyMapCount =
                        originalAtlas.get(ImprovedMapsComponentTypes.ATLAS_EMPTY_MAP_COUNT)
                                + secondAtlas.get(ImprovedMapsComponentTypes.ATLAS_EMPTY_MAP_COUNT);
                int newEmptyMapCount = (emptyMapCount - originalFilledMaps) / 2;
                if (newEmptyMapCount < 0) {
                    ci.setReturnValue(ItemStack.EMPTY);
                    return;
                }
                atlas = originalAtlas.copy();
                atlas.set(ImprovedMapsComponentTypes.ATLAS_EMPTY_MAP_COUNT, newEmptyMapCount);
                atlas.increment(1);
            }

            ci.setReturnValue(atlas);
        }
    }
}
