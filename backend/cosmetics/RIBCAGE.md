# Ribcage

ID: `ribcage`. Category: **Outfits**. Slot: **Chest**.

Grant Ribcage from Cosmetics in the VPS admin panel after loading a player's
Minecraft UUID. There are no automatic grants or purchase mappings. Updated
clients refresh grants within a minute. A client build containing Ribcage is
required to render and equip it.

The supplied `ribcage.geo.json` and 32 × 32 PNG are copied unchanged to
`assets/axial_cosmetics/geo/ribcage.geo.json` and
`assets/axial_cosmetics/textures/entity/accessories/ribcage.png`.
The model has 2 bones and 42 cubes. The shared Bedrock importer preserves the
authored rotations, fractional geometry, integer box UV spans, 16 mirrored
cubes and 16 cubes with 0.01 inflation. Its root follows the native player body,
including torso rotation and sneaking. Invisible players, spectators and chest
item skins use the existing cosmetic visibility rules.

Ribcage coexists with footwear, arm bands, capes and auras. It occupies only the
chest slot. Wardrobe cards use the supplied mannequin skin and a torso closeup;
the main preview shows the player's complete equipped loadout.

To regenerate the preview, run:

```
python3 tools/generate_white_kicks_preview.py --from-model --cosmetic ribcage
```

Copy `build/previews/ribcage_model.png` to `assets/ribcage/preview.png`, then run
`python3 tools/generate_white_kicks_preview.py --cosmetic ribcage` to copy it to
the client fallback image and VPS thumbnail.

Mesh tests compare every generated cube corner, including inflation and
mirrored cubes, against independent Blockbench-space transforms. Grant, slot
coexistence and revocation tests cover chest equipment. An in-game visual and
pose check remains necessary; the existing development launch has an unrelated
Emotecraft/Bendable Cuboids mixin startup failure.
