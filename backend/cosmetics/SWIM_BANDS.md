# Swim Bands

ID: `swim_bands`. Category: Outfits. Assign through the cosmetics admin catalogue; no automatic grants or purchase mappings are created. Equip in Wardrobe → Outfits. Bands have an independent equipment slot and synchronize through the existing authenticated appearance relay.

The supplied `geo/swim_bands.geo.json` and 32×32 texture are preserved in the client assets. The native player model renderer attaches the two 6×3×6 cubes to the matching animated arms. This static model uses native arm poses, including walking, sneaking, and emotes, without a competing full-body Geo renderer.

While a registered Prisonscore Geo item skin is worn in any armour slot, client model cosmetics, auras, and capes are hidden. Their equipped selections remain saved and automatically reappear when the item skin is removed. Vanilla armour and unregistered custom items do not suppress cosmetics.

Regenerate the wardrobe and admin thumbnails with `python3 tools/generate_swim_bands_preview.py` (Pillow and NumPy). Multiplayer visual validation requires updated clients and an assigned cosmetic.

Swim Bands reserves the left/right arm slots. It can be worn together with White Kicks (boots), a cape and an aura. Only another accessory that occupies an arm slot replaces the bands.
