"""Generate antialiased, tintable mod-menu line icons without runtime dependencies."""
import math
from pathlib import Path
import generate_account_ui as masks


def lines(points, closed=False):
    pairs = [(a,b) for a,b in zip(points, points[1:] + (points[:1] if closed else [])) if a != b]
    return lambda x, y: min(masks.segment(x, y, *a, *b, width=1.15) for a, b in pairs)


def ring(cx, cy, radius):
    return lambda x, y: abs(math.hypot(x-cx, y-cy)-radius)-.55


def combine(*parts):
    return lambda x, y: min(part(x, y) for part in parts)


def box(x, y, w, h):
    return lines([(x,y),(x+w,y),(x+w,y+h),(x,y+h)], True)


def main():
    masks.DEST = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/axial_cosmetics/textures/gui/sprites/mods'
    masks.DEST.mkdir(parents=True, exist_ok=True)
    icons = {
        'mountain': lines([(1,13),(6,3),(9,8),(11,5),(15,13)],True),
        'power': combine(lines([(8,1),(8,8)]),lambda x,y: max(ring(8,9,5.5)(x,y),3-y) if 5<x<11 else ring(8,9,5.5)(x,y)),
        'music': combine(lines([(5,12),(5,3),(13,1),(13,10)]),ring(3,12,2),ring(11,10,2)),
        'speaker': combine(lines([(1,6),(4,6),(8,2),(8,14),(4,10),(1,10)],True),lines([(11,5),(13,8),(11,11)])),
        'monitor': combine(box(1.5,2,13,9),lines([(8,11),(8,14)]),lines([(4.5,14),(11.5,14)])),
        'backpack': combine(box(3,5,10,9),box(6,2,4,3),box(5,9,6,3)),
        'sun': combine(ring(8,8,3),*[lines([(8+math.cos(a)*5.5,8+math.sin(a)*5.5),(8+math.cos(a)*7,8+math.sin(a)*7)]) for a in [i*math.pi/4 for i in range(8)]]),
        'pickaxe': combine(lines([(3,14),(11,4)]),lines([(5,2),(9,2),(13,6),(14,10)])),
        'mouse': combine(lines([(4,5),(4,11),(5,14),(11,14),(12,11),(12,5),(11,2),(5,2),(4,5)],True),lines([(4,6),(12,6)]),lines([(8,2),(8,6)])),
        'shirt': lines([(5,2),(2,3),(1,6),(4,7),(4,14),(12,14),(12,7),(15,6),(14,3),(11,2),(10,4),(6,4)],True),
        'cube': combine(lines([(8,1),(14,4.5),(14,11.5),(8,15),(2,11.5),(2,4.5)],True),lines([(2,4.5),(8,8),(14,4.5)]),lines([(8,8),(8,15)])),
        'chart': combine(box(2,9,3,5),box(6.5,5,3,9),box(11,2,3,12)),
        'globe': combine(ring(8,8,6.5),lines([(1.5,8),(14.5,8)]),lambda x,y: abs(math.hypot((x-8)*2,y-8)-6.5)/2-.5),
        'eye': combine(lines([(1,8),(4,4),(8,2.5),(12,4),(15,8),(12,12),(8,13.5),(4,12)],True),ring(8,8,2.7)),
        'grid': combine(*[box(x,y,4,4) for x in (2,10) for y in (2,10)]),
        'crosshair': combine(ring(8,8,4),lines([(8,1),(8,5)]),lines([(8,11),(8,15)]),lines([(1,8),(5,8)]),lines([(11,8),(15,8)])),
        'compass': combine(ring(8,8,6.5),lines([(11,4),(9,9),(5,12),(7,7)],True)),
        'potion': combine(lines([(6,1),(10,1),(10,3),(9.5,3),(9.5,6),(13,11),(12,14),(4,14),(3,11),(6.5,6),(6.5,3),(6,3)],True),lines([(4,10),(12,10)])),
        'map': combine(lines([(1.5,3),(6,1.5),(10,3),(14.5,1.5),(14.5,13),(10,14.5),(6,13),(1.5,14.5)],True),lines([(6,1.5),(6,13)]),lines([(10,3),(10,14.5)])),
        'scale': combine(box(1.5,7.5,7,7),box(10,1.5,4.5,4.5),lines([(2,5),(2,2),(7,2)]),lines([(11,14),(14,14),(14,9)])),
        'move': combine(lines([(8,1),(8,15)]),lines([(1,8),(15,8)]),lines([(5,4),(8,1),(11,4)]),lines([(5,12),(8,15),(11,12)]),lines([(4,5),(1,8),(4,11)]),lines([(12,5),(15,8),(12,11)])),
        'gear': combine(ring(8,8,2),ring(8,8,5),*[lines([(8+math.cos(a)*5,8+math.sin(a)*5),(8+math.cos(a)*7,8+math.sin(a)*7)]) for a in [i*math.pi/4 for i in range(8)]]),
        'close': combine(lines([(3,3),(13,13)]),lines([(13,3),(3,13)])),
        'search': combine(ring(6.5,6.5,4.5),lines([(10,10),(14,14)])),
        'check': lines([(2,8),(6,12),(14,4)]),
        'account': combine(ring(8,4,2.6),lines([(2,14),(2.5,11),(5,9),(11,9),(13.5,11),(14,14)],True)),
        'pin': combine(lines([(8,15),(3,7),(3,4),(5,1.5),(11,1.5),(13,4),(13,7)],True),ring(8,5,1.7)),
        'store': combine(lines([(1,2),(3,2),(5,10),(13,10),(15,4),(4,4)]),ring(6,13.5,1),ring(12,13.5,1)),
        'forums': combine(lines([(1,2),(11,2),(11,9),(6,9),(3,12),(3,9),(1,9)],True),lines([(13,5),(15,5),(15,12),(13,12),(13,15),(10,12),(7,12)])),
    }
    for name, distance in icons.items():
        masks.png(name,16,distance)


if __name__ == '__main__':
    main()
