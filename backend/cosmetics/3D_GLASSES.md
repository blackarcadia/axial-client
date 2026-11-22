# 3D Glasses

Cosmetic ID: `3d_glasses`. PayNow product: `628318350954471424`.
Category: Headwear. Slot: Head. Equipping replaces other headwear.

Updated from `3dglassesmodel2.geo.json` and its supplied texture.

The supplied model preserves all 64 cubes and its 32×32 texture. The shared
headwear renderer follows the animated player head and respects ownership,
invisibility, spectator and worn item-skin conflicts. Wardrobe cards use the
headwear closeup. Authored face UVs retain the existing Sodium compatibility.

The VPS catalog and product mapping are deployed; synthetic signed purchase and
refund checks pass. PayNow returns this product as visible, tagged
`axial-client-cosmetics` and enabled for one-time purchase.

The neighbouring `axialclientwebsite/dist` includes its Headwear card mapping,
thumbnail, texture and native 3D preview mesh. Upload dist contents to publish
website changes. Clients need the updated build to display the cosmetic.
Live Minecraft visual testing and a real checkout remain untested.

Export with `tools/Export3DGlasses.java` on the main runtime classpath, writing
`/private/tmp/3d_glasses.json`, then run `python3 tools/Render3DGlassesPreview.py`.
