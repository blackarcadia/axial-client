# Pookles

Cosmetic ID: `pookles`. Category: **Pets**. Slot: **Pet**.
Private distribution through VPS grants or private redemption codes. No PayNow
product or store listing is configured.

The supplied `Axial_Lime_Plush (3)/lime_plush.bbmodel` contributes all 39 cubes,
the complete bone hierarchy, per-face UVs and four-second idle animation. The
supplied 96 × 8 texture is preserved. The imported animation retains all sampled
body position/rotation/scale, head/hand/ear rotations, blinking eyes and mouth
scale tracks. The runtime uses its standard `animation.pookles.float` loop name.

The existing (-4, +20, 0) shoulder offset places the plush above the shoulder.
It follows the torso, hides for invisible/spectator players, and uses the
Sodium-compatible authored-face renderer. Equip in Wardrobe → Pets. It replaces
another pet while retaining outfits, cape and aura. Ownership is server-authoritative;
revoking the VPS grant removes the cosmetic from the active loadout.

Reimport:

```
python3 tools/ImportPookles.py /path/to/lime_plush.bbmodel /path/to/texture.png
```

Compile `tools/ExportPookles.java` against the development runtime classpath and
pass `/private/tmp/pookles-mesh.json` as the first argument to export native meshes.
`python3 tools/RenderPooklesPreview.py` generates client and VPS thumbnails.
This export is for client/VPS previews only; do not copy it to the store.

Checks cover source geometry, face UVs, texture dimensions, all idle tracks,
loop/reset behavior, ownership and replacing only the pet slot. Live Minecraft
visual testing remains necessary for final moving/sneaking and emote poses.
