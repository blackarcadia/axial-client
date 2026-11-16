package com.axial.cosmetics.client;

import java.util.*;

/** Records actual visible playback state, independently of native vs WebSocket transport. */
final class PlayerEmoteRecording {
    private final Set<UUID> tracked=new HashSet<>();
    void clear(){tracked.clear();}
    void forget(UUID actor){tracked.remove(actor);}
    boolean sample(NpcEmoteTimeline track,UUID actor,NpcEmoteTimeline.Clip clip,float ticks,boolean paused,long time) {
        if(clip==null) {
            if(!tracked.remove(actor))return false;
            track.addAll(List.of(new NpcEmoteTimeline.Cue(time,actor,"",false,0)));return true;
        }
        if(!Float.isFinite(ticks)||ticks<0)return false;
        float phase=paused?Math.min(ticks,Math.nextDown(clip.durationTicks())):ticks%clip.durationTicks();
        var previous=track.pose(actor,time);
        boolean changed=!tracked.contains(actor)||previous==null||!previous.clip().id().equals(clip.id())||previous.cue().paused()!=paused;
        if(!changed) {
            float delta=Math.abs(previous.ticks()-phase);
            changed=Math.min(delta,clip.durationTicks()-delta)>1.5f;
        }
        if(!changed)return false;
        track.addClip(clip);track.addAll(List.of(new NpcEmoteTimeline.Cue(time,actor,clip.id(),true,phase,paused)));tracked.add(actor);return true;
    }
    boolean removeMissing(NpcEmoteTimeline track,Set<UUID> present,long time) {
        boolean changed=false;
        for(var actor:Set.copyOf(tracked))if(!present.contains(actor))changed|=sample(track,actor,null,0,false,time);
        return changed;
    }
}
