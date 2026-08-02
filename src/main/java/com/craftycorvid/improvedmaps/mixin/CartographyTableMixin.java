package com.craftycorvid.improvedmaps.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
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
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import static com.craftycorvid.improvedmaps.ImprovedMapsNetworking.PLAYERS_WITH_CLIENT;

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
    private void setupResultSlot(ItemStack map, ItemStack item, ItemStack oldResult) {
    }

    @Inject(method = "slotsChanged", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z", ordinal = 0), cancellable = true)
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
                    Integer empty_maps = map.getOrDefault(ImprovedMapsComponentTypes.ATLAS_EMPTY_MAP_COUNT, 0);
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

    @Unique
    private Player improvedmaps$owner;

    @Unique
    private boolean improvedmaps$phantomShown;

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V", at = @At("TAIL"))
    private void rememberOwner(int syncId, Inventory inventory, ContainerLevelAccess context,
            CallbackInfo ci) {
        this.improvedmaps$owner = inventory.player;
    }

    // Vanilla's slotsChanged clears the result whenever an input slot is empty, and that branch
    // runs client-side too - so a client without the mod wipes the empty-map result the moment it
    // arrives, since pulling maps out of an atlas needs no material. Show those clients a phantom
    // material item to stop their menu from clearing it. The lie lives only in this packet: the
    // server-side slot stays empty, so nothing can leak into an inventory when the screen closes.
    @Inject(method = "slotsChanged", at = @At("HEAD"))
    private void syncPhantomMaterialForVanillaClients(Container inventory, CallbackInfo ci) {
        if (!(this.improvedmaps$owner instanceof ServerPlayer player)
                || PLAYERS_WITH_CLIENT.contains(player.getUUID()))
            return;

        ItemStack atlas = this.getSlot(CartographyTableMenu.MAP_SLOT).getItem();
        ItemStack material = this.getSlot(CartographyTableMenu.ADDITIONAL_SLOT).getItem();
        // Decided from the inputs alone, so this stays correct however the result turns out.
        boolean show = atlas.is(ImprovedMapsItems.ATLAS) && material.isEmpty()
                && atlas.getOrDefault(ImprovedMapsComponentTypes.ATLAS_EMPTY_MAP_COUNT, 0) > 0;
        if (show == this.improvedmaps$phantomShown)
            return;
        this.improvedmaps$phantomShown = show;

        ItemStack shown = material; // hiding: the real stack, empty unless a material was placed
        if (show) {
            shown = new ItemStack(Items.BOOK);
            shown.set(DataComponents.CUSTOM_NAME, Component.literal("Empty Maps in Atlas"));
        }
        // Sent from HEAD so it lands before the result: a result reaching a client whose material
        // slot still looks empty is cleared on arrival, and the server - believing the client has
        // it - never sends it again.
        player.connection.send(new ClientboundContainerSetSlotPacket(this.containerId,
                this.incrementStateId(), CartographyTableMenu.ADDITIONAL_SLOT, shown));
        // Forget that the client has the result, so the next broadcast re-sends it. Recovers the
        // preview when a full resync has replaced the phantom with the real, empty slot.
        this.setRemoteSlot(CartographyTableMenu.RESULT_SLOT, ItemStack.EMPTY);
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

        @ModifyReturnValue(method = "mayPlace", at = @At("RETURN"))
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
        CartographyTableMenu this$0;

        @Inject(method = "onTake", at = @At("HEAD"), cancellable = true)
        public void onTake(Player player, ItemStack stack, CallbackInfo ci) {
            var slots = ((ScreenHandlerAccessor) this$0).getSlots();
            var firstSlot = slots.get(0).getItem();
            var secondSlot = slots.get(1).getItem();

            if (firstSlot.is(ImprovedMapsItems.ATLAS) && secondSlot.isEmpty()
                    && stack.is(Items.MAP)) {
                int emptyMaps = firstSlot.getOrDefault(
                        ImprovedMapsComponentTypes.ATLAS_EMPTY_MAP_COUNT, 0);
                int remaining = Math.max(0, emptyMaps - stack.getCount());
                ItemStack atlasResult = firstSlot.copy();
                atlasResult.set(ImprovedMapsComponentTypes.ATLAS_EMPTY_MAP_COUNT, remaining);

                slots.get(0).set(ItemStack.EMPTY);
                this.set(ItemStack.EMPTY);
                if (!player.getInventory().add(atlasResult)) {
                    player.drop(atlasResult, false);
                }
                this$0.broadcastChanges();
                ci.cancel();
            }
        }
    }
}
