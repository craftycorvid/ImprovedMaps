package com.craftycorvid.improvedmaps;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import com.craftycorvid.improvedmaps.item.ImprovedMapsItems;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class ImprovedMapsLifecycleEvents {
    // Used to prevent Map creation spam consuming all Empty Maps on auto-create
    private static final Semaphore mutex = new Semaphore(1);

    public static void ImprovedMapsServerTick(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (player.isRemoved() || player.isInTeleportationState() || player.isDisconnected())
                continue;
            ItemStack mainHand = player.getStackInHand(Hand.MAIN_HAND);
            if (mainHand.isOf(ImprovedMapsItems.ATLAS))
                AtlasPlayerHandTick(player, mainHand, EquipmentSlot.MAINHAND);
            ItemStack offHand = player.getStackInHand(Hand.OFF_HAND);
            if (offHand.isOf(ImprovedMapsItems.ATLAS))
                AtlasPlayerHandTick(player, offHand, EquipmentSlot.OFFHAND);
        }
    }

    public static void AtlasPlayerHandTick(ServerPlayerEntity player, ItemStack atlas,
            EquipmentSlot slot) {
        World world = player.getWorld();
        List<ItemStack> currentDimMapItemStacks = getCurrentDimMapsFromAtlas(world, atlas);
        ItemStack mapStack = getActiveAtlasMap(currentDimMapItemStacks, player);
        if (mapStack == null)
            return;

        MapState activeState = FilledMapItem.getMapState(mapStack, world);
        byte scale = activeState.scale;
        int scaleWidth = (1 << scale) * 128;
        // Create new Map entries
        boolean isPlayerOutsideAllMapRegions =
                distanceBetweenMapStateAndPlayer(activeState, player) > scaleWidth;

        if (isPlayerOutsideAllMapRegions) {
            ItemStack newMap = maybeCreateNewMapEntry(player, atlas, scale,
                    MathHelper.floor(player.getX()), MathHelper.floor(player.getZ()));
            if (newMap != null)
                mapStack = newMap;
        }

        if (mapStack.isOf(Items.FILLED_MAP)) {
            atlas.set(DataComponentTypes.MAP_ID, mapStack.get(DataComponentTypes.MAP_ID));
            mapStack.inventoryTick(world, player, slot);
        }
    }

    public static List<ItemStack> getAllMapsFromAtlas(World world, ItemStack atlas) {
        BundleContentsComponent bundleContents = atlas.get(DataComponentTypes.BUNDLE_CONTENTS);
        List<ItemStack> mapStacks = new ArrayList<>();
        bundleContents.iterate().forEach((map) -> {
            if (!map.isEmpty() && map.isOf(Items.FILLED_MAP))
                mapStacks.add(map);
        });
        return mapStacks;
    }

    public static List<ItemStack> getCurrentDimMapsFromAtlas(World world, ItemStack atlas) {
        return getAllMapsFromAtlas(world, atlas).stream().filter(map -> {
            MapState mapState = FilledMapItem.getMapState(map, world);
            return mapState != null
                    ? mapState.dimension.getValue()
                            .compareTo(world.getRegistryKey().getValue()) == 0
                    : false;
        }).toList();
    }

    public static ItemStack getActiveAtlasMap(List<ItemStack> currentDimMapItemStacks,
            ServerPlayerEntity player) {
        ItemStack minDistStack = null;
        for (ItemStack stack : currentDimMapItemStacks) {
            if (minDistStack == null) {
                minDistStack = stack;
                continue;
            }
            double previous = distanceBetweenMapStateAndPlayer(
                    FilledMapItem.getMapState(minDistStack, player.getWorld()), player);
            double current = distanceBetweenMapStateAndPlayer(
                    FilledMapItem.getMapState(stack, player.getWorld()), player);
            if (current < previous) {
                minDistStack = stack;
            }
        }
        return minDistStack;
    }

    public static double distanceBetweenMapStateAndPlayer(MapState mapState, PlayerEntity player) {
        return Math.hypot(Math.abs(mapState.centerX - player.getX()),
                Math.abs(mapState.centerZ - player.getZ()));
    }

    private static ItemStack maybeCreateNewMapEntry(ServerPlayerEntity player, ItemStack atlas,
            int scale, int destX, int destZ) {
        BundleContentsComponent bundleContents = atlas.get(DataComponentTypes.BUNDLE_CONTENTS);
        BundleContentsComponent.Builder builder =
                new BundleContentsComponent.Builder(bundleContents);
        int emptyCount = atlas.get(ImprovedMapsComponentTypes.ATLAS_EMPTY_MAP_COUNT);
        if (mutex.availablePermits() > 0 && (emptyCount > 0 || player.isCreative())) {
            try {
                mutex.acquire();

                // Make the new map
                ItemStack newMap = FilledMapItem.createMap((ServerWorld) player.getWorld(), destX,
                        destZ, (byte) scale, true, false);
                builder.add(newMap);
                atlas.set(DataComponentTypes.BUNDLE_CONTENTS, builder.build());
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
