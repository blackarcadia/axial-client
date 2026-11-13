# Ducking Around

Cosmetic ID: `ducking_around`. PayNow product: `624807205576642560`.
Static cape with matching duck artwork on both large faces and matching side strips.
The separately supplied top and bottom artwork is retained. Source exports are in
`assets/ducking_around`; `python3 tools/GenerateDuckingAroundCape.py` removes the
black export padding and packs nearest-neighbour artwork into the standard
2048 x 1024 vanilla cape atlas. No recolouring or generated animation.

Client support is bundled in the new client build. Ownership is explicitly granted
through the VPS or the existing signed PayNow purchase webhook; no default grants.
The store product must be visible, a one-time purchase, and tagged
`axial-client-cosmetics`. Website model preview assets and the explicit PayNow ID
mapping are included in the website `dist` folder.
