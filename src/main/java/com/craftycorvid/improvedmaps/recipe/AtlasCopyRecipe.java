package com.craftycorvid.improvedmaps.recipe;

import java.util.List;
import com.craftycorvid.improvedmaps.ImprovedMaps;
import com.craftycorvid.improvedmaps.ImprovedMapsUtils;
import com.craftycorvid.improvedmaps.item.ImprovedMapsItems;
import eu.pb4.polymer.core.api.utils.PolymerObject;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.book.RecipeBookCategories;
import net.minecraft.recipe.book.RecipeBookCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;

public class AtlasCopyRecipe extends SpecialCraftingRecipe {
    public AtlasCopyRecipe(CraftingRecipeCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingRecipeInput inventory, World world) {
        List<ItemStack> itemStacks = inventory.getStacks();
        Boolean hasAtlas = false;
        Boolean hasBook = false;
        for (ItemStack stack : itemStacks) {
            if (stack.isOf(ImprovedMapsItems.ATLAS)) {
                hasAtlas = true;
            } else if (stack.isOf(Items.BOOK)) {
                hasBook = true;
            }
        }
        if (itemStacks.size() == 2 && hasAtlas && hasBook) {
            return true;
        }
        return false;
    }

    @Override
    public ItemStack craft(CraftingRecipeInput inventory, RegistryWrapper.WrapperLookup lookup) {
        List<ItemStack> atlases = inventory.getStacks().stream()
                .filter(stack -> stack.isOf(ImprovedMapsItems.ATLAS)).toList();

        ItemStack originalAtlas = atlases.getFirst();
        return ImprovedMapsUtils.copyAtlas(originalAtlas);

    }

    @Override
    public SpecialRecipeSerializer<AtlasCopyRecipe> getSerializer() {
        return ImprovedMaps.ATLAS_COPY_RECIPE_SERIALIZER;
    }

    @Override
    public RecipeBookCategory getRecipeBookCategory() {
        return RecipeBookCategories.CRAFTING_EQUIPMENT;
    }

    public static class Serializer extends SpecialRecipeSerializer<AtlasCopyRecipe>
            implements PolymerObject {

        public Serializer(
                SpecialCraftingRecipe.SpecialRecipeSerializer.Factory<AtlasCopyRecipe> factory) {
            super(factory);
        }
    }
}
