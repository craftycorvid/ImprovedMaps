package com.craftycorvid.improvedmaps;

import org.joml.Matrix3x2fStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import com.craftycorvid.improvedmaps.config.ModConfig.MinimapCorner;
import com.craftycorvid.improvedmaps.item.ImprovedMapsItems;

import static com.craftycorvid.improvedmaps.ImprovedMaps.MOD_CONFIG;

public final class MinimapHud {
    private static final int MARGIN = 8;
    // Also the page of the atlas grid view (AtlasScreen), nine-sliced there.
    static final Identifier MAP_BACKGROUND = Identifier.fromNamespaceAndPath("minecraft",
            "textures/map/map_background.png");
    // Remembered main-hand hotbar slot of the last-held atlas; -1 = none.
    private static int trackedSlot = -1;

    // HudElement: called every frame during the GUI extract phase.
    public static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        MapId mapId = activeMapId(mc);
        if (mapId == null)
            return;

        MapItemSavedData data = mc.level == null ? null : mc.level.getMapData(mapId);
        if (data == null)
            return;

        MapRenderState state = new MapRenderState();
        mc.getMapRenderer().extractRenderState(mapId, data, state);
        // g.map() draws maps item-frame style: it skips decorations whose type has
        // showOnItemFrame=false (the player marker, off-map pointers, ...). Force the
        // flag on so the minimap shows every decoration, like a held map.
        for (MapRenderState.MapDecorationRenderState decoration : state.decorations) {
            decoration.renderOnFrame = true;
        }

        int size = size();
        int border = border(size);
        int widget = size + border * 2;
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        int wx = leftAligned(MOD_CONFIG.minimapCorner) ? MARGIN : w - widget - MARGIN;
        int wy = topAligned(MOD_CONFIG.minimapCorner) ? MARGIN : h - widget - MARGIN;

        // Parchment backing: frames the map and fills unexplored (transparent) map
        // pixels.
        g.blit(RenderPipelines.GUI_TEXTURED, MAP_BACKGROUND, wx, wy, 0f, 0f, widget, widget, widget, widget);

        // map() draws a 128x128 map (texture + decorations) at the current pose origin.
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.translate(wx + border, wy + border);
        pose.scale(size / 128f, size / 128f);
        g.map(state);
        pose.popMatrix();
    }

    // Width the minimap reserves along the right screen edge this frame, or 0 when
    // it isn't drawing there. Vanilla's top-right HUD (status effects, toasts) is
    // shifted left by this so the minimap doesn't cover it.
    public static int rightInset() {
        if (MOD_CONFIG.minimapCorner != MinimapCorner.TOP_RIGHT)
            return 0;
        if (activeMapId(Minecraft.getInstance()) == null)
            return 0;
        int size = size();
        return size + border(size) * 2 + MARGIN * 2;
    }

    // The map the minimap draws this frame, or null when it draws nothing.
    private static MapId activeMapId(Minecraft mc) {
        if (!MOD_CONFIG.minimapEnabled || mc.player == null || mc.level == null)
            return null;

        ItemStack atlas = resolveAtlas(mc);
        if (atlas == null)
            return null;

        MapId mapId = atlas.get(DataComponents.MAP_ID);
        // Map data arrives from the server a little after the id does.
        return mapId != null && mc.level.getMapData(mapId) != null ? mapId : null;
    }

    private static int size() {
        return (int) Math.clamp(MOD_CONFIG.minimapSize, 16, 512);
    }

    private static int border(int size) {
        return Math.max(2, Math.round(size / 16f)); // ~8px paper border at size 128
    }

    // "Last atlas held": prefer a hand, else the remembered slot, else any atlas,
    // else none. Also what the atlas grid view (AtlasScreen) opens on.
    static ItemStack resolveAtlas(Minecraft mc) {
        Inventory inv = mc.player.getInventory();

        ItemStack main = mc.player.getItemInHand(InteractionHand.MAIN_HAND);
        if (main.is(ImprovedMapsItems.ATLAS)) {
            trackedSlot = inv.getSelectedSlot();
            return main;
        }
        // Off-hand is always equipped, so an off-hand atlas counts as continuously
        // held.
        ItemStack off = mc.player.getItemInHand(InteractionHand.OFF_HAND);
        if (off.is(ImprovedMapsItems.ATLAS))
            return off;

        if (trackedSlot >= 0 && trackedSlot < inv.getContainerSize()) {
            ItemStack s = inv.getItem(trackedSlot);
            if (s.is(ImprovedMapsItems.ATLAS))
                return s;
        }
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(ImprovedMapsItems.ATLAS)) {
                trackedSlot = i;
                return s;
            }
        }
        trackedSlot = -1;
        return null;
    }

    private static boolean leftAligned(MinimapCorner c) {
        return c == MinimapCorner.TOP_LEFT || c == MinimapCorner.BOTTOM_LEFT;
    }

    private static boolean topAligned(MinimapCorner c) {
        return c == MinimapCorner.TOP_LEFT || c == MinimapCorner.TOP_RIGHT;
    }
}
