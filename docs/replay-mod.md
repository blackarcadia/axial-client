# Replay Mod integration

The client bundles the supplied `replaymod-1.21.11-2.6.28.jar` unchanged.
Replay Mod's included Fabric libraries remain in its nested JARs. Existing
Fabric API satisfies the declared runtime dependencies; no separate download is
needed. The launcher installs Replay Mod alongside the client and replaces older
managed versions without changing recordings or configuration.

A 24 × 24 filmstrip button sits to the left of the main menu music slider.
It uses the client charcoal surface, a purple hover state, a tooltip and keyboard
activation. Features → World → Replay Mod opens the same replay viewer as a full
card action. It has no artificial enabled/disabled switch; Replay Mod owns its
recording settings, accessible from the viewer's settings button.

`ReplayMenu` calls the supplied viewer's constructor and `toMinecraft()` adapter
through stable Replay Mod API names. Cancel returns to the screen that opened it.
The native Escape behavior remains managed by Replay Mod. The optional
`ReplayMainMenuButtonMixin` suppresses Replay Mod's default main-menu injection
on TitleScreen so it cannot add a duplicate or move Axial's existing menu buttons.

Checks cover bundled dependency presence, pinned viewer API, remapped mixin
signature, packaged JAR bytes, launcher update behavior, retained recordings,
Features card behavior and icon spacing. A real recording/playback and visual
check in the packaged client remain required before release.

Development startup validation: `:runClient` detects Replay Mod 1.21.11-2.6.28,
but exits before the title screen at the existing Bendable Cuboids player-animation
injection and Emotecraft wheel-key injection failures in the named development
environment. No live icon/viewer or recording/playback verification was possible.
