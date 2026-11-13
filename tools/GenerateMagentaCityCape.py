"""Pack supplied artwork into vanilla cape UVs; remove export padding from edge strips."""
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'assets/magenta_city'
UNIT = 32
# Crop only the black canvas padding around the supplied edge artwork.
FACES = {
    'face': ((0, 0, 992, 1585), (10, 16)),
    'side': ((419, 0, 519, 1586), (1, 16)),
    'top': ((0, 0, 2172, 223), (10, 1)),
    'bottom': ((0, 525, 2172, 724), (10, 1)),
}
atlas = Image.new('RGBA', (64 * UNIT, 32 * UNIT))
packed = {}
for name, (crop, size) in FACES.items():
    original = Image.open(SOURCE / (name + '.png')).convert('RGBA')
    face = original.crop(crop).resize(tuple(v * UNIT for v in size), Image.Resampling.NEAREST)
    packed[name] = face
for name, u, v in [('face', 1, 1), ('face', 12, 1), ('side', 0, 1), ('side', 11, 1), ('top', 1, 0), ('bottom', 11, 0)]:
    atlas.paste(packed[name], (u * UNIT, v * UNIT))
texture = ROOT / 'src/main/resources/assets/axial_cosmetics/textures/entity/magenta_city_cape.png'
atlas.save(texture, optimize=True)
packed['face'].save(ROOT / 'backend/cosmetics/public/assets/magenta_city.png', optimize=True)
print('Packed Magenta City: 2048 x 1024 atlas, matching front/back and sides.')
