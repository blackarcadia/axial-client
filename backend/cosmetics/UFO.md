# UFO

Cosmetic ID: `ufo`. Name: **UFO**. Category: **Pets**. Slot: **Pet**.
Available through VPS grants and private redemption codes, matching the existing
private pets. No store product is configured.

Imported from `UFO23.bbmodel`: all 53 cubes, 25 bones, supplied 32 × 32 texture,
authored face UVs, cube inflation and all 1,948 linear animation keys. The
4.04167-second animation loops through the existing shoulder pet renderer.
The model is offset (-6, +24, 0) above the shoulder, retaining its authored root
pivot for rotations. Equipping replaces another pet and retains other slots.

Reimport with `python3 tools/ImportUfo.py /path/to/UFO23.bbmodel /path/to/texture.png`.
Compile and run `tools/ExportUfo.java` on the main runtime classpath, passing
`/private/tmp/ufo-mesh.json`, then run `python3 tools/RenderUfoPreview.py` to
refresh the client and VPS thumbnails.

Tests cover native baking, texture dimensions, animation sampling and reset,
server-authoritative ownership and replacing only the pet slot. Live Minecraft
visual testing remains unverified. Players need an updated client to render UFO.

The importer swaps Blockbench east/west UV names for the Bedrock baker’s reflected
X convention. This keeps the grey hull exterior outside and the green interior
inside. A regression check verifies both sides of an unrotated hull panel.
