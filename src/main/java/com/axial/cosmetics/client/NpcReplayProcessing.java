package com.axial.cosmetics.client;

import com.google.gson.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipFile;

/** Preserves and retimes the actor track when Replay Mod cuts/splits a recording. */
public final class NpcReplayProcessing {
    record Marker(long time,String name) {}
    record Span(long start,long end) {}
    public record Snapshot(NpcEmoteTimeline timeline,List<List<Span>> outputs) {
        public void finish(List<?> files) {
            finish(files,path->{try{return NpcReplayBridge.open(path);}catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}});
        }
        void finish(List<?> files,java.util.function.Function<Path,Object> open) {
            if(files.size()!=outputs.size())throw new IllegalStateException("NPC replay output count differs from marker plan");
            for(int i=0;i<files.size();i++)try {
                var pair=files.get(i);var api=Class.forName("com.replaymod.lib.org.apache.commons.lang3.tuple.Pair",true,pair.getClass().getClassLoader());
                var path=(Path)api.getMethod("getLeft").invoke(pair);
                var file=open.apply(path);
                try {NpcReplayArchive.write(file,retime(timeline,outputs.get(i)).encode());NpcReplayBridge.fileCall(file,"save");}
                finally {NpcReplayBridge.fileCall(file,"close");}
            }catch(Exception ex){throw new IllegalStateException("Could not preserve NPC animation track",ex);}
        }
    }
    public static Snapshot capture(Path path) {
        try(var zip=new ZipFile(path.toFile())) {
            var entry=zip.getEntry(NpcReplayArchive.ENTRY);if(entry==null)return null;
            byte[] bytes;try(var in=zip.getInputStream(entry)){bytes=in.readNBytes(NpcEmoteTimeline.MAX_BYTES+1);}
            var timeline=NpcEmoteTimeline.decode(bytes);
            var markers=new ArrayList<Marker>();
            var metadata=readJson(zip,"metaData.json").getAsJsonObject();
            var values=readJson(zip,"markers.json");
            if(values!=null)for(var value:values.getAsJsonArray()) {
                var m=value.getAsJsonObject();var name=m.getAsJsonObject("value").get("name");
                if(name!=null&&!name.isJsonNull())markers.add(new Marker(m.get("realTimestamp").getAsLong(),name.getAsString()));
            }
            return new Snapshot(timeline,plan(markers,metadata.get("duration").getAsLong()));
        }catch(Exception ex){throw new IllegalStateException("Could not read NPC animation track before processing",ex);}
    }
    private static JsonElement readJson(ZipFile zip,String name)throws java.io.IOException {
        var entry=zip.getEntry(name);if(entry==null)return null;
        try(var in=zip.getInputStream(entry)) {
            var data=in.readNBytes(NpcEmoteTimeline.MAX_BYTES+1);if(data.length>NpcEmoteTimeline.MAX_BYTES)throw new java.io.IOException("Replay metadata too large");
            return JsonParser.parseString(new String(data,StandardCharsets.UTF_8));
        }
    }
    static List<List<Span>> plan(List<Marker> markers,long duration) {
        var result=new ArrayList<List<Span>>();var spans=new ArrayList<Span>();
        boolean writing=true,hasOutput=true;long start=0;
        for(var m:markers.stream().sorted(Comparator.comparingLong(Marker::time)).toList()) {
            long time=Math.clamp(m.time(),0,duration);
            switch(m.name()) {
                case "_RM_START_CUT" -> {if(writing&&time>start)spans.add(new Span(start,time));writing=false;if(time==0)hasOutput=false;}
                case "_RM_END_CUT" -> {if(!writing)start=time;writing=true;hasOutput=true;}
                case "_RM_SPLIT" -> {
                    if(writing&&time>start)spans.add(new Span(start,time));
                    if(hasOutput)result.add(List.copyOf(spans));spans.clear();hasOutput=writing;start=time;
                }
                default -> { }
            }
        }
        if(writing&&duration>start)spans.add(new Span(start,duration));
        if(hasOutput)result.add(List.copyOf(spans));return result;
    }
    static NpcEmoteTimeline retime(NpcEmoteTimeline source,List<Span> spans) {
        var result=new NpcEmoteTimeline();source.clips().values().forEach(result::addClip);long destination=0;
        for(var span:spans) {
            var cues=new ArrayList<NpcEmoteTimeline.Cue>();
            for(var actor:source.actors()) {
                var pose=source.pose(actor,span.start());
                cues.add(new NpcEmoteTimeline.Cue(destination,actor,pose==null?"":pose.clip().id(),pose!=null&&pose.cue().loop(),pose==null?0:pose.ticks()));
            }
            for(var cue:source.cues())if(cue.time()>span.start()&&cue.time()<span.end())
                cues.add(new NpcEmoteTimeline.Cue(destination+cue.time()-span.start(),cue.actor(),cue.clip(),cue.loop(),cue.offsetTicks()));
            result.addAll(cues);destination+=span.end()-span.start();
        }
        return result;
    }
}
