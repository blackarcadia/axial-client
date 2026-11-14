# Pixel Stars

Cosmetic ID: `pixel_stars`. Category: Auras. Display name: Pixel Stars.
White (#FAFDFF), light blue (#A8DFFF), and light pink (#FFB8DF) pixel stars.

Movement emits stars around the player's body. Birth positions are kept in world
space, so stars drift radially outward and upward and form a real fading trail.
Stopping creates no new stars; existing stars finish fading within about 1.3 s.
Teleporting more than 3 blocks per tick, tracking gaps, world changes, and account
changes reset old history. At most 64 stars are stored per player; up to 24 nearby
players within 64 blocks are tracked, with a lighter mesh outside 24 blocks.
No game entities, server-side particles, or gameplay effects are created.

The wardrobe uses a separate deterministic movement demonstration. Live histories
are not modified by preview rendering. `tools/GeneratePixelStarsPreview.java`
creates the animated wardrobe atlas and VPS still/GIF from the same star geometry.
The screenshot reference is not bundled. The live feature renderer follows the
same invisibility/spectator/skin-conflict policy as the existing auras.

Grant in the VPS admin wardrobe editor or generate a private unlock code on its
Unlock codes page. No automatic owner grant or PayNow product mapping is added.
Players need the updated client to see and equip it.

PixelStarsAuraTest checks motion-only emission, idle fade, persistent world-space
births, teleport/tracking resets, bounded history, preview isolation, palette,
finite geometry, preview loop, distance detail and bundled artwork.
