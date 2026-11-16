package com.axial.cosmetics.client;

import com.zigythebird.playeranimcore.animation.Animation;
import io.github.kosmx.emotes.main.EmoteHolder;
import io.github.kosmx.emotes.main.mixinFunctions.IPlayerEntity;
import io.github.kosmx.emotes.common.network.*;
import io.netty.buffer.Unpooled;
import java.util.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.PlayerLikeEntity;

/** Local actor direction; cues and full animation packets are embedded into recorded replays. */
public final class NpcEmoteDirector {
    record Actor(UUID id,String name) {}
    private record Assignment(String clip,boolean loop) {}
    private record Applied(Object entity,NpcEmoteTimeline.Cue cue,io.github.kosmx.emotes.main.emotePlay.EmotePlayer controller,boolean muted) {}
    private static final Map<UUID,Assignment> assignments=new LinkedHashMap<>();
    private static final Map<String,Animation> liveAnimations=new HashMap<>(),replayAnimations=new HashMap<>();
    private static final Map<UUID,Applied> applied=new HashMap<>();
    private static final Map<Object,Float> clocks=Collections.synchronizedMap(new WeakHashMap<>());
    private static NpcEmoteTimeline live=new NpcEmoteTimeline(),recorded=new NpcEmoteTimeline(),replay=new NpcEmoteTimeline();
    private static long origin=System.nanoTime();
    private static Object liveWorld,recorder,playbackHandler;
    private static NpcReplayArchive.Writer writer;
    private static NpcReplayBridge.Recording recording;
    private static NpcReplayBridge.Playback playback;
    private static String problem="";
    private NpcEmoteDirector() {}
    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(client->{sync();if(client.world!=null)for(var player:client.world.getPlayers())apply(player);});
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->resetLive());
    }
    private static long now(){return Math.max(0,(System.nanoTime()-origin)/1_000_000);}
    private static void resetLive() {
        clearApplied();assignments.clear();liveAnimations.clear();live=new NpcEmoteTimeline();origin=System.nanoTime();liveWorld=null;
        recording=null;recorder=null;writer=null;
    }
    private static void clearApplied() {
        for(var value:applied.values())release(value);
        applied.clear();clocks.clear();
    }
    private static void release(Applied value) {
        clocks.remove(value.controller());
        if(value.entity() instanceof IPlayerEntity emote)emote.stopEmote();
        value.controller().muteNbs=value.muted();
    }
    private static void sync() {
        try {
            playback=NpcReplayBridge.playback();
            if(playback!=null) {
                if(playback.handler()!=playbackHandler) {
                    clearApplied();playbackHandler=playback.handler();replayAnimations.clear();replay=new NpcEmoteTimeline();problem="";
                    var data=NpcReplayArchive.read(playback.file());if(data.length>0)replay=NpcEmoteTimeline.decode(data);
                    for(var clip:replay.clips().values())replayAnimations.put(clip.id(),unpack(clip));
                }
                return;
            }
            if(playbackHandler!=null){clearApplied();playbackHandler=null;replay=new NpcEmoteTimeline();replayAnimations.clear();}
            var client=MinecraftClient.getInstance();
            if(liveWorld!=null&&client.world!=liveWorld)resetLive();liveWorld=client.world;
            recording=NpcReplayBridge.recording();
            if(recording==null){recorder=null;return;}
            if(recording.listener()!=recorder) {
                recorder=recording.listener();writer=new NpcReplayArchive.Writer(recording.executor(),recording.file());recorded=new NpcEmoteTimeline();problem="";
                var seed=new ArrayList<NpcEmoteTimeline.Cue>();long time=now();
                for(var id:assignments.keySet()){
                    var pose=live.pose(id,time);if(pose==null)continue;recorded.addClip(pose.clip());
                    seed.add(new NpcEmoteTimeline.Cue(recording.time(),id,pose.clip().id(),pose.cue().loop(),pose.ticks()));
                }
                recorded.addAll(seed);if(!seed.isEmpty())writer.submit(recorded.encode());
            }
            if(writer!=null&&writer.failure()!=null)fail("Replay could not save NPC emotes",writer.failure());
        }catch(Exception ex){
            // Never play a partially decoded replay's actor track.
            if(playback!=null){clearApplied();replay=new NpcEmoteTimeline();replayAnimations.clear();}
            fail("NPC replay support unavailable",ex);
        }
    }
    private static void fail(String message,Throwable failure) {
        if(!problem.equals(message))org.slf4j.LoggerFactory.getLogger(NpcEmoteDirector.class).warn(message,failure);
        problem=message;
    }
    static List<Actor> actors() {
        var client=MinecraftClient.getInstance();if(client.world==null)return List.of();
        return client.world.getPlayers().stream().filter(p->p!=client.player)
                .sorted(Comparator.comparingDouble(p->client.player==null?0:p.squaredDistanceTo(client.player)))
                .map(p->new Actor(p.getUuid(),p.getName().getString())).toList();
    }
    static List<EmoteHolder> emotes(String query) {
        var result=new ArrayList<EmoteHolder>();
        for(var holder:EmotePermissionState.filter(EmoteHolder.list))if(holder.name.getString().toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT)))result.add(holder);
        result.sort(Comparator.comparing(e->e.name.getString(),String.CASE_INSENSITIVE_ORDER));return result;
    }
    static boolean assigned(UUID id){return assignments.containsKey(id);}
    static int assignedCount(){return assignments.size();}
    static boolean editable(){sync();return MinecraftClient.getInstance().world!=null&&playback==null;}
    static String status() {
        sync();if(!problem.isEmpty())return problem;
        if(playback!=null)return "Replay playback • NPC animation track loaded";
        return recording!=null?"Recording • NPC emotes are embedded in this replay":"Live preview • start Replay Mod recording before filming";
    }
    static void assign(UUID actor,EmoteHolder emote,boolean loop) {
        if(!editable())throw new IllegalStateException("Join your filming world before assigning NPCs");
        var client=MinecraftClient.getInstance();var player=client.world.getPlayerByUuid(actor);
        if(!(player instanceof AbstractClientPlayerEntity)||player==client.player)throw new IllegalArgumentException("Select a nearby player-type NPC");
        if(!emotes("").contains(emote))throw new IllegalArgumentException("This emote is not available on your account");
        if(!assignments.containsKey(actor)&&assignments.size()>=NpcEmoteTimeline.MAX_ACTORS)throw new IllegalStateException("Maximum 32 actors");
        var clip=pack(emote);live.addClip(clip);liveAnimations.put(clip.id(),emote.emote);
        var cue=new NpcEmoteTimeline.Cue(now(),actor,clip.id(),loop,0);live.addAll(List.of(cue));
        assignments.put(actor,new Assignment(clip.id(),loop));capture(List.of(cue));apply((AbstractClientPlayerEntity)player);
    }
    static void clear(UUID actor) {
        if(!editable()||actor==null)return;
        var cue=new NpcEmoteTimeline.Cue(now(),actor,"",false,0);live.addAll(List.of(cue));assignments.remove(actor);capture(List.of(cue));
        var player=MinecraftClient.getInstance().world.getPlayerByUuid(actor);if(player instanceof AbstractClientPlayerEntity clientPlayer)apply(clientPlayer);
    }
    static void startAll() {
        if(!editable())return;long time=now();var cues=new ArrayList<NpcEmoteTimeline.Cue>();
        assignments.forEach((id,a)->cues.add(new NpcEmoteTimeline.Cue(time,id,a.clip(),a.loop(),0)));live.addAll(cues);capture(cues);
    }
    static void stopAll() {
        if(!editable())return;long time=now();var cues=assignments.keySet().stream().map(id->new NpcEmoteTimeline.Cue(time,id,"",false,0)).toList();
        live.addAll(cues);capture(cues);for(var player:MinecraftClient.getInstance().world.getPlayers())apply(player);
    }
    private static void capture(List<NpcEmoteTimeline.Cue> cues) {
        if(recording==null||writer==null||cues.isEmpty())return;
        try {
            var events=new ArrayList<NpcEmoteTimeline.Cue>();
            for(var cue:cues){if(!cue.clip().isEmpty())recorded.addClip(live.clips().get(cue.clip()));events.add(new NpcEmoteTimeline.Cue(recording.time(),cue.actor(),cue.clip(),cue.loop(),cue.offsetTicks()));}
            recorded.addAll(events);writer.submit(recorded.encode());
        }catch(Exception ex){fail("Replay could not save NPC emotes",ex);throw new IllegalStateException("NPC preview works, but its replay track could not be saved",ex);}
    }
    public static void beforeRender(PlayerLikeEntity entity) {
        if(!(entity instanceof AbstractClientPlayerEntity player))return;
        sync();apply(player);
    }
    private static void apply(AbstractClientPlayerEntity player) {
        var timeline=playback!=null?replay:live;long time=playback!=null?playback.time():now();var pose=timeline.pose(player.getUuid(),time);
        var previous=applied.get(player.getUuid());
        if(pose==null){if(previous!=null){release(previous);applied.remove(player.getUuid());}return;}
        if(!(player instanceof IPlayerEntity emote))return;
        var animation=(playback!=null?replayAnimations:liveAnimations).get(pose.clip().id());if(animation==null)return;
        var controller=emote.emotecraft$getEmote();
        if(previous==null||previous.entity()!=player||!previous.cue().equals(pose.cue())||!emote.isPlayingEmote()) {
            if(previous!=null)release(previous);
            emote.emotecraft$playEmote(animation,Animation.LoopType.LOOP,pose.ticks(),false);
            boolean muted=controller.muteNbs;controller.muteNbs=true;applied.put(player.getUuid(),new Applied(player,pose.cue(),controller,muted));
        }
        clocks.put(controller,pose.ticks());
    }
    public static Float overrideTicks(Object controller){return clocks.get(controller);}
    private static NpcEmoteTimeline.Clip pack(EmoteHolder holder) {
        var buffer=Unpooled.buffer();
        try {
            new EmotePacket.Builder().configureToStreamEmote(holder.emote).setSizeLimit(NpcEmoteTimeline.MAX_PACKET,true).build().write(buffer,PacketBound.CLIENT);
            if(buffer.readableBytes()>NpcEmoteTimeline.MAX_PACKET)throw new IllegalArgumentException("This emote is too large for a replay actor");
            var bytes=new byte[buffer.readableBytes()];buffer.readBytes(bytes);
            return new NpcEmoteTimeline.Clip(holder.getUuid().toString(),holder.name.getString(),Base64.getEncoder().encodeToString(bytes),holder.emote.length()*20);
        }finally{buffer.release();}
    }
    static Animation unpack(NpcEmoteTimeline.Clip clip) {
        var bytes=Base64.getDecoder().decode(clip.packet());var buffer=Unpooled.wrappedBuffer(bytes);
        try{var packet=new EmotePacket(buffer,PacketBound.CLIENT);var animation=packet.data.emoteData;
            if(!packet.data.valid||packet.data.purpose!=PacketTask.STREAM||animation==null||!animation.uuid().toString().equals(clip.id())||Math.abs(animation.length()*20-clip.durationTicks())>.01f)
                throw new IllegalArgumentException("Invalid embedded NPC emote");
            return animation;
        }finally{buffer.release();}
    }
}
