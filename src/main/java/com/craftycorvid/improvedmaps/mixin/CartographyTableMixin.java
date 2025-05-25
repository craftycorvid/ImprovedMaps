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
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.CartographyTableScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.collection.DefaultedList;

@Mixin(CartographyTableScreenHandler.class)
public abstract class CartographyTableMixin extends ScreenHandler {
    protected CartographyTableMixin(ScreenHandlerType<?> type, int syncId) {
        super(type, syncId);
    }

    @Shadow
    @Final
    private ScreenHandlerContext context;

    @Shadow
    @Final
    private CraftingResultInventory resultInventory;

    @Shadow
    private void updateResult(ItemStack map, ItemStack item, ItemStack oldResult) {}

    @Inject(method = "onContentChanged", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/item/ItemStack;isEmpty()Z", ordinal = 0), cancellable = true)
    private void callUpdateResultOnSingleAtlas(Inventory inventory, CallbackInfo ci,
            @Local(ordinal = 0) ItemStack itemStack, @Local(ordinal = 1) ItemStack itemStack2,
            @Local(ordinal = 2) ItemStack itemStack3) {
        if (itemStack.isOf(ImprovedMapsItems.ATLAS)) {
            this.updateResult(itemStack, itemStack2, itemStack3);
            ci.cancel();
        }
    }

    @Inject(method = "updateResult", at = @At("HEAD"), cancellable = true)
    private void addAtlasInteractionsToUpdateResult(ItemStack map, ItemStack item,
            ItemStack oldResult, CallbackInfo ci) {
        if (map.isOf(ImprovedMapsItems.ATLAS)) {
            this.context.run((world, pos) -> {
                if (world.isClient())
                    return;

                ItemStack newResult = null;

                if (map.isOf(ImprovedMapsItems.ATLAS) && item.isEmpty()) {
                    Integer empty_maps =
                            map.getOrDefault(ImprovedMapsComponentTypes.ATLAS_EMPTY_MAP_COUNT, 0);
                    if (empty_maps > 0) {
                        newResult = new ItemStack(Items.MAP, empty_maps);
                        this.sendContentUpdates();
                    }
                } else if (map.isOf(ImprovedMapsItems.ATLAS) && item.isOf(Items.BOOK)) {
                    newResult = ImprovedMapsUtils.copyAtlas(map);
                    this.sendContentUpdates();
                }

                if (newResult != null && !ItemStack.areEqual(newResult, oldResult)) {
                    this.resultInventory.setStack(CartographyTableScreenHandler.RESULT_SLOT_INDEX,
                            newResult);
                    this.sendContentUpdates();
                }
            });

            ci.cancel();
        }
    }

    // to access the Cartography Table screen and its data in the ResultSlotMixin
    @Mixin(ScreenHandler.class)
    public interface ScreenHandlerAccessor {
        @Accessor
        DefaultedList<Slot> getSlots();
    }

    // target CartographyTableScreenHandler's second slot
    @Mixin(targets = "net/minecraft/screen/CartographyTableScreenHandler$4")
    public static abstract class SecondSlotMixin extends Slot {
        public SecondSlotMixin(Inventory inventory, int slot, int x, int y) {
            super(inventory, slot, x, y);
        }

        @ModifyReturnValue(method = "canInsert", at = @At("RETURN"))
        private boolean canInsert(boolean original, ItemStack stack) {
            return original || stack.isOf(Items.BOOK);
        }
    }

    // target CartographyTableScreenHandler's result slot
    @Mixin(targets = "net/minecraft/screen/CartographyTableScreenHandler$5")
    public static abstract class ResultSlotMixin extends Slot {
        public ResultSlotMixin(Inventory inventory, int slot, int x, int y) {
            super(inventory, slot, x, y);
        }

        @Shadow
        @Final
        CartographyTableScreenHandler field_17303;

        @Inject(method = "onTakeItem", at = @At("HEAD"), cancellable = true)
        public void onTakeItem(PlayerEntity player, ItemStack stack, CallbackInfo ci) {
            var slots = ((ScreenHandlerAccessor) field_17303).getSlots();
            var firstSlot = slots.get(0).getStack();
            var secondSlot = slots.get(1).getStack();

            if (firstSlot.isOf(ImprovedMapsItems.ATLAS) && secondSlot.isEmpty()) {
                firstSlot.set(ImprovedMapsComponentTypes.ATLAS_EMPTY_MAP_COUNT, 0);
                ci.cancel();
            }
        }
    }
}
