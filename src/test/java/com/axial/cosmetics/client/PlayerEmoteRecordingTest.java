package com.axial.cosmetics.client;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlayerEmoteRecordingTest {
    final UUID local=UUID.randomUUID(),remote=UUID.randomUUID();
    final NpcEmoteTimeline.Clip dance=new NpcEmoteTimeline.Clip(UUID.randomUUID().toString(),"Dance","AQID",60);
    @Test void recordsBothLocalAndRemotePerformersAndStopsWithoutRedundantPerTickWrites() {
        var track=new NpcEmoteTimeline();var recorder=new PlayerEmoteRecording();
        assertTrue(recorder.sample(track,local,dance,0,false,1000));assertTrue(recorder.sample(track,remote,dance,10,false,1000));
        assertFalse(recorder.sample(track,local,dance,20,false,2000));assertFalse(recorder.sample(track,remote,dance,30,false,2000));
        assertEquals(2,track.cueCount());assertEquals(20,track.pose(local,2000).ticks());assertEquals(30,track.pose(remote,2000).ticks());
        assertTrue(recorder.sample(track,local,null,0,false,2500));assertFalse(recorder.sample(track,local,null,0,false,2550));
        var saved=NpcEmoteTimeline.decode(track.encode());assertNull(saved.pose(local,2500));assertNotNull(saved.pose(remote,2500));assertNotNull(saved.pose(local,2499));
    }
    @Test void capturesAnEmoteAlreadyRunningAtRecordingStartAndARepeatOfTheSameEmote() {
        var track=new NpcEmoteTimeline();var recorder=new PlayerEmoteRecording();
        recorder.sample(track,local,dance,23,false,10000);assertEquals(25,track.pose(local,10100).ticks());
        assertFalse(recorder.sample(track,local,dance,3,false,12000)); // native cycle wrapped naturally
        assertTrue(recorder.sample(track,local,dance,0,false,12500)); // deliberate restart
        assertEquals(0,track.pose(local,12500).ticks());assertEquals(10,track.pose(local,13000).ticks());
    }
    @Test void pausedPosesStayStillThroughSeekingAndResumeFromTheirActualPhase() {
        var track=new NpcEmoteTimeline();var recorder=new PlayerEmoteRecording();
        recorder.sample(track,local,dance,0,false,0);recorder.sample(track,local,dance,20,true,1000);
        assertEquals(20,track.pose(local,10000).ticks());assertFalse(recorder.sample(track,local,dance,20,true,10000));
        recorder.sample(track,local,dance,20,false,11000);assertEquals(30,track.pose(local,11500).ticks());
        var saved=NpcEmoteTimeline.decode(track.encode());assertEquals(20,saved.pose(local,5000).ticks());assertEquals(10,saved.pose(local,500).ticks());
        var cut=NpcReplayProcessing.retime(saved,List.of(new NpcReplayProcessing.Span(3000,12000)));
        assertEquals(20,cut.pose(local,0).ticks());assertEquals(20,cut.pose(local,7000).ticks());assertEquals(30,cut.pose(local,8500).ticks());
    }
    @Test void despawnEndsOnlyObservedPlayersAndLeavesDirectedNpcTracksAlone() {
        var track=new NpcEmoteTimeline();var recorder=new PlayerEmoteRecording();var npc=UUID.randomUUID();track.addClip(dance);
        track.addAll(List.of(new NpcEmoteTimeline.Cue(0,npc,dance.id(),true,0)));
        recorder.sample(track,local,dance,0,false,0);recorder.sample(track,remote,dance,0,false,0);
        assertTrue(recorder.removeMissing(track,Set.of(local),1000));assertNull(track.pose(remote,1000));assertNotNull(track.pose(local,1000));assertNotNull(track.pose(npc,1000));
        recorder.forget(local);assertFalse(recorder.removeMissing(track,Set.of(),2000));assertNotNull(track.pose(local,2000));
    }
    @Test void worldChangeEndsOldPerformancesWithoutErasingEarlierRecordedCues() {
        var track=new NpcEmoteTimeline();var recorder=new PlayerEmoteRecording();
        recorder.sample(track,local,dance,0,false,0);recorder.removeMissing(track,Set.of(),1000);recorder.clear();
        recorder.sample(track,local,dance,0,false,2000);
        assertEquals(10,track.pose(local,500).ticks());assertNull(track.pose(local,1500));assertEquals(10,track.pose(local,2500).ticks());
    }
    @Test void oldNpcTracksWithoutPauseFieldRemainReadable() {
        var track=new NpcEmoteTimeline();track.addClip(dance);track.addAll(List.of(new NpcEmoteTimeline.Cue(0,local,dance.id(),true,0)));
        var json=com.google.gson.JsonParser.parseString(new String(track.encode())).getAsJsonObject();for(var cue:json.getAsJsonArray("cues"))cue.getAsJsonObject().remove("paused");
        assertEquals(20,NpcEmoteTimeline.decode(json.toString().getBytes()).pose(local,1000).ticks());
    }
}
