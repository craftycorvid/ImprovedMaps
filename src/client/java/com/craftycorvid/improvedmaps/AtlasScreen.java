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
import net.minecraft.client.renderer.RenderPipelines;
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
    // Screen pixels per map pixel: how far in scrolling can go, and how much the view opens showing
    // - the active map plus its neighbours on every side.
    private static final double MAX_SCALE = 4.0;
    private static final int OPENING_CELLS = 3;

    // The book: a leather cover with a dark spine down the middle and gold corner clasps, with the
    // parchment the minimap uses laid on top as the page. Colours are sampled from vanilla's
    // enchanting table book texture - the one the lectern renders - so it sits beside vanilla GUIs.
    private static final int COVER_MARGIN = 6; // screen edge to cover
    // Width to height of the cover. Fixed like every vanilla screen, so the book keeps its shape
    // instead of stretching into a letterbox on a wide monitor; it grows to fit whichever side runs
    // out first. Slightly wide, the way an open book with a centre spine sits.
    private static final double COVER_ASPECT = 4.0 / 3.0;
    // Cover to parchment: the band of cover left showing. Kept narrow so the page gets the room -
    // the spine and clasps are sized off the cover instead, so slimming this does not shrink them.
    private static final int MIN_PAGE_INSET = 12;
    private static final int PAGE_INSET_FRACTION = 18;
    private static final int PARCHMENT_TEXTURE = 64;
    private static final int PARCHMENT_BORDER = 8; // texels kept unstretched by the nine-slice
    private static final int PARCHMENT_SCALE = 2; // screen pixels per border texel
    private static final int BORDER_PX = PARCHMENT_BORDER * PARCHMENT_SCALE;
    private static final int COVER = 0xFFA76E2D;
    private static final int COVER_SHADOW = 0xFF774E22;
    private static final int COVER_HIGHLIGHT = 0xFFC58439;
    private static final int SPINE_EDGE = 0xFF875622;
    private static final int CLASP = 0xFFFFD800;
    private static final int CLASP_SHADE = 0xFFFFB100;
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
    // The cover, at a fixed aspect and centred on screen.
    private int coverX;
    private int coverY;
    private int coverW;
    private int coverH;
    // Band of cover showing around the parchment; the spine and clasps are sized off it.
    private int pageInset;
    // The flat middle of the parchment: where maps are laid out, framed and clipped.
    private int viewX;
    private int viewY;
    private int viewW;
    private int viewH;
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
        // Laid out before anything can bail, so the book still draws when there is nothing to show.
        int availW = Math.max(MAP_PX, width - COVER_MARGIN * 2);
        int availH = Math.max(MAP_PX, height - COVER_MARGIN * 2);
        coverW = Math.min(availW, (int) (availH * COVER_ASPECT));
        coverH = Math.min(availH, (int) (availW / COVER_ASPECT));
        coverX = (width - coverW) / 2;
        coverY = (height - coverH) / 2;

        pageInset = Math.max(MIN_PAGE_INSET, Math.min(coverW, coverH) / PAGE_INSET_FRACTION);
        int edge = pageInset + BORDER_PX;
        viewX = coverX + edge;
        viewY = coverY + edge;
        viewW = Math.max(MAP_PX, coverW - edge * 2);
        viewH = Math.max(MAP_PX, coverH - edge * 2);

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
        drawBook(g);
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
            g.text(font, LOADING.getVisualOrderText(),
                    viewX + (viewW - font.width(LOADING)) / 2, viewY + viewH / 2, COVER_SHADOW,
                    false);
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

        // Panned or zoomed maps stop at the parchment rather than spilling over the cover.
        g.enableScissor(viewX, viewY, viewX + viewW, viewY + viewH);
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
        g.disableScissor();
    }

    private void drawBook(GuiGraphicsExtractor g) {
        int x0 = coverX;
        int y0 = coverY;
        int x1 = coverX + coverW;
        int y1 = coverY + coverH;

        g.fill(x0, y0, x1, y1, COVER_SHADOW);
        g.fill(x0 + 2, y0 + 2, x1 - 2, y1 - 2, COVER);
        g.fill(x0 + 2, y0 + 2, x1 - 2, y0 + 3, COVER_HIGHLIGHT);
        g.fill(x0 + 2, y0 + 2, x0 + 3, y1 - 2, COVER_HIGHLIGHT);

        int half = Math.max(3, coverH / 40);
        int spine = (x0 + x1) / 2;
        g.fill(spine - half, y0 + 2, spine + half, y1 - 2, COVER_SHADOW);
        g.fill(spine - half - 1, y0 + 2, spine - half, y1 - 2, SPINE_EDGE);
        g.fill(spine + half, y0 + 2, spine + half + 1, y1 - 2, SPINE_EDGE);

        // Clasp arms run along the cover edge, so only their thickness has to fit the inset band.
        int arm = Math.max(8, Math.min(coverW, coverH) / 16);
        int thick = Math.max(3, Math.min(arm / 5, pageInset - 2));
        clasp(g, x0 + 3, y0 + 3, 1, 1, arm, thick);
        clasp(g, x1 - 3, y0 + 3, -1, 1, arm, thick);
        clasp(g, x0 + 3, y1 - 3, 1, -1, arm, thick);
        clasp(g, x1 - 3, y1 - 3, -1, -1, arm, thick);

        parchment(g);
    }

    // One corner clasp: an L reaching along both edges from (x, y), pointing (dx, dy).
    private static void clasp(GuiGraphicsExtractor g, int x, int y, int dx, int dy, int arm,
            int thick) {
        int armX = x + arm * dx;
        int armY = y + arm * dy;
        int innerX = x + thick * dx;
        int innerY = y + thick * dy;
        rect(g, x, y, armX, innerY, CLASP);
        rect(g, x, y, innerX, armY, CLASP);
        // A darker inner edge, so it reads as metal rather than a flat block.
        rect(g, x, innerY - dy, armX, innerY, CLASP_SHADE);
        rect(g, innerX - dx, y, innerX, armY, CLASP_SHADE);
    }

    // map_background.png is a flat interior inside a ragged torn edge, so the middle stretches and
    // the border stays at a fixed scale; stretching the whole texture turns a 3px edge into chunky
    // teeth once the page is most of the screen.
    private void parchment(GuiGraphicsExtractor g) {
        int pageX = viewX - BORDER_PX;
        int pageY = viewY - BORDER_PX;
        int inner = PARCHMENT_TEXTURE - PARCHMENT_BORDER * 2;
        int far = PARCHMENT_TEXTURE - PARCHMENT_BORDER;

        slice(g, 0, 0, PARCHMENT_BORDER, PARCHMENT_BORDER, pageX, pageY, BORDER_PX, BORDER_PX);
        slice(g, far, 0, PARCHMENT_BORDER, PARCHMENT_BORDER, viewX + viewW, pageY, BORDER_PX,
                BORDER_PX);
        slice(g, 0, far, PARCHMENT_BORDER, PARCHMENT_BORDER, pageX, viewY + viewH, BORDER_PX,
                BORDER_PX);
        slice(g, far, far, PARCHMENT_BORDER, PARCHMENT_BORDER, viewX + viewW, viewY + viewH,
                BORDER_PX, BORDER_PX);

        slice(g, PARCHMENT_BORDER, 0, inner, PARCHMENT_BORDER, viewX, pageY, viewW, BORDER_PX);
        slice(g, PARCHMENT_BORDER, far, inner, PARCHMENT_BORDER, viewX, viewY + viewH, viewW,
                BORDER_PX);
        slice(g, 0, PARCHMENT_BORDER, PARCHMENT_BORDER, inner, pageX, viewY, BORDER_PX, viewH);
        slice(g, far, PARCHMENT_BORDER, PARCHMENT_BORDER, inner, viewX + viewW, viewY, BORDER_PX,
                viewH);

        slice(g, PARCHMENT_BORDER, PARCHMENT_BORDER, inner, inner, viewX, viewY, viewW, viewH);
    }

    private static void slice(GuiGraphicsExtractor g, int u, int v, int regionW, int regionH,
            int x, int y, int w, int h) {
        g.blit(RenderPipelines.GUI_TEXTURED, MinimapHud.MAP_BACKGROUND, x, y, u, v, w, h, regionW,
                regionH, PARCHMENT_TEXTURE, PARCHMENT_TEXTURE);
    }

    private static void rect(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int colour) {
        g.fill(Math.min(x1, x2), Math.min(y1, y2), Math.max(x1, x2), Math.max(y1, y2), colour);
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
        panX = mouseX - viewX - gridX * viewScale - (viewW - gridWidth * viewScale) / 2;
        panY = mouseY - viewY - gridY * viewScale - (viewH - gridHeight * viewScale) / 2;
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
        panX = viewW / 2.0 - playerX * viewScale - (viewW - gridWidth * viewScale) / 2;
        panY = viewH / 2.0 - playerY * viewScale - (viewH - gridHeight * viewScale) / 2;
        clampPan(viewScale);
        return true;
    }

    // Fitted to the shorter side, so every neighbour of the active map is on the page.
    private double openingScale() {
        return Math.min(viewW, viewH) / (double) (OPENING_CELLS * MAP_PX);
    }

    private double fitScale() {
        return Math.min(viewW / (double) gridWidth, viewH / (double) gridHeight);
    }

    // A tiny atlas can fit on screen larger than MAX_SCALE; never clamp below what already fits.
    private double maxScale() {
        return Math.max(MAX_SCALE, fitScale());
    }

    private double originX(double pixelScale) {
        return viewX + (viewW - gridWidth * pixelScale) / 2 + panX;
    }

    private double originY(double pixelScale) {
        return viewY + (viewH - gridHeight * pixelScale) / 2 + panY;
    }

    // Panning stops at the grid's edges; when the grid fits on the page it stays centred.
    private void clampPan(double pixelScale) {
        panX = clampAxis(panX, gridWidth * pixelScale, viewW);
        panY = clampAxis(panY, gridHeight * pixelScale, viewH);
    }

    private static double clampAxis(double pan, double scaledSize, int viewport) {
        double slack = Math.max(0, (scaledSize - viewport) / 2);
        return Math.clamp(pan, -slack, slack);
    }
}
