# Cooldowns

The Cooldowns mod card has a master toggle and a configuration gear. The submenu
uses the standard feature-settings screen, with independent Pet Cooldowns and
Combat Tag switches. Defaults keep the existing pet labels enabled. The master
switch hides both displays while preserving the individual preferences.

The combat HUD shows a solid charcoal circle, diamond sword, COMBAT label and
seconds remaining. The outer purple arc retreats counterclockwise and becomes
red during the last three seconds. HUD Elements / Move can reposition the widget;
the menu and editor show an illustrative ten-second preview. Position and
visibility are stored in `axial-cosmetics-cooldowns.json`.

Combat display requires the updated normal PrisonsCore plugin. Its existing
CombatLogManager starts/refreshes the timer on tags and clears it on expiry,
explicit removal, logout, death (through the existing listener) and shutdown.
Combat rules remain controlled by that manager. Duration comes from its existing
COMBAT_DURATION_TICKS constant; the current duration is 300 ticks (15 seconds).
The server corrects active countdowns every 20 ticks. No client polling or chat
message matching is used. Joining/leaving a server clears the client timer.

`axial_cosmetics:combat` carries a nine-byte version-1 payload: one version byte,
a big-endian int of remaining milliseconds and a big-endian int of total
milliseconds. The client uses monotonic elapsed time between updates. Test
fixtures in `src/test/resources/cooldowns` come from PrisonsCore's encoder.

Ring frames are generated once into a small texture atlas and reused. Hidden
cooldowns perform no HUD drawing, texture updates or inventory scans.
