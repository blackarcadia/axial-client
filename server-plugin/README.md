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
- Paper loads chunks asynchronously. Snapshots are captured on the main thread, then colour sampling and compressed cache I/O run on a worker. Live world and player operations stay on the main thread.
- Defaults allow one new chunk request per tick and at most two outstanding requests/render operations globally. New work backs off below 18.5 TPS. Initial completion time depends on border size and whether terrain already exists.
- Complete caches survive restarts under `plugins/AxialWorldMap/maps/`. Files are compressed, validated against the current world UUID and border geometry, and atomically replaced.
- Normal block-place/break events refresh affected sampled chunks after a short debounce. A periodic refresh becomes eligible on client requests after `refresh-minutes`. For bulk edits, mine resets, explosions, or changes made without these events, run `/axialmap render world_name` to refresh immediately.
- Transfers are spread over ticks, each packet stays below 29 KB, and unchanged map stripes are not resent. Logout, respawn and world changes reset the per-player transfer state.

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
