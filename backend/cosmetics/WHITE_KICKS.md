# White Kicks

ID: `white_kicks`. Category: **Outfits**. No automatic grants or store mappings.

Grant through https://api.axialclient.com/admin: load the Minecraft UUID, check
White Kicks, and save access. Updated clients refresh their grants within a
minute. Select White Kicks in Wardrobe → Outfits. The existing outfit slot is
independent of capes and auras; selecting shoes replaces Swim Bands in that slot.
Appearance sync already supports arbitrary granted accessory IDs, so the relay
does not need a protocol or service update.

The supplied `whitesneakers.geo.json` is bundled as
`assets/axial_cosmetics/geo/white_kicks.geo.json`. Its original eight cubes,
fractional dimensions, UVs, bone pivots and cube/bone rotations are retained.
The supplied 32 × 32 PNG is copied unchanged. Each shoe follows its matching
native animated leg, including walking, sneaking and other player poses. The
existing 3D armour-skin conflict guard and invisible/spectator exclusions apply.

Regenerate the 128 × 128 wardrobe/admin thumbnail with
`python3 tools/generate_white_kicks_preview.py` (Pillow and NumPy). The preview
uses the same bone/cube transformations and supplied texture as the renderer.

Tests cover model fidelity, asset dimensions, grant/revocation and wardrobe
selection, animated leg attachment and texture tint. An in-game visual/pose
check is still required before distributing the updated client.
