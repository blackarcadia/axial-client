# Pixel Snowflakes

Cosmetic ID: `pixel_snowflakes`. Display name: Pixel Snowflakes. Category: Auras.
Pure white six-armed pixel flakes with forked tips drift gently down and around the
player. Each flake has its own seeded height/phase, radius, tilt, yaw, roll and spin.
They tumble in three dimensions instead of all facing the camera. The effect runs
while stationary or moving, unlike the movement-only Pixel Stars trail.

Uses the unlit, emissive, two-sided aura pipeline to keep white flakes readable
from either side. Flakes are fully opaque through most of their lifetime and fade
only near their wrap points. The six-second loop is seamless. At most 28 flakes
are rendered nearby; distant geometry uses half the flake count. Existing aura
visibility, entitlement and equipment-conflict policies remain in effect.

`tools/GeneratePixelSnowflakesPreview.java` generates the wardrobe's 1024 x 1024
animated atlas and the VPS still/GIF directly from the same live geometry.
The VPS entry supports manual grants and private unlock-code generation.
No automatic ownership or PayNow product mapping is added. Updated client required.

PixelSnowflakesAuraTest covers white colour, opaque lifetimes, finite/bounded mesh,
varied 3D orientations, seamless motion, distant detail and bundled preview assets.
