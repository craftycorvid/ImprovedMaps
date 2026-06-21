# Client-side Minimap — Design

## Goal

Render the active map of the player's most-recently-held atlas in a screen
corner, with all decorations, updating live. Configurable: on/off, corner, size.

## Context

ImprovedMaps is a dedicated-server Polymer mod with an optional client mod.
Players running the client mod ("players with client") receive the **real**
atlas `ItemStack`, so its vanilla-synced components (`MAP_ID`,
`BUNDLE_CONTENTS`) are present client-side. The atlas has no manual "select"
action — the server lifecycle tick auto-picks the map nearest the player and
stores it in `DataComponents.MAP_ID` each tick (always when in hand; when in
inventory if `updateAtlasWhenNotInHand`). That active map is already ticked and
synced to the client, so its texture and decorations update with no new server
code.

## Approach

**Pure client-side, reusing vanilla's client map renderer.** Read `MAP_ID` off
the tracked atlas and hand it to the same renderer vanilla uses for held maps /
item frames, which draws texture + all decoration types correctly for free.

No new mixins, no networking, no server logic. One config edit + one new client
class + client registration.

Rejected: hand-drawing decoration sprites (reinvents vanilla); a server→client
packet for the active MapId (unnecessary — `MAP_ID` already syncs). The packet
is kept only as a fallback if verification shows Polymer strips `MAP_ID` for
modded clients.

## Components

### 1. Config — `ModConfig.java` (common; read only on the client)

Add fields:
- `boolean minimapEnabled = false`
- `MinimapCorner minimapCorner = TOP_RIGHT` — enum `{ TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }`
- `int minimapSize = 128` — rendered size in px (square)

The enum lives in the config class. Gson serializes it by name. These are
read on the client process from the client's own `improved-maps.json`.

### 2. Minimap — new `src/client/.../MinimapHud.java`

**Atlas tracking ("last atlas held"):** on each client tick
- if main- or off-hand holds an `ImprovedMapsItems.ATLAS`, record its inventory
  slot as the tracked slot;
- otherwise, if the tracked slot still holds an atlas, keep it;
- otherwise fall back to the first atlas found in the inventory;
- if no atlas exists in inventory, clear tracking (minimap hidden).

Read `MAP_ID` fresh from the tracked atlas each frame so the minimap stays
live. In the common single-atlas case this reduces to "show your atlas".

**Render:** on the HUD layer, when
- `MOD_CONFIG.minimapEnabled` is true,
- a tracked atlas with a non-null `MAP_ID` exists, and
- the client has `MapItemSavedData` for that id (`level.getMapData(id) != null`),

draw the map via the vanilla client map renderer into the configured corner at
`minimapSize` px, 8px margin from the edges. Respect `options.hideGui`.

### 3. Registration — `ImprovedMapsClient.java`

Register the client tick handler and the HUD layer using the current 26.2
Fabric client HUD API.

## Behavior & caveats

- "Updates as if out" is free: as the player moves, the server switches
  `MAP_ID` to the nearest tile, so the minimap pans seamlessly.
- If the server has `updateAtlasWhenNotInHand=false`, a stowed atlas's map
  won't update — the minimap shows the last synced state. Consistent with the
  mod's existing behavior.
- Briefly after login, before the atlas first ticks, map data may be absent —
  the renderer's `getMapData != null` check hides the minimap until it arrives.

## Files

- `src/main/.../config/ModConfig.java` — edit (3 fields + enum)
- `src/client/.../MinimapHud.java` — new
- `src/client/.../ImprovedMapsClient.java` — edit (registration)

## Verification

- Build green (`./gradlew build`).
- Confirm the client-side atlas stack carries `MAP_ID`; if Polymer strips it
  for modded clients, add the minimal server→client packet fallback.
- In-game (user's call, needs EULA): hold an atlas → minimap appears in the
  configured corner with decorations; move → it pans; switch items → it
  persists; each config option (enabled/corner/size) takes effect.
