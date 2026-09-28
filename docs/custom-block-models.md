# Adding custom block models

Pumpkin Bomb uses custom model data `288` in inventories and the note-block state
`instrument=bell,note=15` in the world (both powered values). These are two
independent mappings. This client changes appearance; the server still owns the
block's placement, state, collision and gameplay.

All resource paths below are relative to `src/main/resources`.

## 1. Put geometry in models, textures in textures

The supplied Blockbench export was moved from
`assets/axial_cosmetics/items/pumpkinbomb.json` to
`assets/axial_cosmetics/models/block/custom/pumpkinbomb.json`.
Its elements, UVs, rotations and inventory/hand display settings are preserved.
Like the other custom models, it does not inherit `minecraft:block/block`.
That vanilla parent supplies a `fixed` scale of `0.5`, which shrinks item displays
using the fixed context. Pumpkin Bomb explicitly sets `display.fixed.scale` to
`[1, 1, 1]` so this context uses the full authored size.

Its texture reference `axial_cosmetics:item/pumpkinbomb` resolves to
`assets/axial_cosmetics/textures/item/pumpkinbomb.png` (already present).

For another model, export its geometry into `models/block/custom/<name>.json`,
copy its textures into `textures/`, and update every texture reference inside the
model. Identifiers omit `assets`, `models`/`textures`, and the file extension.

## 2. Create the inventory item definition

`assets/axial_cosmetics/items/pumpkinbomb.json` now contains:

```json
{
  "model": {
    "type": "minecraft:model",
    "model": "axial_cosmetics:block/custom/pumpkinbomb"
  }
}
```

`items/` contains item definitions; `models/` contains the actual geometry.
For another model, create `items/<name>.json` pointing to its geometry.

## 3. Route the server's inventory item to that definition

`ItemModelManagerCustomItemMixin.java` already routes custom model data float `288`
to item definition `axial_cosmetics:pumpkinbomb`, regardless of the base item.
That existing redirect now resolves to a valid item definition.

For another server item using custom model data, add a unique identifier constant
and an exact-value branch alongside the existing ones in that mixin. Use the ID
the server actually sends, and do not reuse an existing ID. Alternatively, a
server can set the `minecraft:item_model` component directly to
`axial_cosmetics:<name>`; that does not require a new custom-model-data redirect.

Both `assets/minecraft/items/note_block.json` and
`assets/minecraft/items/barrier.json` route `288` to the correct `axial_cosmetics`
geometry. The barrier table resumes its previous model at `289` to avoid changing
unrelated higher values. If maintaining this range-dispatch
table for another note-block item, add its threshold in numeric order. Thresholds
cover values up to the next threshold; they are not exact matches like the mixin.

These barrier **item** overrides also apply when a server renders a barrier item
through an item display entity. They do not choose a model for the actual barrier
block at that position. The separate `resolveBarrier` world-block mapping still
uses model `250` globally. A server may use an invisible barrier for collision and
a separate item display for the visible custom model.

When diagnosing size, first distinguish a real world block from an item display.
World blocks use geometry coordinates (16 units per block); item displays also
use their selected `display` context and any server-supplied entity transform.
Changing `display.gui` only changes the inventory view. Do not enlarge geometry
to compensate for a shrinking display transform, since that enlarges actual
note-block rendering too.

## 4. Map the world block state

`WeatherDetectorModelRegistry.java` now declares `PUMPKIN_BOMB_MODEL_ID` pointing to
`axial_cosmetics:block/custom/pumpkinbomb`, and `PUMPKIN_BOMB_STATE` with bell,
note 15 and powered false. In `resolveNoteBlock`, after vanilla defaults:

```java
context.setModel(PUMPKIN_BOMB_STATE, model(PUMPKIN_BOMB_MODEL_ID));
context.setModel(PUMPKIN_BOMB_STATE.with(NoteBlock.POWERED, true), model(PUMPKIN_BOMB_MODEL_ID));
```

This replaces the previous bell-15 `tester` mapping and removes the broken
Pumpkin Bomb bell-14 mapping. Bell 14 now uses the default note-block model.

For another custom block, pick an unused instrument/note combination agreed with
the server, declare its model ID and state, then register both powered variants
here. Two models cannot occupy the same state. Registration already runs from
`AxialCosmetics`; no additional initializer or blockstate JSON is needed.

## 5. Build and check in game

Run `./gradlew compileJava processResources test --tests '*PumpkinBombModelTest'`
(use `gradlew.bat` on Windows). The test checks the inventory reference, loads
geometry through Minecraft's model parser, checks textures, checks the note-block
and barrier item routes, and verifies full fixed scale with unchanged GUI/hand scale.

Restart the development client after Java changes. With commands enabled:

```mcfunction
/give @s minecraft:note_block[minecraft:custom_model_data={floats:[288.0]}] 1
/give @s minecraft:barrier[minecraft:custom_model_data={floats:[288.0]}] 1
/setblock ~2 ~ ~ minecraft:note_block[instrument=bell,note=15,powered=false]
```

Inspect the inventory, held item, and placed model. Test powered appearance in a
controlled setup or on the server too. Vanilla neighbor updates and interaction
can change a note block's instrument/note; the server must maintain the custom
state. Giving an item custom model data alone does not make vanilla placement
select bell 15.

For JSON/texture-only changes, reload resources with F3+T. Check
`run/logs/latest.log` for missing-model or texture errors. Build a distributable
mod with `./gradlew remapJar` when ready to package the client.
