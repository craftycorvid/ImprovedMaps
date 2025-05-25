package com.craftycorvid.improvedmaps.recipe;

import java.util.List;
import com.craftycorvid.improvedmaps.ImprovedMaps;
import com.craftycorvid.improvedmaps.ImprovedMapsComponentTypes;
import com.craftycorvid.improvedmaps.internal.ICustomBundleContentBuilder;
import com.craftycorvid.improvedmaps.item.ImprovedMapsItems;
import eu.pb4.polymer.core.api.utils.PolymerObject;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.book.RecipeBookCategories;
import net.minecraft.recipe.book.RecipeBookCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;

public class AtlasRecipe extends SpecialCraftingRecipe {
    public AtlasRecipe(CraftingRecipeCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingRecipeInput inventory, World world) {
        List<ItemStack> itemStacks = inventory.getStacks();
        ItemStack filledMap = ItemStack.EMPTY;
        Boolean hasBook = false;
        for (ItemStack stack : itemStacks) {
            if (stack.isOf(Items.FILLED_MAP)) {
                filledMap = stack;
            } else if (stack.isOf(Items.BOOK)) {
                hasBook = true;
            }
        }
        if (itemStacks.size() == 2 && hasBook && !filledMap.isEmpty()) {
            MapState state = FilledMapItem.getMapState(filledMap, world);
            return state != null;
        }
        return false;
    }

    @Override
    public ItemStack craft(CraftingRecipeInput inventory, RegistryWrapper.WrapperLookup lookup) {
        ItemStack atlas = new ItemStack(ImprovedMapsItems.ATLAS);
        ItemStack map = inventory.getStacks().stream().filter(stack -> stack.isOf(Items.FILLED_MAP))
                .findFirst().orElse(null);

        BundleContentsComponent.Builder builder =
                new BundleContentsComponent.Builder(BundleContentsComponent.DEFAULT);
        ((ICustomBundleContentBuilder) builder).setMaxSize(512);
        builder.add(map);
        map.increment(1);
        atlas.set(DataComponentTypes.BUNDLE_CONTENTS, builder.build());
        atlas.set(ImprovedMapsComponentTypes.ATLAS_EMPTY_MAP_COUNT, 0);

        return atlas;
    }

    @Override
    public SpecialRecipeSerializer<AtlasRecipe> getSerializer() {
        return ImprovedMaps.ATLAS_RECIPE_SERIALIZER;
    }

    @Override
    public RecipeBookCategory getRecipeBookCategory() {
        return RecipeBookCategories.CRAFTING_EQUIPMENT;
    }

    public static class Serializer extends SpecialRecipeSerializer<AtlasRecipe>
            implements PolymerObject {

        public Serializer(
                SpecialCraftingRecipe.SpecialRecipeSerializer.Factory<AtlasRecipe> factory) {
            super(factory);
        }
    }
}
