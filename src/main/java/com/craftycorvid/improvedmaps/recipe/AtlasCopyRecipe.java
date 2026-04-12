package com.craftycorvid.improvedmaps.recipe;

import java.util.List;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import com.mojang.serialization.MapCodec;
import com.craftycorvid.improvedmaps.ImprovedMapsUtils;
import com.craftycorvid.improvedmaps.item.ImprovedMapsItems;

public class AtlasCopyRecipe extends CustomRecipe {
    public static final AtlasCopyRecipe INSTANCE = new AtlasCopyRecipe();
    public static final MapCodec<AtlasCopyRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, AtlasCopyRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<AtlasCopyRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public AtlasCopyRecipe() {
        super();
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
    public ItemStack assemble(CraftingInput inventory) {
        List<ItemStack> atlases = inventory.items().stream()
                .filter(stack -> stack.is(ImprovedMapsItems.ATLAS)).toList();

        ItemStack originalAtlas = atlases.getFirst();
        return ImprovedMapsUtils.copyAtlas(originalAtlas);
    }

    @Override
    public RecipeSerializer<AtlasCopyRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_EQUIPMENT;
    }
}
