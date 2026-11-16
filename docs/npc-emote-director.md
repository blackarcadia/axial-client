# NPC emote filming

Open **Features → World → NPC Emotes** in a filming world. This tool uses the
client's existing charcoal and purple button styling. Create player-type Citizens
NPCs on your server first, for example `/npc create Actor1 --type PLAYER` and
`/npc skin YourMinecraftName`.

1. Start Replay Mod recording before the performance. The director status should
   say **Recording**.
2. Look at your NPC before opening the tool to preselect it, or choose its name
   from the nearby actors list. Refresh actors after spawning more NPCs.
3. Search for an available emote, select it, choose Loop on/off, then **Assign & play**.
4. Assign more NPCs and use **Start all together** for a synchronised take.
   **Stop all** ends the performance while preserving assignments; **Clear actor**
   removes an individual assignment.
5. Save the replay normally. Open it and export your video using this updated
   AxialClient installation.

The actor list contains nearby player entities. Citizens NPCs are indistinguishable
from real players to this client tool, so select your actors carefully. These are
local visual assignments, visible on the filming client; no server plugin or VPS
change is required. Only emotes available to your account appear in the picker.

Each recording embeds its animation definitions and timestamped cues in
`axial/npc-emotes-v1.json` inside the `.mcpr`. Playback evaluates poses using replay
time, including seeking backwards, pause, speed changes and offline rendering.
Assignments already playing when recording begins retain their current phase.
NPC positions and skins still come from Replay Mod's normal entity recording.
The animation track is carried into processed replays when recording stops, with
cues retimed for Replay Mod cut and split markers. Emotes already playing at a
trimmed start are seeded with their current phase.

**The updated AxialClient is required to view these local NPC animations in a
replay.** Plain Replay Mod does not read this custom track. Existing replays without
the track are unaffected. Actor emote note-block audio is muted. Assignments reset
when disconnecting or changing worlds.

A session supports 32 actors, 64 distinct animations, 4,096 cues and a 16 MiB track.
If a save fails the director displays an error; do not rely on an unsaved live
preview. Record a short test take and check playback before filming your trailer.
