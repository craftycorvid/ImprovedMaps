package com.craftycorvid.improvedmaps.recipe;

import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;
import com.craftycorvid.improvedmaps.ImprovedMapsUtils;
import com.craftycorvid.improvedmaps.item.ImprovedMapsItems;

public class AtlasCopyRecipe extends CustomRecipe {
    public static final RecipeSerializer<AtlasCopyRecipe> SERIALIZER =
            new SimpleCraftingRecipeSerializer<>(AtlasCopyRecipe::new);

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
    public ItemStack assemble(CraftingInput inventory, HolderLookup.Provider registries) {
        List<ItemStack> atlases = inventory.items().stream()
                .filter(stack -> stack.is(ImprovedMapsItems.ATLAS)).toList();

        ItemStack originalAtlas = atlases.getFirst();
        return ImprovedMapsUtils.copyAtlas(originalAtlas);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<AtlasCopyRecipe> getSerializer() {
        return SERIALIZER;
    }
}
