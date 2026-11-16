package com.axial.cosmetics.client;

import com.google.gson.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Self-contained actor cues, evaluated from replay time rather than wall-clock playback. */
final class NpcEmoteTimeline {
    static final int MAX_BYTES=16*1024*1024,MAX_PACKET=1024*1024,MAX_EVENTS=4096,MAX_ACTORS=32,MAX_CLIPS=64;
    static final long MAX_TIME=7L*24*60*60*1000;
    record Clip(String id,String name,String packet,float durationTicks) {
        Clip {
            UUID.fromString(id);
            if(name==null||name.length()>160||packet==null||packet.length()>((MAX_PACKET+2)/3)*4
                    ||!Float.isFinite(durationTicks)||durationTicks<=0||durationTicks>72000)throw new IllegalArgumentException("Invalid actor emote");
            if(Base64.getDecoder().decode(packet).length>MAX_PACKET)throw new IllegalArgumentException("Emote is too large");
        }
    }
    record Cue(long time,UUID actor,String clip,boolean loop,float offsetTicks) {
        Cue {
            Objects.requireNonNull(actor);Objects.requireNonNull(clip);
            if(time<0||time>MAX_TIME||!Float.isFinite(offsetTicks)||offsetTicks<0||offsetTicks>72000)throw new IllegalArgumentException("Invalid actor cue");
        }
    }
    record Pose(Cue cue,Clip clip,float ticks) {}
    private final Map<String,Clip> clips=new LinkedHashMap<>();
    private final List<Cue> cues=new ArrayList<>();
    private final Map<UUID,NavigableMap<Long,Cue>> tracks=new LinkedHashMap<>();
    Map<String,Clip> clips(){return Collections.unmodifiableMap(clips);}
    Set<UUID> actors(){return Collections.unmodifiableSet(tracks.keySet());}
    List<Cue> cues(){return Collections.unmodifiableList(cues);}
    int cueCount(){return cues.size();}
    void addClip(Clip clip) {
        if(!clips.containsKey(clip.id())&&clips.size()>=MAX_CLIPS)throw new IllegalStateException("Too many emotes in this filming session");
        clips.putIfAbsent(clip.id(),clip);
    }
    void addAll(List<Cue> additions) {
        var actors=new HashSet<>(tracks.keySet());
        for(var cue:additions){actors.add(cue.actor());if(!cue.clip().isEmpty()&&!clips.containsKey(cue.clip()))throw new IllegalArgumentException("Unknown actor emote");}
        if(cues.size()+additions.size()>MAX_EVENTS||actors.size()>MAX_ACTORS)throw new IllegalStateException("Actor timeline limit reached");
        for(var cue:additions){cues.add(cue);tracks.computeIfAbsent(cue.actor(),id->new TreeMap<>()).put(cue.time(),cue);}
    }
    Pose pose(UUID actor,long time) {
        var track=tracks.get(actor);if(track==null)return null;var entry=track.floorEntry(time);if(entry==null)return null;
        var cue=entry.getValue();if(cue.clip().isEmpty())return null;var clip=clips.get(cue.clip());
        double ticks=(time-cue.time())/50d+cue.offsetTicks();
        if(!cue.loop()&&ticks>=clip.durationTicks())return null;
        return new Pose(cue,clip,(float)(cue.loop()?ticks%clip.durationTicks():ticks));
    }
    byte[] encode() {
        var root=new JsonObject();root.addProperty("version",1);var definitions=new JsonArray();
        for(var clip:clips.values()){var c=new JsonObject();c.addProperty("id",clip.id());c.addProperty("name",clip.name());c.addProperty("packet",clip.packet());c.addProperty("durationTicks",clip.durationTicks());definitions.add(c);}
        root.add("clips",definitions);var events=new JsonArray();
        for(var cue:cues){var c=new JsonObject();c.addProperty("time",cue.time());c.addProperty("actor",cue.actor().toString());c.addProperty("clip",cue.clip());c.addProperty("loop",cue.loop());c.addProperty("offsetTicks",cue.offsetTicks());events.add(c);}
        root.add("cues",events);var data=root.toString().getBytes(StandardCharsets.UTF_8);
        if(data.length>MAX_BYTES)throw new IllegalStateException("Actor timeline exceeds replay size limit");return data;
    }
    static NpcEmoteTimeline decode(byte[] data) {
        if(data.length>MAX_BYTES)throw new IllegalArgumentException("Actor timeline is too large");
        try {
            var root=JsonParser.parseString(new String(data,StandardCharsets.UTF_8)).getAsJsonObject();
            if(root.get("version").getAsInt()!=1)throw new IllegalArgumentException("Unsupported actor timeline version");
            var definitions=root.getAsJsonArray("clips");var events=root.getAsJsonArray("cues");
            if(definitions.size()>MAX_CLIPS||events.size()>MAX_EVENTS)throw new IllegalArgumentException("Actor timeline limit exceeded");
            var result=new NpcEmoteTimeline();
            for(var v:definitions){var c=v.getAsJsonObject();var clip=new Clip(c.get("id").getAsString(),c.get("name").getAsString(),c.get("packet").getAsString(),c.get("durationTicks").getAsFloat());if(result.clips.containsKey(clip.id()))throw new IllegalArgumentException("Duplicate emote");result.addClip(clip);}
            var cues=new ArrayList<Cue>();
            for(var v:events){var c=v.getAsJsonObject();cues.add(new Cue(c.get("time").getAsLong(),UUID.fromString(c.get("actor").getAsString()),c.get("clip").getAsString(),c.get("loop").getAsBoolean(),c.get("offsetTicks").getAsFloat()));}
            result.addAll(cues);return result;
        }catch(RuntimeException ex){throw new IllegalArgumentException("Invalid NPC emote timeline",ex);}
    }
}
