# Hexwind Halloween aura

ID: `halloween_hexwind`. Category: Auras. There are no automatic owner or staff grants.

## Assign

Open `https://api.axialclient.com/admin`, load a Minecraft UUID, check **Hexwind** (Auras), and save cosmetic access. It can be granted alongside any cape or on its own. Updated clients refresh grants within a minute. Aura-only owners can open the wardrobe. Unchecking Hexwind revokes it without removing the player's capes or changing their Mojang skin/cape settings.

An owned aura is equipped by default. The wardrobe's Auras tab and All cosmetics tab show an animated Hexwind tile. Click it to toggle the aura independently of the cape slot. Preferences use the existing wardrobe file's `<uuid>.aura` key; the existing `<uuid>` cape key and legacy migrations are preserved.

The authenticated WSS relay synchronizes aura equipment to other updated, visible players. The recipient still checks the UUID's cosmetic grants before rendering. Aura capability negotiation keeps old emote clients from receiving message types they do not understand. Aura equipment syncing works even when a Minecraft server provides native emote syncing. During a relay outage viewers use the last known equipment state, or a granted default if no state has arrived.

## Design and rendering

This is an original procedural cosmetic inspired by the broad orbiting ribbons in the supplied Wind Aura reference. No Lunar model, texture or animation files were copied. Three smoky ribbon arcs rotate at different heights, edged with violet and orange; three small angular bats flap around the player, a jack-o'-lantern orbits below the waist, and small ember squares drift nearby. A six-second loop is evaluated continuously from Minecraft's interpolated render age.

`HexwindGeometry.java` is the editable source for the mesh, colours, orbit radii and animation. `AuraFeatureRenderer.java` submits the mesh through Minecraft 1.21.11's entity render command queue, with normal depth testing, no collision or spawned entities, reduced detail past 24 blocks and a 64-block distance cutoff. Invisible/spectator players and Citizens-style NPC UUIDs are excluded. Local first-person hands are not surrounded with an obstructing aura; the local player's aura is visible in third person and the wardrobe's native player preview. Other players' auras are visible normally.

`tools/GenerateHexwindPreview.java` generates the client preview atlas and admin GIF from the same aura mesh, using an original neutral mannequin for scale. These are illustrative mesh previews; the in-game wardrobe uses the actual player renderer. To regenerate:

```
./gradlew compileJava
java -Djava.awt.headless=true --source 21 -cp build/classes/java/main tools/GenerateHexwindPreview.java .
```

The GIF is flattened onto a dark background because GIF cannot preserve the smoke's partial alpha. The client's PNG atlas retains alpha. The in-game geometry is animated continuously; it does not use the GIF or the thumbnail frame rate.

## Validation

25 client tests cover grants/revocation, aura-only wardrobe access, separate selections, category/layout behavior, original cape behavior, mesh bounds, seamless looping, preview assets and emote packet compatibility. Seven relay tests include authenticated aura equip/unequip sync, late-viewer snapshots and old-client compatibility. A live admin test granted combined cape+aura access, aura-only access, and then revoked it for a temporary UUID; all preexisting grants were preserved and the fixture was removed.

The admin catalog and relay are deployed. New rendering requires the updated client build. A live in-game visual check is still needed; the IDE development client has existing animation dependency startup errors documented in the preceding skin debugging work. This change does not claim that those unrelated launch errors were fixed.

Adding this aura to the PayNow storefront is separate: create/tag a product and map its product ID to `halloween_hexwind` before selling it. This change creates no paid product and changes no Prisons storefront settings.
