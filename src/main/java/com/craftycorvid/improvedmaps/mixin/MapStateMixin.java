package com.craftycorvid.improvedmaps.mixin;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapState;
import net.minecraft.world.PersistentState;
import java.util.Iterator;
import java.util.function.Predicate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import com.craftycorvid.improvedmaps.item.ImprovedMapsItems;


@Mixin(MapState.class)
public abstract class MapStateMixin extends PersistentState {
	@Redirect(method = "update", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/entity/player/PlayerInventory;contains(Ljava/util/function/Predicate;)Z"))
	private boolean update(PlayerInventory inventory, Predicate<ItemStack> predicate) {
		Iterator<ItemStack> inventoryIterator = inventory.iterator();

		// Maybe we should do this in PlayerInventory.contains instead?
		do {
			ItemStack itemStack = (ItemStack) inventoryIterator.next();
			if (itemStack.isOf(ImprovedMapsItems.ATLAS) && itemStack
					.get(DataComponentTypes.BUNDLE_CONTENTS).stream().anyMatch(predicate)) {
				return true;
			}
		} while (inventoryIterator.hasNext());

		return inventory.contains(predicate);
	}
}
