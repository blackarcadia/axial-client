"""Synchronize CMD 246 into the bundled, always-enabled resource pack."""
import json
from pathlib import Path
from tempfile import NamedTemporaryFile
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets"
JAR = ROOT / "src/main/resources/axialutils-1.0-SNAPSHOT.jar"
PREFIX = "resourcepacks/nexo_required/assets/"
MODEL = "minecraft:item/calculus"


def route_calculus(data):
    dispatch = data.get("model", {})
    if dispatch.get("property") != "minecraft:custom_model_data":
        return False
    entries = dispatch["entries"]
    entries[:] = [entry for entry in entries if entry["threshold"] != 246]
    entries.append({"threshold": 246, "model": {"type": "minecraft:model", "model": MODEL}})
    entries.sort(key=lambda entry: entry["threshold"])
    return True


def encode(data):
    return (json.dumps(data, indent=2) + "\n").encode()


def main():
    count = 0
    for path in sorted((ASSETS / "minecraft/items").glob("*.json")):
        original = path.read_bytes()
        data = json.loads(original)
        if route_calculus(data):
            result = encode(data)
            if result != original:
                path.write_bytes(result)
            count += 1

    additions = {
        PREFIX + relative: (ASSETS / relative).read_bytes()
        for relative in ("minecraft/models/item/calculus.json", "minecraft/textures/item/calculus.png")
    }
    packed_count = 0
    with NamedTemporaryFile(dir=JAR.parent, suffix=".tmp", delete=False) as temporary:
        staged = Path(temporary.name)
    try:
        with ZipFile(JAR) as source, ZipFile(staged, "w") as target:
            target.comment = source.comment
            for info in source.infolist():
                payload = source.read(info)
                if info.filename.startswith(PREFIX + "minecraft/items/") and info.filename.endswith(".json"):
                    data = json.loads(payload)
                    if route_calculus(data):
                        payload = encode(data)
                        packed_count += 1
                payload = additions.pop(info.filename, payload)
                target.writestr(info, payload)
            for name, payload in additions.items():
                target.writestr(name, payload)
        staged.replace(JAR)
    finally:
        staged.unlink(missing_ok=True)
    print(f"Synchronized {count} source and {packed_count} required-pack item definitions")


if __name__ == "__main__":
    main()
