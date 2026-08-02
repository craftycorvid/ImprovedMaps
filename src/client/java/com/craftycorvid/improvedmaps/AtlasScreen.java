package com.craftycorvid.improvedmaps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.joml.Matrix3x2fStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import com.craftycorvid.improvedmaps.ImprovedMapsNetworking.AtlasViewRequest;
import com.craftycorvid.improvedmaps.ImprovedMapsNetworking.MapCenter;

// Every map in the tracked atlas, stitched into one big map in its true grid layout.
public final class AtlasScreen extends Screen {
    private static final int MAP_PX = 128;
    private static final int MARGIN = 16;
    // Screen pixels per map pixel: how far in scrolling can go, and how much the view opens showing
    // - the active map plus its neighbours on every side.
    private static final double MAX_SCALE = 4.0;
    private static final int OPENING_CELLS = 3;
    private static final Component LOADING =
            Component.translatable("screen.improved-maps.atlas.loading");
    // A map's centre never reaches the client on its own, so the server sends it. Worth keeping
    // for the session: neither a map's id nor its centre ever changes.
    private static final Map<MapId, MapCenter> CENTERS = new HashMap<>();

    private final List<MapId> ids = new ArrayList<>();
    // The atlas's selected map - the only one whose player marker is kept up to date.
    private MapId activeMapId;
    private double viewScale;
    private double panX;
    private double panY;
    // Grid size in map pixels, from the last frame drawn - zoom and pan need it.
    private int gridWidth;
    private int gridHeight;
    // Cleared on open so the view re-frames on the player once their map has arrived.
    private boolean framed;

    public AtlasScreen() {
        super(Component.translatable("screen.improved-maps.atlas"));
    }

    public static void cacheCenters(List<MapCenter> centers) {
        centers.forEach(center -> CENTERS.put(center.id(), center));
    }

    // Map ids are per-world, so they must not outlive the connection.
    public static void forgetCenters() {
        CENTERS.clear();
    }

    @Override
    protected void init() {
        Minecraft mc = this.minecraft;
        if (mc == null || mc.level == null || mc.player == null)
            return;

        ItemStack atlas = MinimapHud.resolveAtlas(mc);
        if (atlas == null)
            return;

        ids.clear();
        framed = false;
        activeMapId = atlas.get(DataComponents.MAP_ID);
        atlas.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY).itemCopyStream()
                .forEach(map -> {
                    MapId id = map.get(DataComponents.MAP_ID);
                    if (id != null)
                        ids.add(id);
                });

        // The client is only ever sent the atlas's active map, and never any map's centre, so ask
        // for whatever is missing. Both halves arrive per map, so this asks once per map per world.
        // Batched, because the request codec refuses (and the connection dies on) a longer list;
        // an atlas past that size finishes filling in the next time the view is opened.
        List<MapId> missing = ids.stream()
                .filter(id -> !CENTERS.containsKey(id) || mc.level.getMapData(id) == null)
                .limit(ImprovedMapsNetworking.MAX_REQUESTED_MAPS).toList();
        if (!missing.isEmpty() && ClientPlayNetworking.canSend(AtlasViewRequest.TYPE))
            ClientPlayNetworking.send(new AtlasViewRequest(missing));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY,
            float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        Minecraft mc = this.minecraft;
        if (mc == null || mc.level == null)
            return;

        int minCol = Integer.MAX_VALUE;
        int minRow = Integer.MAX_VALUE;
        int maxCol = Integer.MIN_VALUE;
        int maxRow = Integer.MIN_VALUE;
        for (MapId id : ids) {
            MapCenter center = CENTERS.get(id);
            MapItemSavedData data = center == null ? null : mc.level.getMapData(id);
            if (data == null)
                continue;
            minCol = Math.min(minCol, col(center, data));
            maxCol = Math.max(maxCol, col(center, data));
            minRow = Math.min(minRow, row(center, data));
            maxRow = Math.max(maxRow, row(center, data));
        }
        if (minCol > maxCol) { // nothing placeable yet - the reply is still in flight
            g.text(font, LOADING.getVisualOrderText(), (width - font.width(LOADING)) / 2,
                    height / 2, -1, true);
            return;
        }

        gridWidth = (maxCol - minCol + 1) * MAP_PX;
        gridHeight = (maxRow - minRow + 1) * MAP_PX;
        if (!framed) {
            // Show the whole grid until the active map lands, then frame on the player.
            viewScale = fitScale();
            framed = frameOnPlayer(mc, minCol, minRow);
        }
        float pixelScale = (float) viewScale;

        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.translate((float) originX(pixelScale), (float) originY(pixelScale));
        pose.scale(pixelScale, pixelScale);
        for (MapId id : ids) {
            MapCenter center = CENTERS.get(id);
            MapItemSavedData data = center == null ? null : mc.level.getMapData(id);
            if (data == null)
                continue;

            MapRenderState state = new MapRenderState();
            mc.getMapRenderer().extractRenderState(id, data, state);
            // g.map() draws maps item-frame style, which keeps banners and structure markers but
            // hides the player-position family (their type has showOnItemFrame=false). That is what
            // we want everywhere but the active map: tickCarriedBy only refreshes a player's marker
            // on the map being carried, so on every other map it is frozen wherever they last
            // stood. Force the flag on for the active map alone, where the position is live.
            if (id.equals(activeMapId)) {
                for (MapRenderState.MapDecorationRenderState decoration : state.decorations) {
                    decoration.renderOnFrame = true;
                }
            }

            pose.pushMatrix();
            pose.translate((col(center, data) - minCol) * MAP_PX,
                    (row(center, data) - minRow) * MAP_PX);
            g.map(state);
            pose.popMatrix();
        }
        pose.popMatrix();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY == 0 || gridWidth == 0)
            return false;

        double before = viewScale;
        viewScale = Math.clamp(viewScale * (scrollY > 0 ? 1.25 : 0.8), fitScale(), maxScale());
        if (viewScale == before)
            return true;

        // Zoom about the cursor: whatever grid point is under it stays under it.
        double gridX = (mouseX - originX(before)) / before;
        double gridY = (mouseY - originY(before)) / before;
        panX = mouseX - gridX * viewScale - (width - gridWidth * viewScale) / 2;
        panY = mouseY - gridY * viewScale - (height - gridHeight * viewScale) / 2;
        clampPan(viewScale);
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (gridWidth == 0)
            return false;
        panX += dragX;
        panY += dragY;
        clampPan(viewScale);
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (ImprovedMapsClient.OPEN_ATLAS.matches(event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // Grid cell of a map, from the scale carried by its own data: an atlas's ATLAS_SCALE component
    // is missing on atlases that never went through crafting, and guessing 0 there put every map
    // of a scale-1 atlas two cells apart.
    private static int col(MapCenter center, MapItemSavedData data) {
        return Math.floorDiv(center.x() + 64, MAP_PX << data.scale);
    }

    private static int row(MapCenter center, MapItemSavedData data) {
        return Math.floorDiv(center.z() + 64, MAP_PX << data.scale);
    }

    // Opens centred on the player, zoomed so the active map and its neighbours fill the view. Fails
    // while the active map's centre or pixels are still in flight, so the caller retries next frame.
    private boolean frameOnPlayer(Minecraft mc, int minCol, int minRow) {
        MapCenter center = activeMapId == null ? null : CENTERS.get(activeMapId);
        MapItemSavedData data = center == null ? null : mc.level.getMapData(activeMapId);
        if (data == null || mc.player == null)
            return false;

        // Where the player stands in grid pixels: their map's cell, plus their spot inside it.
        int blocksPerPixel = 1 << data.scale;
        double playerX = (col(center, data) - minCol) * MAP_PX
                + (mc.player.getX() - center.x()) / blocksPerPixel + MAP_PX / 2.0;
        double playerY = (row(center, data) - minRow) * MAP_PX
                + (mc.player.getZ() - center.z()) / blocksPerPixel + MAP_PX / 2.0;

        viewScale = Math.clamp(openingScale(), fitScale(), maxScale());
        panX = width / 2.0 - playerX * viewScale - (width - gridWidth * viewScale) / 2;
        panY = height / 2.0 - playerY * viewScale - (height - gridHeight * viewScale) / 2;
        clampPan(viewScale);
        return true;
    }

    // Fitted to the shorter side, so every neighbour of the active map is on screen.
    private double openingScale() {
        return Math.min(width - MARGIN * 2.0, height - MARGIN * 2.0) / (OPENING_CELLS * MAP_PX);
    }

    private double fitScale() {
        return Math.min((width - MARGIN * 2.0) / gridWidth, (height - MARGIN * 2.0) / gridHeight);
    }

    // A tiny atlas can fit on screen larger than MAX_SCALE; never clamp below what already fits.
    private double maxScale() {
        return Math.max(MAX_SCALE, fitScale());
    }

    private double originX(double pixelScale) {
        return (width - gridWidth * pixelScale) / 2 + panX;
    }

    private double originY(double pixelScale) {
        return (height - gridHeight * pixelScale) / 2 + panY;
    }

    // Panning stops at the grid's edges; when the grid fits on screen it stays centred.
    private void clampPan(double pixelScale) {
        panX = clampAxis(panX, gridWidth * pixelScale, width);
        panY = clampAxis(panY, gridHeight * pixelScale, height);
    }

    private static double clampAxis(double pan, double scaledSize, int viewport) {
        double slack = Math.max(0, (scaledSize - viewport) / 2);
        return Math.clamp(pan, -slack, slack);
    }
}
