# AxialWorldMap

A standalone **Paper 1.21.11 / Java 21** server plugin that sends AxialClient a cached surface overview of the area inside each world's border. Players do not need to visit the terrain first.

## Install

1. Build with `./gradlew :server-plugin:build` from the client repository, or `../gradlew build` from this folder.
2. Copy `server-plugin/build/libs/AxialWorldMap-1.0.0.jar` into the Paper server's `plugins/` directory and restart the server.
3. Use the updated AxialClient from this change. Older client builds do not register the new map channel.
4. Ensure the world has the intended border. The plugin deliberately skips borders larger than `max-border-diameter` (16,384 blocks by default), including Minecraft's huge default border.
5. Check `/axialmap status`. Eligible loaded worlds begin preparing their full caches at startup. Wait for `Full map cached for <world>` in the server log; the first build is not instantaneous. To explicitly render a world, use `/axialmap render world_name`.
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
- Loaded asynchronous chunks receive a temporary plugin ticket immediately, keeping them loaded until the time-budgeted surface sample finishes. Tickets are released after sampling, cancellation, failure, world unload or shutdown.
- Unfinished asynchronous requests time out after `chunk-load-timeout-seconds` (60 seconds by default). The plugin retries at most three times, ignores stale completions, and then saves initial scan progress and reports a failure instead of waiting indefinitely. `/axialmap status` shows pending coordinates, age and attempt count. Run `/axialmap render world_name` to retry a stopped render; its checkpoint is retained. Client status distinguishes final pending chunks, saving the cache and TPS backoff.
- Defaults allow one new unloaded-chunk request per tick and at most two outstanding requests globally. Already-loaded terrain has a separate fast lane of up to 64 chunks per tick, sharing a 2 ms budget with completed loads. The budget is checked between chunks; one chunk may run past it. New work backs off below 18.5 TPS. Existing configurations inherit these fast-lane defaults without needing new keys added manually.
- Complete caches survive restarts under `plugins/AxialWorldMap/maps/`. Files are compressed, validated against the current world UUID and border geometry, and atomically replaced.
- Startup (`auto-render: true`, `refresh-on-startup: true`) queues every eligible loaded world independently of player joins. Worlds loaded later are prepared too. A previous complete cache is immediately available while its replacement builds. Set `refresh-on-startup: false` to reuse recent completed maps without a boot-time rescan.
- Work-in-progress snapshots use separate `.checkpoint.gz` files every 30 seconds and on clean shutdown/unload. They never replace the published `.map.gz` cache. Initial progress resumes on restart, including sampled void terrain and unfinished caches from the previous plugin. A replacement becomes available only after the entire scan completes and its compressed cache is saved successfully.
- Render jobs rotate between worlds, with three out of four scheduling ticks preferring worlds containing players. Already-loaded chunks are prioritised, followed by chunks nearest a player (or spawn). All worlds retain their configured final resolution.
- A previously uncached or ungenerated large world still needs its initial terrain scan. The fast lane accelerates loaded terrain; unloading/generating hundreds of thousands of chunks cannot be instantaneous. Cached images remain visible throughout subsequent scans.
- Block place/break, explosion and piston events refresh affected chunks after a short debounce. Periodic refreshes are checked automatically once per minute against `refresh-minutes`. Bulk edits that do not fire these events are picked up by startup/periodic scans, or `/axialmap render world_name`. Joining or changing worlds does not initiate a terrain scan.
- Clients subscribe while the HUD, fullscreen map or a map preview is visible. Hiding the map unsubscribes, cancels queued transfers and stops polling; server caches continue updating independently. Joins do not push map data to unsubscribed players. World changes and respawns proactively send the cache to existing subscribers after one tick. Initial transfers can send up to 12 packets per tick globally (a 512² map takes about four ticks of slots for one player); background snapshot transfers retain `messages-per-tick`. Actual latency also depends on the network and other transfers. Every packet remains below 29 KB.
- Every join/world change requests a complete copy of the latest published cache. The old arrival-rescan settings (`refresh-on-world-change` and `world-change-refresh-cooldown-seconds`) are no longer used. Cache generation and refresh happen on startup, world load, block changes and the independent maintenance timer.
- Updated clients assemble revision-tagged snapshots separately and display them only after all rows arrive. A background replacement swaps in as one complete image, and late packets from older revisions cannot overwrite it. World transitions clear pending transfers and visible terrain. The new transfer never merges a revisited world's old client image with a partially received update. Install the updated client and plugin together.
- The complete server snapshot supplies the world overview. Detailed local terrain still takes priority when zoomed in; it starts fresh on each world transition and is isolated by the plugin's actual world UUID so different Paper worlds sharing a dimension cannot reuse each other's terrain.
- Complete opaque server overviews are uploaded to one reusable client texture. Fullscreen panning/zooming transforms that texture instead of repainting a 1024² image; nearby zoom and maps containing unknown pixels retain the detailed composition path. HUD relief, water shading and fullscreen detail resolution are retained.
- Client exploration files retain their existing format. Their initial read now runs on the save worker through a streaming parser, and periodic saves run at most once a minute (plus disconnect/shutdown flushes).
- The full-map screen displays server render/transfer progress and explains excluded worlds or oversized borders. If only the PrisonsCore fallback appears and **Whole World** is disabled, check that AxialWorldMap is installed and enabled, then run `/axialmap status`. An incomplete transfer with no progress for 30 seconds is requested again in full.

## Existing PrisonsCore support

No PrisonsCore modifications are required. This plugin uses the separate `axial_cosmetics:world_map` channel; PrisonsCore's `axial_cosmetics:minimap_map` channel remains available for zones and points of interest. AxialClient prioritises the full-world terrain overview, so a legacy loaded-chunks snapshot cannot replace it.

PrisonsCore's updated overlay sender also includes armour-region boundaries from its existing Armour Zone service: Chain `#555555`, Diamond `#80D8FF`, Gold `#FFD54F`, Iron `#FFFFFF`. Use the matching PrisonsCore build to supply those regions. The updated client draws their outlines on the corner minimap as well as the fullscreen map.

This does not disable or reconfigure PrisonsCore's own overview service.

## Validation

`./gradlew :server-plugin:build` tests border coverage, negative chunk coordinates, disk-cache round trips, corruption handling, message sizes, checkpoint isolation, atomic cache promotion and snapshot transfers.

`./gradlew :test --tests com.axial.worldmap.WorldMapClientCompatibilityTest` passes packets from the actual plugin encoder through the actual client decoder. This also checks priority over PrisonsCore snapshots and clearing cached terrain on world changes.

The build produces an installable plugin JAR. It does not install the plugin onto a running server, alter your live world border, or deploy an updated client.

## API references

- [Paper plugin messaging](https://docs.papermc.io/paper/dev/plugin-messaging/)
- [Paper 1.21.11 asynchronous chunk loading](https://jd.papermc.io/paper/1.21.11/org/bukkit/World.html)
- [Thread-safe chunk snapshots](https://jd.papermc.io/paper/1.21.11/org/bukkit/ChunkSnapshot.html)
