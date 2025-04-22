package com.craftycorvid.improvedmaps.item;

import java.util.List;
import org.jetbrains.annotations.Nullable;
import com.craftycorvid.improvedmaps.ImprovedMapsComponentTypes;
import com.craftycorvid.improvedmaps.ImprovedMapsUtils;
import eu.pb4.polymer.core.api.item.PolymerItem;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.BundleItem;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.item.consume.UseAction;
import net.minecraft.item.map.MapState;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ClickType;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import xyz.nucleoid.packettweaker.PacketContext;

public class AtlasItem extends BundleItem implements PolymerItem {
    public AtlasItem(Settings settings) {
        super(settings);
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
        return itemStack.getCount() > 1 ? Items.BOOK : Items.BUNDLE;
    }

    @Override
    public Identifier getPolymerItemModel(ItemStack itemStack, PacketContext context) {
        return Identifier.of("minecraft", "book");
    }

    @Override
    public void modifyClientTooltip(List<Text> tooltip, ItemStack stack, PacketContext context) {
        Integer scale = stack.getOrDefault(ImprovedMapsComponentTypes.ATLAS_SCALE, null);
        Integer empty_maps =
                stack.getOrDefault(ImprovedMapsComponentTypes.ATLAS_EMPTY_MAP_COUNT, 0);
        tooltip.clear();
        if (scale != null)
            tooltip.add(Text.literal("Scale " + ImprovedMapsUtils.scaleToString(scale))
                    .formatted(Formatting.GRAY));
        tooltip.add(Text.literal(empty_maps + " Empty Maps").formatted(Formatting.GRAY));
    }

    @Override
    public void onCraft(ItemStack stack, World world) {
        BundleContentsComponent bundleContents = stack.get(DataComponentTypes.BUNDLE_CONTENTS);
        ItemStack map = bundleContents.get(0);
        MapState activeState = FilledMapItem.getMapState(map, world);
        stack.set(ImprovedMapsComponentTypes.ATLAS_SCALE, (int) activeState.scale);
    }

    @Override
    public boolean onStackClicked(ItemStack atlas, Slot slot, ClickType clickType,
            PlayerEntity player) {
        if (atlas.getCount() > 1)
            return false;
        ItemStack itemStack = slot.getStack();
        if (clickType == ClickType.RIGHT && itemStack.isEmpty()) {
            atlas.set(DataComponentTypes.MAP_ID, null);
            return super.onStackClicked(atlas, slot, clickType, player);
        } else if (itemStack.isOf(Items.MAP)) {
            return handleEmptyMapCLick(atlas, itemStack, clickType);
        } else if (itemStack.isOf(Items.FILLED_MAP)) {
            int scale = atlas.getOrDefault(ImprovedMapsComponentTypes.ATLAS_SCALE, 0);
            MapState mapState = FilledMapItem.getMapState(itemStack, player.getWorld());

            if ((int) mapState.scale != scale)
                return false;
            return super.onStackClicked(atlas, slot, clickType, player);
        }
        return false;
    }

    @Override
    public boolean onClicked(ItemStack atlas, ItemStack otherStack, Slot slot, ClickType clickType,
            PlayerEntity player, StackReference cursorStackReference) {
        if (atlas.getCount() > 1)
            return false;
        if (clickType == ClickType.RIGHT && otherStack.isEmpty()) {
            atlas.set(DataComponentTypes.MAP_ID, null);
            return super.onClicked(atlas, otherStack, slot, clickType, player,
                    cursorStackReference);
        } else if (otherStack.isOf(Items.MAP)) {
            return handleEmptyMapCLick(atlas, otherStack, clickType);
        } else if (otherStack.isOf(Items.FILLED_MAP)) {
            int scale = atlas.getOrDefault(ImprovedMapsComponentTypes.ATLAS_SCALE, 0);
            MapState mapState = FilledMapItem.getMapState(otherStack, player.getWorld());

            if ((int) mapState.scale != scale)
                return false;
            return super.onClicked(atlas, otherStack, slot, clickType, player,
                    cursorStackReference);
        }
        return false;
    }

    private boolean handleEmptyMapCLick(ItemStack atlas, ItemStack map, ClickType clickType) {
        int emptyMapCount = atlas.getOrDefault(ImprovedMapsComponentTypes.ATLAS_EMPTY_MAP_COUNT, 0);
        int transferCount = clickType == ClickType.RIGHT ? 1 : map.getCount();
        atlas.set(ImprovedMapsComponentTypes.ATLAS_EMPTY_MAP_COUNT, emptyMapCount + transferCount);
        map.decrement(transferCount);
        return true;
    }

    public ActionResult useOnBlock(ItemUsageContext context) {
        BlockState blockState = context.getWorld().getBlockState(context.getBlockPos());
        if (blockState.isIn(BlockTags.BANNERS)) {
            World world = context.getWorld();
            if (!world.isClient) {
                MapIdComponent mapIdComponent = context.getStack().get(DataComponentTypes.MAP_ID);
                MapState mapState = world.getMapState(mapIdComponent);
                if (mapState != null
                        && !mapState.addBanner(context.getWorld(), context.getBlockPos())) {
                    return ActionResult.FAIL;
                }
            }

            return ActionResult.SUCCESS;
        } else {
            return super.useOnBlock(context);
        }
    }

    // Clear MAP_ID from unequiped AtlasItem, prevents AtlasItem from going into a Cartography Table
    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity,
            @Nullable EquipmentSlot slot) {
        if (slot == null) {
            stack.set(DataComponentTypes.MAP_ID, null);
        }
        super.inventoryTick(stack, world, entity, slot);
    }

    // Disable onUse for AtlasItem
    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        return ActionResult.PASS;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.NONE;
    }
}
