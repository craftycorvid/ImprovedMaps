package com.craftycorvid.improvedmaps.item;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ARGB;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import com.craftycorvid.improvedmaps.AtlasTooltipData;
import com.craftycorvid.improvedmaps.ImprovedMapsComponentTypes;
import com.craftycorvid.improvedmaps.ImprovedMapsUtils;
import com.craftycorvid.improvedmaps.internal.ICustomBundleContentBuilder;
import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;

import static com.craftycorvid.improvedmaps.ImprovedMaps.MOD_CONFIG;
import static com.craftycorvid.improvedmaps.ImprovedMaps.id;
import static com.craftycorvid.improvedmaps.ImprovedMapsNetworking.PLAYERS_WITH_CLIENT;

public class AtlasItem extends BundleItem implements PolymerItem {
    private static final int FULL_ITEM_BAR_COLOR = ARGB.colorFromFloat(1.0F, 1.0F, 0.33F, 0.33F);
    private static final int ITEM_BAR_COLOR = ARGB.colorFromFloat(1.0F, 0.44F, 0.53F, 1.0F);

    public AtlasItem(Properties settings) {
        super(settings);
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
        ServerPlayer player = eu.pb4.polymer.common.api.PolymerCommonUtils.getPlayer(context);
        if (player == null || PLAYERS_WITH_CLIENT.contains(player.getUUID()))
            return this;
        else
            return itemStack.getCount() > 1 ? Items.BOOK : Items.BUNDLE;
    }

    @Override
    public Identifier getPolymerItemModel(ItemStack itemStack, PacketContext context, HolderLookup.Provider lookup) {
        if (PolymerResourcePackUtils.hasMainPack(context)) {
            return id("atlas");
        } else {
            return Identifier.fromNamespaceAndPath("minecraft", "book");
        }
    }

    @Override
    public ItemStack getPolymerItemStack(ItemStack itemStack, TooltipFlag tooltipType,
            PacketContext context, HolderLookup.Provider lookup) {
        ItemStack clientStack = PolymerItem.super.getPolymerItemStack(itemStack, tooltipType, context, lookup);

        String dimension = itemStack.getOrDefault(ImprovedMapsComponentTypes.ATLAS_DIMENSION, "");

        List<String> stringList = new ArrayList<>();
        stringList.add(dimension);

        clientStack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(
                new ArrayList<>(), new ArrayList<>(), stringList, new ArrayList<>()));
        return clientStack;
    }

    @Override
    public void modifyClientTooltip(List<Component> tooltip, ItemStack stack, PacketContext context) {
        String dimension = stack.getOrDefault(ImprovedMapsComponentTypes.ATLAS_DIMENSION, null);
        Byte scale = stack.getOrDefault(ImprovedMapsComponentTypes.ATLAS_SCALE, null);
        int filled_maps = stack
                .getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY)
                .size();
        Integer empty_maps = stack.getOrDefault(ImprovedMapsComponentTypes.ATLAS_EMPTY_MAP_COUNT, 0);
        tooltip.clear();
        tooltip.add(Component.literal(filled_maps + "/" + MOD_CONFIG.atlasMapCapacity + " Filled Maps")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(empty_maps + " Empty Maps").withStyle(ChatFormatting.GRAY));
        if (dimension != null)
            tooltip.add(
                    Component.literal("Dimension " + ImprovedMapsUtils.formatDimensionString(dimension))
                            .withStyle(ChatFormatting.GRAY));
        if (scale != null)
            tooltip.add(Component.literal("Scale " + ImprovedMapsUtils.scaleToString(scale))
                    .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public void onCraftedPostProcess(ItemStack stack, Level world) {
        BundleContents bundleContents = stack
                .getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
        ItemStack map = bundleContents.itemCopyStream().findFirst().orElse(ItemStack.EMPTY);
        MapId mapIdComponent = map.get(DataComponents.MAP_ID);
        MapItemSavedData activeState = MapItem.getSavedData(mapIdComponent, world);
        if (activeState != null) {
            stack.set(ImprovedMapsComponentTypes.ATLAS_SCALE, activeState.scale);
            stack.set(ImprovedMapsComponentTypes.ATLAS_DIMENSION,
                    activeState.dimension.identifier().toString());
        }
        stack.set(DataComponents.MAP_ID, mapIdComponent);
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack atlas, Slot slot, ClickAction clickType,
            Player player) {
        if (player.isCreative() && player.containerMenu instanceof InventoryMenu)
            return false;
        if (atlas.getCount() > 1)
            return false;

        BundleContents bundleContentsComponent = atlas.get(DataComponents.BUNDLE_CONTENTS);
        if (bundleContentsComponent == null)
            return false;

        ItemStack itemStack = slot.getItem();
        BundleContents.Mutable builder = new BundleContents.Mutable(bundleContentsComponent);
        ((ICustomBundleContentBuilder) builder).setMaxSize(MOD_CONFIG.atlasMapCapacity);
        if (clickType == ClickAction.PRIMARY && !itemStack.isEmpty()) {
            if (itemStack.is(Items.MAP)) {
                return handleEmptyMapCLick(atlas, itemStack, clickType);
            } else if (itemStack.is(Items.FILLED_MAP)) {
                String dimension = atlas.getOrDefault(ImprovedMapsComponentTypes.ATLAS_DIMENSION, "");
                Byte scale = atlas.getOrDefault(ImprovedMapsComponentTypes.ATLAS_SCALE, (byte) -1);
                MapItemSavedData mapState = MapItem.getSavedData(itemStack, player.level());

                if (mapState.scale != scale
                        || !mapState.dimension.identifier().toString().equals(dimension))
                    return false;

                if (builder.tryTransfer(slot, player) > 0) {
                    BundleItem.playInsertSound(player);
                } else {
                    BundleItem.playInsertFailSound(player);
                }
                atlas.set(DataComponents.BUNDLE_CONTENTS, builder.toImmutable());
                this.broadcastChangesOnContainerMenu(player);
                return true;
            }
        } else if (clickType == ClickAction.SECONDARY && itemStack.isEmpty()) {
            atlas.set(DataComponents.MAP_ID, null);
            ItemStack itemStack2 = builder.removeOne();
            if (itemStack2 != null) {
                ItemStack itemStack3 = slot.safeInsert(itemStack2);
                if (itemStack3.getCount() > 0) {
                    builder.tryInsert(itemStack3);
                } else {
                    BundleItem.playRemoveOneSound(player);
                }
            }

            atlas.set(DataComponents.BUNDLE_CONTENTS, builder.toImmutable());
            this.broadcastChangesOnContainerMenu(player);
            return true;
        }
        return false;
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack atlas, ItemStack otherStack, Slot slot, ClickAction clickType,
            Player player, SlotAccess cursorStackReference) {
        if (player.isCreative() && player.containerMenu instanceof InventoryMenu)
            return false;
        if (atlas.getCount() > 1)
            return false;
        if (clickType == ClickAction.PRIMARY && otherStack.isEmpty()) {
            toggleSelectedItem(atlas, -1);
            return false;
        }

        BundleContents bundleContentsComponent = atlas.get(DataComponents.BUNDLE_CONTENTS);
        if (bundleContentsComponent == null)
            return false;

        BundleContents.Mutable builder = new BundleContents.Mutable(bundleContentsComponent);
        ((ICustomBundleContentBuilder) builder).setMaxSize(MOD_CONFIG.atlasMapCapacity);
        if (clickType == ClickAction.PRIMARY && !otherStack.isEmpty()) {
            if (otherStack.is(Items.MAP)) {
                return handleEmptyMapCLick(atlas, otherStack, clickType);
            } else if (otherStack.is(Items.FILLED_MAP)) {
                String dimension = atlas.getOrDefault(ImprovedMapsComponentTypes.ATLAS_DIMENSION, null);
                Byte scale = atlas.getOrDefault(ImprovedMapsComponentTypes.ATLAS_SCALE, (byte) 0);
                MapItemSavedData mapState = MapItem.getSavedData(otherStack, player.level());

                if (mapState.scale != scale
                        || !mapState.dimension.identifier().toString().equals(dimension)) {
                    BundleItem.playInsertFailSound(player);
                    return false;
                }

                if (slot.allowModification(player) && builder.tryInsert(otherStack) > 0) {
                    BundleItem.playInsertSound(player);
                } else {
                    BundleItem.playInsertFailSound(player);
                }

                atlas.set(DataComponents.BUNDLE_CONTENTS, builder.toImmutable());
                this.broadcastChangesOnContainerMenu(player);
                return true;
            }
        } else if (clickType == ClickAction.SECONDARY && otherStack.isEmpty()) {
            atlas.set(DataComponents.MAP_ID, null);
            if (slot.allowModification(player)) {
                ItemStack itemStack = builder.removeOne();
                if (itemStack != null) {
                    BundleItem.playRemoveOneSound(player);
                    cursorStackReference.set(itemStack);
                } else {
                    int emptyCount = atlas.getOrDefault(ImprovedMapsComponentTypes.ATLAS_EMPTY_MAP_COUNT, 0);
                    if (emptyCount > 0) {
                        BundleItem.playRemoveOneSound(player);
                        cursorStackReference.set(new ItemStack(Items.MAP, emptyCount));
                        atlas.set(ImprovedMapsComponentTypes.ATLAS_EMPTY_MAP_COUNT, 0);
                    }
                }
            }

            atlas.set(DataComponents.BUNDLE_CONTENTS, builder.toImmutable());
            this.broadcastChangesOnContainerMenu(player);
            return true;
        }
        toggleSelectedItem(atlas, -1);
        return false;
    }

    private boolean handleEmptyMapCLick(ItemStack atlas, ItemStack map, ClickAction clickType) {
        int emptyMapCount = atlas.getOrDefault(ImprovedMapsComponentTypes.ATLAS_EMPTY_MAP_COUNT, 0);
        int transferCount = clickType == ClickAction.SECONDARY ? 1 : map.getCount();
        atlas.set(ImprovedMapsComponentTypes.ATLAS_EMPTY_MAP_COUNT, emptyMapCount + transferCount);
        map.shrink(transferCount);
        return true;
    }

    public InteractionResult useOn(UseOnContext context) {
        BlockState blockState = context.getLevel().getBlockState(context.getClickedPos());
        if (blockState.is(BlockTags.BANNERS)) {
            Level world = context.getLevel();
            if (world instanceof net.minecraft.server.level.ServerLevel) {
                MapId mapIdComponent = context.getItemInHand().get(DataComponents.MAP_ID);
                MapItemSavedData mapState = world.getMapData(mapIdComponent);
                if (mapState != null
                        && !mapState.toggleBanner(context.getLevel(), context.getClickedPos())) {
                    return InteractionResult.FAIL;
                }
            }

            return InteractionResult.SUCCESS;
        } else {
            return super.useOn(context);
        }
    }

    // Disable onUse for AtlasItem
    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.NONE;
    }

    // Render the bundle tooltip's fullness bar against atlasMapCapacity instead of the
    // vanilla 64-item bundle weight. The client maps AtlasTooltipData -> a ClientBundleTooltip
    // with this scaled fullness (see ImprovedMapsClient + ClientBundleTooltipMixin).
    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        BundleContents contents = stack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
        if (contents.isEmpty())
            return super.getTooltipImage(stack);
        int used = Math.min(contents.size(), MOD_CONFIG.atlasMapCapacity);
        return Optional.of(new AtlasTooltipData(contents,
                org.apache.commons.lang3.math.Fraction.getFraction(used, MOD_CONFIG.atlasMapCapacity)));
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int usedSpace = stack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY).size();
        return (int) Math.clamp(Math.floor(13f * usedSpace / MOD_CONFIG.atlasMapCapacity), 1, 13);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        int usedSpace = stack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY).size();
        if (usedSpace >= MOD_CONFIG.atlasMapCapacity) {
            return FULL_ITEM_BAR_COLOR;
        } else {
            return ITEM_BAR_COLOR;
        }
    }
}
