package com.craftycorvid.improvedmaps.mixin;

import java.util.function.Predicate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import com.craftycorvid.improvedmaps.MapBiomes;
import com.craftycorvid.improvedmaps.internal.IMapBiomeHolder;
import com.craftycorvid.improvedmaps.item.ImprovedMapsItems;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;


@Mixin(MapItemSavedData.class)
public abstract class MapStateMixin extends SavedData implements IMapBiomeHolder {
	// Vanilla map data does not know which map it is; MapBiomeCaptureMixin stamps it on as the data
	// is looked up, so the biome record can be filed under the same id.
	@Unique
	private MapId improvedmaps$mapId;

	@Unique
	private MapBiomes improvedmaps$biomes;

	@Override
	public MapId improvedmaps$mapId() {
		return improvedmaps$mapId;
	}

	@Override
	public void improvedmaps$setMapId(MapId id) {
		this.improvedmaps$mapId = id;
	}

	@Override
	public MapBiomes improvedmaps$biomes() {
		return improvedmaps$biomes;
	}

	@Override
	public void improvedmaps$setBiomes(MapBiomes biomes) {
		this.improvedmaps$biomes = biomes;
	}

	@WrapOperation(method = "tickCarriedBy", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/player/Inventory;contains(Ljava/util/function/Predicate;)Z"))
	private boolean checkAtlasInventory(Inventory inventory, Predicate<ItemStack> predicate,
			Operation<Boolean> original) {
		for (ItemStack itemStack : inventory) {
			if (itemStack.is(ImprovedMapsItems.ATLAS)
					&& itemStack.getOrDefault(DataComponents.BUNDLE_CONTENTS,
							BundleContents.EMPTY).itemCopyStream().anyMatch(predicate)) {
				return true;
			}
		}

		return original.call(inventory, predicate);
	}
}
