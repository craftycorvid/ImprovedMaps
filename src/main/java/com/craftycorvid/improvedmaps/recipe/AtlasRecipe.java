package com.craftycorvid.improvedmaps.recipe;

import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import com.craftycorvid.improvedmaps.ImprovedMaps;
import com.craftycorvid.improvedmaps.ImprovedMapsComponentTypes;
import com.craftycorvid.improvedmaps.internal.ICustomBundleContentBuilder;
import com.craftycorvid.improvedmaps.item.ImprovedMapsItems;
import eu.pb4.polymer.core.api.utils.PolymerObject;

public class AtlasRecipe extends CustomRecipe {
    public AtlasRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput inventory, Level world) {
        List<ItemStack> itemStacks = inventory.items();
        ItemStack filledMap = ItemStack.EMPTY;
        Boolean hasBook = false;
        for (ItemStack stack : itemStacks) {
            if (stack.is(Items.FILLED_MAP)) {
                filledMap = stack;
            } else if (stack.is(Items.BOOK)) {
                hasBook = true;
            }
        }
        if (itemStacks.size() == 2 && hasBook && !filledMap.isEmpty()) {
            MapItemSavedData state = MapItem.getSavedData(filledMap, world);
            return state != null;
        }
        return false;
    }

    @Override
    public ItemStack assemble(CraftingInput inventory, HolderLookup.Provider lookup) {
        ItemStack atlas = new ItemStack(ImprovedMapsItems.ATLAS);
        ItemStack map = inventory.items().stream().filter(stack -> stack.is(Items.FILLED_MAP))
                .findFirst().orElse(null);

        BundleContents.Mutable builder = new BundleContents.Mutable(BundleContents.EMPTY);
        ((ICustomBundleContentBuilder) builder).setMaxSize(512);
        builder.tryInsert(map);
        map.grow(1);
        atlas.set(DataComponents.BUNDLE_CONTENTS, builder.toImmutable());
        atlas.set(ImprovedMapsComponentTypes.ATLAS_EMPTY_MAP_COUNT, 0);

        return atlas;
    }

    @Override
    public net.minecraft.world.item.crafting.CustomRecipe.Serializer<AtlasRecipe> getSerializer() {
        return ImprovedMaps.ATLAS_RECIPE_SERIALIZER;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_EQUIPMENT;
    }

    public static class Serializer extends net.minecraft.world.item.crafting.CustomRecipe.Serializer<AtlasRecipe>
            implements PolymerObject {

        public Serializer(
                CustomRecipe.Serializer.Factory<AtlasRecipe> factory) {
            super(factory);
        }
    }
}
