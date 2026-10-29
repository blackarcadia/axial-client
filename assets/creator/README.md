# Creator cape

The cape has six independently authored faces:

- `front.png`: the matching black-and-gold diamond/star design, facing the player.
- `back.png`: the owner's supplied black-and-gold skull design, preserved as received, facing outward.
- `left.png` and `right.png`: gold OWNER block lettering arranged vertically over almost the full 512-pixel edge length.
- `top.png` and `bottom.png`: black fabric with gold borders and repeating square details.

`cape-panel.png` is the front resampled to the in-game face size (320 × 512).
The original purple `original-logo.png` is retained as a previous source asset.

Run `java -Djava.awt.headless=true tools/GenerateCreatorCape.java` from the project
root to rebuild the side/end panels and sixteen gold shimmer frames under
`src/main/resources/assets/axial_cosmetics/textures/entity/creator_cape*.png`.
The main faces are 320 × 512, side faces 32 × 512, and end faces 320 × 32.
The client selects one frame every five ticks for a four-second loop.
Only gold artwork catches the light; charcoal fabric stays dark.
Normal vanilla cape movement and visibility still apply. The sigil is removed.

UV coordinates in vanilla 64 × 32 units (each unit is 32 pixels):
front/inward `(12,1,10,16)`, back/outward `(1,1,10,16)`,
sides `(0,1,1,16)` and `(11,1,1,16)`,
top `(1,0,10,1)`, bottom `(11,0,10,1)`.

`cape-preview.png` is an earlier mockup; its FRONT/BACK labels predate the face swap.

## Diamond panel generation prompt

Create only the reverse/back flat texture panel for the Minecraft cape whose
FRONT is the supplied reference. Portrait rectangle aspect ratio 5:8. Match the
reference exactly in palette and pixel cluster style: black and very dark charcoal
angular woven faceted background, warm metallic gold stepped diamond outlines at
upper and lower corners, scattered tiny square gold sparks and short vertical
gold segments. Keep it elegant and complementary. In the center use a smaller gold
angular diamond medallion with a tiny four-point gold star inside, rather than
repeating the skull. NO skull, NO text, NO letters, NO white, NO purple. Flat 2D
production texture, no folds, no perspective, no model or mockup, image fills the
entire rectangle edge to edge. Crisp pixel clusters matching the reference. Keep
all central ornamentation within the middle 70 percent width. This panel will be
used for the reverse side of the same cape.
