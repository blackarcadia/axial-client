# White Sneakers

ID: `white_sneakers`. Category: **Outfits**. Slot: **Boots**.

Grant White Sneakers through the Cosmetics page in the VPS admin panel. There
are no automatic grants or store mappings. Updated clients refresh grants within
a minute. The new client build is required to render and equip this cosmetic.

White Sneakers is a separate cosmetic from White Kicks, using the revised
supplied model and 32 × 32 texture. Its assets and grants are independent.
Equipping either shoe cosmetic replaces the other and preserves
accessories in other slots, capes and auras. Both shoes follow the player's
animated legs; card previews use the wardrobe mannequin and footwear closeup.

The source model is packaged at `geo/white_sneakers.geo.json`, and its texture at
`textures/entity/accessories/white_sneakers.png`, under `assets/axial_cosmetics`.
The supplied files are copied unchanged. The shared shoe renderer preserves the
eight cubes, bone hierarchy, pivots, UVs, fractional dimensions and rotations,
including the left leg pivot at [2, 3, 0], its 180° Y rotation and the right
shoe’s authored cube rotations. Both shoes attach beneath the native animated
leg roots while retaining these model transforms. Rotation conversion matches
Blockbench's Bedrock importer: X/Y are negated in editor space, and the conversion
to Java's Y-down space results in positive X/Y/Z radians. Box UV spans use floored
integer dimensions, while per-cube mesh scaling retains the exact fractional
geometry and leaves pivots unchanged. This keeps the tongues and texture layout
consistent with the authored model.

`BedrockCosmeticModelTest` compares every baked shoe corner to an independent Blockbench-space
transform reference and checks the sole's UV spans. The reference follows
[Blockbench's Bedrock parser](https://github.com/JannisX11/blockbench/blob/master/js/formats/bedrock/bedrock.js)
and [cube UV sizing](https://github.com/JannisX11/blockbench/blob/master/js/outliner/types/cube.js).

To regenerate the model thumbnail:

```
python3 tools/generate_white_kicks_preview.py --from-model --cosmetic white_sneakers
```

Copy `build/previews/white_sneakers_model.png` to
`assets/white_sneakers/preview.png`, then run
`python3 tools/generate_white_kicks_preview.py --cosmetic white_sneakers` to
update the client fallback image and admin thumbnail. The preview renderer
uses Minecraft’s box-face UV vertex order and a camera above the shoes, so the
image shows the uppers rather than the undersides.

The VPS update adds only the catalogue entry and public thumbnail; it preserves
all existing grants, feedback, polls and configuration. The existing appearance
relay accepts the new accessory ID without a protocol change. An in-game pose
and visual check is still needed before distributing the client build.
