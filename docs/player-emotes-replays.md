# Player emotes in replays

With Replay Mod recording enabled, Axial captures the actual emote animation state
of the local player and other loaded client players. This covers both native
Emotecraft networking and Axial's separate WebSocket relay; the relay's packets
are not part of Replay Mod's Minecraft packet stream.

Player emotes share the existing self-contained animation track with directed
NPCs (`axial/npc-emotes-v1.json`, retained for compatibility). The recorder embeds
the full animations and start/stop/phase/pause cues on Replay Mod's save executor.
An emote already playing when capture starts keeps its phase. Stable loops do not
emit redundant per-tick cues. Actors leaving the loaded world receive stop cues,
and dimension changes preserve earlier recorded performances. Explicit NPC
assignments keep their director-owned timing.

The processed replay retains and retimes the track through cut/split operations.
Playback and video export evaluate poses using replay time, including seeking and
pause. The live WebSocket relay is suspended while viewing a replay so it cannot
overwrite historical animation state. The track records animation visuals; note-block emote audio stays muted,
as with NPC director playback. Limits remain shared: 32 performers, 64 animation
clips, 4,096 cues and a 16 MiB embedded track per recording.

Use an updated AxialClient to record and play back these tracks. No VPS deployment
is needed. Make a new short recording with a local and nearby player's emote, save
it normally, then test seeking before exporting a trailer.

Older recordings that contain neither an embedded player-emote track nor native
emote packets cannot reconstruct the missing player performances retroactively.
Their ordinary gameplay and any existing NPC track remain usable.
