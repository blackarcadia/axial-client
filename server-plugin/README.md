# AxialWorldMap

A standalone **Paper 1.21.11 / Java 21** server plugin that sends AxialClient a cached surface overview of the area inside each world's border. Players do not need to visit the terrain first.

## Install

1. Build with `./gradlew :server-plugin:build` from the client repository, or `../gradlew build` from this folder.
2. Copy `server-plugin/build/libs/AxialWorldMap-1.0.0.jar` into the Paper server's `plugins/` directory and restart the server.
3. Use the updated AxialClient from this change. Older client builds do not register the new map channel.
4. Ensure the world has the intended border. The plugin deliberately skips borders larger than `max-border-diameter` (16,384 blocks by default), including Minecraft's huge default border.
5. Check `/axialmap status`. Eligible loaded worlds render automatically. To explicitly render a world, use `/axialmap render world_name`.
6. Open the client's fullscreen map. The first overview automatically fits the world; **Whole World** restores that view after panning or zooming.

The generated server configuration is `plugins/AxialWorldMap/config.yml`. Restrict `worlds` to specific world names if desired. Change `max-border-diameter` to allow a larger intended border, then `/axialmap reload` and `/axialmap render world_name`.

## Commands

All management commands require OP or `axialworldmap.admin`.

| Command | Effect |
| --- | --- |
| `/axialmap status` | Show cached worlds and per-world chunk progress. |
| `/axialmap render [world]` | Queue a full rebuild; players can omit their current world. Console must name it. |
| `/axialmap pause` | Stop starting new chunk requests. In-flight work can finish. |
| `/axialmap resume` | Resume rendering. |
| `/axialmap reload` | Reload configuration without discarding existing caches. |

Players require `axialworldmap.view`, granted by default, to receive terrain.

## Rendering and performance

- The overview covers the border at up to **512 × 512 pixels**. A larger world uses more blocks per pixel. This is an overview, not a high-resolution tile pyramid; zooming into distant areas cannot reveal detail finer than this raster. The HUD minimap continues to prefer detailed, locally observed terrain.
- Only chunks containing overview sample points are requested. With a coarse overview, this does **not** pre-generate every chunk inside the border.
- `generate-missing-chunks: true` allows terrain never generated before to appear. This creates real chunks on the server. Set it to `false` to restrict rendering to existing terrain; unavailable samples remain unexplored and can fall back to the client's local terrain.
- Unloaded chunks are requested asynchronously. Loaded chunks are sampled directly from their required surface columns on the main thread, avoiding whole-chunk snapshot copies. Planning and compressed cache I/O run on a worker.
- Defaults allow one new unloaded-chunk request per tick and at most two outstanding requests globally. Already-loaded terrain has a separate fast lane of up to 64 chunks per tick, sharing a 2 ms budget with completed loads. The budget is checked between chunks; one chunk may run past it. New work backs off below 18.5 TPS. Existing configurations inherit these fast-lane defaults without needing new keys added manually.
- Complete caches survive restarts under `plugins/AxialWorldMap/maps/`. Files are compressed, validated against the current world UUID and border geometry, and atomically replaced.
- Initial renders publish their first chunk immediately and updated map stripes every two seconds. Unfinished maps checkpoint every 30 seconds and on clean shutdown; completed sample coverage is restored on restart, including sampled void terrain. Existing complete caches from the previous format remain readable. The old plugin did not checkpoint unfinished jobs, so its in-memory progress cannot be recovered after replacing it.
- Render jobs rotate between worlds, with three out of four scheduling ticks preferring worlds containing players. Already-loaded chunks are prioritised, followed by chunks nearest a player (or spawn). All worlds retain their configured final resolution.
- A previously uncached or ungenerated large world still needs its initial terrain scan. The fast lane accelerates loaded terrain; unloading/generating hundreds of thousands of chunks cannot be instantaneous. Cached images remain visible throughout subsequent scans.
- Block place/break, explosion and piston events refresh affected chunks after a short debounce. Periodic refreshes are checked automatically once per minute against `refresh-minutes`. Bulk edits that do not fire these events are picked up by the automatic full rescan on world arrival, or `/axialmap render world_name`.
- Joins, world changes and respawns proactively send the current cached map after one tick. Initial transfers can send up to 12 packets per tick globally (a 512² map takes about four ticks of slots for one player); ordinary updates retain `messages-per-tick`. Actual latency also depends on the network and other transfers. Every packet remains below 29 KB.
- With `refresh-on-world-change: true`, arrivals also request a background full rescan while retaining the cached colours. Worlds already rendering continue their current job. Refresh requests within `world-change-refresh-cooldown-seconds` (30 seconds) are coalesced, including shortly after a scan finishes, so rapid hops cannot continually restart work.
- Updated clients retain an LRU of eight world images in memory, isolated by server, dimension, world UUID and geometry. On a repeat visit the first stripe identifies the target world and restores its whole cached image, then incoming stripes refresh it. Unmatched worlds and changed borders do not reuse another world's data. Install the updated client and plugin together.
- The full-map screen displays server render/transfer progress and explains excluded worlds or oversized borders. If only the PrisonsCore fallback appears and **Whole World** is disabled, check that AxialWorldMap is installed and enabled, then run `/axialmap status`. An incomplete transfer with no progress for 30 seconds is requested again in full.

## Existing PrisonsCore support

No PrisonsCore modifications are required. This plugin uses the separate `axial_cosmetics:world_map` channel; PrisonsCore's `axial_cosmetics:minimap_map` channel remains available for zones and points of interest. AxialClient prioritises the full-world terrain overview, so a legacy loaded-chunks snapshot cannot replace it.

This does not disable or reconfigure PrisonsCore's own overview service.

## Validation

`./gradlew :server-plugin:build` tests border coverage, negative chunk coordinates, disk-cache round trips, corruption handling, message sizes, and incremental stripe transfers.

`./gradlew :test --tests com.axial.worldmap.WorldMapClientCompatibilityTest` passes packets from the actual plugin encoder through the actual client decoder. This also checks priority over PrisonsCore snapshots and clearing cached terrain on world changes.

The build produces an installable plugin JAR. It does not install the plugin onto a running server, alter your live world border, or deploy an updated client.

## API references

- [Paper plugin messaging](https://docs.papermc.io/paper/dev/plugin-messaging/)
- [Paper 1.21.11 asynchronous chunk loading](https://jd.papermc.io/paper/1.21.11/org/bukkit/World.html)
- [Thread-safe chunk snapshots](https://jd.papermc.io/paper/1.21.11/org/bukkit/ChunkSnapshot.html)
