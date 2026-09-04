package com.craftycorvid.improvedmaps.item;

import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import com.craftycorvid.improvedmaps.AtlasTooltipData;
import com.craftycorvid.improvedmaps.ImprovedMapsComponentTypes;
import com.craftycorvid.improvedmaps.ImprovedMapsUtils;
import com.craftycorvid.improvedmaps.internal.ICustomBundleContentBuilder;
import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.resourcepack.api.PolymerModelData;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;

import static com.craftycorvid.improvedmaps.ImprovedMaps.MOD_CONFIG;
import static com.craftycorvid.improvedmaps.ImprovedMaps.id;
import static com.craftycorvid.improvedmaps.ImprovedMapsNetworking.PLAYERS_WITH_CLIENT;

public class AtlasItem extends BundleItem implements PolymerItem {
    private static final int FULL_ITEM_BAR_COLOR = Mth.color(1.0F, 0.33F, 0.33F);
    private static final int ITEM_BAR_COLOR = Mth.color(0.44F, 0.53F, 1.0F);

    // A vanilla client draws the atlas from the Polymer resource pack: a book carrying the custom
    // model data these hand out, one model per dimension.
    private static final PolymerModelData OVERWORLD_MODEL = model("atlas_overworld");
    private static final PolymerModelData NETHER_MODEL = model("atlas_nether");
    private static final PolymerModelData END_MODEL = model("atlas_end");
    private static final PolymerModelData UNKNOWN_MODEL = model("atlas_unknown");

    private static PolymerModelData model(String name) {
        return PolymerResourcePackUtils.requestModel(Items.BOOK, id("item/" + name));
    }

    public AtlasItem(Properties settings) {
        super(settings);
    }

    private static PolymerModelData modelFor(ItemStack stack) {
        return switch (stack.getOrDefault(ImprovedMapsComponentTypes.ATLAS_DIMENSION, "")) {
            case "minecraft:overworld" -> OVERWORLD_MODEL;
            case "minecraft:the_nether" -> NETHER_MODEL;
            case "minecraft:the_end" -> END_MODEL;
            default -> UNKNOWN_MODEL;
        };
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, ServerPlayer player) {
        // A client that cannot resolve our item id drops the connection over it.
        if (player != null && PLAYERS_WITH_CLIENT.contains(player.getUUID()))
            return this;
        if (PolymerResourcePackUtils.hasMainPack(player))
            return modelFor(itemStack).item();
        return itemStack.getCount() > 1 ? Items.BOOK : Items.BUNDLE;
    }

    @Override
    public int getPolymerCustomModelData(ItemStack itemStack, ServerPlayer player) {
        return PolymerResourcePackUtils.hasMainPack(player) ? modelFor(itemStack).value() : -1;
    }

    @Override
    public ItemStack getPolymerItemStack(ItemStack itemStack, TooltipFlag tooltipType,
            HolderLookup.Provider lookup, ServerPlayer player) {
        ItemStack clientStack = PolymerItem.super.getPolymerItemStack(itemStack, tooltipType, lookup, player);
        // Polymer builds the client stack from a whitelist of components, so our own never make
        // the trip. The minimap picks its atlas by dimension, so put that one back - the component
        // gates itself to clients running the mod, a vanilla one drops it at encode.
        clientStack.set(ImprovedMapsComponentTypes.ATLAS_DIMENSION,
                itemStack.getOrDefault(ImprovedMapsComponentTypes.ATLAS_DIMENSION, ""));
        return clientStack;
    }

    @Override
    public void modifyClientTooltip(List<Component> tooltip, ItemStack stack, ServerPlayer player) {
        String dimension = stack.getOrDefault(ImprovedMapsComponentTypes.ATLAS_DIMENSION, null);
        Byte scale = stack.getOrDefault(ImprovedMapsComponentTypes.ATLAS_SCALE, null);
        int filled_maps = stack
                .getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY)
                .size();
        Integer empty_maps = stack.getOrDefault(ImprovedMapsComponentTypes.ATLAS_EMPTY_MAP_COUNT, 0);
        tooltip.clear();
        tooltip.add(Component.literal(filled_maps + "/" + MOD_CONFIG.server_atlasMapCapacity + " Filled Maps")
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
                    activeState.dimension.location().toString());
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
        ((ICustomBundleContentBuilder) builder).setMaxSize(MOD_CONFIG.server_atlasMapCapacity);
        if (clickType == ClickAction.PRIMARY && !itemStack.isEmpty()) {
            if (itemStack.is(Items.MAP)) {
                return handleEmptyMapCLick(atlas, itemStack, clickType);
            } else if (itemStack.is(Items.FILLED_MAP)) {
                String dimension = atlas.getOrDefault(ImprovedMapsComponentTypes.ATLAS_DIMENSION, "");
                Byte scale = atlas.getOrDefault(ImprovedMapsComponentTypes.ATLAS_SCALE, (byte) -1);
                MapItemSavedData mapState = MapItem.getSavedData(itemStack, player.level());

                if (mapState == null || mapState.scale != scale
                        || !mapState.dimension.location().toString().equals(dimension))
                    return false;

                if (builder.tryTransfer(slot, player) > 0) {
                    this.playInsertSound(player);
                }
                atlas.set(DataComponents.BUNDLE_CONTENTS, builder.toImmutable());
                player.containerMenu.broadcastChanges();
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
                    this.playRemoveOneSound(player);
                }
            }

            atlas.set(DataComponents.BUNDLE_CONTENTS, builder.toImmutable());
            player.containerMenu.broadcastChanges();
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
            return false;
        }

        BundleContents bundleContentsComponent = atlas.get(DataComponents.BUNDLE_CONTENTS);
        if (bundleContentsComponent == null)
            return false;

        BundleContents.Mutable builder = new BundleContents.Mutable(bundleContentsComponent);
        ((ICustomBundleContentBuilder) builder).setMaxSize(MOD_CONFIG.server_atlasMapCapacity);
        if (clickType == ClickAction.PRIMARY && !otherStack.isEmpty()) {
            if (otherStack.is(Items.MAP)) {
                return handleEmptyMapCLick(atlas, otherStack, clickType);
            } else if (otherStack.is(Items.FILLED_MAP)) {
                String dimension = atlas.getOrDefault(ImprovedMapsComponentTypes.ATLAS_DIMENSION, null);
                Byte scale = atlas.getOrDefault(ImprovedMapsComponentTypes.ATLAS_SCALE, (byte) 0);
                MapItemSavedData mapState = MapItem.getSavedData(otherStack, player.level());

                if (mapState == null || mapState.scale != scale
                        || !mapState.dimension.location().toString().equals(dimension)) {
                    return false;
                }

                if (slot.allowModification(player) && builder.tryInsert(otherStack) > 0) {
                    this.playInsertSound(player);
                }

                atlas.set(DataComponents.BUNDLE_CONTENTS, builder.toImmutable());
                player.containerMenu.broadcastChanges();
                return true;
            }
        } else if (clickType == ClickAction.SECONDARY && otherStack.isEmpty()) {
            atlas.set(DataComponents.MAP_ID, null);
            if (slot.allowModification(player)) {
                ItemStack itemStack = builder.removeOne();
                if (itemStack != null) {
                    this.playRemoveOneSound(player);
                    cursorStackReference.set(itemStack);
                } else {
                    int emptyCount = atlas.getOrDefault(ImprovedMapsComponentTypes.ATLAS_EMPTY_MAP_COUNT, 0);
                    if (emptyCount > 0) {
                        this.playRemoveOneSound(player);
                        cursorStackReference.set(new ItemStack(Items.MAP, emptyCount));
                        atlas.set(ImprovedMapsComponentTypes.ATLAS_EMPTY_MAP_COUNT, 0);
                    }
                }
            }

            atlas.set(DataComponents.BUNDLE_CONTENTS, builder.toImmutable());
            player.containerMenu.broadcastChanges();
            return true;
        }
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
                MapItemSavedData mapState = MapItem.getSavedData(mapIdComponent, world);
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
    public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
        return InteractionResultHolder.pass(user.getItemInHand(hand));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    // The vanilla bundle tooltip greys its slots out once the contents weigh a full bundle, which
    // an atlas hits at 64 maps however large atlasMapCapacity is. The client maps AtlasTooltipData
    // -> a ClientBundleTooltip that is only "full" at capacity (see ImprovedMapsClient +
    // ClientBundleTooltipMixin).
    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        BundleContents contents = stack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
        if (contents.isEmpty())
            return super.getTooltipImage(stack);
        return Optional.of(new AtlasTooltipData(contents,
                contents.size() >= MOD_CONFIG.server_atlasMapCapacity));
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int usedSpace = stack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY).size();
        return (int) Mth.clamp(Math.floor(13f * usedSpace / MOD_CONFIG.server_atlasMapCapacity), 1, 13);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        int usedSpace = stack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY).size();
        if (usedSpace >= MOD_CONFIG.server_atlasMapCapacity) {
            return FULL_ITEM_BAR_COLOR;
        } else {
            return ITEM_BAR_COLOR;
        }
    }
}
