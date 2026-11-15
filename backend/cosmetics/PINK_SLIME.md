# Pink Slime shoulder pet

ID: `pink_slime`. Category: **Pets**. Independent **Pet** slot.

Assign the cosmetic through the VPS dashboard or create a private redemption code.
No PayNow product is configured yet. Ownership and appearance synchronization use
existing authenticated grants and accessory loadouts. Removing a grant removes the pet.
It can be worn alongside outfits, cape and aura; another pet replaces the pet slot.

The supplied Blockbench geometry is converted without changing cube dimensions,
pivots or face UVs. Its 32 × 8 texture is preserved. The model's local Y coordinates
are raised 24 pixels to float above the shoulder, anchored to the torso rather than
the swinging arm. It follows the torso when sneaking. Invisible and spectator
players do not show the pet. The supplied 3.2-second position, rotation and scale
animation loops in-world and on the wardrobe mannequin cards.

Reimport with:

```
python3 tools/ImportPinkSlime.py /path/to/Axial_Pink_Slime /path/to/pink_slime.png
```

`tools/RenderPinkSlimePreview.py` creates the compact client/VPS thumbnails from
`/private/tmp/pink-slime-mesh.json`, exported from the native baked model by `tools/ExportPinkSlimePreview.java`
(compiled against the development Minecraft runtime classpath).
In-game visual validation is still required for moving/sneaking players and emotes.
