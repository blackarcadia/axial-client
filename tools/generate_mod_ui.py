"""Generate antialiased, tintable mod-menu icons without runtime dependencies."""
import math
import shutil
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
        'profile_folder': combine(lines([(1,4),(6,4),(8,6),(15,6),(15,14),(1,14)],True),lines([(1,4),(1,2),(6,2),(8,4),(14,4),(14,6)])),
        'profile_save': combine(box(2,1,12,14),box(5,1,6,5),box(5,10,6,5)),
        'profile_load': combine(lines([(8,1),(8,10),(4,6)]),lines([(8,10),(12,6)]),lines([(2,10),(2,14),(14,14),(14,10)])),
        'profile_update': combine(lines([(13,6),(11,2),(5,2),(2,5),(2,10),(5,14),(11,14),(14,11)]),lines([(9,6),(13,6),(13,2)])),
        'profile_rename': combine(lines([(2,11),(10,3),(13,6),(5,14),(2,14),(2,11)],True),lines([(8,5),(11,8)])),
        'profile_delete': combine(lines([(4,5),(4,14),(12,14),(12,5)]),lines([(2,4),(14,4)]),lines([(6,4),(6,1),(10,1),(10,4)]),lines([(7,7),(7,11)]),lines([(9,7),(9,11)])),
        'profile_back': combine(lines([(10,3),(5,8),(10,13)]),lines([(5,8),(14,8)])),
        'profile_next': combine(lines([(6,3),(11,8),(6,13)]),lines([(2,8),(11,8)])),
        'monitor': combine(box(1.5,2,13,9),lines([(8,11),(8,14)]),lines([(4.5,14),(11.5,14)])),
        'mouse': combine(lines([(4,5),(4,11),(5,14),(11,14),(12,11),(12,5),(11,2),(5,2),(4,5)],True),lines([(4,6),(12,6)]),lines([(8,2),(8,6)])),
        'cube': combine(lines([(8,1),(14,4.5),(14,11.5),(8,15),(2,11.5),(2,4.5)],True),lines([(2,4.5),(8,8),(14,4.5)]),lines([(8,8),(8,15)])),
        'globe': combine(ring(8,8,6.5),lines([(1.5,8),(14.5,8)]),lambda x,y: abs(math.hypot((x-8)*2,y-8)-6.5)/2-.5),
        'grid': combine(*[box(x,y,4,4) for x in (2,10) for y in (2,10)]),
        'close': combine(lines([(3,3),(13,13)]),lines([(13,3),(3,13)])),
        'search': combine(ring(6.5,6.5,4.5),lines([(10,10),(14,14)])),
        'check': lines([(2,8),(6,12),(14,4)]),
        'account': combine(ring(8,4,2.6),lines([(2,14),(2.5,11),(5,9),(11,9),(13.5,11),(14,14)],True)),
        'pin': combine(lines([(8,15),(3,7),(3,4),(5,1.5),(11,1.5),(13,4),(13,7)],True),ring(8,5,1.7)),
        'store': combine(lines([(1,2),(3,2),(5,10),(13,10),(15,4),(4,4)]),ring(6,13.5,1),ring(12,13.5,1)),
        'forums': combine(lines([(1,2),(11,2),(11,9),(6,9),(3,12),(3,9),(1,9)],True),lines([(13,5),(15,5),(15,12),(13,12),(13,15),(10,12),(7,12)])),
    }
    # Preserve the user-supplied gear artwork when regenerating procedural icons.
    shutil.copyfile(Path(__file__).resolve().parents[1] / 'assets/ui-icons/gear.png', masks.DEST / 'gear.png')
    shutil.copyfile(Path(__file__).resolve().parents[1] / 'assets/ui-icons/quick_waypoints.png', masks.DEST / 'quick_waypoints.png')
    shutil.copyfile(Path(__file__).resolve().parents[1] / 'assets/ui-icons/quick_accounts.png', masks.DEST / 'quick_accounts.png')
    shutil.copyfile(Path(__file__).resolve().parents[1] / 'assets/ui-icons/quick_store.png', masks.DEST / 'quick_store.png')
    shutil.copyfile(Path(__file__).resolve().parents[1] / 'assets/ui-icons/quick_forums.png', masks.DEST / 'quick_forums.png')
    shutil.copyfile(Path(__file__).resolve().parents[1] / 'assets/ui-icons/move.png', masks.DEST / 'move.png')
    shutil.copyfile(Path(__file__).resolve().parents[1] / 'assets/ui-icons/backpack.png', masks.DEST / 'backpack.png')
    shutil.copyfile(Path(__file__).resolve().parents[1] / 'assets/ui-icons/sun.png', masks.DEST / 'sun.png')
    shutil.copyfile(Path(__file__).resolve().parents[1] / 'assets/ui-icons/pickaxe.png', masks.DEST / 'pickaxe.png')
    shutil.copyfile(Path(__file__).resolve().parents[1] / 'assets/ui-icons/cps.png', masks.DEST / 'cps.png')
    shutil.copyfile(Path(__file__).resolve().parents[1] / 'assets/ui-icons/shirt.png', masks.DEST / 'shirt.png')
    shutil.copyfile(Path(__file__).resolve().parents[1] / 'assets/ui-icons/contraband.png', masks.DEST / 'contraband.png')
    shutil.copyfile(Path(__file__).resolve().parents[1] / 'assets/ui-icons/chart.png', masks.DEST / 'chart.png')
    shutil.copyfile(Path(__file__).resolve().parents[1] / 'assets/ui-icons/world_border.png', masks.DEST / 'world_border.png')
    shutil.copyfile(Path(__file__).resolve().parents[1] / 'assets/ui-icons/title_overlay.png', masks.DEST / 'title_overlay.png')
    shutil.copyfile(Path(__file__).resolve().parents[1] / 'assets/ui-icons/scale.png', masks.DEST / 'scale.png')
    shutil.copyfile(Path(__file__).resolve().parents[1] / 'assets/ui-icons/crosshair.png', masks.DEST / 'crosshair.png')
    shutil.copyfile(Path(__file__).resolve().parents[1] / 'assets/ui-icons/chunk_borders.png', masks.DEST / 'chunk_borders.png')
    shutil.copyfile(Path(__file__).resolve().parents[1] / 'assets/ui-icons/compass.png', masks.DEST / 'compass.png')
    shutil.copyfile(Path(__file__).resolve().parents[1] / 'assets/ui-icons/waypoints.png', masks.DEST / 'waypoints.png')
    shutil.copyfile(Path(__file__).resolve().parents[1] / 'assets/ui-icons/potion.png', masks.DEST / 'potion.png')
    shutil.copyfile(Path(__file__).resolve().parents[1] / 'assets/ui-icons/map.png', masks.DEST / 'map.png')
    shutil.copyfile(Path(__file__).resolve().parents[1] / 'assets/ui-icons/keystrokes.png', masks.DEST / 'keystrokes.png')
    shutil.copyfile(Path(__file__).resolve().parents[1] / 'assets/ui-icons/cooldowns.png', masks.DEST / 'cooldowns.png')
    shutil.copyfile(Path(__file__).resolve().parents[1] / 'assets/ui-icons/gangs.png', masks.DEST / 'gangs.png')
    shutil.copyfile(Path(__file__).resolve().parents[1] / 'assets/ui-icons/enchantments.png', masks.DEST / 'enchantments.png')
    shutil.copyfile(Path(__file__).resolve().parents[1] / 'assets/ui-icons/eye.png', masks.DEST / 'eye.png')
    for name, distance in icons.items():
        masks.png(name,16,distance)


if __name__ == '__main__':
    main()
