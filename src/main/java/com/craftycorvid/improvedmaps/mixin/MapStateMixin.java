package com.craftycorvid.improvedmaps.mixin;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapState;
import net.minecraft.world.PersistentState;
import java.util.function.Predicate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.craftycorvid.improvedmaps.item.ImprovedMapsItems;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;


@Mixin(MapState.class)
public abstract class MapStateMixin extends PersistentState {
	@WrapOperation(method = "update", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/entity/player/PlayerInventory;contains(Ljava/util/function/Predicate;)Z"))
	private boolean checkAtlasInventory(PlayerInventory inventory, Predicate<ItemStack> predicate,
			Operation<Boolean> original) {
		for (ItemStack itemStack : inventory) {
			if (itemStack.isOf(ImprovedMapsItems.ATLAS) && itemStack
					.get(DataComponentTypes.BUNDLE_CONTENTS).stream().anyMatch(predicate)) {
				return true;
			}
		}

		return original.call(inventory, predicate);
	}
}
