# Client Minimap Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers-extended-cc:subagent-driven-development (recommended) or superpowers-extended-cc:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Render the active map of the player's most-recently-held atlas in a configurable screen corner, with all decorations, updating live.

**Architecture:** Pure client-side. The atlas's `MAP_ID` is a vanilla-synced component already on the client, and the active map's texture/decorations are already ticked and synced by the server. A Fabric HUD element resolves the tracked atlas's `MAP_ID`, extracts a `MapRenderState` via vanilla's `MapRenderer`, and draws it with `GuiGraphicsExtractor.map(...)`. No mixins, no networking, no server changes.

**Tech Stack:** Fabric (fabric-rendering-v1 HUD API), Minecraft 26.2 client GUI (`GuiGraphicsExtractor`, `MapRenderer`/`MapRenderState`), Gson config.

**Testing note:** Client rendering is not unit-testable without launching the game. Automated verification is `./gradlew build` (compile + accesswidener). Behavioral verification is manual in-game and is the user's call (server needs EULA). Each task states both.

---

### Task 1: Minimap config fields

**Goal:** Add `minimapEnabled`, `minimapCorner`, and `minimapSize` to the mod config, plus the `MinimapCorner` enum.

**Files:**
- Modify: `src/main/java/com/craftycorvid/improvedmaps/config/ModConfig.java`

**Acceptance Criteria:**
- [ ] `ModConfig` has `boolean minimapEnabled = false`, `MinimapCorner minimapCorner = MinimapCorner.TOP_RIGHT`, `int minimapSize = 128`.
- [ ] `MinimapCorner` enum exists with `TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT`.
- [ ] On load, missing keys are written back with defaults (existing `loadConfig` already re-saves) and the enum round-trips by name through Gson.

**Verify:** `./gradlew build` → BUILD SUCCESSFUL. (Manual: launch client once, confirm `<config>/improved-maps.json` now contains the three keys with defaults.)

**Steps:**

- [ ] **Step 1: Add the enum and fields**

In `ModConfig.java`, add the enum (top-level inside the class is fine) and three fields alongside the existing config values:

```java
public enum MinimapCorner {
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
}

// Config values
public int atlasMapCapacity = 512;
public boolean updateAtlasWhenNotInHand = true;
public boolean minimapEnabled = false;
public MinimapCorner minimapCorner = MinimapCorner.TOP_RIGHT;
public int minimapSize = 128;
```

(Keep the existing two fields; just add the three new ones and the enum. Gson serializes the enum by name and `loadConfig()` already re-saves to backfill new keys.)

- [ ] **Step 2: Build**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/craftycorvid/improvedmaps/config/ModConfig.java
git commit -m "Add minimap config fields"
```

---

### Task 2: Minimap HUD element

**Goal:** A client HUD element that tracks the last-held atlas and renders its active map in the configured corner.

**Files:**
- Create: `src/client/java/com/craftycorvid/improvedmaps/MinimapHud.java`
- Modify: `src/client/java/com/craftycorvid/improvedmaps/ImprovedMapsClient.java`

**Acceptance Criteria:**
- [ ] HUD element registered via `HudElementRegistry.addLast(ImprovedMaps.id("minimap"), MinimapHud::render)`.
- [ ] Renders nothing when `!MOD_CONFIG.minimapEnabled`, when `options.hideGui`, when no atlas is tracked, when the atlas has no `MAP_ID`, or when the client has no `MapItemSavedData` for that id.
- [ ] "Last held" tracking: main-hand atlas → tracked (remember selected hotbar slot); else off-hand atlas; else the remembered slot if still an atlas; else first atlas in inventory; else nothing.
- [ ] Map is drawn at `minimapSize` px (clamped 16–512) with an 8px margin in the configured corner, including decorations.

**Verify:** `./gradlew build` → BUILD SUCCESSFUL. (Manual, user's call: enable in config, hold an atlas → minimap appears with decorations; move → it pans; switch to another item → it persists; change corner/size/enabled → each takes effect.)

**Steps:**

- [ ] **Step 1: Create `MinimapHud.java`**

```java
package com.craftycorvid.improvedmaps;

import org.joml.Matrix3x2fStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.core.component.DataComponents;
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
    // Remembered main-hand hotbar slot of the last-held atlas; -1 = none.
    private static int trackedSlot = -1;

    // HudElement: called every frame during the GUI extract phase.
    public static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        if (!MOD_CONFIG.minimapEnabled) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null || mc.level == null) return;

        ItemStack atlas = resolveAtlas(mc);
        if (atlas == null) return;

        MapId mapId = atlas.get(DataComponents.MAP_ID);
        if (mapId == null) return;

        MapItemSavedData data = mc.level.getMapData(mapId);
        if (data == null) return;

        MapRenderState state = new MapRenderState();
        mc.getMapRenderer().extractRenderState(mapId, data, state);

        int size = (int) Math.clamp(MOD_CONFIG.minimapSize, 16, 512);
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        int x = leftAligned(MOD_CONFIG.minimapCorner) ? MARGIN : w - size - MARGIN;
        int y = topAligned(MOD_CONFIG.minimapCorner) ? MARGIN : h - size - MARGIN;

        // map() draws a 128x128 map (texture + decorations) at the current pose origin.
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(size / 128f, size / 128f);
        g.map(state);
        pose.popMatrix();
    }

    // "Last atlas held": prefer a hand, else the remembered slot, else any atlas, else none.
    private static ItemStack resolveAtlas(Minecraft mc) {
        Inventory inv = mc.player.getInventory();

        ItemStack main = mc.player.getItemInHand(InteractionHand.MAIN_HAND);
        if (main.is(ImprovedMapsItems.ATLAS)) {
            trackedSlot = inv.getSelectedSlot();
            return main;
        }
        // Off-hand is always equipped, so an off-hand atlas counts as continuously held.
        ItemStack off = mc.player.getItemInHand(InteractionHand.OFF_HAND);
        if (off.is(ImprovedMapsItems.ATLAS)) return off;

        if (trackedSlot >= 0 && trackedSlot < inv.getContainerSize()) {
            ItemStack s = inv.getItem(trackedSlot);
            if (s.is(ImprovedMapsItems.ATLAS)) return s;
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
```

- [ ] **Step 2: Register in `ImprovedMapsClient.java`**

Add the HUD registration in `onInitializeClient()` (keep the existing `setClientMetadata` line):

```java
package com.craftycorvid.improvedmaps;

import eu.pb4.polymer.networking.api.client.PolymerClientNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.nbt.IntTag;

public class ImprovedMapsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        PolymerClientNetworking.setClientMetadata(ImprovedMaps.HELLO_PACKET, IntTag.valueOf(1));
        HudElementRegistry.addLast(ImprovedMaps.id("minimap"), MinimapHud::render);
    }
}
```

- [ ] **Step 3: Build**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL. (If `MAP_ID` turns out absent on the client atlas stack — Polymer stripping it for modded clients — the minimap simply never shows; the fallback is a tiny server→client packet carrying the active MapId. Confirm during manual test before adding it.)

- [ ] **Step 4: Commit**

```bash
git add src/client/java/com/craftycorvid/improvedmaps/MinimapHud.java \
        src/client/java/com/craftycorvid/improvedmaps/ImprovedMapsClient.java
git commit -m "Add client-side minimap HUD"
```

---

## Self-Review

- **Spec coverage:** corner/on-off/size config → Task 1; last-held atlas + all decorations + live update + render-in-corner → Task 2. Verification step (confirm `MAP_ID` reaches client, packet fallback) noted in Task 2 Step 3.
- **Placeholders:** none — all code is complete and uses signatures verified against the 26.2 jars.
- **Type consistency:** `MinimapCorner` defined in Task 1, consumed in Task 2; `render(GuiGraphicsExtractor, DeltaTracker)` matches the `HudElement` functional interface; `pose()` returns `Matrix3x2fStack`.
