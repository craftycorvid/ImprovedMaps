package com.craftycorvid.improvedmaps.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.craftycorvid.improvedmaps.item.ImprovedMapsItems;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import eu.pb4.polymer.core.api.utils.PolymerUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.CartographyTableScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.collection.DefaultedList;

@Mixin(CartographyTableScreenHandler.class)
public abstract class CartographyTableMixin extends ScreenHandler {
    protected CartographyTableMixin(ScreenHandlerType<?> type, int syncId) {
        super(type, syncId);
    }

    @Shadow
    @Final
    private CraftingResultInventory resultInventory;

    @Inject(method = "updateResult", at = @At("TAIL"))
    private void updateResult(ItemStack map, ItemStack item, ItemStack oldResult, CallbackInfo ci) {
        if (map.isOf(ImprovedMapsItems.ATLAS) && item.isOf(ImprovedMapsItems.ATLAS)) {
            this.resultInventory.setStack(CartographyTableScreenHandler.RESULT_SLOT_INDEX,
                    map.copy());
            sendContentUpdates();
        }

        if (map.isOf(ImprovedMapsItems.ATLAS)) {
            ci.cancel();
        }
    }

    // to access the Grindstone screen and its data in the ResultSlotMixin
    @Mixin(ScreenHandler.class)
    public interface CartographyTableScreenHandlerAccessor {
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
            return original || stack.isOf(ImprovedMapsItems.ATLAS);
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
            System.out.println("Mixin");
            if (stack.isOf(ImprovedMapsItems.ATLAS)) {
                System.out.println("Atlas taken from cartography table.");
                if (player instanceof ServerPlayerEntity)
                    PolymerUtils.reloadInventory((ServerPlayerEntity) player);
                ((CartographyTableScreenHandlerAccessor) field_17303).getSlots().get(1)
                        .takeStack(1);
                ci.cancel();
            }
        }
    }
}
