# White Kicks

ID: `white_kicks`. Category: **Outfits**. No automatic grants or store mappings.

Grant through https://api.axialclient.com/admin: load the Minecraft UUID, check
White Kicks, and save access. Updated clients refresh their grants within a
minute. Select White Kicks in Wardrobe → Outfits. The boots slot is
independent of capes, auras and arm accessories, so White Kicks and Swim Bands
can be equipped together. Equipping another boots cosmetic replaces only the shoes.
Appearance sync already supports arbitrary granted accessory IDs, so the relay
does not need a protocol or service update.

The updated supplied `whitekicks.geo.json` is bundled as
`assets/axial_cosmetics/geo/white_kicks.geo.json`. Its original eight cubes,
fractional dimensions, UVs, bone pivots and cube/bone rotations are retained.
The supplied 32 × 32 PNG is copied unchanged. Each shoe follows its matching
native animated leg, including walking, sneaking and other player poses. The
existing 3D armour-skin conflict guard and invisible/spectator exclusions apply.

The user-supplied 500 × 500 image is the wardrobe/admin preview, preserved
unchanged at `assets/white_kicks/preview.png`. Run
`python3 tools/generate_white_kicks_preview.py` to copy it to both destinations.
For a separate geometry-only check, use `--from-model` (Pillow/NumPy); that writes
`build/previews/white_kicks_model.png` without replacing the supplied image.

Tests cover model fidelity, asset dimensions, grant/revocation and wardrobe
selection, animated leg attachment and texture tint. An in-game visual/pose
check is still required before distributing the updated client.
