# VPS emotes and wardrobe wheel placement

The six current emotes are managed in the existing admin panel:
**Cosmetics → Load player UUID → Emotes → Save cosmetic access**. They use the
same grant, redemption-code and audit system as other wardrobe items. No PayNow
products or store listings are created by this change.

Grant IDs:

| Emote | Grant ID |
|---|---|
| Gangnam Style | `emote_gangnam_style` |
| Rat Dance | `emote_rat_dance` |
| Death | `emote_death` |
| Fear | `emote_fear` |
| Keep Your Head Off | `emote_keep_your_head_off` |
| Spooky Dance | `emote_spooky_dance` |

The initial rollout grants all six to the owner account. Other players need
individual grants or redeem codes. Existing non-emote ownership is preserved.
Updated clients refresh VPS grants within a minute; revoked managed emotes cannot
be played through an old saved wheel position. Connected-server emote restrictions
still apply in addition to VPS ownership. Older clients retain their old behavior.

In the client, open **Wardrobe → Emotes**, select a card to preview its animation
on the spaceman, then choose **Assign to wheel**. Choose a wheel page,
then click a slot to save immediately. Occupied slots show their current emote;
clicking replaces that position. **Undo** restores the last replaced or cleared
slot. The screen uses Emotecraft's native 15 pages of 8 positions;
slot 1 is at the bottom, following the native wheel's order. Assigning replaces only
that slot, preserves other positions, and saves to the native Emotecraft config.
**Clear selected** removes the last selected position. Previewing never plays an emote on the live
player, sends animation packets, or requires a world to be open.

Assets and a SHA-256 manifest are hosted under `/assets/emotes/`. The manifest is
`public/assets/emotes/catalog.json`; binary files alongside it are copied from the
current bundled animations. The client checks UUIDs, filenames, checksums and size
limits before caching assets in its normal `emotes` folder. Downloads happen off
the render thread, with the existing HTTPS fallback hostname if needed. Bundled
copies remain as bootstrap/offline assets; ownership comes from VPS grants and its
last successful cache, rather than possession of the downloaded file.

Deploy the catalog additions, `public/assets/emote_*` admin icons and
`public/assets/emotes/` together. Adding future emotes still requires registering
its grant ID/UUID in the client catalog, as with other current wardrobe cosmetics.
Never include private VPS settings or admin passwords in these public files.
