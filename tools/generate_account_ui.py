"""Generate white, tintable UI masks with antialiased edges; no third-party dependencies."""
from pathlib import Path
import json
import math
import struct
import zlib

DEST = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/axial_cosmetics/textures/gui/sprites/accounts'


def segment(x, y, x1, y1, x2, y2, width=1.3):
    dx, dy = x2 - x1, y2 - y1
    t = max(0, min(1, ((x - x1) * dx + (y - y1) * dy) / (dx * dx + dy * dy)))
    return math.hypot(x - x1 - t * dx, y - y1 - t * dy) - width / 2


def rounded(x, y):
    qx, qy = abs(x - 16) - 10, abs(y - 16) - 10
    return math.hypot(max(qx, 0), max(qy, 0)) + min(max(qx, qy), 0) - 6


def png(name, logical_size, distance):
    size = 128
    pixels = bytearray()
    for y in range(size):
        pixels.append(0)  # PNG scanline filter: none.
        for x in range(size):
            px, py = (x + 0.5) * logical_size / size, (y + 0.5) * logical_size / size
            coverage = max(0, min(1, 0.5 - distance(px, py) / 0.65))
            pixels.extend((255, 255, 255, round(255 * coverage)))

    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data) & 0xffffffff)

    result = b'\x89PNG\r\n\x1a\n'
    result += chunk(b'IHDR', struct.pack('>2I5B', size, size, 8, 6, 0, 0, 0))
    result += chunk(b'IDAT', zlib.compress(bytes(pixels), 9)) + chunk(b'IEND', b'')
    (DEST / (name + '.png')).write_bytes(result)


def main():
    DEST.mkdir(parents=True, exist_ok=True)
    png('rounded', 32, rounded)
    (DEST / 'rounded.png.mcmeta').write_text(json.dumps({'gui': {'scaling': {
        'type': 'nine_slice', 'width': 32, 'height': 32, 'border': 8, 'stretch_inner': True
    }}}, indent=2) + '\n')
    png('circle', 16, lambda x, y: math.hypot(x - 8, y - 8) - 7)
    png('close', 16, lambda x, y: min(segment(x, y, 3, 3, 13, 13), segment(x, y, 13, 3, 3, 13)))
    png('plus', 16, lambda x, y: min(segment(x, y, 8, 2, 8, 14), segment(x, y, 2, 8, 14, 8)))
    png('check', 16, lambda x, y: min(segment(x, y, 2, 8, 6, 12, 1.7), segment(x, y, 6, 12, 14, 4, 1.7)))
    png('more', 16, lambda x, y: min(math.hypot(x - 8, y - cy) - 1.2 for cy in (3, 8, 13)))
    png('previous', 16, lambda x, y: min(segment(x, y, 10, 3, 5, 8), segment(x, y, 5, 8, 10, 13)))
    png('next', 16, lambda x, y: min(segment(x, y, 6, 3, 11, 8), segment(x, y, 11, 8, 6, 13)))
    png('search', 16, lambda x, y: min(abs(math.hypot(x - 6.5, y - 6.5) - 4.2) - 0.65,
                                     segment(x, y, 10, 10, 14, 14, 1.5)))


if __name__ == '__main__':
    main()
