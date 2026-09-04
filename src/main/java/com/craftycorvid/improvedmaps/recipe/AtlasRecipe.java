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
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import com.craftycorvid.improvedmaps.ImprovedMapsComponentTypes;
import com.craftycorvid.improvedmaps.internal.ICustomBundleContentBuilder;
import com.craftycorvid.improvedmaps.item.ImprovedMapsItems;

import static com.craftycorvid.improvedmaps.ImprovedMaps.MOD_CONFIG;

public class AtlasRecipe extends CustomRecipe {
    public static final RecipeSerializer<AtlasRecipe> SERIALIZER =
            new SimpleCraftingRecipeSerializer<>(AtlasRecipe::new);

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
    public ItemStack assemble(CraftingInput inventory, HolderLookup.Provider registries) {
        ItemStack atlas = new ItemStack(ImprovedMapsItems.ATLAS);
        ItemStack map = inventory.items().stream().filter(stack -> stack.is(Items.FILLED_MAP))
                .findFirst().orElse(null);

        BundleContents.Mutable builder = new BundleContents.Mutable(BundleContents.EMPTY);
        ((ICustomBundleContentBuilder) builder).setMaxSize(MOD_CONFIG.server_atlasMapCapacity);
        builder.tryInsert(map);
        map.grow(1);
        atlas.set(DataComponents.BUNDLE_CONTENTS, builder.toImmutable());
        atlas.set(ImprovedMapsComponentTypes.ATLAS_EMPTY_MAP_COUNT, 0);

        return atlas;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<AtlasRecipe> getSerializer() {
        return SERIALIZER;
    }
}
