# Black Sneakers

Cosmetic ID: `black_sneakers`. PayNow product: `625139324073017344`.
Boot-slot outfit with the supplied 32 x 32 texture and original Bedrock geometry.
The renderer attaches each authored leg hierarchy to the corresponding player leg.
Selecting these shoes replaces other boot cosmetics and retains chest/arm items.

This model mixes box UVs and explicitly authored per-face UVs, including negative
UV sizes on top and bottom faces. The importer preserves those mappings, fractional
cube sizes, inflation and native rotations. Existing box-UV models are unchanged.
The store exports the actual baked client mesh and uses the shared mannequin skin.
`tools/RenderBlackSneakersPreview.py` renders the exported shoe mesh for the private
VPS catalogue thumbnail and compact wardrobe image; the texture is not recoloured.

The VPS catalogue and PayNow product mapping are installed. Existing code-generation
controls can generate private unlock codes for this cosmetic. Website model assets,
manifest and explicit product ID mapping are included in the website `dist` folder.
Players need the updated client to equip the new item.
