package com.axial.cosmetics.client;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class NpcEmoteTimelineTest {
    final UUID actor=UUID.randomUUID(),other=UUID.randomUUID();
    final NpcEmoteTimeline.Clip clip=new NpcEmoteTimeline.Clip(UUID.randomUUID().toString(),"Dance","AQID",40);
    NpcEmoteTimeline timeline(){var t=new NpcEmoteTimeline();t.addClip(clip);return t;}
    NpcEmoteTimeline.Cue cue(long time,UUID id,boolean loop,float offset){return new NpcEmoteTimeline.Cue(time,id,clip.id(),loop,offset);}
    @Test void replaySeekingPauseAndSpeedUseAbsoluteTime() {
        var t=timeline();t.addAll(List.of(cue(1000,actor,true,0)));
        assertNull(t.pose(actor,999));assertEquals(10,t.pose(actor,1500).ticks());
        assertEquals(10,t.pose(actor,3500).ticks()); // a full loop later
        assertEquals(10,t.pose(actor,3500).ticks()); // paused
        assertEquals(5,t.pose(actor,1250).ticks()); // backwards seek
        assertEquals(30,t.pose(actor,2500).ticks()); // fast forward
    }
    @Test void groupStartStopAndSinglePlayRoundTrip() {
        var t=timeline();t.addAll(List.of(cue(1000,actor,false,0),cue(1000,other,false,0)));
        t.addAll(List.of(new NpcEmoteTimeline.Cue(1600,other,"",false,0)));
        var restored=NpcEmoteTimeline.decode(t.encode());assertEquals(clip,restored.clips().get(clip.id()));
        assertEquals(restored.pose(actor,1500).ticks(),restored.pose(other,1500).ticks());
        assertNull(restored.pose(other,1600));assertNotNull(restored.pose(actor,2999));assertNull(restored.pose(actor,3000));
        assertNotNull(restored.pose(other,1599)); // seek behind stop
    }
    @Test void recordingSeedRetainsPhaseAndLatestSameTimestampWins() {
        var t=timeline();t.addAll(List.of(cue(0,actor,true,33)));
        assertEquals(3,t.pose(actor,500).ticks());
        t.addAll(List.of(new NpcEmoteTimeline.Cue(500,actor,"",false,0),cue(500,actor,true,0)));
        assertEquals(0,NpcEmoteTimeline.decode(t.encode()).pose(actor,500).ticks());
    }
    @Test void invalidAndOversizedInputsFailClosedAndLimitsAreAtomic() {
        var t=timeline();assertThrows(IllegalArgumentException.class,()->t.addAll(List.of(new NpcEmoteTimeline.Cue(0,actor,UUID.randomUUID().toString(),true,0))));
        assertEquals(0,t.cueCount());
        var group=new ArrayList<NpcEmoteTimeline.Cue>();for(int i=0;i<33;i++)group.add(cue(0,UUID.randomUUID(),true,0));
        assertThrows(IllegalStateException.class,()->t.addAll(group));assertEquals(0,t.cueCount());
        assertThrows(IllegalArgumentException.class,()->NpcEmoteTimeline.decode(new byte[NpcEmoteTimeline.MAX_BYTES+1]));
        for(String json:List.of("{}","null","{\"version\":2,\"clips\":[],\"cues\":[]}"))assertThrows(IllegalArgumentException.class,()->NpcEmoteTimeline.decode(json.getBytes()));
        assertThrows(IllegalArgumentException.class,()->new NpcEmoteTimeline.Clip(clip.id(),"bad","!!!",10));
        assertThrows(IllegalArgumentException.class,()->cue(0,actor,true,Float.NaN));
    }
}
