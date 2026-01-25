package com.craftycorvid.improvedmaps.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.craftycorvid.improvedmaps.ImprovedMapsComponentTypes;
import com.craftycorvid.improvedmaps.ImprovedMapsUtils;
import com.craftycorvid.improvedmaps.item.ImprovedMapsItems;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@Mixin(CartographyTableMenu.class)
public abstract class CartographyTableMixin extends AbstractContainerMenu {
    protected CartographyTableMixin(MenuType<?> type, int syncId) {
        super(type, syncId);
    }

    @Shadow
    @Final
    private ContainerLevelAccess access;

    @Shadow
    @Final
    private ResultContainer resultContainer;

    @Shadow
    private void setupResultSlot(ItemStack map, ItemStack item, ItemStack oldResult) {}

    @Inject(method = "slotsChanged", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z", ordinal = 0), cancellable = true)
    private void callUpdateResultOnSingleAtlas(Container inventory, CallbackInfo ci,
            @Local(ordinal = 0) ItemStack itemStack, @Local(ordinal = 1) ItemStack itemStack2,
            @Local(ordinal = 2) ItemStack itemStack3) {
        if (itemStack.is(ImprovedMapsItems.ATLAS)) {
            this.setupResultSlot(itemStack, itemStack2, itemStack3);
            ci.cancel();
        }
    }

    @Inject(method = "setupResultSlot", at = @At("HEAD"), cancellable = true)
    private void addAtlasInteractionsToUpdateResult(ItemStack map, ItemStack item,
            ItemStack oldResult, CallbackInfo ci) {
        if (map.is(ImprovedMapsItems.ATLAS)) {
            this.access.execute((world, pos) -> {
                if (world.isClientSide())
                    return;

                ItemStack newResult = null;

                if (map.is(ImprovedMapsItems.ATLAS) && item.isEmpty()) {
                    Integer empty_maps =
                            map.getOrDefault(ImprovedMapsComponentTypes.ATLAS_EMPTY_MAP_COUNT, 0);
                    if (empty_maps > 0) {
                        newResult = new ItemStack(Items.MAP, empty_maps);
                        this.broadcastChanges();
                    }
                } else if (map.is(ImprovedMapsItems.ATLAS) && item.is(Items.BOOK)) {
                    newResult = ImprovedMapsUtils.copyAtlas(map);
                    this.broadcastChanges();
                }

                if (newResult != null && !ItemStack.matches(newResult, oldResult)) {
                    this.resultContainer.setItem(CartographyTableMenu.RESULT_SLOT,
                            newResult);
                    this.broadcastChanges();
                }
            });

            ci.cancel();
        }
    }

    // to access the Cartography Table screen and its data in the ResultSlotMixin
    @Mixin(AbstractContainerMenu.class)
    public interface ScreenHandlerAccessor {
        @Accessor
        NonNullList<Slot> getSlots();
    }

    // target CartographyTableScreenHandler's second slot
    @Mixin(targets = "net/minecraft/world/inventory/CartographyTableMenu$4")
    public static abstract class SecondSlotMixin extends Slot {
        public SecondSlotMixin(Container inventory, int slot, int x, int y) {
            super(inventory, slot, x, y);
        }

        @ModifyReturnValue(method = "canInsert", at = @At("RETURN"))
        private boolean canInsert(boolean original, ItemStack stack) {
            return original || stack.is(Items.BOOK);
        }
    }

    // target CartographyTableScreenHandler's result slot
    @Mixin(targets = "net/minecraft/world/inventory/CartographyTableMenu$5")
    public static abstract class ResultSlotMixin extends Slot {
        public ResultSlotMixin(Container inventory, int slot, int x, int y) {
            super(inventory, slot, x, y);
        }

        @Shadow
        @Final
        CartographyTableMenu field_17303;

        @Inject(method = "onTakeItem", at = @At("HEAD"), cancellable = true)
        public void onTakeItem(Player player, ItemStack stack, CallbackInfo ci) {
            var slots = ((ScreenHandlerAccessor) field_17303).getSlots();
            var firstSlot = slots.get(0).getItem();
            var secondSlot = slots.get(1).getItem();

            if (firstSlot.is(ImprovedMapsItems.ATLAS) && secondSlot.isEmpty()) {
                firstSlot.set(ImprovedMapsComponentTypes.ATLAS_EMPTY_MAP_COUNT, 0);
                ci.cancel();
            }
        }
    }
}
