# Replay Mod integration

The client bundles `replaymod-1.21.11-2.6.28.jar` unchanged. Its included Fabric
libraries remain nested. The launcher installs Replay Mod alongside the client;
existing replay files and native Replay Mod settings are preserved.

## Viewer appearance

The 24 × 24 filmstrip beside the main-menu music slider opens the replay viewer.
The Replay Mod card's gear action opens the same viewer. Full-screen Replay Mod
menus use Axial's `sub_menu_background.png`; text buttons use the same
`packbutton2.png` skin, clean font and hover shading as the client's other menus.
Disabled buttons retain a muted appearance. Icon/texture controls keep their symbols.
Replay Mod uses its own GUI library, so dedicated mixins theme its screen adapter
and buttons rather than relying on the vanilla PressableWidget skin.

## Recording switch

**Features → World → Replay Mod** now has an on/off toggle. This controls
**recording**, not loading the mod or replay playback. No restart is needed to
switch recording off. The preference is saved atomically in
`config/axial-replay-recording.json` and defaults to on for existing installs.

Switching off blocks recorder initialization and packet capture, removes only
Replay Mod's raw/decoded handlers from the active Netty pipeline, and calls its
normal finalization routine to flush/save the current recording. The Minecraft
connection stays open. Finalization is guarded against a second call when the
connection later disconnects. Any queued packet buffers skipped by the gate are
released. Replay Mod unregisters its recording event handler and HUD as part of
normal finalization. Start/Stop and Pause/Resume controls are not added to the
pause screen while the feature is off. Controls cached by an older pause screen
are also hidden and prevented from restarting a finalized recorder. Native recording options on the world/server
screens remain available but cannot override the global recording gate.

Saved replays and the viewer remain available with recording off, including NPC
animation playback. Re-enabling immediately restores a **Start Recording** button
in the pause menu if no live recorder exists. It offers a reconnect for multiplayer,
or saving and reopening the world in singleplayer, because a valid fresh replay
needs its login/world-entry packets. Nothing disconnects when merely enabling the
feature or dismissing the prompt. The explicit start enables recording for that
one connection without overwriting the native auto-record settings. Existing live
recorders keep their native Start/Stop/Pause controls without a duplicate button.

## Checks

Focused tests cover preference persistence, malformed-config handling, detaching
recorders without closing the game connection, packet-buffer release, duplicate
finalization, the pinned native GUI/recording APIs, production packaging, feature
card behavior, replay archive preservation and NPC animation timing.

The development client previously exits before the title screen at existing
Bendable Cuboids and Emotecraft injection failures in the named development
environment. A live visual and recording/playback check in the packaged client is
still required; the automated checks do not replace that check.
