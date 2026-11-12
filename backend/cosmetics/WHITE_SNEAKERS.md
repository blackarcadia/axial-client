# White Sneakers

ID: `white_sneakers`. Category: **Outfits**. Slot: **Boots**.

Grant White Sneakers through the Cosmetics page in the VPS admin panel. There
are no automatic grants or store mappings. Updated clients refresh grants within
a minute. The new client build is required to render and equip this cosmetic.

White Sneakers is a separate cosmetic from White Kicks. The supplied model and
32 × 32 texture currently match White Kicks, but its assets and grants are
independent. Equipping either shoe cosmetic replaces the other and preserves
accessories in other slots, capes and auras. Both shoes follow the player's
animated legs; card previews use the wardrobe mannequin and footwear closeup.

The source model is packaged at `geo/white_sneakers.geo.json`, and its texture at
`textures/entity/accessories/white_sneakers.png`, under `assets/axial_cosmetics`.
The supplied files are copied unchanged. The shared shoe renderer preserves the
eight cubes, bone hierarchy, pivots, UVs, fractional dimensions and rotations.

To regenerate the model thumbnail:

```
python3 tools/generate_white_kicks_preview.py --from-model --cosmetic white_sneakers
```

Copy `build/previews/white_sneakers_model.png` to
`assets/white_sneakers/preview.png`, then run
`python3 tools/generate_white_kicks_preview.py --cosmetic white_sneakers` to
update the client fallback image and admin thumbnail.

The VPS update adds only the catalogue entry and public thumbnail; it preserves
all existing grants, feedback, polls and configuration. The existing appearance
relay accepts the new accessory ID without a protocol change. An in-game pose
and visual check is still needed before distributing the client build.
