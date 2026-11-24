"""Route pickaxe CMD 115 to Nebulafang in both client assets and the bundled pack."""
import json
from pathlib import Path
from tempfile import NamedTemporaryFile
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets'
JAR = ROOT / 'src/main/resources/axialutils-1.0-SNAPSHOT.jar'
PREFIX = 'resourcepacks/nexo_required/assets/'
PICKAXES = ('wooden', 'stone', 'copper', 'iron', 'golden', 'diamond', 'netherite')


def route(data):
    dispatch = data['model']
    assert dispatch['type'] == 'minecraft:range_dispatch'
    assert dispatch['property'] == 'minecraft:custom_model_data'
    entries = dispatch['entries']
    entries[:] = [entry for entry in entries if entry['threshold'] != 115]
    entries.append({'threshold': 115, 'model': {
        'type': 'minecraft:model', 'model': 'minecraft:item/nebulafang'}})
    entries.sort(key=lambda entry: entry['threshold'])
    return (json.dumps(data, indent=2) + '\n').encode()


def main():
    paths = {f'minecraft/items/{material}_pickaxe.json' for material in PICKAXES}
    for relative in sorted(paths):
        path = ASSETS / relative
        payload = route(json.loads(path.read_bytes()))
        if payload != path.read_bytes():
            path.write_bytes(payload)

    with NamedTemporaryFile(dir=JAR.parent, suffix='.tmp', delete=False) as temporary:
        staged = Path(temporary.name)
    changed = False
    try:
        with ZipFile(JAR) as source, ZipFile(staged, 'w') as target:
            target.comment = source.comment
            for info in source.infolist():
                payload = source.read(info)
                if info.filename in {PREFIX + relative for relative in paths}:
                    updated = route(json.loads(payload))
                    changed |= updated != payload
                    payload = updated
                target.writestr(info, payload)
        if changed:
            staged.replace(JAR)
    finally:
        staged.unlink(missing_ok=True)


if __name__ == '__main__':
    main()
