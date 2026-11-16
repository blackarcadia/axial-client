# VPS emotes and wardrobe wheel placement

The 19 current emotes are managed in the existing admin panel:
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
| Oddball | `emote_oddball` |
| Say So | `emote_say_so` |
| Dylan Dance | `emote_dylan_dance` |
| Rasputin | `emote_rasputin` |
| Sad Cat | `emote_sad_cat` |
| The Phitty | `emote_the_phitty` |
| Slick Back | `emote_slick_back` |
| Default Dance | `emote_default_dance` |
| Odd Loop | `emote_odd_loop` |
| Take The L | `emote_take_the_l` |
| Caramel | `emote_caramel` |
| Twerk | `emote_twerk` |
| Breakdance | `emote_breakdance` |

The owner account is granted all 19 emotes in the current catalog. Other players need
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

The additional 13 emotes retain their original binary data and embedded icons.
Catalog display names are applied to native Emotecraft holders by animation UUID,
so the wheel tooltip and NPC picker use the same names as the wardrobe. The extra
Lethal Company attachment was not imported because it was not in the requested list.
