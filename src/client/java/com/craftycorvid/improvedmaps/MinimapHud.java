package com.craftycorvid.improvedmaps;

import java.util.List;
import org.joml.Matrix3x2fStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
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
    // Between the minimap and the readout beside it.
    private static final int READOUT_GAP = 2;
    // Drawn with a shadow: the readout sits on the world, not on the parchment.
    private static final int READOUT_COLOUR = 0xFFFFFFFF;
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

        ClientLevel level = mc.level;
        MapItemSavedData data = level == null ? null : level.getMapData(mapId);
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
        int wx = leftAligned(MOD_CONFIG.client_minimapCorner) ? MARGIN : w - widget - MARGIN;
        int wy = topAligned(MOD_CONFIG.client_minimapCorner) ? MARGIN : h - widget - MARGIN;

        // Parchment backing: frames the map and fills unexplored (transparent) map
        // pixels.
        g.blit(RenderPipelines.GUI_TEXTURED, MAP_BACKGROUND, wx, wy, 0f, 0f, widget, widget, widget, widget);

        // map() draws a 128x128 map (texture + decorations) at the current pose origin.
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.translate(wx + border, wy + border);
        pose.scale(mapScale(size), mapScale(size));
        g.map(state);
        pose.popMatrix();

        drawReadout(g, mc, wx, wy, widget, mapScale(size));
    }

    // Position and biome, centred on the widget. Below it in the top corners, above it in the
    // bottom two - "below" there would put the text behind the hotbar and experience bar.
    private static void drawReadout(GuiGraphicsExtractor g, Minecraft mc, int wx, int wy,
            int widget, float scale) {
        List<Component> lines = readoutLines(mc);
        if (lines.isEmpty())
            return;

        Font font = mc.font;
        int block = (int) Math.ceil(lines.size() * font.lineHeight * scale);
        int y = topAligned(MOD_CONFIG.client_minimapCorner) ? wy + widget + READOUT_GAP
                : wy - READOUT_GAP - block;

        // Drawn at the map's own pixel scale, so the readout keeps its proportions whatever the
        // minimap is sized to. Centred by translating to the middle of the widget first and then
        // halving each line about it: font.width is in unscaled units, like the offsets below it.
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.translate(wx + widget / 2f, y);
        pose.scale(scale, scale);
        int lineY = 0;
        for (Component line : lines) {
            g.text(font, line, -font.width(line) / 2, lineY, READOUT_COLOUR, true);
            lineY += font.lineHeight;
        }
        pose.popMatrix();
    }

    // The readout's lines, or empty when it isn't drawing. rightInset needs their width too, so
    // this is the one place they are built.
    private static List<Component> readoutLines(Minecraft mc) {
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (!MOD_CONFIG.client_minimapCoordinates || player == null || level == null)
            return List.of();

        Component position = Component.literal(
                player.getBlockX() + " " + player.getBlockY() + " " + player.getBlockZ());
        // Biomes are a registry of data-driven entries, so a datapack one may have no lang key and
        // an unregistered one no key at all; drop the line rather than print a raw identifier.
        return level.getBiome(player.blockPosition()).unwrapKey()
                .map(key -> List.of(position, Component
                        .translatable(Util.makeDescriptionId("biome", key.identifier()))))
                .orElse(List.of(position));
    }

    // Width the minimap reserves along the right screen edge this frame, or 0 when
    // it isn't drawing there. Vanilla's top-right HUD (status effects, toasts) is
    // shifted left by this so the minimap doesn't cover it.
    public static int rightInset() {
        Minecraft mc = Minecraft.getInstance();
        if (MOD_CONFIG.client_minimapCorner != MinimapCorner.TOP_RIGHT)
            return 0;
        if (activeMapId(mc) == null)
            return 0;

        int size = size();
        int widest = size + border(size) * 2;
        // The readout hangs below the widget, which is where effect icons stack, so a line wider
        // than the minimap has to push them too. Measured at the scale it is drawn at.
        for (Component line : readoutLines(mc)) {
            widest = Math.max(widest, (int) Math.ceil(mc.font.width(line) * mapScale(size)));
        }
        return widest + MARGIN * 2;
    }

    // The map the minimap draws this frame, or null when it draws nothing.
    private static MapId activeMapId(Minecraft mc) {
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (!MOD_CONFIG.client_minimapEnabled || player == null || level == null)
            return null;

        ItemStack atlas = resolveAtlas(player);
        if (atlas == null)
            return null;

        MapId mapId = atlas.get(DataComponents.MAP_ID);
        // Map data arrives from the server a little after the id does.
        return mapId != null && level.getMapData(mapId) != null ? mapId : null;
    }

    private static int size() {
        return (int) Math.clamp(MOD_CONFIG.client_minimapSize, 16, 512);
    }

    private static int border(int size) {
        return Math.max(2, Math.round(size / 16f)); // ~8px paper border at size 128
    }

    // Screen pixels per map pixel. The readout is drawn at this too, so text and map keep the same
    // proportions at any minimap size - at the default 96 that is 0.75, a quarter smaller than the
    // font's own size.
    private static float mapScale(int size) {
        return size / 128f;
    }

    // "Last atlas held": prefer a hand, else the remembered slot, else any atlas,
    // else none. Also what the atlas grid view (AtlasScreen) opens on.
    static ItemStack resolveAtlas(LocalPlayer player) {
        Inventory inv = player.getInventory();

        ItemStack main = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (main.is(ImprovedMapsItems.ATLAS)) {
            trackedSlot = inv.getSelectedSlot();
            return main;
        }
        // Off-hand is always equipped, so an off-hand atlas counts as continuously
        // held.
        ItemStack off = player.getItemInHand(InteractionHand.OFF_HAND);
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
