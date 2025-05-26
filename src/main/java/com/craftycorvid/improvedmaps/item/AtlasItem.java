package com.craftycorvid.improvedmaps.item;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.math.Fraction;
import com.craftycorvid.improvedmaps.ImprovedMaps;
import com.craftycorvid.improvedmaps.ImprovedMapsComponentTypes;
import com.craftycorvid.improvedmaps.ImprovedMapsUtils;
import com.craftycorvid.improvedmaps.internal.ICustomBundleContentBuilder;
import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.networking.api.server.PolymerServerNetworking;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.component.type.TooltipDisplayComponent;
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
import net.minecraft.item.tooltip.BundleTooltipData;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtInt;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ClickType;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import xyz.nucleoid.packettweaker.PacketContext;

import static com.craftycorvid.improvedmaps.ImprovedMaps.id;

public class AtlasItem extends BundleItem implements PolymerItem {
    private static final int FULL_ITEM_BAR_COLOR = ColorHelper.fromFloats(1.0F, 1.0F, 0.33F, 0.33F);
    private static final int ITEM_BAR_COLOR = ColorHelper.fromFloats(1.0F, 0.44F, 0.53F, 1.0F);

    public AtlasItem(Settings settings) {
        super(settings);
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
        ServerPlayerEntity player = context.getPlayer();
        if (player != null && player instanceof ServerPlayerEntity && PolymerServerNetworking
                .getMetadata(player.networkHandler, ImprovedMaps.HELLO_PACKET, NbtInt.TYPE) != null)
            return this;
        else
            return itemStack.getCount() > 1 ? Items.BOOK : Items.BUNDLE;
    }

    @Override
    public Identifier getPolymerItemModel(ItemStack itemStack, PacketContext context) {
        if (PolymerResourcePackUtils.hasMainPack(context)) {
            return id("atlas");
        } else {
            return Identifier.of("minecraft", "book");
        }
    }

    @Override
    public ItemStack getPolymerItemStack(ItemStack itemStack, TooltipType tooltipType,
            PacketContext context) {
        ItemStack clientStack =
                PolymerItem.super.getPolymerItemStack(itemStack, tooltipType, context);

        String dimension = itemStack.getOrDefault(ImprovedMapsComponentTypes.ATLAS_DIMENSION, "");

        List<String> stringList = new ArrayList<>();
        stringList.add(dimension);

        clientStack.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(
                new ArrayList<>(), new ArrayList<>(), stringList, new ArrayList<>()));
        return clientStack;
    }

    @Override
    public void modifyClientTooltip(List<Text> tooltip, ItemStack stack, PacketContext context) {
        String dimension = stack.getOrDefault(ImprovedMapsComponentTypes.ATLAS_DIMENSION, null);
        Byte scale = stack.getOrDefault(ImprovedMapsComponentTypes.ATLAS_SCALE, null);
        int filled_maps = stack
                .getOrDefault(DataComponentTypes.BUNDLE_CONTENTS, BundleContentsComponent.DEFAULT)
                .size();
        Integer empty_maps =
                stack.getOrDefault(ImprovedMapsComponentTypes.ATLAS_EMPTY_MAP_COUNT, 0);
        tooltip.clear();
        tooltip.add(Text.literal(filled_maps + "/512 Filled Maps").formatted(Formatting.GRAY));
        tooltip.add(Text.literal(empty_maps + " Empty Maps").formatted(Formatting.GRAY));
        if (dimension != null)
            tooltip.add(
                    Text.literal("Dimension " + ImprovedMapsUtils.formatDimensionString(dimension))
                            .formatted(Formatting.GRAY));
        if (scale != null)
            tooltip.add(Text.literal("Scale " + ImprovedMapsUtils.scaleToString(scale))
                    .formatted(Formatting.GRAY));
    }

    @Override
    public void onCraft(ItemStack stack, World world) {
        BundleContentsComponent bundleContents = stack
                .getOrDefault(DataComponentTypes.BUNDLE_CONTENTS, BundleContentsComponent.DEFAULT);
        ItemStack map = bundleContents.get(0);
        MapIdComponent mapIdComponent = map.get(DataComponentTypes.MAP_ID);
        MapState activeState = FilledMapItem.getMapState(mapIdComponent, world);
        if (activeState != null) {
            stack.set(ImprovedMapsComponentTypes.ATLAS_SCALE, activeState.scale);
            stack.set(ImprovedMapsComponentTypes.ATLAS_DIMENSION,
                    activeState.dimension.getValue().toString());
        }
        stack.set(DataComponentTypes.MAP_ID, mapIdComponent);
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
                        atlas.getOrDefault(ImprovedMapsComponentTypes.ATLAS_DIMENSION, "");
                Byte scale = atlas.getOrDefault(ImprovedMapsComponentTypes.ATLAS_SCALE, (byte) -1);
                MapState mapState = FilledMapItem.getMapState(itemStack, player.getWorld());

                if (mapState.scale != scale
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
                Byte scale = atlas.getOrDefault(ImprovedMapsComponentTypes.ATLAS_SCALE, (byte) 0);
                MapState mapState = FilledMapItem.getMapState(otherStack, player.getWorld());

                if (mapState.scale != scale
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
                } else {
                    int emptyCount =
                            atlas.getOrDefault(ImprovedMapsComponentTypes.ATLAS_EMPTY_MAP_COUNT, 0);
                    if (emptyCount > 0) {
                        playRemoveOneSound(player);
                        cursorStackReference.set(new ItemStack(Items.MAP, emptyCount));
                        atlas.set(ImprovedMapsComponentTypes.ATLAS_EMPTY_MAP_COUNT, 0);
                    }
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

    // Disable onUse for AtlasItem
    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        return ActionResult.PASS;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.NONE;
    }


    // Client-side rendering of the bundle fullness bar
    // Based on
    // https://github.com/FaeWulf/Diversity/blob/sub-mod-1.21.5/common-better-bundle/src/main/java/xyz/faewulf/diversity_better_bundle/mixin/item/buildingBundle/BundleItemMixin.java
    public static float getAmountFilled(ItemStack stack) {
        int usedSpace = MathHelper.multiplyFraction(stack
                .getOrDefault(DataComponentTypes.BUNDLE_CONTENTS, BundleContentsComponent.DEFAULT)
                .getOccupancy(), 64);
        int maxValue = 512;

        return usedSpace * 1f / maxValue;
    }

    // override the weight value when pass the bundlecontens to the client side for rendering the
    // fullness bar
    @Override
    public Optional<TooltipData> getTooltipData(ItemStack stack) {
        Optional<TooltipData> original = super.getTooltipData(stack);

        TooltipDisplayComponent tooltipdisplay = stack
                .getOrDefault(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplayComponent.DEFAULT);

        if (!tooltipdisplay.shouldDisplay(DataComponentTypes.BUNDLE_CONTENTS)) {
            return original;
        } else {
            int usedSpace = MathHelper
                    .multiplyFraction(stack.getOrDefault(DataComponentTypes.BUNDLE_CONTENTS,
                            BundleContentsComponent.DEFAULT).getOccupancy(), 64);
            int maxValue = 512;

            BundleContentsComponent bundleContents = stack.getOrDefault(
                    DataComponentTypes.BUNDLE_CONTENTS, BundleContentsComponent.DEFAULT);

            // override to List
            List<ItemStack> itemStacks = new ArrayList<>();
            bundleContents.stream().forEach(itemStacks::add);

            // create new bundle content
            BundleContentsComponent bundleContents1 = new BundleContentsComponent(itemStacks,
                    Fraction.getFraction(usedSpace * 1f / maxValue),
                    bundleContents.getSelectedStackIndex());

            // pass to client renderer
            return Optional.ofNullable(bundleContents1).map(BundleTooltipData::new);
        }
    }

    @Override
    public int getItemBarStep(ItemStack stack) {
        int usedSpace = MathHelper.multiplyFraction(stack
                .getOrDefault(DataComponentTypes.BUNDLE_CONTENTS, BundleContentsComponent.DEFAULT)
                .getOccupancy(), 64);
        int maxValue = 512;

        return (int) Math.clamp(Math.floor(13f * usedSpace / maxValue), 1, 13);
    }

    @Override
    public int getItemBarColor(ItemStack stack) {
        int usedSpace = MathHelper.multiplyFraction(stack
                .getOrDefault(DataComponentTypes.BUNDLE_CONTENTS, BundleContentsComponent.DEFAULT)
                .getOccupancy(), 64);
        int maxValue = 512;

        if (usedSpace >= maxValue) {
            return FULL_ITEM_BAR_COLOR;
        } else {
            return ITEM_BAR_COLOR;
        }
    }
}
