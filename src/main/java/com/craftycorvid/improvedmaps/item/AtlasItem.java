package com.craftycorvid.improvedmaps.item;

import java.util.List;
import org.jetbrains.annotations.Nullable;
import com.craftycorvid.improvedmaps.ImprovedMapsComponentTypes;
import com.craftycorvid.improvedmaps.ImprovedMapsUtils;
import com.craftycorvid.improvedmaps.internal.ICustomBundleContentBuilder;
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
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
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
        String dimension = stack.getOrDefault(ImprovedMapsComponentTypes.ATLAS_DIMENSION, null);
        Integer scale = stack.getOrDefault(ImprovedMapsComponentTypes.ATLAS_SCALE, null);
        Integer empty_maps =
                stack.getOrDefault(ImprovedMapsComponentTypes.ATLAS_EMPTY_MAP_COUNT, 0);
        tooltip.clear();
        if (dimension != null)
            tooltip.add(
                    Text.literal("Dimension " + ImprovedMapsUtils.formatDimensionString(dimension))
                            .formatted(Formatting.GRAY));
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
        stack.set(ImprovedMapsComponentTypes.ATLAS_DIMENSION,
                activeState.dimension.getValue().toString());
    }

    @Override
    public boolean onStackClicked(ItemStack atlas, Slot slot, ClickType clickType,
            PlayerEntity player) {
        if (atlas.getCount() > 1)
            return false;

        BundleContentsComponent bundleContentsComponent =
                atlas.get(DataComponentTypes.BUNDLE_CONTENTS);
        if (bundleContentsComponent == null)
            return false;

        ItemStack itemStack = slot.getStack();
        BundleContentsComponent.Builder builder =
                new BundleContentsComponent.Builder(bundleContentsComponent);
        ((ICustomBundleContentBuilder) builder).setMaxSize(512);
        if (clickType == ClickType.LEFT && !itemStack.isEmpty()) {
            if (itemStack.isOf(Items.MAP)) {
                return handleEmptyMapCLick(atlas, itemStack, clickType);
            } else if (itemStack.isOf(Items.FILLED_MAP)) {
                String dimension =
                        atlas.getOrDefault(ImprovedMapsComponentTypes.ATLAS_DIMENSION, null);
                int scale = atlas.getOrDefault(ImprovedMapsComponentTypes.ATLAS_SCALE, 0);
                MapState mapState = FilledMapItem.getMapState(itemStack, player.getWorld());

                if ((int) mapState.scale != scale
                        || !mapState.dimension.getValue().toString().equals(dimension))
                    return false;

                if (builder.add(slot, player) > 0) {
                    playInsertSound(player);
                } else {
                    playInsertFailSound(player);
                }
                atlas.set(DataComponentTypes.BUNDLE_CONTENTS, builder.build());
                this.onContentChanged(player);
                return true;
            }
        } else if (clickType == ClickType.RIGHT && itemStack.isEmpty()) {
            atlas.set(DataComponentTypes.MAP_ID, null);
            ItemStack itemStack2 = builder.removeSelected();
            if (itemStack2 != null) {
                ItemStack itemStack3 = slot.insertStack(itemStack2);
                if (itemStack3.getCount() > 0) {
                    builder.add(itemStack3);
                } else {
                    playRemoveOneSound(player);
                }
            }

            atlas.set(DataComponentTypes.BUNDLE_CONTENTS, builder.build());
            this.onContentChanged(player);
            return true;
        }
        return false;
    }

    @Override
    public boolean onClicked(ItemStack atlas, ItemStack otherStack, Slot slot, ClickType clickType,
            PlayerEntity player, StackReference cursorStackReference) {
        if (atlas.getCount() > 1)
            return false;
        if (clickType == ClickType.LEFT && otherStack.isEmpty()) {
            setSelectedStackIndex(atlas, -1);
            return false;
        }

        BundleContentsComponent bundleContentsComponent =
                atlas.get(DataComponentTypes.BUNDLE_CONTENTS);
        if (bundleContentsComponent == null)
            return false;

        BundleContentsComponent.Builder builder =
                new BundleContentsComponent.Builder(bundleContentsComponent);
        ((ICustomBundleContentBuilder) builder).setMaxSize(512);
        if (clickType == ClickType.LEFT && !otherStack.isEmpty()) {
            if (otherStack.isOf(Items.MAP)) {
                return handleEmptyMapCLick(atlas, otherStack, clickType);
            } else if (otherStack.isOf(Items.FILLED_MAP)) {
                String dimension =
                        atlas.getOrDefault(ImprovedMapsComponentTypes.ATLAS_DIMENSION, null);
                int scale = atlas.getOrDefault(ImprovedMapsComponentTypes.ATLAS_SCALE, 0);
                MapState mapState = FilledMapItem.getMapState(otherStack, player.getWorld());

                if ((int) mapState.scale != scale
                        || !mapState.dimension.getValue().toString().equals(dimension)) {
                    playInsertFailSound(player);
                    return false;
                }

                if (slot.canTakePartial(player) && builder.add(otherStack) > 0) {
                    playInsertSound(player);
                } else {
                    playInsertFailSound(player);
                }

                atlas.set(DataComponentTypes.BUNDLE_CONTENTS, builder.build());
                this.onContentChanged(player);
                return true;
            }
        } else if (clickType == ClickType.RIGHT && otherStack.isEmpty()) {
            atlas.set(DataComponentTypes.MAP_ID, null);
            if (slot.canTakePartial(player)) {
                ItemStack itemStack = builder.removeSelected();
                if (itemStack != null) {
                    playRemoveOneSound(player);
                    cursorStackReference.set(itemStack);
                }
            }

            atlas.set(DataComponentTypes.BUNDLE_CONTENTS, builder.build());
            this.onContentChanged(player);
            return true;
        }
        setSelectedStackIndex(atlas, -1);
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

    // Private methods copied from BundleItem
    private static void playRemoveOneSound(Entity entity) {
        entity.playSound(SoundEvents.ITEM_BUNDLE_REMOVE_ONE, 0.8F,
                0.8F + entity.getWorld().getRandom().nextFloat() * 0.4F);
    }

    private static void playInsertSound(Entity entity) {
        entity.playSound(SoundEvents.ITEM_BUNDLE_INSERT, 0.8F,
                0.8F + entity.getWorld().getRandom().nextFloat() * 0.4F);
    }

    private static void playInsertFailSound(Entity entity) {
        entity.playSound(SoundEvents.ITEM_BUNDLE_INSERT_FAIL, 1.0F, 1.0F);
    }

    private void onContentChanged(PlayerEntity user) {
        ScreenHandler screenHandler = user.currentScreenHandler;
        if (screenHandler != null) {
            screenHandler.onContentChanged(user.getInventory());
        }
    }
}
