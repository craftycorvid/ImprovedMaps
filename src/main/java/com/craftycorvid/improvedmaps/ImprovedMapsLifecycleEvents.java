package com.craftycorvid.improvedmaps;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import com.craftycorvid.improvedmaps.internal.ICustomBundleContentBuilder;
import com.craftycorvid.improvedmaps.item.ImprovedMapsItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

// Logic based on
// https://github.com/Pepperoni-Jabroni/MapAtlases/blob/main/src/main/java/pepjebs/mapatlases/lifecycle/MapAtlasesServerLifecycleEvents.java
public final class ImprovedMapsLifecycleEvents {
    // Used to prevent Map creation spam consuming all Empty Maps on auto-create
    private static final Semaphore mutex = new Semaphore(1);

    public static void ImprovedMapsServerTick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isRemoved() || player.isChangingDimension() || player.hasDisconnected())
                continue;
            ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
            if (mainHand.is(ImprovedMapsItems.ATLAS))
                AtlasPlayerHandTick(player, mainHand, EquipmentSlot.MAINHAND);
            ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);
            if (offHand.is(ImprovedMapsItems.ATLAS))
                AtlasPlayerHandTick(player, offHand, EquipmentSlot.OFFHAND);
        }
    }

    public static void initializeEmptyAtlas(ServerPlayer player, ItemStack atlas) {
        Boolean initialized = atlas.get(ImprovedMapsComponentTypes.ATLAS_INITIALIZED);
        if (initialized == null || !initialized) {

            atlas.set(ImprovedMapsComponentTypes.ATLAS_INITIALIZED, true);

            BundleContents contents = atlas.get(DataComponents.BUNDLE_CONTENTS);
            if (contents.isEmpty()) {
                BundleContents.Mutable builder = new BundleContents.Mutable(BundleContents.EMPTY);
                ((ICustomBundleContentBuilder) builder).setMaxSize(512);

                int emptyCount = atlas.get(ImprovedMapsComponentTypes.ATLAS_EMPTY_MAP_COUNT);

                if (emptyCount > 0 || player.isCreative()) {
                    ItemStack newMap = MapItem.create(player.level(),
                            Mth.floor(player.getX()), Mth.floor(player.getZ()),
                            atlas.get(ImprovedMapsComponentTypes.ATLAS_SCALE), true, false);

                    builder.tryInsert(newMap);
                    atlas.set(DataComponents.BUNDLE_CONTENTS, builder.toImmutable());

                    if (!player.isCreative()) {
                        atlas.set(ImprovedMapsComponentTypes.ATLAS_EMPTY_MAP_COUNT, emptyCount - 1);
                    }
                }
            }
        }
    }

    public static void AtlasPlayerHandTick(ServerPlayer player, ItemStack atlas,
            EquipmentSlot slot) {
        initializeEmptyAtlas(player, atlas);

        ServerLevel world = player.level();
        List<ItemStack> currentDimMapItemStacks = getCurrentDimMapsFromAtlas(world, atlas);
        ItemStack mapStack = getActiveAtlasMap(currentDimMapItemStacks, player);
        if (mapStack == null)
            return;

        MapItemSavedData activeState = MapItem.getSavedData(mapStack, world);
        // Create new Map entries
        if (isPlayerOutsideAllMapRegions(activeState, player)
                && atlas.getOrDefault(ImprovedMapsComponentTypes.ATLAS_DIMENSION, "")
                        .equals(world.dimension().identifier().toString())) {
            ItemStack newMap = maybeCreateNewMapEntry(player, atlas, activeState,
                    Mth.floor(player.getX()), Mth.floor(player.getZ()));
            if (newMap != null)
                mapStack = newMap;
        }

        if (mapStack.is(Items.FILLED_MAP)) {
            atlas.set(DataComponents.MAP_ID, mapStack.get(DataComponents.MAP_ID));
            mapStack.inventoryTick(world, player, slot);
        }
    }

    private static boolean isPlayerOutsideAllMapRegions(MapItemSavedData activeState, Player player) {
        byte scale = activeState.scale;
        int scaleWidthFromCenter = ((1 << scale) * 64) + 8;
        return Math.abs(activeState.centerX - player.getX()) > scaleWidthFromCenter
                || Math.abs(activeState.centerZ - player.getZ()) > scaleWidthFromCenter;
    }

    public static List<ItemStack> getAllMapsFromAtlas(ServerLevel world, ItemStack atlas) {
        BundleContents bundleContents = atlas
                .getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
        List<ItemStack> mapStacks = new ArrayList<>();
        bundleContents.items().forEach((map) -> {
            if (!map.isEmpty() && map.is(Items.FILLED_MAP))
                mapStacks.add(map);
        });
        return mapStacks;
    }

    public static List<ItemStack> getCurrentDimMapsFromAtlas(ServerLevel world, ItemStack atlas) {
        return getAllMapsFromAtlas(world, atlas).stream().filter(map -> {
            MapItemSavedData mapState = MapItem.getSavedData(map, world);
            return mapState != null
                    ? mapState.dimension.identifier()
                            .compareTo(world.dimension().identifier()) == 0
                    : false;
        }).toList();
    }

    public static ItemStack getActiveAtlasMap(List<ItemStack> currentDimMapItemStacks,
            ServerPlayer player) {
        ItemStack minDistStack = null;
        for (ItemStack stack : currentDimMapItemStacks) {
            if (minDistStack == null) {
                minDistStack = stack;
                continue;
            }
            double previous = distanceBetweenMapStateAndPlayer(
                    MapItem.getSavedData(minDistStack, player.level()), player);
            double current = distanceBetweenMapStateAndPlayer(
                    MapItem.getSavedData(stack, player.level()), player);
            if (current < previous) {
                minDistStack = stack;
            }
        }
        return minDistStack;
    }

    public static double distanceBetweenMapStateAndPlayer(MapItemSavedData mapState, Player player) {
        return Math.hypot(Math.abs(mapState.centerX - player.getX()),
                Math.abs(mapState.centerZ - player.getZ()));
    }

    private static ItemStack maybeCreateNewMapEntry(ServerPlayer player, ItemStack atlas,
            MapItemSavedData activeState, int playerX, int playerZ) {
        BundleContents bundleContents = atlas
                .getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
        BundleContents.Mutable builder = new BundleContents.Mutable(bundleContents);
        ((ICustomBundleContentBuilder) builder).setMaxSize(512);
        int emptyCount = atlas.getOrDefault(ImprovedMapsComponentTypes.ATLAS_EMPTY_MAP_COUNT, 0);
        if (mutex.availablePermits() > 0 && (emptyCount > 0 || player.isCreative())) {
            try {
                mutex.acquire();
                byte scale = activeState.scale;
                int currentX = activeState.centerX;
                int currentZ = activeState.centerZ;
                int scaleWidth = (1 << scale) * 128;

                int newX = Math.abs(currentX - playerX) > (scaleWidth / 2)
                        ? currentX > playerX ? currentX - scaleWidth : currentX + scaleWidth
                        : currentX;
                int newZ = Math.abs(currentZ - playerZ) > (scaleWidth / 2)
                        ? currentZ > playerZ ? currentZ - scaleWidth : currentZ + scaleWidth
                        : currentZ;

                // Make the new map
                ItemStack newMap = MapItem.create(player.level(), newX, newZ,
                        (byte) scale, true, false);
                builder.tryInsert(newMap);
                atlas.set(DataComponents.BUNDLE_CONTENTS, builder.toImmutable());
                if (!player.isCreative())
                    atlas.set(ImprovedMapsComponentTypes.ATLAS_EMPTY_MAP_COUNT, emptyCount - 1);
                return newMap;
            } catch (InterruptedException e) {
                ImprovedMaps.LOGGER.warn(e.getMessage());
            } finally {
                mutex.release();
            }
        }
        return null;
    }
}
