# Ghostly Ghost

Cosmetic ID: `ghostly_ghost`. Category: **Pets**. Slot: **Pet**.
PayNow product: `625822431184945152`.

The 31 supplied Blockbench cubes, hierarchy and 64 × 8 texture are preserved.
An offset of (-4, +20, 0) model pixels places the ghost above the shoulder,
with its chain and iron ball hanging beside the player. The supplied embedded
four-second animation drives the ghost's floating motion, blinking eyes,
chain and weight. The pet follows the animated torso and hides for invisible
and spectator players. Equipping it replaces another pet while retaining outfits,
cape and aura. Ownership is server-authoritative and supports private unlock codes.

Reimport:

```
python3 tools/ImportGhostlyGhost.py /path/to/cube_ghost.bbmodel /path/to/texture.png
```

`tools/ExportGhostlyGhost.java`, compiled against the development runtime,
exports 32 native animation frames to a JSON path passed as its first argument.
`tools/RenderGhostlyGhostPreview.py` reads `/private/tmp/ghostly-ghost-mesh.json`
and writes the client and VPS thumbnails.

The website uses the same exported geometry on the wardrobe mannequin, under
Pets. It maps the explicit product ID so renamed PayNow products keep the preview.
The PayNow product must use tag slug `axial-client-cosmetics` and be visible.
Upload the website's complete `dist` contents to publish it. Existing order,
refund and chargeback handlers grant or revoke this product normally.

Client checks cover UVs, animation sampling, model clearance and pet replacement.
Backend checks use synthetic signed events and an in-memory database. A live
Minecraft visual check and actual PayNow checkout remain separate validation.

## Per-face UV rendering compatibility

Imported face-mapped cubes render their authored quads directly. Sodium snapshots
box UV coordinates in its base cuboid constructor, so changing the standard
quad UVs alone leaves the optimized renderer sampling the wrong texture regions.
This client-side compatibility path preserves fractional cube scales and leaves
ordinary box-UV cubes on their existing rendering path.
