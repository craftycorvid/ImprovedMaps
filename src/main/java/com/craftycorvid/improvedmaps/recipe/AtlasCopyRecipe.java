package com.craftycorvid.improvedmaps.recipe;

import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.level.Level;
import com.craftycorvid.improvedmaps.ImprovedMaps;
import com.craftycorvid.improvedmaps.ImprovedMapsUtils;
import com.craftycorvid.improvedmaps.item.ImprovedMapsItems;
import eu.pb4.polymer.core.api.utils.PolymerObject;

public class AtlasCopyRecipe extends CustomRecipe {
    public AtlasCopyRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput inventory, Level world) {
        List<ItemStack> itemStacks = inventory.items();
        Boolean hasAtlas = false;
        Boolean hasBook = false;
        for (ItemStack stack : itemStacks) {
            if (stack.is(ImprovedMapsItems.ATLAS)) {
                hasAtlas = true;
            } else if (stack.is(Items.BOOK)) {
                hasBook = true;
            }
        }
        if (itemStacks.size() == 2 && hasAtlas && hasBook) {
            return true;
        }
        return false;
    }

    @Override
    public ItemStack assemble(CraftingInput inventory, HolderLookup.Provider lookup) {
        List<ItemStack> atlases = inventory.items().stream()
                .filter(stack -> stack.is(ImprovedMapsItems.ATLAS)).toList();

        ItemStack originalAtlas = atlases.getFirst();
        return ImprovedMapsUtils.copyAtlas(originalAtlas);

    }

    @Override
    public net.minecraft.world.item.crafting.CustomRecipe.Serializer<AtlasCopyRecipe> getSerializer() {
        return ImprovedMaps.ATLAS_COPY_RECIPE_SERIALIZER;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_EQUIPMENT;
    }

    public static class Serializer extends net.minecraft.world.item.crafting.CustomRecipe.Serializer<AtlasCopyRecipe>
            implements PolymerObject {

        public Serializer(
                CustomRecipe.Serializer.Factory<AtlasCopyRecipe> factory) {
            super(factory);
        }
    }
}
